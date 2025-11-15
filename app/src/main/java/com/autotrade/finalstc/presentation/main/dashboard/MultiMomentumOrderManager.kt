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

class MultiMomentumOrderManager(
    private val scope: CoroutineScope,
    private val onMultiMomentumOrdersUpdate: (List<MultiMomentumOrder>) -> Unit,
    private val onExecuteMultiMomentumTrade: (String, String, Long, String) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val getUserSession: suspend () -> UserSession?,
    private val serverTimeService: ServerTimeService?,
    private val onMultiMomentumMartingaleResult: (MultiMomentumMartingaleResult) -> Unit,
    private val tradeManager: TradeManager,
    private val onMultiMomentumTradeStatsUpdate: (tradeId: String, orderId: String, result: String) -> Unit
) {
    companion object {
        private const val TAG = "MultiMomentumManager"
        private const val MAX_CANDLES_STORAGE = 100
        private const val MIN_CANDLES_FOR_BB_SAR = 10
        private const val CANDLE_FETCH_INTERVAL = 60000L

        private const val CANDLES_5SEC_PER_MINUTE = 12
        private const val FETCH_5SEC_OFFSET = 300L

        private const val SIGNAL_COOLDOWN_MS = 3 * 60 * 1000L
        private const val PRICE_MOVE_THRESHOLD = 0.0003
        private const val MAX_SIGNALS_PER_HOUR = 10
        private const val SIGNAL_HISTORY_CLEANUP_MS = 60 * 60 * 1000L
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val priceApi = retrofit.create(PriceDataApi::class.java)

    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:00:00", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }
    private val displayTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    private var isModeActive = false
    private val executionMutex = Mutex()
    private var multiMomentumOrders = mutableListOf<MultiMomentumOrder>()
    private var serverTimeOffset = ServerTimeService.cachedServerTimeOffset

    private var candleStorageJob: Job? = null
    private val candleStorage = mutableListOf<Candle>()
    private var lastCandleFetchTime = 0L

    private var currentSelectedAsset: Asset? = null
    private var currentIsDemoAccount: Boolean = true
    private var currentMartingaleSettings: MartingaleState? = null

    private val activeMartingaleOrders = mutableMapOf<String, MultiMomentumMartingaleOrder>()
    private val pendingMartingaleExecutions = mutableSetOf<String>()

    private var totalExecutions = 0
    private var totalWins = 0
    private var totalLosses = 0

    private lateinit var multiMomentumContinuousMonitor: MultiMomentumOrderContinuousMonitor

    private data class SignalState(
        var lastSignal: String? = null,
        var lastSignalTime: Long = 0L,
        var lastPrice: Double? = null,
        var consecutiveSignals: Int = 0,
        val signalHistory: MutableList<Long> = mutableListOf()
    ) {
        fun shouldAllowSignal(
            currentSignal: String,
            currentPrice: Double,
            currentTime: Long
        ): Boolean {
            if (currentSignal == lastSignal) {
                if (currentTime - lastSignalTime < SIGNAL_COOLDOWN_MS) {
                    Log.d(TAG, "Signal sama dalam cooldown period")
                    return false
                }

                lastPrice?.let { prevPrice ->
                    val priceChange = kotlin.math.abs((currentPrice - prevPrice) / prevPrice)
                    if (priceChange < PRICE_MOVE_THRESHOLD) {
                        Log.d(TAG, "Price movement terlalu kecil: ${priceChange * 100}%")
                        return false
                    }
                }
            }

            cleanupOldSignals(currentTime)
            if (signalHistory.size >= MAX_SIGNALS_PER_HOUR) {
                Log.d(TAG, "Rate limit tercapai: ${signalHistory.size} signals dalam 1 jam")
                return false
            }

            return true
        }

        fun recordSignal(signal: String, price: Double, time: Long) {
            lastSignal = signal
            lastSignalTime = time
            lastPrice = price
            consecutiveSignals++
            signalHistory.add(time)

            Log.d(TAG, "Signal recorded: $signal @ $price (consecutive: $consecutiveSignals)")
        }

        fun reset() {
            lastSignal = null
            lastSignalTime = 0L
            lastPrice = null
            consecutiveSignals = 0
            signalHistory.clear()
        }

        private fun cleanupOldSignals(currentTime: Long) {
            signalHistory.removeAll { time ->
                currentTime - time > SIGNAL_HISTORY_CLEANUP_MS
            }
        }

        fun getDebugInfo(): Map<String, Any> {
            return mapOf(
                "last_signal" to (lastSignal ?: "none"),
                "last_time" to if (lastSignalTime > 0) Date(lastSignalTime).toString() else "never",
                "last_price" to (lastPrice ?: 0.0),
                "consecutive" to consecutiveSignals,
                "signals_in_hour" to signalHistory.size
            )
        }
    }

    private data class MomentumStates(
        val candleSabit: SignalState = SignalState(),
        val dojiTerjepit: SignalState = SignalState(),
        val dojiPembatalan: SignalState = SignalState(),
        val bbSarBreak: SignalState = SignalState()
    ) {
        fun reset() {
            candleSabit.reset()
            dojiTerjepit.reset()
            dojiPembatalan.reset()
            bbSarBreak.reset()
        }

        fun getDebugInfo(): Map<String, Map<String, Any>> {
            return mapOf(
                "candle_sabit" to candleSabit.getDebugInfo(),
                "doji_terjepit" to dojiTerjepit.getDebugInfo(),
                "doji_pembatalan" to dojiPembatalan.getDebugInfo(),
                "bb_sar_break" to bbSarBreak.getDebugInfo()
            )
        }
    }

    private val momentumStates = MomentumStates()

    private fun getCurrentServerTime(): Long {
        return serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + serverTimeOffset)
    }

    private fun getCurrentUTCTime(): Date {
        return Date(getCurrentServerTime())
    }

    private fun initializeUltraFastMonitoring() {
        multiMomentumContinuousMonitor = MultiMomentumOrderContinuousMonitor(
            scope = scope,
            getUserSession = getUserSession,
            onMultiMomentumTradeResult = { orderId, isWin, details ->
                handleMultiMomentumTradeResult(orderId, isWin, details)
            },
            serverTimeService = serverTimeService
        )

        Log.d(TAG, "Ultra-fast monitoring initialized for Multi-Momentum mode")
    }

    fun startMultiMomentumMode(
        selectedAsset: Asset,
        isDemoAccount: Boolean,
        martingaleSettings: MartingaleState
    ): Result<String> {
        if (isModeActive) {
            return Result.failure(Exception("Multi-Momentum mode sudah aktif"))
        }

        return try {
            initializeUltraFastMonitoring()

            currentSelectedAsset = selectedAsset
            currentIsDemoAccount = isDemoAccount
            currentMartingaleSettings = martingaleSettings

            isModeActive = true
            resetStatistics()
            candleStorage.clear()
            activeMartingaleOrders.clear()
            pendingMartingaleExecutions.clear()

            momentumStates.reset()

            val accountType = if (isDemoAccount) "Demo" else "Real"
            Log.d(TAG, "=== MULTI-MOMENTUM MODE: Starting ===")
            Log.d(TAG, "Asset: ${selectedAsset.name} (${selectedAsset.ric})")
            Log.d(TAG, "Account: $accountType")
            Log.d(TAG, "Strategy: 5-second candles -> 1-minute aggregation")
            Log.d(TAG, "4 Momentum: Candle Sabit, Doji Terjepit, Doji Pembatalan, BB/SAR Break")
            Log.d(TAG, "ANTI OVER-TRADING: Enabled")
            Log.d(TAG, "   - Signal cooldown: ${SIGNAL_COOLDOWN_MS / 1000}s")
            Log.d(TAG, "   - Price threshold: ${PRICE_MOVE_THRESHOLD * 100}%")
            Log.d(TAG, "   - Max signals/hour: $MAX_SIGNALS_PER_HOUR")

            multiMomentumContinuousMonitor.startMonitoring()
            startCandleStorageLoop()

            onModeStatusUpdate("Multi-Momentum aktif - 4 momentum paralel dengan anti over-trading ($accountType)")

            Result.success("Multi-Momentum mode dimulai untuk ${selectedAsset.name} ($accountType)")
        } catch (e: Exception) {
            isModeActive = false
            Result.failure(Exception("Gagal memulai Multi-Momentum: ${e.message}"))
        }
    }

    fun stopMultiMomentumMode(): Result<String> {
        if (!isModeActive) {
            return Result.failure(Exception("Multi-Momentum mode tidak aktif"))
        }

        return try {
            isModeActive = false

            candleStorageJob?.cancel()
            candleStorageJob = null

            if (::multiMomentumContinuousMonitor.isInitialized) {
                multiMomentumContinuousMonitor.stopMonitoring()
            }

            candleStorage.clear()
            activeMartingaleOrders.clear()
            pendingMartingaleExecutions.clear()
            currentSelectedAsset = null
            currentMartingaleSettings = null

            momentumStates.reset()

            onModeStatusUpdate("Multi-Momentum dihentikan")

            Log.d(TAG, "Multi-Momentum mode dihentikan")
            Log.d(TAG, "Total executions: $totalExecutions (Win: $totalWins, Loss: $totalLosses)")

            Result.success("Multi-Momentum mode berhasil dihentikan")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghentikan Multi-Momentum: ${e.message}"))
        }
    }

    private fun startCandleStorageLoop() {
        candleStorageJob = scope.launch {
            while (isModeActive) {
                try {
                    val serverNow = getCurrentServerTime()
                    val nextMinuteStart = calculateNextMinuteStart(serverNow)
                    val waitTime = nextMinuteStart - serverNow

                    if (waitTime > 0) {
                        Log.d(TAG, "Waiting ${waitTime}ms until next minute for candle fetch")
                        delay(waitTime)
                    }

                    if (!isModeActive) break

                    delay(FETCH_5SEC_OFFSET)

                    Log.d(TAG, "Fetching and aggregating 5-second candles...")

                    val newCandle = fetchAndAggregateOneMinuteCandle()

                    if (newCandle != null) {
                        addCandleToStorage(newCandle)

                        if (candleStorage.size >= 2) {
                            analyzeAllMomentums()
                        } else {
                            Log.d(TAG, "Need at least 2 candles (current: ${candleStorage.size})")
                        }
                    } else {
                        Log.w(TAG, "Failed to fetch/aggregate candle data")
                    }

                } catch (ce: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in candle storage loop: ${e.message}", e)
                    delay(5000L)
                }
            }
        }
    }

    private fun calculateNextMinuteStart(serverTime: Long): Long {
        val seconds = (serverTime / 1000) % 60
        val millis = serverTime % 1000
        return serverTime + ((60 - seconds) * 1000) - millis
    }

    private suspend fun fetchAndAggregateOneMinuteCandle(): Candle? {
        val selectedAsset = currentSelectedAsset ?: return null

        return try {
            val userSession = getUserSession() ?: return null
            val currentUTC = getCurrentUTCTime()
            val dateForApi = apiDateFormat.format(currentUTC)
            val encodedSymbol = selectedAsset.ric.replace("/", "%2F")

            val response = priceApi.getLastCandle(
                symbol = encodedSymbol,
                date = dateForApi,
                locale = "id",
                authToken = userSession.authtoken,
                deviceId = userSession.deviceId,
                deviceType = userSession.deviceType,
                userAgent = userSession.userAgent ?: "Mozilla/5.0"
            )

            if (response.isSuccessful && response.body() != null) {
                val candleResponse = response.body()!!
                val candles5Sec = parseCandleResponse(candleResponse)

                if (candles5Sec.isEmpty()) {
                    Log.w(TAG, "No 5-second candles returned from API")
                    return null
                }

                val last12Candles = candles5Sec.takeLast(CANDLES_5SEC_PER_MINUTE)

                if (last12Candles.size < CANDLES_5SEC_PER_MINUTE) {
                    Log.w(TAG, "Only ${last12Candles.size} candles available")
                }

                val aggregatedCandle = aggregateCandlesToOneMinute(last12Candles)

                if (aggregatedCandle != null) {
                    val bodySize = (aggregatedCandle.close - aggregatedCandle.open).abs()
                    val range = aggregatedCandle.high - aggregatedCandle.low

                    Log.d(TAG, "Successfully aggregated 1-minute candle:")
                    Log.d(TAG, "   Open: ${aggregatedCandle.open.toPlainString()}")
                    Log.d(TAG, "   Close: ${aggregatedCandle.close.toPlainString()}")
                    Log.d(TAG, "   Body Size: ${bodySize.toPlainString()}")
                    Log.d(TAG, "   Trend: ${aggregatedCandle.getTrend()}")
                }

                aggregatedCandle
            } else {
                Log.w(TAG, "API response unsuccessful: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching 5-second candles: ${e.message}", e)
            null
        }
    }

    private fun aggregateCandlesToOneMinute(candles5Sec: List<Candle>): Candle? {
        if (candles5Sec.isEmpty()) return null

        return try {
            val open = candles5Sec.first().open
            val close = candles5Sec.last().close
            val high = candles5Sec.maxOfOrNull { it.high } ?: candles5Sec.first().high
            val low = candles5Sec.minOfOrNull { it.low } ?: candles5Sec.first().low
            val createdAt = candles5Sec.last().createdAt

            val aggregated = Candle(
                open = open,
                close = close,
                high = high,
                low = low,
                createdAt = createdAt
            )

            if (!aggregated.isValidCandle()) {
                Log.w(TAG, "Aggregated candle validation failed")
                return null
            }

            aggregated
        } catch (e: Exception) {
            Log.e(TAG, "Error aggregating candles: ${e.message}", e)
            null
        }
    }

    private fun parseCandleResponse(response: CandleApiResponse): List<Candle> {
        return try {
            val candles = response.data.mapNotNull { candleData ->
                try {
                    val candle = Candle(
                        open = BigDecimal(candleData.open),
                        close = BigDecimal(candleData.close),
                        high = BigDecimal(candleData.high),
                        low = BigDecimal(candleData.low),
                        createdAt = candleData.created_at
                    )
                    if (candle.isValidCandle()) candle else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt }

            candles
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing candle response: ${e.message}", e)
            emptyList()
        }
    }

    private fun addCandleToStorage(candle: Candle) {
        candleStorage.add(candle)
        if (candleStorage.size > MAX_CANDLES_STORAGE) {
            candleStorage.removeAt(0)
        }

        val bodySize = (candle.close - candle.open).abs()
        val range = candle.high - candle.low

        val timeStr = displayTimeFormat.format(Date(getCurrentServerTime()))
        Log.d(TAG, "Candle added at $timeStr")
        Log.d(TAG, "   Storage: ${candleStorage.size}/$MAX_CANDLES_STORAGE")
        Log.d(TAG, "   Body: ${bodySize.toPlainString()}, Range: ${range.toPlainString()}")
    }

    private suspend fun analyzeAllMomentums() {
        if (candleStorage.size < 2) {
            Log.d(TAG, "Not enough candles for analysis")
            return
        }

        Log.d(TAG, "=== ANALYZING ALL 4 MOMENTUMS (WITH FILTERS) ===")
        Log.d(TAG, "Available candles: ${candleStorage.size}")

        val signals = mutableListOf<MomentumSignal>()

        analyzeCandleSabit()?.let {
            signals.add(it)
            Log.d(TAG, "Signal detected: CANDLE_SABIT (${it.trend})")
        }

        analyzeDojiTerjepit()?.let {
            signals.add(it)
            Log.d(TAG, "Signal detected: DOJI_TERJEPIT (${it.trend})")
        }

        analyzeDojiPembatalan()?.let {
            signals.add(it)
            Log.d(TAG, "Signal detected: DOJI_PEMBATALAN (${it.trend})")
        }

        if (candleStorage.size >= MIN_CANDLES_FOR_BB_SAR) {
            analyzeBBSARBreak()?.let {
                signals.add(it)
                Log.d(TAG, "Signal detected: BB_SAR_BREAK (${it.trend})")
            }
        }

        Log.d(TAG, "Total valid signals: ${signals.size}")

        if (signals.isNotEmpty()) {
            signals.forEach { signal ->
                executeMultiMomentumOrder(signal)
            }
        } else {
            Log.d(TAG, "No valid signals (filters applied)")
        }

        Log.d(TAG, "=================================")
    }

    private fun analyzeCandleSabit(): MomentumSignal? {
        if (candleStorage.size < 4) return null

        val last4 = candleStorage.takeLast(4)
        val candle4 = last4[3]

        val trend2 = last4[1].getTrend()
        val trend3 = last4[2].getTrend()
        val trend4 = candle4.getTrend()

        if (trend2 != trend3 || trend3 != trend4) return null

        val body2 = (last4[1].close - last4[1].open).abs()
        val body3 = (last4[2].close - last4[2].open).abs()
        val body4 = (candle4.close - candle4.open).abs()

        if (body2 < body3 && body3 < body4) {
            val signalTrend = if (trend2 == "buy") "call" else "put"
            val currentPrice = candle4.close.toDouble()
            val currentTime = getCurrentServerTime()

            if (!momentumStates.candleSabit.shouldAllowSignal(signalTrend, currentPrice, currentTime)) {
                Log.d(TAG, "CANDLE_SABIT: Signal filtered out")
                return null
            }

            momentumStates.candleSabit.recordSignal(signalTrend, currentPrice, currentTime)

            Log.d(TAG, "MOMENTUM 1 (Candle Sabit): VALID Signal $signalTrend")

            return MomentumSignal(
                momentumType = "CANDLE_SABIT",
                trend = signalTrend,
                confidence = calculateConfidenceBigDecimal(body2, body3, body4),
                details = "Candle Sabit: 4 candles increasing body size"
            )
        }

        return null
    }

    private fun analyzeDojiTerjepit(): MomentumSignal? {
        if (candleStorage.size < 4) return null

        val last4 = candleStorage.takeLast(4)
        val candle4 = last4[3]

        val trend1 = last4[0].getTrend()
        val trend2 = last4[1].getTrend()
        val trend3 = last4[2].getTrend()

        if (trend1 != trend2 || trend2 != trend3) return null

        val body1Pct = calculateBodyPercentageBigDecimal(last4[0])
        val body2Pct = calculateBodyPercentageBigDecimal(last4[1])
        val body3Pct = calculateBodyPercentageBigDecimal(last4[2])
        val body4Pct = calculateBodyPercentageBigDecimal(candle4)

        if (body1Pct > 60.0 && body2Pct > 60.0 && body3Pct > 60.0 && body4Pct < 10.0) {
            val trend4 = candle4.getTrend()

            val signalTrend = if (trend1 == "buy" && trend4 == "sell") {
                "call"
            } else if (trend1 == "sell" && trend4 == "buy") {
                "put"
            } else {
                return null
            }

            val currentPrice = candle4.close.toDouble()
            val currentTime = getCurrentServerTime()

            if (!momentumStates.dojiTerjepit.shouldAllowSignal(signalTrend, currentPrice, currentTime)) {
                Log.d(TAG, "DOJI_TERJEPIT: Signal filtered out")
                return null
            }

            momentumStates.dojiTerjepit.recordSignal(signalTrend, currentPrice, currentTime)

            Log.d(TAG, "MOMENTUM 2 (Doji Terjepit): VALID Signal $signalTrend")

            return MomentumSignal(
                momentumType = "DOJI_TERJEPIT",
                trend = signalTrend,
                confidence = 0.8,
                details = "Doji Terjepit: 3 long + 1 doji reversal hint"
            )
        }

        return null
    }

    private fun analyzeDojiPembatalan(): MomentumSignal? {
        if (candleStorage.size < 2) return null

        val last2 = candleStorage.takeLast(2)
        val previous = last2[0]
        val current = last2[1]

        val currentBodyPct = calculateBodyPercentageBigDecimal(current)

        if (currentBodyPct < 10.0) {
            val prevTrend = previous.getTrend()
            val dojiTrend = current.getTrend()

            val signalTrend = if (prevTrend == "sell" && dojiTrend == "buy") {
                "call"
            } else if (prevTrend == "buy" && dojiTrend == "sell") {
                "put"
            } else {
                return null
            }

            val currentPrice = current.close.toDouble()
            val currentTime = getCurrentServerTime()

            if (!momentumStates.dojiPembatalan.shouldAllowSignal(signalTrend, currentPrice, currentTime)) {
                Log.d(TAG, "DOJI_PEMBATALAN: Signal filtered out")
                return null
            }

            momentumStates.dojiPembatalan.recordSignal(signalTrend, currentPrice, currentTime)

            Log.d(TAG, "MOMENTUM 3 (Doji Pembatalan): VALID Signal $signalTrend")

            return MomentumSignal(
                momentumType = "DOJI_PEMBATALAN",
                trend = signalTrend,
                confidence = 0.75,
                details = "Doji Pembatalan: Reversal detected"
            )
        }

        return null
    }

    private fun analyzeBBSARBreak(): MomentumSignal? {
        if (candleStorage.size < MIN_CANDLES_FOR_BB_SAR) return null

        val lastCandle = candleStorage.last()
        val closePrice = lastCandle.close.toDouble()

        val bb = calculateBollingerBands(candleStorage, 20, 2.0)
        val sar = calculateParabolicSAR(candleStorage)

        if (bb == null) return null

        val currentSignal = when {
            closePrice > bb.upper || closePrice > sar -> "call"
            closePrice < bb.lower || closePrice < sar -> "put"
            else -> "neutral"
        }

        if (currentSignal == "neutral") {
            return null
        }

        val currentTime = getCurrentServerTime()

        if (!momentumStates.bbSarBreak.shouldAllowSignal(currentSignal, closePrice, currentTime)) {
            Log.d(TAG, "BB_SAR_BREAK: Signal filtered out")
            return null
        }

        momentumStates.bbSarBreak.recordSignal(currentSignal, closePrice, currentTime)

        Log.d(TAG, "MOMENTUM 4 (BB/SAR Break): VALID Signal $currentSignal")
        Log.d(TAG, "   Close: $closePrice, BB Upper: ${bb.upper}, BB Lower: ${bb.lower}, SAR: $sar")

        return MomentumSignal(
            momentumType = "BB_SAR_BREAK",
            trend = currentSignal,
            confidence = 0.85,
            details = "BB/SAR Break: Strong trend with filters passed"
        )
    }

    private fun calculateBodyPercentageBigDecimal(candle: Candle): Double {
        val range = (candle.high - candle.low).abs()
        if (range == BigDecimal.ZERO) return 0.0

        val body = (candle.close - candle.open).abs()
        return (body.divide(range, 10, RoundingMode.HALF_UP) * BigDecimal("100")).toDouble()
    }

    private fun calculateConfidenceBigDecimal(body2: BigDecimal, body3: BigDecimal, body4: BigDecimal): Double {
        if (body2 == BigDecimal.ZERO || body3 == BigDecimal.ZERO) return 0.5

        val ratio1 = body3.divide(body2, 10, RoundingMode.HALF_UP).toDouble()
        val ratio2 = body4.divide(body3, 10, RoundingMode.HALF_UP).toDouble()

        return kotlin.math.min(0.9, 0.5 + (ratio1 + ratio2) * 0.1)
    }

    private fun calculateBollingerBands(candles: List<Candle>, period: Int, stdDevMultiplier: Double): BollingerBands? {
        if (candles.size < period) return null

        val recentCandles = candles.takeLast(period)
        val closes = recentCandles.map { it.close.toDouble() }

        val sma = closes.average()
        val variance = closes.map { (it - sma) * (it - sma) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        return BollingerBands(
            upper = sma + (stdDev * stdDevMultiplier),
            middle = sma,
            lower = sma - (stdDev * stdDevMultiplier)
        )
    }

    private fun calculateParabolicSAR(candles: List<Candle>): Double {
        if (candles.size < 2) return candles.last().close.toDouble()

        val last = candles.last()
        val previous = candles[candles.size - 2]

        val isUptrend = last.close > previous.close
        val af = 0.02

        return if (isUptrend) {
            kotlin.math.min(last.low.toDouble(), previous.low.toDouble())
        } else {
            kotlin.math.max(last.high.toDouble(), previous.high.toDouble())
        }
    }

    private suspend fun executeMultiMomentumOrder(signal: MomentumSignal) {
        executionMutex.withLock {
            if (!isModeActive) return@withLock

            val selectedAsset = currentSelectedAsset ?: return@withLock
            val martingaleSettings = currentMartingaleSettings ?: return@withLock
            val accountType = if (currentIsDemoAccount) "Demo" else "Real"

            try {
                val currentServerTime = getCurrentServerTime()
                val orderId = UUID.randomUUID().toString()

                Log.d(TAG, "Executing ${signal.momentumType} order: ${signal.trend}")
                Log.d(TAG, "  Amount: ${formatAmount(martingaleSettings.baseAmount)}")
                Log.d(TAG, "  Confidence: ${signal.confidence}")

                val order = MultiMomentumOrder(
                    id = orderId,
                    assetRic = selectedAsset.ric,
                    assetName = selectedAsset.name,
                    trend = signal.trend,
                    amount = martingaleSettings.baseAmount,
                    executionTime = currentServerTime,
                    momentumType = signal.momentumType,
                    confidence = signal.confidence,
                    sourceCandle = candleStorage.last()
                )

                multiMomentumOrders.add(order)
                onMultiMomentumOrdersUpdate(multiMomentumOrders.toList())

                multiMomentumContinuousMonitor.addOrderForMonitoring(
                    orderId = orderId,
                    trend = signal.trend,
                    amount = martingaleSettings.baseAmount,
                    assetRic = selectedAsset.ric,
                    isDemoAccount = currentIsDemoAccount,
                    momentumType = signal.momentumType
                )

                onExecuteMultiMomentumTrade(signal.trend, orderId, martingaleSettings.baseAmount, signal.momentumType)
                totalExecutions++

                onModeStatusUpdate("${signal.momentumType}: ${signal.trend} - ${selectedAsset.name} ($accountType)")

                Log.d(TAG, "${signal.momentumType} order executed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error executing ${signal.momentumType} order: ${e.message}", e)
            }
        }
    }

    fun handleMultiMomentumTradeResult(orderId: String, isWin: Boolean, details: Map<String, Any>) {
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isModeActive) return
        if (::multiMomentumContinuousMonitor.isInitialized) {
            multiMomentumContinuousMonitor.handleWebSocketTradeUpdate(message)
        }
    }

    fun getPerformanceStats(): Map<String, Any> {
        val successRate = if (totalExecutions > 0) {
            (totalWins.toDouble() / totalExecutions * 100)
        } else 0.0

        val accountType = if (currentIsDemoAccount) "demo" else "real"
        val monitoringStats = if (::multiMomentumContinuousMonitor.isInitialized) {
            multiMomentumContinuousMonitor.getMonitoringStats()
        } else {
            emptyMap()
        }

        val recentCandles = candleStorage.takeLast(10).map { candle ->
            val bodySize = (candle.close - candle.open).abs()
            val range = candle.high - candle.low
            val priceChange = candle.close - candle.open

            mapOf(
                "open" to candle.open.toPlainString(),
                "high" to candle.high.toPlainString(),
                "low" to candle.low.toPlainString(),
                "close" to candle.close.toPlainString(),
                "trend" to candle.getTrend(),
                "body_size" to bodySize.setScale(8, RoundingMode.HALF_UP).toPlainString(),
                "range" to range.setScale(8, RoundingMode.HALF_UP).toPlainString(),
                "price_change" to priceChange.setScale(8, RoundingMode.HALF_UP).toPlainString(),
                "body_percentage" to String.format("%.2f%%", calculateBodyPercentageBigDecimal(candle)),
                "timestamp" to candle.createdAt,
                "formatted_time" to displayTimeFormat.format(Date(getCurrentServerTime()))
            )
        }

        return mapOf(
            "is_active" to isModeActive,
            "account_type" to accountType,
            "execution_mode" to "MULTI_MOMENTUM_4_PARALLEL",
            "data_source" to "5_SECOND_CANDLES_AGGREGATED_TO_1_MINUTE",
            "candle_count" to candleStorage.size,
            "total_executions" to totalExecutions,
            "total_wins" to totalWins,
            "total_losses" to totalLosses,
            "success_rate" to String.format("%.1f%%", successRate),
            "active_martingale_count" to activeMartingaleOrders.size,
            "orders_count" to multiMomentumOrders.size,
            "ultra_fast_monitoring" to monitoringStats,
            "momentum_types" to listOf(
                "CANDLE_SABIT",
                "DOJI_TERJEPIT",
                "DOJI_PEMBATALAN",
                "BB_SAR_BREAK"
            ),
            "min_candles_for_bb_sar" to MIN_CANDLES_FOR_BB_SAR,
            "recent_candles" to recentCandles,
            "candle_aggregation" to mapOf(
                "source_interval" to "5_SECONDS",
                "target_interval" to "1_MINUTE",
                "candles_per_minute" to CANDLES_5SEC_PER_MINUTE,
                "aggregation_method" to "OHLC_AGGREGATION",
                "bigdecimal_precision" to "ENABLED"
            ),
            "anti_overtrading" to mapOf(
                "enabled" to true,
                "signal_cooldown_ms" to SIGNAL_COOLDOWN_MS,
                "price_move_threshold" to PRICE_MOVE_THRESHOLD,
                "max_signals_per_hour" to MAX_SIGNALS_PER_HOUR,
                "momentum_states" to momentumStates.getDebugInfo()
            )
        )
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    private fun resetStatistics() {
        totalExecutions = 0
        totalWins = 0
        totalLosses = 0
        multiMomentumOrders.clear()
        onMultiMomentumOrdersUpdate(emptyList())
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
    }

    fun isActive(): Boolean = isModeActive
    fun getCandleCount(): Int = candleStorage.size
    fun getActiveMartingaleCount(): Int = activeMartingaleOrders.size
    fun getCandleHistory(): List<Candle> = candleStorage.toList()
    fun getLatestCandles(count: Int = 5): List<Candle> = candleStorage.takeLast(count)

    fun getModeStatus(): String {
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"
        val candleInfo = "Candles: ${candleStorage.size}/$MAX_CANDLES_STORAGE"
        val activeMartingales = activeMartingaleOrders.size

        return when {
            !isModeActive -> "Multi-Momentum tidak aktif"
            activeMartingales > 0 -> "Multi-Momentum - $activeMartingales martingale aktif - $candleInfo ($accountType)"
            candleStorage.size < MIN_CANDLES_FOR_BB_SAR -> {
                "Multi-Momentum - Collecting candles ($candleInfo) - 3 momentum aktif ($accountType)"
            }
            else -> "Multi-Momentum - 4 momentum aktif (with filters) - $candleInfo ($accountType)"
        }
    }

    fun cleanup() {
        stopMultiMomentumMode()
        candleStorageJob?.cancel()
        if (::multiMomentumContinuousMonitor.isInitialized) {
            multiMomentumContinuousMonitor.cleanup()
        }
        candleStorage.clear()
        multiMomentumOrders.clear()
        activeMartingaleOrders.clear()
        pendingMartingaleExecutions.clear()
        momentumStates.reset()
        resetStatistics()
    }
}

data class MomentumSignal(
    val momentumType: String,
    val trend: String,
    val confidence: Double,
    val details: String
)

data class BollingerBands(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

data class MultiMomentumMartingaleOrder(
    val originalOrderId: String,
    val momentumType: String,
    val currentStep: Int,
    val maxSteps: Int,
    val totalLoss: Long,
    val nextAmount: Long,
    val trend: String,
    val isActive: Boolean
)

data class MultiMomentumMartingaleResult(
    val isWin: Boolean,
    val step: Int,
    val amount: Long,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L,
    val message: String,
    val shouldContinue: Boolean = false,
    val isMaxReached: Boolean = false,
    val momentumType: String
)