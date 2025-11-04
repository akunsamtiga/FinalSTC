package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.math.MathContext
import kotlin.math.abs

class IndicatorOrderManager(
    private val scope: CoroutineScope,
    private val onIndicatorOrdersUpdate: (List<IndicatorOrder>) -> Unit,
    private val onExecuteIndicatorTrade: (String, String, Long) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val getUserSession: suspend () -> UserSession?,
    private val serverTimeService: ServerTimeService?,
    private val onIndicatorMartingaleResult: (IndicatorMartingaleResult) -> Unit,
    private val priceApi: PriceDataApi,
    private val onIndicatorModeComplete: (String, String) -> Unit
) {
    companion object {
        private const val TAG = "IndicatorOrderManager"
        private const val HISTORICAL_CANDLES_COUNT = 180
        private const val PRICE_MONITOR_INTERVAL = 3000L
        private const val MINUTE_BOUNDARY_OFFSET_MS = 100L

        private val ZERO = BigDecimal.ZERO
        private val ONE = BigDecimal.ONE
        private val TWO = BigDecimal("2")
        private val ONE_HUNDRED = BigDecimal("100")
        private val CONFIDENCE_70 = BigDecimal("0.7")
        private val CONFIDENCE_80 = BigDecimal("0.8")
        private val FRACTION_HALF = BigDecimal("0.5")
        private val MATH_CONTEXT = MathContext(34, RoundingMode.HALF_UP)
        private val CALCULATION_SCALE = 10
        private val DISPLAY_SCALE = 10

        private const val CANDLE_INTERVAL_SECONDS = 60
        private const val CANDLE_INTERVAL_MS = CANDLE_INTERVAL_SECONDS * 1000L
        private const val SOURCE_INTERVAL_SECONDS = 5
        private const val CANDLES_PER_MINUTE = 60 / SOURCE_INTERVAL_SECONDS
    }

    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:00:00", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }
    private val displayTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    private var isIndicatorModeActive = false
    private var isAutoRestartEnabled = true
    private var consecutiveRestarts = 0
    private var maxConsecutiveRestarts = 50

    private var coreSelectedAsset: Asset? = null
    private var coreIsDemoAccount: Boolean = true
    private var coreIndicatorSettings: IndicatorSettings? = null
    private var coreMartingaleSettings: MartingaleState? = null
    private var coreMaxConsecutiveLosses: Int = 5
    private var coreEnableConsecutiveLossLimit: Boolean = true

    private var currentSettings: IndicatorSettings? = null
    private var selectedAsset: Asset? = null
    private var isDemoAccount: Boolean = true
    private var martingaleSettings: MartingaleState? = null

    private var waitingForMinuteBoundary = false
    private var dataCollectionStartTime = 0L
    private var targetMinuteBoundary = 0L

    private val historicalCandles = mutableListOf<Candle>()
    private var analysisResult: IndicatorAnalysisResult? = null
    private var pricePredictions = mutableListOf<PricePrediction>()

    private var minuteBoundaryJob: Job? = null
    private var predictionMonitoringJob: Job? = null
    private var executionDelayJob: Job? = null
    private var currentRealTimePrice: BigDecimal = ZERO
    private var lastPriceUpdateTime = 0L

    private val indicatorOrders = mutableListOf<IndicatorOrder>()
    private val pendingTradeResults = mutableMapOf<String, PendingIndicatorTrade>()

    private val queuedExecutions = mutableListOf<QueuedExecution>()
    private var isWaitingForExecutionBoundary = false
    private var nextExecutionBoundary = 0L

    private var currentMartingaleOrder: IndicatorMartingaleOrder? = null
    private var activeMartingaleOrderId: String? = null
    private var isMartingalePendingExecution = false

    private var totalExecutions = 0
    private var totalWins = 0
    private var totalLosses = 0
    private var consecutiveWins = 0
    private var consecutiveLosses = 0
    private var serverTimeOffset = 0L

    private var maxConsecutiveLossesEnabled = false
    private var maxConsecutiveLossesCount = 5

    private val executionMutex = Mutex()

    private var isTradeExecuted = false
    private var activeTradeOrderId: String? = null
    private var isMonitoringActiveTradeOnly = false
    private var tradeResultReceived = false

    private lateinit var indicatorContinuousMonitor: IndicatorContinuousMonitor

    data class IndicatorAnalysisResult(
        val indicatorType: IndicatorType,
        val calculatedValues: List<BigDecimal>,
        val finalIndicatorValue: BigDecimal,
        val trend: String,
        val strength: String,
        val analysisTime: Long = System.currentTimeMillis()
    )

    data class PricePrediction(
        val id: String = UUID.randomUUID().toString(),
        val targetPrice: BigDecimal,
        val predictionType: String,
        val recommendedTrend: String,
        val confidence: BigDecimal,
        val isTriggered: Boolean = false,
        val triggeredAt: Long = 0L,
        val createdAt: Long = System.currentTimeMillis(),
        val isDisabled: Boolean = false
    ) {
        fun getDisplayInfo(): String {
            val confidencePercent = confidence.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP)
            val status = when {
                isDisabled -> "[DISABLED]"
                isTriggered -> "[EXECUTED]"
                else -> "[ACTIVE]"
            }
            return "$status $predictionType: ${targetPrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString()} ($recommendedTrend) - ${confidencePercent.toPlainString()}%"
        }
    }

    data class QueuedExecution(
        val prediction: PricePrediction,
        val queuedAt: Long = System.currentTimeMillis(),
        val executionPrice: BigDecimal,
        val isMartingale: Boolean = false
    )

    data class PendingIndicatorTrade(
        val indicatorOrderId: String,
        val amount: Long,
        val trend: String,
        val executionTime: Long,
        val triggerPrice: BigDecimal,
        val indicatorType: String,
        val tradeUuid: String? = null
    )

    private data class IndicatorCalculationResult(
        val calculatedValues: List<BigDecimal>,
        val finalValue: BigDecimal,
        val trend: String,
        val strength: String
    )

    private data class PriceMovementAnalysis(
        val averageMovement: BigDecimal,
        val maxMovement: BigDecimal,
        val significantDigits: Int,
        val typicalRange: BigDecimal
    )

    init {
        initializeContinuousMonitor()
    }

    private fun initializeContinuousMonitor() {
        indicatorContinuousMonitor = IndicatorContinuousMonitor(
            scope = scope,
            getUserSession = getUserSession,
            onIndicatorTradeResult = { indicatorOrderId, isWin, details ->
                handleIndicatorTradeResultFromMonitor(indicatorOrderId, isWin, details)
            },
            onIndicatorModeComplete = { reason, message ->
                handleIndicatorCompletionWithRestart(reason, message)
            },
            serverTimeService = serverTimeService
        )

        Log.d(TAG, "IndicatorContinuousMonitor initialized with auto-restart capability")
    }

    private fun getCurrentServerTime(): Long {
        return serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + serverTimeOffset)
    }

    private fun getCurrentUTCTime(): Date {
        return Date(getCurrentServerTime())
    }

    private fun calculateNextMinuteBoundary(currentTimeMs: Long): Long {
        val seconds = (currentTimeMs / 1000) % 60
        val millis = currentTimeMs % 1000
        return currentTimeMs + ((60 - seconds) * 1000) - millis
    }

    fun startIndicatorMode(
        selectedAsset: Asset,
        isDemoAccount: Boolean,
        indicatorSettings: IndicatorSettings,
        martingaleSettings: MartingaleState,
        maxConsecutiveLosses: Int = 5,
        enableConsecutiveLossLimit: Boolean = true
    ): Result<String> {
        if (isIndicatorModeActive) {
            return Result.failure(Exception("Indicator Order mode already active"))
        }

        return try {
            coreSelectedAsset = selectedAsset
            coreIsDemoAccount = isDemoAccount
            coreIndicatorSettings = indicatorSettings
            coreMartingaleSettings = martingaleSettings
            coreMaxConsecutiveLosses = maxConsecutiveLosses
            coreEnableConsecutiveLossLimit = enableConsecutiveLossLimit

            consecutiveRestarts = 0
            isAutoRestartEnabled = true

            return startIndicatorCycle()

        } catch (e: Exception) {
            isIndicatorModeActive = false
            indicatorContinuousMonitor.stopMonitoring()
            Result.failure(Exception("Failed to start Indicator Order: ${e.message}"))
        }
    }

    private fun startIndicatorCycle(): Result<String> {
        return try {
            selectedAsset = coreSelectedAsset
            isDemoAccount = coreIsDemoAccount
            currentSettings = coreIndicatorSettings
            martingaleSettings = coreMartingaleSettings
            maxConsecutiveLossesEnabled = coreEnableConsecutiveLossLimit
            maxConsecutiveLossesCount = coreMaxConsecutiveLosses

            isIndicatorModeActive = true
            resetTradeStateCompletely()
            resetCycleStateCompletely()

            indicatorContinuousMonitor.startMonitoring()

            val currentServerTime = getCurrentServerTime()
            val currentUTC = getCurrentUTCTime()
            val timeString = displayTimeFormat.format(currentUTC)
            val accountType = if (isDemoAccount) "Demo" else "Real"

            val nextMinuteBoundary = calculateNextMinuteBoundary(currentServerTime)
            val waitTime = nextMinuteBoundary - currentServerTime

            targetMinuteBoundary = nextMinuteBoundary
            waitingForMinuteBoundary = true

            val cycleInfo = if (consecutiveRestarts > 0) {
                " (Auto-restart #$consecutiveRestarts)"
            } else {
                " (Initial start)"
            }

            Log.d(TAG, "Starting indicator cycle with ultra-fast monitoring$cycleInfo")
            Log.d(TAG, "Start Time: $timeString")
            Log.d(TAG, "Account: $accountType")
            Log.d(TAG, "Asset: ${selectedAsset?.name}")
            Log.d(TAG, "Indicator: ${currentSettings?.type}")
            Log.d(TAG, "Auto-restart: ${if (isAutoRestartEnabled) "ENABLED" else "DISABLED"}")

            onModeStatusUpdate("Indicator Order cycle starting$cycleInfo - Waiting minute boundary in ${waitTime/1000}s ($accountType)")

            scope.launch {
                executeMinuteBoundaryFlow()
            }

            val restartInfo = if (consecutiveRestarts > 0) " (Auto-restart #$consecutiveRestarts)" else ""
            Result.success("Indicator Order cycle started$restartInfo - ${displayTimeFormat.format(Date(nextMinuteBoundary))} ($accountType)")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting indicator cycle: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun resetTradeStateCompletely() {
        isTradeExecuted = false
        activeTradeOrderId = null
        isMonitoringActiveTradeOnly = false
        tradeResultReceived = false

        currentMartingaleOrder = null
        activeMartingaleOrderId = null
        isMartingalePendingExecution = false

        pendingTradeResults.clear()

        Log.d(TAG, "Trade state COMPLETELY reset for new cycle - ALL states cleared")
    }

    private fun resetCycleStateCompletely() {
        waitingForMinuteBoundary = false
        dataCollectionStartTime = 0L
        targetMinuteBoundary = 0L

        historicalCandles.clear()
        analysisResult = null
        pricePredictions.clear()

        queuedExecutions.clear()
        isWaitingForExecutionBoundary = false
        nextExecutionBoundary = 0L

        currentRealTimePrice = ZERO
        lastPriceUpdateTime = 0L

        minuteBoundaryJob?.cancel()
        minuteBoundaryJob = null
        predictionMonitoringJob?.cancel()
        predictionMonitoringJob = null
        executionDelayJob?.cancel()
        executionDelayJob = null

        if (::indicatorContinuousMonitor.isInitialized) {
            indicatorContinuousMonitor.resetCompletionState()
        }

        Log.d(TAG, "Cycle state COMPLETELY reset - ALL jobs cancelled, ALL state cleared")
    }

    private fun handleIndicatorCompletionWithRestart(reason: String, message: String) {
        Log.d(TAG, "Indicator completion detected - $reason: $message")

        if (!isAutoRestartEnabled) {
            Log.d(TAG, "Auto-restart disabled, stopping mode")
            onIndicatorModeComplete(reason, message)
            return
        }

        if (consecutiveRestarts >= maxConsecutiveRestarts) {
            Log.w(TAG, "Max consecutive restarts reached ($maxConsecutiveRestarts), stopping mode")
            isAutoRestartEnabled = false
            onIndicatorModeComplete("MAX_RESTARTS", "Maximum auto-restart limit reached: $maxConsecutiveRestarts")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Auto-restarting indicator mode after completion")

                if (::indicatorContinuousMonitor.isInitialized) {
                    indicatorContinuousMonitor.stopMonitoring()
                }

                delay(500L)

                if (!isIndicatorModeActive) {
                    Log.w(TAG, "Mode deactivated during restart delay, aborting restart")
                    return@launch
                }

                consecutiveRestarts++

                val restartReason = when (reason) {
                    "INDICATOR_WIN" -> "after WIN"
                    "MARTINGALE_WIN" -> "after MARTINGALE WIN"
                    "MARTINGALE_FAILED" -> "after MARTINGALE FAILURE"
                    "SINGLE_LOSS" -> "after SINGLE LOSS"
                    else -> "after completion"
                }

                Log.d(TAG, "Starting COMPLETE new indicator cycle $restartReason (restart #$consecutiveRestarts)")

                resetTradeStateCompletely()
                resetCycleStateCompletely()

                val result = startIndicatorCycle()

                result.fold(
                    onSuccess = {
                        val accountType = if (coreIsDemoAccount) "Demo" else "Real"
                        onModeStatusUpdate("Auto-restarted $restartReason - COMPLETELY NEW cycle ready ($accountType)")
                        Log.d(TAG, "Auto-restart COMPLETELY successful (restart #$consecutiveRestarts)")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Auto-restart failed: ${exception.message}")
                        isAutoRestartEnabled = false
                        onIndicatorModeComplete("RESTART_FAILED", "Auto-restart failed: ${exception.message}")
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error during auto-restart: ${e.message}", e)
                isAutoRestartEnabled = false
                onIndicatorModeComplete("RESTART_ERROR", "Auto-restart error: ${e.message}")
            }
        }
    }

    private suspend fun executeMinuteBoundaryFlow() {
        try {
            val asset = selectedAsset ?: return
            val settings = currentSettings ?: return
            val accountType = if (isDemoAccount) "Demo" else "Real"

            Log.d(TAG, "PHASE 1: Waiting for minute boundary")
            if (!waitForMinuteBoundary()) {
                onModeStatusUpdate("Failed waiting minute boundary - Mode stopped ($accountType)")
                stopIndicatorMode()
                return
            }

            onModeStatusUpdate("PHASE 2/5: Collecting data ($accountType)")
            if (!collectAndAggregateToMinuteCandles(asset)) {
                onModeStatusUpdate("Failed collecting data - Mode stopped ($accountType)")
                stopIndicatorMode()
                return
            }

            onModeStatusUpdate("PHASE 3/5: Analyzing ${historicalCandles.size} candles ($accountType)")
            val analysis = analyzeCollectedData(settings)
            if (analysis == null) {
                onModeStatusUpdate("Failed analyzing data - Mode stopped ($accountType)")
                stopIndicatorMode()
                return
            }

            onModeStatusUpdate("PHASE 4/5: Creating 2 price predictions ($accountType)")
            val predictions = generateTwoPricePredictions(analysis, settings)
            if (predictions.size != 2) {
                onModeStatusUpdate("Failed creating 2 predictions - Mode stopped ($accountType)")
                stopIndicatorMode()
                return
            }

            pricePredictions.clear()
            pricePredictions.addAll(predictions)

            Log.d(TAG, "2 Predictions generated:")
            predictions.forEach { prediction ->
                Log.d(TAG, "  - ${prediction.getDisplayInfo()}")
            }

            onModeStatusUpdate("PHASE 5/5: Monitoring 2 predictions ($accountType)")
            startPredictionMonitoring()

            onModeStatusUpdate("Indicator Order active - Monitoring 2 targets ($accountType)")

        } catch (e: Exception) {
            val accountType = if (isDemoAccount) "Demo" else "Real"
            Log.e(TAG, "Error in indicator flow ($accountType): ${e.message}", e)
            onModeStatusUpdate("Error in indicator flow ($accountType): ${e.message}")
            stopIndicatorMode()
        }
    }

    private suspend fun waitForMinuteBoundary(): Boolean {
        try {
            val currentTime = getCurrentServerTime()
            val waitTime = targetMinuteBoundary - currentTime

            if (waitTime > 0) {
                Log.d(TAG, "Waiting ${waitTime}ms for minute boundary")
                delay(waitTime)
            }

            delay(MINUTE_BOUNDARY_OFFSET_MS)
            waitingForMinuteBoundary = false
            dataCollectionStartTime = getCurrentServerTime()

            Log.d(TAG, "Minute boundary reached")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for minute boundary: ${e.message}", e)
            return false
        }
    }

    private suspend fun collectAndAggregateToMinuteCandles(asset: Asset): Boolean {
        try {
            historicalCandles.clear()

            val userSession = getUserSession() ?: return false
            val encodedSymbol = asset.ric.replace("/", "%2F")

            val boundaryTime = Date(targetMinuteBoundary)
            val boundaryUTC = Date(boundaryTime.time)

            Log.d(TAG, "Collecting 5-second data for aggregation to $HISTORICAL_CANDLES_COUNT candles")

            val fiveSecondCandles = mutableListOf<Candle>()

            for (hoursBack in 0..5) {
                val targetTime = Date(boundaryUTC.time - (hoursBack * 60 * 60 * 1000))
                val dateForApi = apiDateFormat.format(targetTime)

                try {
                    val response = withTimeoutOrNull(5000L) {
                        priceApi.getLastCandle(
                            symbol = encodedSymbol,
                            date = dateForApi,
                            locale = "id",
                            authToken = userSession.authtoken,
                            deviceId = userSession.deviceId,
                            deviceType = userSession.deviceType,
                            userAgent = userSession.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        )
                    }

                    if (response?.isSuccessful == true && response.body() != null) {
                        val candleResponse = response.body()!!
                        val fetchedCandles = parseCandleResponse(candleResponse)

                        val validCandles = fetchedCandles.filter { candle ->
                            val candleTime = parseISO8601ToMillis(candle.createdAt)
                            val boundaryMs = boundaryUTC.time
                            val extendedRangeMs = boundaryMs - (6 * 60 * 60 * 1000)

                            candleTime >= extendedRangeMs && candleTime <= boundaryMs && candle.isValidCandle()
                        }

                        fiveSecondCandles.addAll(validCandles)

                        if (fiveSecondCandles.size >= 8000) {
                            break
                        }

                    } else {
                        Log.w(TAG, "Failed to fetch 5-second data for hour $hoursBack")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching 5-second data for hour $hoursBack: ${e.message}")
                }

                delay(200)
            }

            Log.d(TAG, "Collected ${fiveSecondCandles.size} total 5-second candles")

            if (fiveSecondCandles.size < 2160) {
                Log.e(TAG, "Insufficient 5-second data: ${fiveSecondCandles.size} < 2160")
                return false
            }

            val aggregatedCandles = aggregateFiveSecondToOneMinute(fiveSecondCandles, boundaryUTC)

            if (aggregatedCandles.size >= (HISTORICAL_CANDLES_COUNT * 0.8)) {
                historicalCandles.addAll(aggregatedCandles.takeLast(HISTORICAL_CANDLES_COUNT))

                Log.d(TAG, "Final aggregated data: ${historicalCandles.size} x 1-minute candles")
                return true
            } else {
                Log.e(TAG, "Insufficient aggregated data: ${aggregatedCandles.size} < ${(HISTORICAL_CANDLES_COUNT * 0.8).toInt()}")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in collect and aggregate: ${e.message}", e)
            return false
        }
    }

    private fun aggregateFiveSecondToOneMinute(fiveSecondCandles: List<Candle>, boundaryUTC: Date): List<Candle> {
        val oneMinuteCandles = mutableListOf<Candle>()

        try {
            val sortedCandles = fiveSecondCandles.sortedBy { parseISO8601ToMillis(it.createdAt) }

            val groupedByMinute = sortedCandles.groupBy { candle ->
                val candleTimeMs = parseISO8601ToMillis(candle.createdAt)
                (candleTimeMs / CANDLE_INTERVAL_MS) * CANDLE_INTERVAL_MS
            }

            groupedByMinute.entries.sortedBy { it.key }.forEach { (minuteMs, candlesInMinute) ->
                if (candlesInMinute.size >= 3) {
                    try {
                        val open = candlesInMinute.first().open
                        val close = candlesInMinute.last().close
                        val high = candlesInMinute.maxOf { it.high }
                        val low = candlesInMinute.minOf { it.low }

                        val minuteTime = Date(minuteMs)
                        val isoTimestamp = serverDateFormat.format(minuteTime)

                        val aggregatedCandle = Candle(
                            open = open,
                            close = close,
                            high = high,
                            low = low,
                            createdAt = isoTimestamp
                        )

                        oneMinuteCandles.add(aggregatedCandle)

                    } catch (e: Exception) {
                        Log.w(TAG, "Error aggregating minute $minuteMs: ${e.message}")
                    }
                }
            }

            val boundaryMs = boundaryUTC.time
            val hoursBeforeMs = boundaryMs - (HISTORICAL_CANDLES_COUNT * 60 * 1000)

            val filteredCandles = oneMinuteCandles.filter { candle ->
                val candleTime = parseISO8601ToMillis(candle.createdAt)
                candleTime >= hoursBeforeMs && candleTime <= boundaryMs
            }.takeLast(HISTORICAL_CANDLES_COUNT)

            return filteredCandles

        } catch (e: Exception) {
            Log.e(TAG, "Error in aggregation process: ${e.message}", e)
            return emptyList()
        }
    }

    private fun parseCandleResponse(response: CandleApiResponse): List<Candle> {
        return try {
            response.data.mapNotNull { candleData ->
                try {
                    val candle = Candle(
                        open = BigDecimal(candleData.open).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP),
                        close = BigDecimal(candleData.close).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP),
                        high = BigDecimal(candleData.high).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP),
                        low = BigDecimal(candleData.low).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP),
                        createdAt = candleData.created_at
                    )

                    if (candle.isValidCandle()) {
                        candle
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing candle response: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseISO8601ToMillis(timeString: String): Long {
        return try {
            when {
                timeString.endsWith("Z") -> java.time.Instant.parse(timeString).toEpochMilli()
                timeString.contains("T") -> java.time.Instant.parse("${timeString}Z").toEpochMilli()
                else -> {
                    val timestamp = timeString.toLongOrNull()
                    if (timestamp != null) {
                        if (timestamp > 9999999999L) timestamp else timestamp * 1000
                    } else {
                        System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun analyzeCollectedData(settings: IndicatorSettings): IndicatorAnalysisResult? {
        try {
            if (historicalCandles.size < settings.period) {
                Log.e(TAG, "Not enough data for analysis: ${historicalCandles.size} < ${settings.period}")
                return null
            }

            val values = when (settings.type) {
                IndicatorType.SMA -> calculateSMAForAnalysis(settings.period)
                IndicatorType.EMA -> calculateEMAForAnalysis(settings.period)
                IndicatorType.RSI -> calculateRSIForAnalysis(settings.period, BigDecimal(settings.rsiOverbought.toString()), BigDecimal(settings.rsiOversold.toString()))
            }

            analysisResult = IndicatorAnalysisResult(
                indicatorType = settings.type,
                calculatedValues = values.calculatedValues,
                finalIndicatorValue = values.finalValue,
                trend = values.trend,
                strength = values.strength
            )

            Log.d(TAG, "Analysis Result (from ${historicalCandles.size} candles):")
            Log.d(TAG, "  Type: ${settings.type}")
            Log.d(TAG, "  Final Value: ${values.finalValue.toPlainString()}")
            Log.d(TAG, "  Trend: ${values.trend}")
            Log.d(TAG, "  Strength: ${values.strength}")

            return analysisResult

        } catch (e: Exception) {
            Log.e(TAG, "Error in data analysis: ${e.message}", e)
            return null
        }
    }

    private fun calculateSMAForAnalysis(period: Int): IndicatorCalculationResult {
        val values = mutableListOf<BigDecimal>()

        for (i in period - 1 until historicalCandles.size) {
            val prices = historicalCandles.subList(i - period + 1, i + 1).map { it.close }
            val sum = prices.fold(ZERO) { acc, price -> acc.add(price, MATH_CONTEXT) }
            val sma = sum.divide(BigDecimal(period), MATH_CONTEXT)
            values.add(sma)
        }

        val finalValue = values.last()
        val currentPrice = historicalCandles.last().close
        val trend = if (currentPrice > finalValue) "BULLISH" else "BEARISH"
        val strength = calculateTrendStrength(values)

        return IndicatorCalculationResult(values, finalValue, trend, strength)
    }

    private fun calculateEMAForAnalysis(period: Int): IndicatorCalculationResult {
        val values = mutableListOf<BigDecimal>()
        val multiplier = TWO.divide(BigDecimal(period + 1), MATH_CONTEXT)
        val oneMinusMultiplier = ONE.subtract(multiplier, MATH_CONTEXT)

        var ema = historicalCandles[0].close

        historicalCandles.forEach { candle ->
            val price = candle.close
            val multipliedPrice = price.multiply(multiplier, MATH_CONTEXT)
            val multipliedEma = ema.multiply(oneMinusMultiplier, MATH_CONTEXT)
            ema = multipliedPrice.add(multipliedEma, MATH_CONTEXT)
            values.add(ema)
        }

        val finalValue = values.last()
        val currentPrice = historicalCandles.last().close
        val trend = if (currentPrice > finalValue) "BULLISH" else "BEARISH"
        val strength = calculateTrendStrength(values)

        return IndicatorCalculationResult(values, finalValue, trend, strength)
    }

    private fun calculateRSIForAnalysis(period: Int, overbought: BigDecimal, oversold: BigDecimal): IndicatorCalculationResult {
        val values = mutableListOf<BigDecimal>()

        if (historicalCandles.size < period + 1) {
            throw IllegalStateException("Not enough data for RSI calculation")
        }

        var gains = ZERO
        var losses = ZERO

        for (i in 1..period) {
            val change = historicalCandles[i].close.subtract(historicalCandles[i-1].close, MATH_CONTEXT)
            if (change > ZERO) {
                gains = gains.add(change, MATH_CONTEXT)
            } else {
                losses = losses.add(change.abs(), MATH_CONTEXT)
            }
        }

        var avgGain = gains.divide(BigDecimal(period), MATH_CONTEXT)
        var avgLoss = losses.divide(BigDecimal(period), MATH_CONTEXT)

        val periodMinusOne = BigDecimal(period - 1)
        val periodBD = BigDecimal(period)

        for (i in period + 1 until historicalCandles.size) {
            val change = historicalCandles[i].close.subtract(historicalCandles[i-1].close, MATH_CONTEXT)
            val gain = if (change > ZERO) change else ZERO
            val loss = if (change < ZERO) change.abs() else ZERO

            avgGain = avgGain.multiply(periodMinusOne, MATH_CONTEXT)
                .add(gain, MATH_CONTEXT)
                .divide(periodBD, MATH_CONTEXT)

            avgLoss = avgLoss.multiply(periodMinusOne, MATH_CONTEXT)
                .add(loss, MATH_CONTEXT)
                .divide(periodBD, MATH_CONTEXT)

            val rs = if (avgLoss > ZERO) {
                avgGain.divide(avgLoss, MATH_CONTEXT)
            } else {
                BigDecimal("100")
            }

            val rsi = ONE_HUNDRED.subtract(
                ONE_HUNDRED.divide(ONE.add(rs, MATH_CONTEXT), MATH_CONTEXT),
                MATH_CONTEXT
            )

            values.add(rsi)
        }

        val finalValue = values.last()

        val trend = when {
            finalValue > overbought -> "BEARISH"
            finalValue < oversold -> "BULLISH"
            else -> "NEUTRAL"
        }

        val sixty = BigDecimal("60")
        val forty = BigDecimal("40")

        val strength = when {
            finalValue > overbought || finalValue < oversold -> "STRONG"
            finalValue > sixty || finalValue < forty -> "MODERATE"
            else -> "WEAK"
        }

        return IndicatorCalculationResult(values, finalValue, trend, strength)
    }

    private fun calculateTrendStrength(values: List<BigDecimal>): String {
        if (values.size < 5) return "WEAK"

        val recentValues = values.takeLast(5)
        val isUpTrend = recentValues.zipWithNext().all { (a, b) -> b >= a }
        val isDownTrend = recentValues.zipWithNext().all { (a, b) -> b <= a }

        return when {
            isUpTrend || isDownTrend -> "STRONG"
            recentValues.first() != recentValues.last() -> "MODERATE"
            else -> "WEAK"
        }
    }

    private suspend fun generateTwoPricePredictions(analysis: IndicatorAnalysisResult, settings: IndicatorSettings): List<PricePrediction> {
        try {
            val currentPrice = historicalCandles.last().close
            val predictions = mutableListOf<PricePrediction>()

            val movementAnalysis = analyzePriceMovements()
            val sensitivityMultiplier = settings.sensitivity
            val baseMovement = movementAnalysis.averageMovement.multiply(sensitivityMultiplier, MATH_CONTEXT)

            val baseConfidence = when (analysis.strength) {
                "STRONG" -> CONFIDENCE_80
                "MODERATE" -> CONFIDENCE_70
                else -> BigDecimal("0.6")
            }

            val sensitivityBonus = calculateSensitivityConfidenceBonus(settings.sensitivity)
            val finalConfidence = (baseConfidence.add(sensitivityBonus, MATH_CONTEXT)).min(BigDecimal.ONE)

            when (analysis.indicatorType) {
                IndicatorType.SMA, IndicatorType.EMA -> {
                    val resistanceTarget = currentPrice.add(baseMovement, MATH_CONTEXT)
                    val supportTarget = currentPrice.subtract(baseMovement, MATH_CONTEXT)

                    predictions.add(PricePrediction(
                        targetPrice = resistanceTarget,
                        predictionType = "RESISTANCE_TARGET_1",
                        recommendedTrend = "put",
                        confidence = finalConfidence
                    ))

                    predictions.add(PricePrediction(
                        targetPrice = supportTarget,
                        predictionType = "SUPPORT_TARGET_1",
                        recommendedTrend = "call",
                        confidence = finalConfidence
                    ))
                }

                IndicatorType.RSI -> {
                    val rsiValue = analysis.finalIndicatorValue
                    val overboughtThreshold = settings.rsiOverbought
                    val oversoldThreshold = settings.rsiOversold

                    when {
                        rsiValue >= overboughtThreshold -> {
                            val resistanceTarget = currentPrice.add(baseMovement.multiply(FRACTION_HALF, MATH_CONTEXT), MATH_CONTEXT)
                            val supportTarget = currentPrice.subtract(baseMovement, MATH_CONTEXT)

                            predictions.add(PricePrediction(
                                targetPrice = resistanceTarget,
                                predictionType = "RESISTANCE_TARGET_1",
                                recommendedTrend = "put",
                                confidence = finalConfidence.multiply(BigDecimal("0.9"), MATH_CONTEXT)
                            ))

                            predictions.add(PricePrediction(
                                targetPrice = supportTarget,
                                predictionType = "SUPPORT_TARGET_1",
                                recommendedTrend = "put",
                                confidence = finalConfidence
                            ))
                        }

                        rsiValue <= oversoldThreshold -> {
                            val resistanceTarget = currentPrice.add(baseMovement, MATH_CONTEXT)
                            val supportTarget = currentPrice.subtract(baseMovement.multiply(FRACTION_HALF, MATH_CONTEXT), MATH_CONTEXT)

                            predictions.add(PricePrediction(
                                targetPrice = resistanceTarget,
                                predictionType = "RESISTANCE_TARGET_1",
                                recommendedTrend = "call",
                                confidence = finalConfidence
                            ))

                            predictions.add(PricePrediction(
                                targetPrice = supportTarget,
                                predictionType = "SUPPORT_TARGET_1",
                                recommendedTrend = "call",
                                confidence = finalConfidence.multiply(BigDecimal("0.9"), MATH_CONTEXT)
                            ))
                        }

                        else -> {
                            val neutralMovement = baseMovement.multiply(BigDecimal("0.7"), MATH_CONTEXT)
                            val resistanceTarget = currentPrice.add(neutralMovement, MATH_CONTEXT)
                            val supportTarget = currentPrice.subtract(neutralMovement, MATH_CONTEXT)

                            predictions.add(PricePrediction(
                                targetPrice = resistanceTarget,
                                predictionType = "RESISTANCE_TARGET_1",
                                recommendedTrend = "put",
                                confidence = finalConfidence.multiply(BigDecimal("0.8"), MATH_CONTEXT)
                            ))

                            predictions.add(PricePrediction(
                                targetPrice = supportTarget,
                                predictionType = "SUPPORT_TARGET_1",
                                recommendedTrend = "call",
                                confidence = finalConfidence.multiply(BigDecimal("0.8"), MATH_CONTEXT)
                            ))
                        }
                    }
                }
            }

            predictions.sortByDescending { it.confidence }

            Log.d(TAG, "Generated exactly 2 price predictions:")
            predictions.forEach { prediction ->
                Log.d(TAG, "  ${prediction.getDisplayInfo()}")
            }

            return predictions

        } catch (e: Exception) {
            Log.e(TAG, "Error generating predictions: ${e.message}", e)
            return emptyList()
        }
    }

    private fun calculateSensitivityConfidenceBonus(sensitivity: BigDecimal): BigDecimal {
        return when {
            sensitivity <= IndicatorSettings.SENSITIVITY_LOW -> BigDecimal("-0.05")
            sensitivity <= IndicatorSettings.SENSITIVITY_MEDIUM -> BigDecimal.ZERO
            sensitivity <= IndicatorSettings.SENSITIVITY_HIGH -> BigDecimal("0.05")
            sensitivity <= IndicatorSettings.SENSITIVITY_VERY_HIGH -> BigDecimal("0.10")
            else -> BigDecimal("0.15")
        }
    }

    private fun analyzePriceMovements(): PriceMovementAnalysis {
        if (historicalCandles.size < 2) {
            return PriceMovementAnalysis(
                averageMovement = BigDecimal("0.00001"),
                maxMovement = BigDecimal("0.00001"),
                significantDigits = 5,
                typicalRange = BigDecimal("0.00001")
            )
        }

        val movements = mutableListOf<BigDecimal>()
        val ranges = mutableListOf<BigDecimal>()

        for (i in 1 until historicalCandles.size) {
            val currentCandle = historicalCandles[i]
            val previousCandle = historicalCandles[i - 1]

            val movement = (currentCandle.close - previousCandle.close).abs()
            movements.add(movement)

            val range = currentCandle.high - currentCandle.low
            ranges.add(range)
        }

        val averageMovement = movements.fold(ZERO) { acc, mov -> acc.add(mov, MATH_CONTEXT) }
            .divide(BigDecimal(movements.size), MATH_CONTEXT)

        val maxMovement = movements.maxOrNull() ?: BigDecimal("0.00001")

        val averageRange = ranges.fold(ZERO) { acc, range -> acc.add(range, MATH_CONTEXT) }
            .divide(BigDecimal(ranges.size), MATH_CONTEXT)

        val currentPrice = historicalCandles.last().close
        val thousand = BigDecimal("1000")
        val hundred = BigDecimal("100")
        val ten = BigDecimal("10")

        val significantDigits = when {
            currentPrice > thousand -> 2
            currentPrice > hundred -> 3
            currentPrice > ten -> 4
            else -> 5
        }

        val typicalRange = if (averageMovement < averageRange) averageMovement else averageRange

        return PriceMovementAnalysis(
            averageMovement = averageMovement,
            maxMovement = maxMovement,
            significantDigits = significantDigits,
            typicalRange = typicalRange
        )
    }

    private fun startPredictionMonitoring() {
        predictionMonitoringJob?.cancel()
        predictionMonitoringJob = scope.launch {
            Log.d(TAG, "Starting prediction monitoring with ${pricePredictions.size} targets")

            while (isIndicatorModeActive) {
                try {
                    updateCurrentRealTimePrice()

                    if (isMonitoringActiveTradeOnly) {
                        monitorActiveTrade()
                    } else {
                        checkPricePredictionsWithDelay()
                    }

                    delay(PRICE_MONITOR_INTERVAL)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.e(TAG, "Error in prediction monitoring: ${e.message}", e)
                    delay(5000)
                }
            }
        }
    }

    private suspend fun monitorActiveTrade() {
        val activeOrderId = activeTradeOrderId ?: return
        val activeOrder = indicatorOrders.find { it.id == activeOrderId }

        if (activeOrder != null && !activeOrder.isExecuted) {
            val currentPriceDisplay = currentRealTimePrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString()
            val accountType = if (isDemoAccount) "Demo" else "Real"

            val statusMessage = if (isMartingaleActive()) {
                val currentStep = getCurrentMartingaleStep()
                "Monitoring Martingale Step $currentStep IMMEDIATE - Price: $currentPriceDisplay - Ultra-fast detection ($accountType)"
            } else {
                "Monitoring Trade ${activeOrder.getTrendDisplay()} - Price: $currentPriceDisplay ($accountType)"
            }

            onModeStatusUpdate(statusMessage)

            // ENSURE prediction monitoring doesn't interfere
            if (isMartingaleActive() && predictionMonitoringJob?.isActive == true) {
                Log.d(TAG, "Martingale active - temporarily suspending prediction monitoring to avoid conflicts")
            }
        }
    }


    private suspend fun updateCurrentRealTimePrice() {
        try {
            val asset = selectedAsset ?: return
            val userSession = getUserSession() ?: return

            val encodedSymbol = asset.ric.replace("/", "%2F")
            val currentUTC = getCurrentUTCTime()
            val dateForApi = apiDateFormat.format(currentUTC)

            val response = withTimeoutOrNull(3000L) {
                priceApi.getLastCandle(
                    symbol = encodedSymbol,
                    date = dateForApi,
                    locale = "id",
                    authToken = userSession.authtoken,
                    deviceId = userSession.deviceId,
                    deviceType = userSession.deviceType,
                    userAgent = userSession.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            }

            if (response?.isSuccessful == true && response.body() != null) {
                val candleResponse = response.body()!!
                val latestCandleData = candleResponse.data.lastOrNull()

                if (latestCandleData != null) {
                    val newPrice = BigDecimal(latestCandleData.close).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
                    if (newPrice > ZERO) {
                        currentRealTimePrice = newPrice
                        lastPriceUpdateTime = System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating realtime price: ${e.message}", e)
        }
    }

    private suspend fun checkPricePredictionsWithDelay() {
        if (currentRealTimePrice <= ZERO) return
        if (hasPendingTrades()) return
        if (shouldStopDueToConsecutiveLosses()) return

        // CRITICAL FIX: Skip ALL prediction logic saat ada martingale activity
        if (isMartingaleActive()) {
            val currentStep = getCurrentMartingaleStep()
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $currentStep ACTIVE - Bypassing prediction logic ($accountType)")
            return // COMPLETELY SKIP prediction checking
        }

        if (currentMartingaleOrder != null && activeMartingaleOrderId != null) {
            val martingaleOrder = currentMartingaleOrder!!
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step ${martingaleOrder.currentStep} IN PROGRESS - Bypassing predictions ($accountType)")
            return // COMPLETELY SKIP prediction checking
        }

        if (isMartingalePendingExecution) {
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale PENDING EXECUTION - Bypassing predictions ($accountType)")
            return // COMPLETELY SKIP prediction checking
        }

        if (isTradeExecuted) return

        // ONLY do prediction logic if NO martingale activity
        val availablePredictions = pricePredictions.filter { !it.isTriggered && !it.isDisabled }
        if (availablePredictions.isEmpty()) {
            onModeStatusUpdate("All predictions processed")
            return
        }

        for (prediction in availablePredictions) {
            if (isPredictionTriggered(prediction)) {
                Log.d(TAG, "PREDICTION HIT: ${prediction.getDisplayInfo()}")
                Log.d(TAG, "  Current Price: ${currentRealTimePrice.toPlainString()}")
                Log.d(TAG, "  Target Price: ${prediction.targetPrice.toPlainString()}")

                markPredictionAsTriggered(prediction)
                disableOtherPredictions(prediction.id)

                // HANYA queue prediction jika BUKAN martingale
                queuePredictionForDelayedExecution(prediction)

                isTradeExecuted = true
                break
            }
        }

        val remaining = pricePredictions.count { !it.isTriggered && !it.isDisabled }
        val currentPriceDisplay = currentRealTimePrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString()
        val accountType = if (isDemoAccount) "Demo" else "Real"

        val statusMessage = if (isWaitingForExecutionBoundary && queuedExecutions.isNotEmpty()) {
            val nextBoundaryTime = displayTimeFormat.format(Date(nextExecutionBoundary))
            "Waiting execution boundary $nextBoundaryTime - Trade queued ($accountType)"
        } else {
            "Monitoring $remaining predictions - Price: $currentPriceDisplay ($accountType)"
        }

        onModeStatusUpdate(statusMessage)
    }



    private fun disableOtherPredictions(triggeredPredictionId: String) {
        for (i in pricePredictions.indices) {
            if (pricePredictions[i].id != triggeredPredictionId) {
                pricePredictions[i] = pricePredictions[i].copy(isDisabled = true)
            }
        }
        Log.d(TAG, "Disabled other predictions after $triggeredPredictionId was triggered")
    }

    private suspend fun handleMartingaleLossImmediate(indicatorOrderId: String, amount: Long, details: Map<String, Any>) {
        val martingaleOrder = currentMartingaleOrder ?: return
        val settings = martingaleSettings ?: return

        val newTotalLoss = martingaleOrder.totalLoss + amount
        val nextStep = martingaleOrder.currentStep + 1

        if (nextStep <= settings.maxSteps) {
            try {
                val nextAmount = settings.getMartingaleAmountForStep(nextStep)

                currentMartingaleOrder = martingaleOrder.copy(
                    currentStep = nextStep,
                    totalLoss = newTotalLoss,
                    nextAmount = nextAmount
                )

                // PERBAIKAN CRITICAL: Set flags untuk bypass semua logic prediction
                isMartingalePendingExecution = false
                isTradeExecuted = false  // Reset untuk allow immediate execution
                isMonitoringActiveTradeOnly = false  // Reset monitoring mode

                // Stop prediction monitoring temporarily untuk avoid conflict
                predictionMonitoringJob?.cancel()
                executionDelayJob?.cancel()
                queuedExecutions.clear()
                isWaitingForExecutionBoundary = false

                Log.d(TAG, "MARTINGALE IMMEDIATE STEP $nextStep - BYPASSING ALL PREDICTION LOGIC")
                Log.d(TAG, "  Amount: ${formatAmount(nextAmount)}")
                Log.d(TAG, "  Method: DIRECT EXECUTION (No boundary, no queue, no delay)")

                val result = IndicatorMartingaleResult(
                    isWin = false,
                    step = nextStep,
                    amount = nextAmount,
                    totalLoss = newTotalLoss,
                    message = "Martingale step $nextStep - IMMEDIATE EXECUTION",
                    shouldContinue = true,
                    indicatorOrderId = indicatorOrderId
                )

                onIndicatorMartingaleResult(result)

                // LANGSUNG EKSEKUSI TANPA DELAY/QUEUE/BOUNDARY
                executeMartingaleDirectImmediate(nextStep, nextAmount, indicatorOrderId)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating immediate martingale step: ${e.message}", e)
                handleMartingaleFailure(newTotalLoss, indicatorOrderId)
            }
        } else {
            handleMartingaleFailure(newTotalLoss, indicatorOrderId)
        }
    }

    private suspend fun executeMartingaleDirectImmediate(step: Int, amount: Long, originalOrderId: String) {
        try {
            val settings = currentSettings ?: return
            val asset = selectedAsset ?: return
            val lastTrend = findLastTrend()

            Log.d(TAG, "DIRECT IMMEDIATE MARTINGALE EXECUTION:")
            Log.d(TAG, "  Step: $step")
            Log.d(TAG, "  Amount: ${formatAmount(amount)}")
            Log.d(TAG, "  Method: COMPLETELY BYPASS ALL QUEUES AND BOUNDARIES")

            val martingaleIndicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = asset.ric,
                assetName = asset.name,
                trend = lastTrend,
                amount = amount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = currentRealTimePrice,
                triggerType = "MARTINGALE_DIRECT_IMMEDIATE",
                indicatorType = settings.type.name,
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            // Setup all necessary states
            indicatorOrders.add(martingaleIndicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = martingaleIndicatorOrder.id,
                amount = amount,
                trend = lastTrend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = currentRealTimePrice,
                indicatorType = settings.type.name
            )
            pendingTradeResults[martingaleIndicatorOrder.id] = pendingInfo
            activeTradeOrderId = martingaleIndicatorOrder.id

            // Register untuk continuous monitoring
            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = martingaleIndicatorOrder.id,
                trend = lastTrend,
                amount = amount,
                assetRic = asset.ric,
                isDemoAccount = isDemoAccount,
                indicatorType = settings.type.name,
                isMartingaleAttempt = true,
                martingaleStep = step
            )

            // CRITICAL: IMMEDIATE EXECUTION - BYPASS ALL QUEUE/BOUNDARY LOGIC
            onExecuteIndicatorTrade(lastTrend, martingaleIndicatorOrder.id, amount)
            totalExecutions++

            activeMartingaleOrderId = martingaleIndicatorOrder.id
            isMartingalePendingExecution = false

            // Set monitoring mode khusus untuk martingale
            isMonitoringActiveTradeOnly = true
            tradeResultReceived = false

            startResultMonitoring(martingaleIndicatorOrder.id)

            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $step EXECUTED IMMEDIATELY - ${formatAmount(amount)} - Ultra-fast monitoring ($accountType)")

            Log.d(TAG, "DIRECT IMMEDIATE EXECUTION COMPLETED - NO DELAYS, NO QUEUES, NO BOUNDARIES")

        } catch (e: Exception) {
            Log.e(TAG, "Error in direct immediate martingale execution: ${e.message}", e)
            isMartingalePendingExecution = false
            activeMartingaleOrderId = null

            scope.launch {
                val martingaleOrder = currentMartingaleOrder
                if (martingaleOrder != null) {
                    handleMartingaleFailure(martingaleOrder.totalLoss + amount, originalOrderId)
                }
            }
        }
    }

    private suspend fun executeMartingaleImmediatelyBypassQueue(step: Int, amount: Long, originalOrderId: String) {
        try {
            val settings = currentSettings ?: return
            val asset = selectedAsset ?: return
            val lastTrend = findLastTrend()

            val martingaleIndicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = asset.ric,
                assetName = asset.name,
                trend = lastTrend,
                amount = amount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = currentRealTimePrice,
                triggerType = "MARTINGALE_IMMEDIATE_BYPASS",
                indicatorType = settings.type.name,
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            indicatorOrders.add(martingaleIndicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = martingaleIndicatorOrder.id,
                amount = amount,
                trend = lastTrend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = currentRealTimePrice,
                indicatorType = settings.type.name
            )
            pendingTradeResults[martingaleIndicatorOrder.id] = pendingInfo
            activeTradeOrderId = martingaleIndicatorOrder.id

            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = martingaleIndicatorOrder.id,
                trend = lastTrend,
                amount = amount,
                assetRic = asset.ric,
                isDemoAccount = isDemoAccount,
                indicatorType = settings.type.name,
                isMartingaleAttempt = true,
                martingaleStep = step
            )

            onExecuteIndicatorTrade(lastTrend, martingaleIndicatorOrder.id, amount)
            totalExecutions++

            activeMartingaleOrderId = martingaleIndicatorOrder.id
            isMartingalePendingExecution = false
            startResultMonitoring(martingaleIndicatorOrder.id)

            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $step EXECUTED IMMEDIATELY - ${formatAmount(amount)} ($accountType)")

        } catch (e: Exception) {
            Log.e(TAG, "Error executing immediate martingale: ${e.message}", e)
            isMartingalePendingExecution = false
            activeMartingaleOrderId = null

            scope.launch {
                val martingaleOrder = currentMartingaleOrder
                if (martingaleOrder != null) {
                    handleMartingaleFailure(martingaleOrder.totalLoss + amount, originalOrderId)
                }
            }
        }
    }

    private suspend fun checkPredictionsWithDelay() {
        if (currentRealTimePrice <= ZERO) return
        if (hasPendingTrades()) return
        if (shouldStopDueToConsecutiveLosses()) return

        if (isMartingaleActive()) {
            val currentStep = getCurrentMartingaleStep()
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $currentStep ACTIVE - Monitoring result ($accountType)")
            return
        }

        if (isMartingalePendingExecution && currentMartingaleOrder != null) {
            val martingaleOrder = currentMartingaleOrder!!
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step ${martingaleOrder.currentStep} PENDING EXECUTION ($accountType)")
            return
        }

        if (isTradeExecuted) return

        val availablePredictions = pricePredictions.filter { !it.isTriggered && !it.isDisabled }
        if (availablePredictions.isEmpty()) {
            onModeStatusUpdate("All predictions processed")
            return
        }

        for (prediction in availablePredictions) {
            if (isPredictionTriggered(prediction)) {
                Log.d(TAG, "PREDICTION HIT: ${prediction.getDisplayInfo()}")
                Log.d(TAG, "  Current Price: ${currentRealTimePrice.toPlainString()}")
                Log.d(TAG, "  Target Price: ${prediction.targetPrice.toPlainString()}")

                markPredictionAsTriggered(prediction)
                disableOtherPredictions(prediction.id)
                queuePredictionForDelayedExecution(prediction)

                isTradeExecuted = true
                break
            }
        }

        val remaining = pricePredictions.count { !it.isTriggered && !it.isDisabled }
        val currentPriceDisplay = currentRealTimePrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString()
        val accountType = if (isDemoAccount) "Demo" else "Real"

        val statusMessage = if (isWaitingForExecutionBoundary && queuedExecutions.isNotEmpty()) {
            val nextBoundaryTime = displayTimeFormat.format(Date(nextExecutionBoundary))
            "Waiting execution boundary $nextBoundaryTime - Trade queued ($accountType)"
        } else {
            "Monitoring $remaining predictions - Price: $currentPriceDisplay ($accountType)"
        }

        onModeStatusUpdate(statusMessage)
    }

    private suspend fun queuePredictionForDelayedExecution(prediction: PricePrediction) {
        try {
            if (isMartingaleActive() || isMartingalePendingExecution) {
                Log.e(TAG, "ERROR: Martingale should NOT use prediction queue")
                return
            }

            val queuedExecution = QueuedExecution(
                prediction = prediction,
                executionPrice = currentRealTimePrice,
                isMartingale = false
            )
            queuedExecutions.add(queuedExecution)
            Log.d(TAG, "Regular prediction queued for boundary execution")

            if (!isWaitingForExecutionBoundary) {
                startExecutionBoundaryWaiting()
            }

            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("PREDICTION HIT: ${prediction.predictionType} - Queued for execution ($accountType)")

        } catch (e: Exception) {
            Log.e(TAG, "Error queueing prediction: ${e.message}", e)
        }
    }


    private suspend fun executeImmediateMartingale(nextStep: Int, nextAmount: Long, originalOrderId: String) {
        try {
            val settings = currentSettings ?: return
            val asset = selectedAsset ?: return

            val lastTrend = findLastTrend()

            val martingaleIndicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = asset.ric,
                assetName = asset.name,
                trend = lastTrend,
                amount = nextAmount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = currentRealTimePrice,
                triggerType = "MARTINGALE_IMMEDIATE_EXECUTION",
                indicatorType = settings.type.name,
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            indicatorOrders.add(martingaleIndicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = martingaleIndicatorOrder.id,
                amount = nextAmount,
                trend = lastTrend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = currentRealTimePrice,
                indicatorType = settings.type.name
            )
            pendingTradeResults[martingaleIndicatorOrder.id] = pendingInfo

            activeTradeOrderId = martingaleIndicatorOrder.id

            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = martingaleIndicatorOrder.id,
                trend = lastTrend,
                amount = nextAmount,
                assetRic = asset.ric,
                isDemoAccount = isDemoAccount,
                indicatorType = settings.type.name,
                isMartingaleAttempt = true,
                martingaleStep = nextStep
            )

            Log.d(TAG, "IMMEDIATE MARTINGALE EXECUTION (BYPASSING QUEUE):")
            Log.d(TAG, "  Order ID: ${martingaleIndicatorOrder.id}")
            Log.d(TAG, "  Step: $nextStep")
            Log.d(TAG, "  Trend: $lastTrend")
            Log.d(TAG, "  Amount: ${formatAmount(nextAmount)}")
            Log.d(TAG, "  Execution: IMMEDIATE REALTIME (No boundary waiting)")
            Log.d(TAG, "  Queue Status: BYPASSED for martingale")

            // IMMEDIATE EXECUTION - tidak melalui queue atau boundary
            onExecuteIndicatorTrade(lastTrend, martingaleIndicatorOrder.id, nextAmount)
            totalExecutions++

            activeMartingaleOrderId = martingaleIndicatorOrder.id
            isMartingalePendingExecution = false

            startResultMonitoring(martingaleIndicatorOrder.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error executing immediate martingale: ${e.message}", e)
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Error executing immediate martingale step ($accountType): ${e.message}")
        }
    }


    private fun startExecutionBoundaryWaiting() {
        if (isWaitingForExecutionBoundary) return

        executionDelayJob?.cancel()
        executionDelayJob = scope.launch {
            try {
                val currentTime = getCurrentServerTime()
                nextExecutionBoundary = calculateNextMinuteBoundary(currentTime)
                isWaitingForExecutionBoundary = true

                val waitTime = nextExecutionBoundary - currentTime - 2000L

                if (waitTime > 0) {
                    delay(waitTime)
                }

                executeQueuedPredictions()

            } catch (ce: CancellationException) {
                Log.d(TAG, "Execution boundary waiting cancelled")
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Error in execution boundary waiting: ${e.message}", e)
            } finally {
                isWaitingForExecutionBoundary = false
                queuedExecutions.clear()
            }
        }
    }

    private suspend fun executeQueuedPredictions() {
        if (queuedExecutions.isEmpty()) return

        executionMutex.withLock {
            try {
                Log.d(TAG, "Executing ${queuedExecutions.size} queued predictions at boundary")

                for (queuedExecution in queuedExecutions.toList()) {
                    if (!isIndicatorModeActive) break

                    if (queuedExecution.isMartingale) {
                        executeQueuedMartingale(queuedExecution)
                    } else {
                        executeQueuedRegularTrade(queuedExecution)
                    }

                    delay(500)
                }

                val accountType = if (isDemoAccount) "Demo" else "Real"
                onModeStatusUpdate("Trade executed - Monitoring result ($accountType)")

            } catch (e: Exception) {
                Log.e(TAG, "Error executing queued predictions: ${e.message}", e)
                val accountType = if (isDemoAccount) "Demo" else "Real"
                onModeStatusUpdate("Error executing queued predictions ($accountType): ${e.message}")
            }
        }
    }

    private suspend fun executeQueuedRegularTrade(queuedExecution: QueuedExecution) {
        try {
            val prediction = queuedExecution.prediction
            val baseAmount = martingaleSettings?.baseAmount ?: 1_400_000L

            val indicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = selectedAsset?.ric ?: "",
                assetName = selectedAsset?.name ?: "",
                trend = prediction.recommendedTrend,
                amount = baseAmount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = prediction.targetPrice,
                triggerType = "${prediction.predictionType}_DELAYED",
                indicatorType = currentSettings?.type?.name ?: "",
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            indicatorOrders.add(indicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = indicatorOrder.id,
                amount = baseAmount,
                trend = prediction.recommendedTrend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = prediction.targetPrice,
                indicatorType = currentSettings?.type?.name ?: ""
            )
            pendingTradeResults[indicatorOrder.id] = pendingInfo

            activeTradeOrderId = indicatorOrder.id
            isMonitoringActiveTradeOnly = true
            tradeResultReceived = false

            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = indicatorOrder.id,
                trend = prediction.recommendedTrend,
                amount = baseAmount,
                assetRic = selectedAsset?.ric ?: "",
                isDemoAccount = isDemoAccount,
                indicatorType = currentSettings?.type?.name ?: "",
                isMartingaleAttempt = false,
                martingaleStep = 0
            )

            Log.d(TAG, "PREDICTION TRADE EXECUTED with ultra-fast monitoring:")
            Log.d(TAG, "  Order ID: ${indicatorOrder.id}")
            Log.d(TAG, "  Prediction: ${prediction.predictionType}")
            Log.d(TAG, "  Trend: ${prediction.recommendedTrend}")
            Log.d(TAG, "  Amount: ${formatAmount(baseAmount)}")

            onExecuteIndicatorTrade(prediction.recommendedTrend, indicatorOrder.id, baseAmount)
            totalExecutions++

            startResultMonitoring(indicatorOrder.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error executing regular trade: ${e.message}", e)
        }
    }

    private suspend fun executeQueuedMartingale(queuedExecution: QueuedExecution) {
        try {
            val martingaleOrder = currentMartingaleOrder ?: return
            val settings = currentSettings ?: return
            val prediction = queuedExecution.prediction

            val martingaleIndicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = selectedAsset?.ric ?: "",
                assetName = selectedAsset?.name ?: "",
                trend = findLastTrend(),
                amount = martingaleOrder.nextAmount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = prediction.targetPrice,
                triggerType = "MARTINGALE_${prediction.predictionType}_DELAYED",
                indicatorType = settings.type.name,
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            indicatorOrders.add(martingaleIndicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = martingaleIndicatorOrder.id,
                amount = martingaleOrder.nextAmount,
                trend = martingaleIndicatorOrder.trend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = prediction.targetPrice,
                indicatorType = settings.type.name
            )
            pendingTradeResults[martingaleIndicatorOrder.id] = pendingInfo

            activeTradeOrderId = martingaleIndicatorOrder.id

            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = martingaleIndicatorOrder.id,
                trend = martingaleIndicatorOrder.trend,
                amount = martingaleOrder.nextAmount,
                assetRic = selectedAsset?.ric ?: "",
                isDemoAccount = isDemoAccount,
                indicatorType = settings.type.name,
                isMartingaleAttempt = true,
                martingaleStep = martingaleOrder.currentStep
            )

            Log.d(TAG, "Martingale executed:")
            Log.d(TAG, "  Step: ${martingaleOrder.currentStep}")
            Log.d(TAG, "  Amount: ${formatAmount(martingaleOrder.nextAmount)}")

            onExecuteIndicatorTrade(martingaleIndicatorOrder.trend, martingaleIndicatorOrder.id, martingaleOrder.nextAmount)
            totalExecutions++

            activeMartingaleOrderId = martingaleIndicatorOrder.id
            isMartingalePendingExecution = false

            startResultMonitoring(martingaleIndicatorOrder.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error executing queued martingale: ${e.message}", e)
            isMartingalePendingExecution = false
        }
    }

    private fun isPredictionTriggered(prediction: PricePrediction): Boolean {
        val currentPrice = currentRealTimePrice
        val targetPrice = prediction.targetPrice
        val settings = currentSettings ?: return false

        val movementAnalysis = analyzePriceMovements()
        val baseTolerance = movementAnalysis.averageMovement.multiply(BigDecimal("0.1"), MATH_CONTEXT)

        val sensitivityAdjustment = when {
            settings.sensitivity <= IndicatorSettings.SENSITIVITY_LOW -> BigDecimal("1.5")
            settings.sensitivity <= IndicatorSettings.SENSITIVITY_MEDIUM -> BigDecimal.ONE
            settings.sensitivity <= IndicatorSettings.SENSITIVITY_HIGH -> BigDecimal("0.8")
            settings.sensitivity <= IndicatorSettings.SENSITIVITY_VERY_HIGH -> BigDecimal("0.6")
            else -> BigDecimal("0.4")
        }

        val adjustedTolerance = baseTolerance.multiply(sensitivityAdjustment, MATH_CONTEXT)
        val difference = (currentPrice - targetPrice).abs()

        return difference <= adjustedTolerance
    }

    private fun markPredictionAsTriggered(prediction: PricePrediction) {
        val index = pricePredictions.indexOfFirst { it.id == prediction.id }
        if (index >= 0) {
            pricePredictions[index] = prediction.copy(
                isTriggered = true,
                triggeredAt = System.currentTimeMillis()
            )
        }
    }

    private fun startResultMonitoring(indicatorOrderId: String) {
        scope.launch {
            delay(2000)

            val startTime = System.currentTimeMillis()
            val maxWaitTime = 120000L

            while (pendingTradeResults.containsKey(indicatorOrderId) && isIndicatorModeActive) {
                if (System.currentTimeMillis() - startTime > maxWaitTime) {
                    Log.w(TAG, "Timeout waiting for result of indicator order: $indicatorOrderId")
                    pendingTradeResults.remove(indicatorOrderId)
                    handleIndicatorTradeResult(indicatorOrderId, false, mapOf(
                        "trade_id" to "timeout",
                        "amount" to 0L,
                        "status" to "timeout",
                        "detection_method" to "timeout"
                    ))
                    break
                }
                delay(1000L)
            }
        }
    }

    fun handleIndicatorTradeResult(indicatorOrderId: String, isWin: Boolean, details: Map<String, Any>) {
        scope.launch {
            executionMutex.withLock {
                try {
                    val indicatorOrder = indicatorOrders.find { it.id == indicatorOrderId }
                    if (indicatorOrder == null) {
                        Log.w(TAG, "Indicator order not found: $indicatorOrderId")
                        return@withLock
                    }

                    pendingTradeResults.remove(indicatorOrderId)

                    val updatedOrder = indicatorOrder.copy(isExecuted = true)
                    val index = indicatorOrders.indexOfFirst { it.id == indicatorOrderId }
                    if (index >= 0) {
                        indicatorOrders[index] = updatedOrder
                        onIndicatorOrdersUpdate(indicatorOrders.toList())
                    }

                    val amount = details["amount"] as? Long ?: indicatorOrder.amount
                    val tradeId = details["trade_id"] as? String ?: "unknown"

                    Log.d(TAG, "Trade result received:")
                    Log.d(TAG, "  Order ID: $indicatorOrderId")
                    Log.d(TAG, "  Trade ID: $tradeId")
                    Log.d(TAG, "  Result: ${if (isWin) "WIN" else "LOSE"}")
                    Log.d(TAG, "  Amount: ${formatAmount(amount)}")

                    if (isWin) {
                        handleIndicatorWin(indicatorOrderId, amount, details)
                    } else {
                        handleIndicatorLoss(indicatorOrderId, amount, details)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling indicator trade result: ${e.message}", e)
                }
            }
        }
    }

    private fun handleIndicatorTradeResultFromMonitor(indicatorOrderId: String, isWin: Boolean, details: Map<String, Any>) {
        scope.launch {
            executionMutex.withLock {
                try {
                    if (tradeResultReceived) {
                        Log.d(TAG, "Trade result already processed, ignoring duplicate")
                        return@withLock
                    }

                    val indicatorOrder = indicatorOrders.find { it.id == indicatorOrderId }
                    if (indicatorOrder == null) {
                        Log.w(TAG, "Indicator order not found for ID: $indicatorOrderId")
                        return@withLock
                    }

                    tradeResultReceived = true

                    indicatorContinuousMonitor.removeIndicatorOrderFromMonitoring(indicatorOrderId)

                    pendingTradeResults.remove(indicatorOrderId)

                    val updatedOrder = indicatorOrder.copy(isExecuted = true)
                    val index = indicatorOrders.indexOfFirst { it.id == indicatorOrderId }
                    if (index >= 0) {
                        indicatorOrders[index] = updatedOrder
                        onIndicatorOrdersUpdate(indicatorOrders.toList())
                    }

                    val amount = details["amount"] as? Long ?: indicatorOrder.amount
                    val tradeId = details["trade_id"] as? String ?: "unknown"
                    val detectionMethod = details["detection_method"] as? String ?: "unknown"

                    Log.d(TAG, "Ultra-fast trade result received:")
                    Log.d(TAG, "  Order ID: $indicatorOrderId")
                    Log.d(TAG, "  Trade ID: $tradeId")
                    Log.d(TAG, "  Result: ${if (isWin) "WIN" else "LOSE"}")
                    Log.d(TAG, "  Amount: ${formatAmount(amount)}")
                    Log.d(TAG, "  Detection: $detectionMethod")

                    if (isWin) {
                        handleIndicatorWin(indicatorOrderId, amount, details)
                    } else {
                        handleIndicatorLoss(indicatorOrderId, amount, details)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling ultra-fast trade result: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun handleIndicatorWin(indicatorOrderId: String, amount: Long, details: Map<String, Any>) {
        totalWins++
        consecutiveWins++
        consecutiveLosses = 0
        val accountType = if (isDemoAccount) "Demo" else "Real"

        Log.d(TAG, "INDICATOR TRADE WIN - Preparing COMPLETE restart")

        if (activeMartingaleOrderId == indicatorOrderId && currentMartingaleOrder != null) {
            val martingaleOrder = currentMartingaleOrder!!
            val winAmount = details["win_amount"] as? Long ?: 0L
            val totalRecovered = winAmount

            Log.d(TAG, "Martingale WIN at step ${martingaleOrder.currentStep} - Resetting COMPLETELY")

            val result = IndicatorMartingaleResult(
                isWin = true,
                step = martingaleOrder.currentStep,
                amount = amount,
                totalRecovered = totalRecovered,
                totalLoss = martingaleOrder.totalLoss,
                message = "Martingale WIN at step ${martingaleOrder.currentStep} - COMPLETE restart enabled",
                indicatorOrderId = indicatorOrderId
            )

            stopCurrentMartingaleCompletely()

            onModeStatusUpdate("Martingale WIN - Complete reset to base amount ($accountType)")
            onIndicatorMartingaleResult(result)

            indicatorContinuousMonitor.handleMartingaleCompletion(
                isWin = true,
                step = martingaleOrder.currentStep,
                isMaxReached = false
            )
        } else {
            Log.d(TAG, "Regular Trade WIN - Resetting COMPLETELY")
            onModeStatusUpdate("Trade WIN - Complete reset to base amount ($accountType)")
        }
    }

    private suspend fun handleIndicatorLoss(indicatorOrderId: String, amount: Long, details: Map<String, Any>) {
        totalLosses++
        consecutiveLosses++
        consecutiveWins = 0

        val settings = martingaleSettings
        if (settings != null && settings.isEnabled) {
            if (activeMartingaleOrderId == indicatorOrderId && currentMartingaleOrder != null) {
                handleMartingaleLossImmediate(indicatorOrderId, amount, details)
            } else {
                startNewIndicatorMartingaleImmediate(indicatorOrderId, amount, details, settings)
            }
        } else {
            Log.d(TAG, "Trade LOSE - Martingale not active, will auto-restart")
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Trade LOSE - Auto-restart in progress ($accountType)")

            if (shouldStopDueToConsecutiveLosses()) {
                onIndicatorModeComplete("CONSECUTIVE_LOSS", "Consecutive loss limit reached")
                return
            }

            onIndicatorModeComplete("SINGLE_LOSS", "Single trade loss - will auto-restart")
        }
    }

    private suspend fun startNewIndicatorMartingaleImmediate(
        indicatorOrderId: String,
        lossAmount: Long,
        details: Map<String, Any>,
        settings: MartingaleState
    ) {
        try {
            val nextStep = 1
            val nextAmount = settings.getMartingaleAmountForStep(nextStep)

            currentMartingaleOrder = IndicatorMartingaleOrder(
                originalOrderId = indicatorOrderId,
                currentStep = nextStep,
                maxSteps = settings.maxSteps,
                totalLoss = lossAmount,
                nextAmount = nextAmount,
                isActive = true
            )

            activeMartingaleOrderId = indicatorOrderId
            isMartingalePendingExecution = false

            // CRITICAL: Clear prediction states
            isTradeExecuted = false
            isMonitoringActiveTradeOnly = false
            predictionMonitoringJob?.cancel()
            executionDelayJob?.cancel()
            queuedExecutions.clear()
            isWaitingForExecutionBoundary = false

            Log.d(TAG, "NEW MARTINGALE IMMEDIATE START - STEP $nextStep")

            val result = IndicatorMartingaleResult(
                isWin = false,
                step = nextStep,
                amount = nextAmount,
                totalLoss = lossAmount,
                message = "Martingale started - Step $nextStep IMMEDIATE EXECUTION",
                shouldContinue = true,
                indicatorOrderId = indicatorOrderId
            )

            onIndicatorMartingaleResult(result)
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $nextStep - IMMEDIATE EXECUTION ($accountType)")

            // LANGSUNG EKSEKUSI
            executeMartingaleDirectImmediate(nextStep, nextAmount, indicatorOrderId)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting immediate martingale: ${e.message}", e)
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Error starting martingale ($accountType): ${e.message}")
        }
    }


    private suspend fun executeMartingaleOrderImmediately(step: Int, amount: Long, originalOrderId: String) {
        try {
            val settings = currentSettings ?: return
            val asset = selectedAsset ?: return

            val lastTrend = findLastTrend()

            // PERBAIKAN 1: Buat order baru untuk martingale
            val martingaleIndicatorOrder = IndicatorOrder(
                id = UUID.randomUUID().toString(),
                assetRic = asset.ric,
                assetName = asset.name,
                trend = lastTrend,
                amount = amount,
                executionTime = System.currentTimeMillis(),
                triggerLevel = currentRealTimePrice,
                triggerType = "MARTINGALE_IMMEDIATE_EXECUTION",
                indicatorType = settings.type.name,
                indicatorValue = analysisResult?.finalIndicatorValue ?: ZERO
            )

            // PERBAIKAN 2: Setup semua state yang diperlukan
            indicatorOrders.add(martingaleIndicatorOrder)
            onIndicatorOrdersUpdate(indicatorOrders.toList())

            val pendingInfo = PendingIndicatorTrade(
                indicatorOrderId = martingaleIndicatorOrder.id,
                amount = amount,
                trend = lastTrend,
                executionTime = System.currentTimeMillis(),
                triggerPrice = currentRealTimePrice,
                indicatorType = settings.type.name
            )
            pendingTradeResults[martingaleIndicatorOrder.id] = pendingInfo

            // PERBAIKAN 3: Set active trade untuk monitoring
            activeTradeOrderId = martingaleIndicatorOrder.id

            // PERBAIKAN 4: Register untuk continuous monitoring
            indicatorContinuousMonitor.addIndicatorOrderForMonitoring(
                indicatorOrderId = martingaleIndicatorOrder.id,
                trend = lastTrend,
                amount = amount,
                assetRic = asset.ric,
                isDemoAccount = isDemoAccount,
                indicatorType = settings.type.name,
                isMartingaleAttempt = true,
                martingaleStep = step
            )

            Log.d(TAG, "MARTINGALE IMMEDIATE EXECUTION (BYPASSING ALL QUEUES):")
            Log.d(TAG, "  Order ID: ${martingaleIndicatorOrder.id}")
            Log.d(TAG, "  Step: $step")
            Log.d(TAG, "  Trend: $lastTrend")
            Log.d(TAG, "  Amount: ${formatAmount(amount)}")
            Log.d(TAG, "  Execution: IMMEDIATE REALTIME (No boundary waiting)")
            Log.d(TAG, "  Timing: Uses current server time + realtime duration")
            Log.d(TAG, "  Differentiation: Regular orders use boundary, Martingale uses immediate")
            Log.d(TAG, "  Queue Status: COMPLETELY BYPASSED")
            Log.d(TAG, "  Prediction Queue: NOT USED")
            Log.d(TAG, "  Boundary Waiting: SKIPPED")

            // PERBAIKAN 5: IMMEDIATE EXECUTION - langsung call tanpa delay atau queue
            onExecuteIndicatorTrade(lastTrend, martingaleIndicatorOrder.id, amount)
            totalExecutions++

            // PERBAIKAN 6: Update martingale state
            activeMartingaleOrderId = martingaleIndicatorOrder.id
            isMartingalePendingExecution = false // Reset flag setelah eksekusi

            // PERBAIKAN 7: Start monitoring untuk result detection
            startResultMonitoring(martingaleIndicatorOrder.id)

            // PERBAIKAN 8: Update UI status untuk immediate execution
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Martingale Step $step EXECUTED IMMEDIATELY - ${formatAmount(amount)} - Ultra-fast monitoring ($accountType)")

            Log.d(TAG, "MARTINGALE IMMEDIATE EXECUTION COMPLETED")
            Log.d(TAG, "  TradeManager will receive: immediate execution request")
            Log.d(TAG, "  TradeManager forceOneMinute: false (realtime)")
            Log.d(TAG, "  Continuous Monitor: active for ultra-fast detection")
            Log.d(TAG, "  Next result: will trigger immediate next step or completion")

        } catch (e: Exception) {
            Log.e(TAG, "Error executing immediate martingale: ${e.message}", e)
            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Error executing immediate martingale step ($accountType): ${e.message}")

            // PERBAIKAN 9: Cleanup pada error
            isMartingalePendingExecution = false
            activeMartingaleOrderId = null

            // Handle failure gracefully
            scope.launch {
                val martingaleOrder = currentMartingaleOrder
                if (martingaleOrder != null) {
                    handleMartingaleFailure(martingaleOrder.totalLoss + amount, originalOrderId)
                }
            }
        }
    }

    private suspend fun handleMartingaleFailure(totalLoss: Long, indicatorOrderId: String) {
        val martingaleOrder = currentMartingaleOrder
        val finalStep = martingaleOrder?.currentStep ?: 0

        val result = IndicatorMartingaleResult(
            isWin = false,
            step = finalStep,
            amount = 0L,
            totalLoss = totalLoss,
            message = "Martingale failed - Max step reached",
            isMaxReached = true,
            indicatorOrderId = indicatorOrderId
        )

        stopCurrentMartingaleCompletely()
        onIndicatorMartingaleResult(result)

        indicatorContinuousMonitor.handleMartingaleCompletion(
            isWin = false,
            step = finalStep,
            isMaxReached = true
        )

        val accountType = if (isDemoAccount) "Demo" else "Real"
        onModeStatusUpdate("Martingale failed - Will auto-restart ($accountType)")

        if (shouldStopDueToConsecutiveLosses()) {
            return
        }
    }

    private fun stopCurrentMartingaleCompletely() {
        currentMartingaleOrder = null
        activeMartingaleOrderId = null
        isMartingalePendingExecution = false

        executionDelayJob?.cancel()
        executionDelayJob = null
        queuedExecutions.clear()
        isWaitingForExecutionBoundary = false

        Log.d(TAG, "Martingale stopped COMPLETELY with all pending executions cancelled")
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isIndicatorModeActive) return

        indicatorContinuousMonitor.handleWebSocketTradeUpdate(message)

        scope.launch {
            try {
                val event = message.optString("event", "")
                val payload = message.optJSONObject("payload")

                when (event) {
                    "closed", "deal_result", "trade_update" -> {
                        handleTradeStatusUpdate(payload, event)
                    }
                    "balance_changed" -> {
                        handleBalanceChanged(payload)
                    }
                    "opened" -> {
                        handleTradeOpened(payload)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing WebSocket update: ${e.message}", e)
            }
        }
    }

    private suspend fun handleTradeStatusUpdate(payload: JSONObject?, eventType: String) {
        if (payload == null) return

        val tradeId = payload.optString("id", "")
        val uuid = payload.optString("uuid", "")
        val status = payload.optString("status", "")
        val amount = payload.optLong("amount", 0L)

        if (tradeId.isEmpty() && uuid.isEmpty()) return

        val normalizedStatus = status.lowercase()
        val isWin = listOf("win", "won", "profit").any { normalizedStatus.contains(it) }
        val isLose = listOf("lose", "lost", "loses", "losing", "failed").any { normalizedStatus.contains(it) }

        if (isWin || isLose) {
            val matchingOrderId = findMatchingPendingIndicatorOrder(amount, tradeId, uuid)
            if (matchingOrderId != null) {
                val winAmount = payload.optLong("win", 0L)
                val details = mapOf(
                    "trade_id" to (if (uuid.isNotEmpty()) uuid else tradeId),
                    "amount" to amount,
                    "status" to status,
                    "win_amount" to winAmount,
                    "detection_method" to "websocket_$eventType"
                )

                Log.d(TAG, "Trade result detected via $eventType: ${if (isWin) "WIN" else "LOSE"}")
                handleIndicatorTradeResult(matchingOrderId, isWin, details)
            }
        }
    }

    private suspend fun handleBalanceChanged(payload: JSONObject?) {
        // Balance monitoring is handled by IndicatorContinuousMonitor
    }

    private suspend fun handleTradeOpened(payload: JSONObject?) {
        if (payload == null) return
        val uuid = payload.optString("uuid", "")
        val amount = payload.optLong("amount", 0L)

        val matchingOrderId = findMatchingPendingIndicatorOrder(amount, "", uuid)
        if (matchingOrderId != null && uuid.isNotEmpty()) {
            pendingTradeResults[matchingOrderId] = pendingTradeResults[matchingOrderId]?.copy(
                tradeUuid = uuid
            ) ?: return
        }
    }

    private fun findMatchingPendingIndicatorOrder(amount: Long, tradeId: String, uuid: String): String? {
        val currentTime = System.currentTimeMillis()
        return pendingTradeResults.entries.find { (orderId, info) ->
            val timeMatch = currentTime - info.executionTime < 180000L
            val amountMatch = amount == 0L || info.amount == amount
            val uuidMatch = uuid.isEmpty() || info.tradeUuid == uuid
            val idMatch = tradeId.isEmpty() || info.tradeUuid == tradeId
            timeMatch && amountMatch && (uuidMatch || idMatch)
        }?.key
    }

    fun stopIndicatorMode(): Result<String> {
        if (!isIndicatorModeActive) {
            return Result.failure(Exception("Indicator Order mode not active"))
        }

        return try {
            isAutoRestartEnabled = false
            consecutiveRestarts = 0

            isIndicatorModeActive = false
            waitingForMinuteBoundary = false
            isWaitingForExecutionBoundary = false

            indicatorContinuousMonitor.stopMonitoring()

            minuteBoundaryJob?.cancel()
            predictionMonitoringJob?.cancel()
            executionDelayJob?.cancel()

            currentMartingaleOrder = null
            activeMartingaleOrderId = null
            isMartingalePendingExecution = false
            pendingTradeResults.clear()
            queuedExecutions.clear()

            historicalCandles.clear()
            analysisResult = null
            pricePredictions.clear()
            currentRealTimePrice = ZERO
            lastPriceUpdateTime = 0L
            dataCollectionStartTime = 0L
            targetMinuteBoundary = 0L
            nextExecutionBoundary = 0L

            resetTradeStateCompletely()

            val accountType = if (isDemoAccount) "Demo" else "Real"
            onModeStatusUpdate("Indicator Order manually stopped ($accountType)")

            Log.d(TAG, "Indicator Order mode manually stopped")

            Result.success("Indicator Order mode successfully stopped")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to stop Indicator Order: ${e.message}"))
        }
    }

    private fun shouldStopDueToConsecutiveLosses(): Boolean {
        return maxConsecutiveLossesEnabled && consecutiveLosses >= maxConsecutiveLossesCount
    }

    private fun hasPendingTrades(): Boolean {
        val currentTime = System.currentTimeMillis()
        val expiredTrades = pendingTradeResults.filter { (_, info) ->
            currentTime - info.executionTime > 300000L
        }
        expiredTrades.keys.forEach { orderId ->
            Log.w(TAG, "Cleaning up expired pending indicator trade: $orderId")
            pendingTradeResults.remove(orderId)
        }
        return pendingTradeResults.isNotEmpty()
    }

    private fun findLastTrend(): String = indicatorOrders.lastOrNull()?.trend ?: "call"

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
        Log.d(TAG, "Server time offset updated: $offset ms")
    }

    fun isActive(): Boolean = isIndicatorModeActive
    fun getCurrentMartingaleStep(): Int = currentMartingaleOrder?.currentStep ?: 0
    fun isMartingaleActive(): Boolean = activeMartingaleOrderId != null
    fun getIndicatorOrders(): List<IndicatorOrder> = indicatorOrders.toList()
    fun getCurrentSettings(): IndicatorSettings? = currentSettings

    fun getSupportResistanceLevels(): Pair<Double, Double> {
        val predictions = pricePredictions.filter { !it.isTriggered && !it.isDisabled }
        if (predictions.isEmpty()) return Pair(0.0, 0.0)

        val supportPredictions = predictions.filter { it.recommendedTrend == "call" }
        val resistancePredictions = predictions.filter { it.recommendedTrend == "put" }

        val support = supportPredictions.minByOrNull { it.targetPrice }?.targetPrice?.toDouble() ?: 0.0
        val resistance = resistancePredictions.maxByOrNull { it.targetPrice }?.targetPrice?.toDouble() ?: 0.0

        return Pair(support, resistance)
    }

    fun getLastIndicatorValues(): IndicatorValues? {
        val analysis = analysisResult ?: return null
        return IndicatorValues(
            primaryValue = analysis.finalIndicatorValue,
            secondaryValue = null,
            trend = analysis.trend,
            strength = analysis.strength
        )
    }

    fun getPerformanceStats(): Map<String, Any> {
        val successRate = if (totalExecutions > 0) {
            (totalWins.toDouble() / totalExecutions * 100)
        } else 0.0

        val accountType = if (isDemoAccount) "demo" else "real"
        val monitoringStats = if (::indicatorContinuousMonitor.isInitialized) {
            indicatorContinuousMonitor.getMonitoringStats()
        } else {
            emptyMap()
        }

        return mapOf(
            "is_active" to isIndicatorModeActive,
            "account_type" to accountType,
            "execution_mode" to "ENHANCED_AUTO_RESTART_ULTRA_FAST",
            "monitoring_speed" to "SAME_AS_FOLLOW_ORDER",
            "trade_monitoring" to if (isMonitoringActiveTradeOnly) "ACTIVE_TRADE_ONLY" else "PREDICTIONS",
            "active_trade_id" to (activeTradeOrderId ?: "NONE"),
            "auto_restart_enabled" to isAutoRestartEnabled,
            "consecutive_restarts" to consecutiveRestarts,
            "max_restarts" to maxConsecutiveRestarts,
            "predictions_total" to pricePredictions.size,
            "predictions_triggered" to pricePredictions.count { it.isTriggered },
            "predictions_disabled" to pricePredictions.count { it.isDisabled },
            "total_executions" to totalExecutions,
            "total_wins" to totalWins,
            "total_losses" to totalLosses,
            "success_rate" to String.format("%.1f%%", successRate),
            "current_martingale_step" to getCurrentMartingaleStep(),
            "martingale_active" to isMartingaleActive(),
            "current_price" to currentRealTimePrice.toPlainString(),
            "indicator_type" to (currentSettings?.type?.name ?: "NONE"),
            "execution_method" to "ENHANCED_ULTRA_FAST_AUTO_RESTART",
            "detection_speed" to "50ms_ultra_fast",
            "restart_capability" to "ENABLED",
            "continuous_monitor" to monitoringStats
        )
    }

    fun getModeStatus(): String {
        val accountType = if (isDemoAccount) "Demo" else "Real"
        val restartInfo = if (consecutiveRestarts > 0) " (Cycle #${consecutiveRestarts + 1})" else ""

        return when {
            !isIndicatorModeActive -> "Indicator Order not active"
            waitingForMinuteBoundary -> {
                val waitTime = (targetMinuteBoundary - getCurrentServerTime()) / 1000
                val boundaryTime = displayTimeFormat.format(Date(targetMinuteBoundary))
                "Waiting minute boundary $boundaryTime (${waitTime}s)$restartInfo ($accountType)"
            }
            historicalCandles.isEmpty() -> "Collecting data$restartInfo ($accountType)"
            analysisResult == null -> "Analyzing ${historicalCandles.size} candles$restartInfo ($accountType)"
            pricePredictions.isEmpty() -> "Creating predictions$restartInfo ($accountType)"
            isMartingaleActive() -> {
                val currentStep = getCurrentMartingaleStep()
                "Executing/Monitoring Martingale Step $currentStep$restartInfo ($accountType)"
            }
            isMonitoringActiveTradeOnly -> {
                val activeOrder = activeTradeOrderId?.let { id -> indicatorOrders.find { it.id == id } }
                if (activeOrder != null) {
                    "Monitoring Trade ${activeOrder.getTrendDisplay()}$restartInfo ($accountType)"
                } else {
                    "Monitoring active trade$restartInfo ($accountType)"
                }
            }
            hasPendingTrades() -> "Waiting trade result$restartInfo ($accountType)"
            isWaitingForExecutionBoundary -> {
                val waitTime = (nextExecutionBoundary - getCurrentServerTime()) / 1000
                val boundaryTime = displayTimeFormat.format(Date(nextExecutionBoundary))
                "Waiting execution boundary $boundaryTime (${waitTime}s)$restartInfo ($accountType)"
            }
            else -> {
                val active = pricePredictions.count { !it.isTriggered && !it.isDisabled }
                "Monitoring $active predictions$restartInfo ($accountType)"
            }
        }
    }

    fun getPredictionInfo(): Map<String, Any> {
        val triggeredPredictions = pricePredictions.count { it.isTriggered }
        val disabledPredictions = pricePredictions.count { it.isDisabled }
        val activePredictions = pricePredictions.count { !it.isTriggered && !it.isDisabled }

        return mapOf(
            "total_predictions" to pricePredictions.size,
            "triggered_predictions" to triggeredPredictions,
            "disabled_predictions" to disabledPredictions,
            "active_predictions" to activePredictions,
            "queued_executions" to queuedExecutions.size,
            "waiting_for_execution_boundary" to isWaitingForExecutionBoundary,
            "monitoring_mode" to if (isMonitoringActiveTradeOnly) "ACTIVE_TRADE_ONLY" else "PREDICTIONS",
            "active_trade_id" to (activeTradeOrderId ?: "NONE"),
            "execution_mode" to "SINGLE_PREDICTION_FOCUS",
            "predictions" to pricePredictions.map { prediction ->
                mapOf(
                    "type" to prediction.predictionType,
                    "target_price" to prediction.targetPrice.toPlainString(),
                    "recommended_trend" to prediction.recommendedTrend,
                    "confidence" to "${prediction.confidence.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toPlainString()}%",
                    "is_triggered" to prediction.isTriggered,
                    "is_disabled" to prediction.isDisabled,
                    "status" to when {
                        prediction.isTriggered -> "EXECUTED"
                        prediction.isDisabled -> "DISABLED"
                        else -> "ACTIVE"
                    },
                    "triggered_at" to if (prediction.triggeredAt > 0) {
                        displayTimeFormat.format(Date(prediction.triggeredAt))
                    } else "N/A",
                    "distance_from_current" to if (currentRealTimePrice > ZERO) {
                        val distance = (prediction.targetPrice - currentRealTimePrice).abs()
                        val percentage = distance.divide(currentRealTimePrice, MATH_CONTEXT).multiply(ONE_HUNDRED, MATH_CONTEXT)
                        "${percentage.setScale(2, RoundingMode.HALF_UP).toPlainString()}%"
                    } else "N/A"
                )
            }
        )
    }

    fun cleanup() {
        isAutoRestartEnabled = false
        stopIndicatorMode()

        if (::indicatorContinuousMonitor.isInitialized) {
            indicatorContinuousMonitor.cleanup()
        }

        minuteBoundaryJob?.cancel()
        predictionMonitoringJob?.cancel()
        executionDelayJob?.cancel()
        Log.d(TAG, "Indicator Order Manager cleaned up with auto-restart disabled")
    }
}