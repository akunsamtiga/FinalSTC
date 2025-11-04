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
import kotlin.math.abs

class CTCOrderManager(
    private val scope: CoroutineScope,
    private val onCTCOrdersUpdate: (List<CTCOrder>) -> Unit,
    private val onExecuteCTCTrade: (String, String, Long, Boolean) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val getUserSession: suspend () -> UserSession?,
    private val serverTimeService: ServerTimeService?,
    private val onCTCMartingaleResult: (CTCMartingaleResult) -> Unit,
    private val tradeManager: TradeManager,
    private val onCTCTradeStatsUpdate: (tradeId: String, orderId: String, result: String) -> Unit
) {
    companion object {
        private const val TAG = "CTCOrderManager"

        private const val FIRST_FETCH_OFFSET_MS = 300L
        private const val SECOND_FETCH_OFFSET_MS = 300L

        private const val EXECUTION_WAIT_AFTER_ANALYSIS = 300L
        private const val EXECUTION_SYNC_TO_BOUNDARY = true
        private const val EXECUTION_MIN_ADVANCE_MS = 5000L

        private const val PRE_WARM_ADVANCE_MS = 5000L
        private const val NETWORK_BUFFER_MS = 200L
        private const val MAX_PRICE_FETCH_TIME = 5000L
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val priceApi = retrofit.create(PriceDataApi::class.java)

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

    private var isCTCModeActive = false
    private val executionMutex = Mutex()
    private var ctcOrders = mutableListOf<CTCOrder>()
    private var serverTimeOffset = ServerTimeService.cachedServerTimeOffset

    private var currentCycle = 0
    private var currentCycleTrend: String? = null
    private var isCycleInProgress = false
    private var cycleJob: Job? = null

    private var priceDataFirstMinute: List<Candle>? = null
    private var priceDataSecondMinute: List<Candle>? = null
    private var isWaitingForSecondMinute = false

    private var currentSelectedAsset: Asset? = null
    private var currentIsDemoAccount: Boolean = true
    private var currentMartingaleSettings: MartingaleState? = null

    private var currentMartingaleOrder: CTCMartingaleOrder? = null
    private var ctcMartingaleSettings: MartingaleState? = null
    private var activeMartingaleOrderId: String? = null
    private var isMartingalePendingExecution = false

    private var currentActiveTrend: String? = null

    private var totalExecutions = 0
    private var totalWins = 0
    private var totalLosses = 0
    private var consecutiveWins = 0
    private var consecutiveLosses = 0

    private var isApiPreWarmed = false
    private var preWarmJob: Job? = null
    private var lastSuccessfulPreWarm = 0L

    private lateinit var ctcOrderContinuousMonitor: CTCOrderContinuousMonitor

    private fun getCurrentServerTime(): Long {
        return serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + serverTimeOffset)
    }

    private fun getCurrentUTCTime(): Date {
        return Date(getCurrentServerTime())
    }

    private fun initializeUltraFastMonitoring() {
        ctcOrderContinuousMonitor = CTCOrderContinuousMonitor(
            scope = scope,
            getUserSession = getUserSession,
            onCTCTradeResult = { ctcOrderId, isWin, details ->
                handleCTCTradeResult(ctcOrderId, isWin, details)
            },
            serverTimeService = serverTimeService
        )

        Log.d(TAG, "Ultra-fast monitoring initialized for CTC Order mode")
    }

    fun startCTCMode(
        selectedAsset: Asset,
        isDemoAccount: Boolean,
        martingaleSettings: MartingaleState
    ): Result<String> {
        if (isCTCModeActive) {
            return Result.failure(Exception("CTC Order mode sudah aktif"))
        }

        return try {
            initializeUltraFastMonitoring()

            currentSelectedAsset = selectedAsset
            currentIsDemoAccount = isDemoAccount
            currentMartingaleSettings = martingaleSettings
            ctcMartingaleSettings = martingaleSettings

            isCTCModeActive = true
            isCycleInProgress = false
            currentCycle = 0
            currentCycleTrend = null
            currentActiveTrend = null
            resetStatistics()
            resetPriceData()

            val accountType = if (isDemoAccount) "Demo" else "Real"
            Log.d(TAG, "CTC MODE: Starting with FIXED timing and trend logic")
            Log.d(TAG, "Asset: ${selectedAsset.name} (${selectedAsset.ric})")
            Log.d(TAG, "Account: $accountType")
            Log.d(TAG, "Timing: SYNCED to 5-second boundaries")
            Log.d(TAG, "Logic: WIN -> SAME trend | LOSE -> REVERSE trend (martingale only)")

            ctcOrderContinuousMonitor.startMonitoring()
            startApiPreWarming()
            startNewCycleWithFixedTiming()

            onModeStatusUpdate("CTC Order ULTRA FAST aktif - Fixed Timing ($accountType)")

            Result.success("CTC Order mode dimulai dengan fixed timing untuk ${selectedAsset.name} ($accountType)")
        } catch (e: Exception) {
            isCTCModeActive = false
            Result.failure(Exception("Gagal memulai CTC Order: ${e.message}"))
        }
    }

    private fun startApiPreWarming() {
        preWarmJob?.cancel()
        preWarmJob = scope.launch {
            while (isCTCModeActive) {
                try {
                    val serverNow = getCurrentServerTime()
                    val nextMinuteStart = calculateNextMinuteStart(serverNow)
                    val preWarmTime = nextMinuteStart - PRE_WARM_ADVANCE_MS

                    val delayToPreWarm = preWarmTime - serverNow
                    if (delayToPreWarm > 0) {
                        delay(delayToPreWarm)
                    }

                    performApiPreWarming()
                    delay(30000L)

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Pre-warming error: ${e.message}", e)
                    delay(5000L)
                }
            }
        }
    }

    private suspend fun performApiPreWarming() {
        try {
            val userSession = getUserSession() ?: return
            val selectedAsset = currentSelectedAsset ?: return

            val currentUTC = getCurrentUTCTime()
            val dateForApi = apiDateFormat.format(currentUTC)
            val encodedSymbol = selectedAsset.ric.replace("/", "%2F")

            Log.d(TAG, "Pre-warming API for CTC mode")

            val response = priceApi.getLastCandle(
                symbol = encodedSymbol,
                date = dateForApi,
                locale = "id",
                authToken = userSession.authtoken,
                deviceId = userSession.deviceId,
                deviceType = userSession.deviceType,
                userAgent = userSession.userAgent ?: "Mozilla/5.0"
            )

            if (response.isSuccessful) {
                isApiPreWarmed = true
                lastSuccessfulPreWarm = getCurrentServerTime()
                Log.d(TAG, "CTC API pre-warmed successfully")
            } else {
                isApiPreWarmed = false
            }

        } catch (e: Exception) {
            isApiPreWarmed = false
            Log.e(TAG, "CTC Pre-warming error: ${e.message}", e)
        }
    }

    fun stopCTCMode(): Result<String> {
        if (!isCTCModeActive) {
            return Result.failure(Exception("CTC Order mode tidak aktif"))
        }

        return try {
            isCTCModeActive = false
            isCycleInProgress = false

            preWarmJob?.cancel()
            preWarmJob = null
            isApiPreWarmed = false

            if (::ctcOrderContinuousMonitor.isInitialized) {
                ctcOrderContinuousMonitor.stopMonitoring()
            }

            cycleJob?.cancel()
            cycleJob = null

            currentCycleTrend = null
            currentActiveTrend = null
            currentCycle = 0
            currentMartingaleOrder = null
            activeMartingaleOrderId = null
            ctcMartingaleSettings = null
            isMartingalePendingExecution = false
            currentSelectedAsset = null
            currentMartingaleSettings = null
            resetPriceData()

            onModeStatusUpdate("CTC Order dihentikan")

            Log.d(TAG, "CTC Order mode dihentikan")
            Log.d(TAG, "Total cycles: $currentCycle")
            Log.d(TAG, "Total executions: $totalExecutions (Win: $totalWins, Loss: $totalLosses)")

            Result.success("CTC Order mode berhasil dihentikan")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghentikan CTC Order: ${e.message}"))
        }
    }

    private fun startNewCycleWithFixedTiming() {
        cycleJob?.cancel()
        currentCycle++
        currentCycleTrend = null
        isCycleInProgress = true
        isWaitingForSecondMinute = false
        resetPriceData()

        cycleJob = scope.launch {
            try {
                val accountType = if (currentIsDemoAccount) "Demo" else "Real"
                Log.d(TAG, "CTC CYCLE $currentCycle: Starting with FIXED TIMING ($accountType)")

                val serverNow = getCurrentServerTime()
                val firstMinuteStart = calculateNextMinuteStart(serverNow)
                val waitToFirstMinute = firstMinuteStart - serverNow

                Log.d(TAG, "CTC CYCLE $currentCycle: Wait ${waitToFirstMinute}ms to first fetch")
                onModeStatusUpdate("CTC Order CYCLE $currentCycle: Wait ${waitToFirstMinute}ms ($accountType)")

                if (waitToFirstMinute > 0) {
                    delay(waitToFirstMinute)
                }

                if (!isCTCModeActive) return@launch

                delay(FIRST_FETCH_OFFSET_MS)

                Log.d(TAG, "CTC CYCLE $currentCycle: Performing FIRST fetch")
                priceDataFirstMinute = performPrecisePriceFetch("FIRST_MINUTE_00")

                if (priceDataFirstMinute == null) {
                    Log.w(TAG, "CTC CYCLE $currentCycle: First minute fetch failed")
                    delay(2000L)
                    if (isCTCModeActive) {
                        startNewCycleWithFixedTiming()
                    }
                    return@launch
                }

                Log.d(TAG, "CTC CYCLE $currentCycle: First minute OK, waiting for next")
                isWaitingForSecondMinute = true
                onModeStatusUpdate("CTC Order CYCLE $currentCycle: First minute OK ($accountType)")

                val secondMinuteStart = firstMinuteStart + 60000L
                val waitToSecondMinute = secondMinuteStart - getCurrentServerTime()

                Log.d(TAG, "CTC CYCLE $currentCycle: Wait ${waitToSecondMinute}ms to second minute")
                if (waitToSecondMinute > 0) {
                    delay(waitToSecondMinute)
                }

                if (!isCTCModeActive) return@launch

                delay(SECOND_FETCH_OFFSET_MS)

                Log.d(TAG, "CTC CYCLE $currentCycle: Performing SECOND fetch")
                priceDataSecondMinute = performPrecisePriceFetch("SECOND_MINUTE_00")

                if (priceDataSecondMinute == null) {
                    Log.w(TAG, "CTC CYCLE $currentCycle: Second minute fetch failed")
                    delay(2000L)
                    if (isCTCModeActive) {
                        startNewCycleWithFixedTiming()
                    }
                    return@launch
                }

                val trend = compareFullMinutesAndDetermineTrend()
                if (trend == null) {
                    Log.w(TAG, "CTC CYCLE $currentCycle: Comparison failed")
                    delay(2000L)
                    if (isCTCModeActive) {
                        startNewCycleWithFixedTiming()
                    }
                    return@launch
                }

                currentCycleTrend = trend
                currentActiveTrend = trend
                isWaitingForSecondMinute = false

                Log.d(TAG, "CTC CYCLE $currentCycle: Comparison complete - Trend: $trend")
                Log.d(TAG, "Active trend set to: $currentActiveTrend")
                onModeStatusUpdate("CTC Order CYCLE $currentCycle: Analysis complete - $trend ($accountType)")

                val executionTime = calculateOptimalExecutionTime()
                val waitForExecution = executionTime - getCurrentServerTime()

                if (waitForExecution > 0) {
                    Log.d(TAG, "CTC CYCLE $currentCycle: Waiting ${waitForExecution}ms for optimal execution time")
                    delay(waitForExecution)
                }

                if (!isCTCModeActive) return@launch

                Log.d(TAG, "CTC CYCLE $currentCycle: Executing at optimal time")
                executeFirstOrderWithFixedTiming(trend)

            } catch (ce: CancellationException) {
                Log.d(TAG, "CTC CYCLE $currentCycle: Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "CTC CYCLE $currentCycle: Error: ${e.message}", e)
                if (isCTCModeActive) {
                    delay(3000L)
                    startNewCycleWithFixedTiming()
                }
            }
        }
    }

    private fun calculateOptimalExecutionTime(): Long {
        val serverNow = getCurrentServerTime()
        val currentSeconds = (serverNow / 1000) % 60

        return if (EXECUTION_SYNC_TO_BOUNDARY) {
            val nextBoundary = when {
                currentSeconds % 5 == 0L -> 0L
                else -> 5L - (currentSeconds % 5)
            }

            val targetTime = serverNow + (nextBoundary * 1000)

            val secondsUntilMinuteEnd = 60 - ((targetTime / 1000) % 60)
            if (secondsUntilMinuteEnd * 1000 < EXECUTION_MIN_ADVANCE_MS) {
                targetTime + 60000L
            } else {
                targetTime
            }
        } else {
            serverNow + EXECUTION_WAIT_AFTER_ANALYSIS
        }
    }

    private fun calculateNextMinuteStart(serverTime: Long): Long {
        val seconds = (serverTime / 1000) % 60
        val millis = serverTime % 1000
        return serverTime + ((60 - seconds) * 1000) - millis
    }

    private suspend fun performPrecisePriceFetch(fetchType: String): List<Candle>? {
        val selectedAsset = currentSelectedAsset ?: return null

        return try {
            val startTime = getCurrentServerTime()
            val currentUTC = getCurrentUTCTime()
            val timeString = displayTimeFormat.format(currentUTC)

            Log.d(TAG, "CTC CYCLE $currentCycle: $fetchType at $timeString")

            val latestCandles = fetchPriceDataWithPreWarming(selectedAsset)
            if (latestCandles == null || latestCandles.size < 2) {
                Log.w(TAG, "CTC CYCLE $currentCycle: Insufficient data for $fetchType")
                return null
            }

            val fetchTime = getCurrentServerTime() - startTime
            Log.d(TAG, "CTC CYCLE $currentCycle: $fetchType completed in ${fetchTime}ms")

            latestCandles
        } catch (e: Exception) {
            Log.e(TAG, "CTC CYCLE $currentCycle: Error in $fetchType: ${e.message}", e)
            null
        }
    }

    private fun compareFullMinutesAndDetermineTrend(): String? {
        val dataFirstMinute = priceDataFirstMinute
        val dataSecondMinute = priceDataSecondMinute

        if (dataFirstMinute == null || dataSecondMinute == null) {
            return null
        }

        if (dataFirstMinute.isEmpty() || dataSecondMinute.isEmpty()) {
            return null
        }

        return try {
            val candleFirstMinute = dataFirstMinute.last()
            val candleSecondMinute = dataSecondMinute.last()

            val priceFirstMinute = candleFirstMinute.close
            val priceSecondMinute = candleSecondMinute.close

            val trend = if (priceSecondMinute > priceFirstMinute) "call" else "put"
            val priceChange = priceSecondMinute - priceFirstMinute

            Log.d(TAG, "CTC CYCLE $currentCycle: Minute comparison:")
            Log.d(TAG, "  Price first: $priceFirstMinute")
            Log.d(TAG, "  Price second: $priceSecondMinute")
            Log.d(TAG, "  Change: $priceChange")
            Log.d(TAG, "  Trend: $trend")

            trend
        } catch (e: Exception) {
            Log.e(TAG, "CTC CYCLE $currentCycle: Error comparing: ${e.message}", e)
            null
        }
    }

    private suspend fun executeFirstOrderWithFixedTiming(trend: String) {
        executionMutex.withLock {
            if (!isCTCModeActive || !isCycleInProgress) return@withLock

            val martingaleSettings = currentMartingaleSettings
            val selectedAsset = currentSelectedAsset

            if (martingaleSettings == null || selectedAsset == null) {
                return@withLock
            }

            val accountType = if (currentIsDemoAccount) "Demo" else "Real"

            try {
                val executionServerTime = getCurrentServerTime()
                val currentSeconds = (executionServerTime / 1000) % 60

                Log.d(TAG, "CTC CYCLE $currentCycle: Executing first order ($accountType)")
                Log.d(TAG, "  Trend: $trend")
                Log.d(TAG, "  Amount: ${formatAmount(martingaleSettings.baseAmount)}")
                Log.d(TAG, "  Execution second: $currentSeconds")
                Log.d(TAG, "  Active trend: $currentActiveTrend")

                val ctcOrder = CTCOrder(
                    id = UUID.randomUUID().toString(),
                    assetRic = selectedAsset.ric,
                    assetName = selectedAsset.name,
                    trend = trend,
                    amount = martingaleSettings.baseAmount,
                    executionTime = executionServerTime,
                    sourceCandle = createDummyCandle()
                )

                ctcOrders.add(ctcOrder)
                onCTCOrdersUpdate(ctcOrders.toList())

                ctcOrderContinuousMonitor.addCTCOrderForMonitoring(
                    ctcOrderId = ctcOrder.id,
                    trend = trend,
                    amount = martingaleSettings.baseAmount,
                    assetRic = selectedAsset.ric,
                    isDemoAccount = currentIsDemoAccount,
                    cycleNumber = currentCycle
                )

                onModeStatusUpdate("CTC Order CYCLE $currentCycle: Execution $trend INSTANT - ${selectedAsset.name} ($accountType)")

                onExecuteCTCTrade(trend, ctcOrder.id, martingaleSettings.baseAmount, false)
                totalExecutions++

                Log.d(TAG, "CTC CYCLE $currentCycle: Order executed with FIXED timing")

            } catch (e: Exception) {
                Log.e(TAG, "CTC CYCLE $currentCycle: Error executing: ${e.message}", e)
                delay(3000L)
                if (isCTCModeActive) {
                    startNewCycleWithFixedTiming()
                }
            }
        }
    }

    private suspend fun fetchPriceDataWithPreWarming(asset: Asset): List<Candle>? {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = getCurrentServerTime()

                if (!isApiPreWarmed || (startTime - lastSuccessfulPreWarm) > 30000L) {
                    performApiPreWarming()
                }

                val userSession = getUserSession() ?: return@withContext null
                val currentUTC = getCurrentUTCTime()

                val dateForApi = apiDateFormat.format(currentUTC)
                val encodedSymbol = asset.ric.replace("/", "%2F")

                val response = withTimeoutOrNull(MAX_PRICE_FETCH_TIME) {
                    priceApi.getLastCandle(
                        symbol = encodedSymbol,
                        date = dateForApi,
                        locale = "id",
                        authToken = userSession.authtoken,
                        deviceId = userSession.deviceId,
                        deviceType = userSession.deviceType,
                        userAgent = userSession.userAgent ?: "Mozilla/5.0"
                    )
                }

                if (response?.isSuccessful == true && response.body() != null) {
                    val candleResponse = response.body()!!
                    val candles = parseCandleResponse(candleResponse)
                    candles
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching price data: ${e.message}", e)
                null
            }
        }
    }

    private fun parseCandleResponse(response: CandleApiResponse): List<Candle> {
        return try {
            response.data.mapNotNull { candleData ->
                try {
                    val candle = Candle(
                        open = java.math.BigDecimal(candleData.open),
                        close = java.math.BigDecimal(candleData.close),
                        high = java.math.BigDecimal(candleData.high),
                        low = java.math.BigDecimal(candleData.low),
                        createdAt = candleData.created_at
                    )
                    if (candle.isValidCandle()) candle else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun resetPriceData() {
        priceDataFirstMinute = null
        priceDataSecondMinute = null
        isWaitingForSecondMinute = false
    }

    private fun getReverseTrend(originalTrend: String): String {
        return when (originalTrend.lowercase()) {
            "call", "buy" -> "put"
            "put", "sell" -> "call"
            else -> originalTrend
        }
    }

    fun executeImmediateOrderUltraFast() {
        scope.launch {
            try {
                delay(50L)

                if (!isCTCModeActive || !isCycleInProgress) return@launch

                val trend = currentActiveTrend ?: currentCycleTrend ?: return@launch

                executionMutex.withLock {
                    val selectedAsset = currentSelectedAsset ?: return@withLock
                    val martingaleSettings = currentMartingaleSettings ?: return@withLock
                    val accountType = if (currentIsDemoAccount) "Demo" else "Real"

                    try {
                        if (isMartingalePendingExecution && currentMartingaleOrder != null && activeMartingaleOrderId != null) {
                            Log.d(TAG, "Martingale ACTIVE - executing martingale step")
                            executeImmediateMartingaleUltraFast(selectedAsset, accountType, trend)
                            return@withLock
                        }

                        val currentServerTime = getCurrentServerTime()

                        Log.d(TAG, "Executing FRESH immediate order with trend: $trend")
                        Log.d(TAG, "   Martingale active: ${isMartingaleActive()}")
                        Log.d(TAG, "   Active martingale ID: $activeMartingaleOrderId")

                        val ctcOrder = CTCOrder(
                            id = UUID.randomUUID().toString(),
                            assetRic = selectedAsset.ric,
                            assetName = selectedAsset.name,
                            trend = trend,
                            amount = martingaleSettings.baseAmount,
                            executionTime = currentServerTime,
                            sourceCandle = createDummyCandle()
                        )

                        ctcOrders.add(ctcOrder)
                        onCTCOrdersUpdate(ctcOrders.toList())

                        ctcOrderContinuousMonitor.addCTCOrderForMonitoring(
                            ctcOrderId = ctcOrder.id,
                            trend = trend,
                            amount = martingaleSettings.baseAmount,
                            assetRic = selectedAsset.ric,
                            isDemoAccount = currentIsDemoAccount,
                            cycleNumber = currentCycle
                        )

                        onExecuteCTCTrade(trend, ctcOrder.id, martingaleSettings.baseAmount, false)
                        totalExecutions++

                        Log.d(TAG, "FRESH order executed: trend=$trend, amount=${formatAmount(martingaleSettings.baseAmount)}")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error in immediate execution: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in immediate job: ${e.message}", e)
            }
        }
    }

    private suspend fun executeImmediateMartingaleUltraFast(
        selectedAsset: Asset,
        accountType: String,
        martingaleTrend: String
    ) {
        val martingaleOrder = currentMartingaleOrder ?: return

        try {
            Log.d(TAG, "EXECUTING MARTINGALE")
            Log.d(TAG, "  Step: ${martingaleOrder.currentStep}")
            Log.d(TAG, "  Trend: $martingaleTrend (from currentActiveTrend)")
            Log.d(TAG, "  Amount: ${formatAmount(martingaleOrder.nextAmount)}")

            val martingaleCTCOrder = CTCOrder(
                id = UUID.randomUUID().toString(),
                assetRic = selectedAsset.ric,
                assetName = selectedAsset.name,
                trend = martingaleTrend,
                amount = martingaleOrder.nextAmount,
                executionTime = getCurrentServerTime(),
                sourceCandle = createDummyCandle()
            )

            ctcOrders.add(martingaleCTCOrder)
            onCTCOrdersUpdate(ctcOrders.toList())

            ctcOrderContinuousMonitor.addCTCOrderForMonitoring(
                ctcOrderId = martingaleCTCOrder.id,
                trend = martingaleTrend,
                amount = martingaleOrder.nextAmount,
                assetRic = selectedAsset.ric,
                isDemoAccount = currentIsDemoAccount,
                cycleNumber = currentCycle
            )

            onExecuteCTCTrade(martingaleTrend, martingaleCTCOrder.id, martingaleOrder.nextAmount, false)
            totalExecutions++

            activeMartingaleOrderId = martingaleCTCOrder.id
            isMartingalePendingExecution = false

            onModeStatusUpdate("CTC Martingale Step ${martingaleOrder.currentStep}: $martingaleTrend - ${formatAmount(martingaleOrder.nextAmount)} ($accountType)")

        } catch (e: Exception) {
            Log.e(TAG, "Error immediate martingale: ${e.message}", e)
            isMartingalePendingExecution = false
        }
    }

    fun handleCTCTradeResult(
        ctcOrderId: String,
        isWin: Boolean,
        details: Map<String, Any>
    ) {
        scope.launch {
            executionMutex.withLock {
                try {
                    val ctcOrder = ctcOrders.find { it.id == ctcOrderId }
                    if (ctcOrder == null) {
                        Log.w(TAG, "CTC order not found: $ctcOrderId")
                        return@withLock
                    }

                    if (ctcOrder.isExecuted) {
                        Log.w(TAG, "CTC order $ctcOrderId already processed - SKIPPING to prevent duplicate")
                        return@withLock
                    }

                    if (::ctcOrderContinuousMonitor.isInitialized) {
                        ctcOrderContinuousMonitor.removeCTCOrderFromMonitoring(ctcOrderId)
                    }

                    val updatedOrder = ctcOrder.copy(isExecuted = true)
                    val index = ctcOrders.indexOfFirst { it.id == ctcOrderId }
                    if (index >= 0) {
                        ctcOrders[index] = updatedOrder
                        onCTCOrdersUpdate(ctcOrders.toList())
                    }

                    val amount = details["amount"] as? Long ?: ctcOrder.amount
                    val tradeId = details["trade_id"] as? String ?: ctcOrderId

                    val resultStr = if (isWin) "WIN"
                    else if (details["status"]?.toString()?.equals("draw", ignoreCase = true) == true) "DRAW"
                    else "LOSE"

                    onCTCTradeStatsUpdate(tradeId, ctcOrderId, resultStr)

                    Log.d(TAG, "CTC Trade Result: $ctcOrderId = $resultStr")
                    Log.d(TAG, "   Active martingale: ${isMartingaleActive()}")
                    Log.d(TAG, "   Active martingale ID: $activeMartingaleOrderId")
                    Log.d(TAG, "   Is this martingale order: ${ctcOrderId == activeMartingaleOrderId}")

                    if (isWin) {
                        handleCTCWin(ctcOrderId, amount, details)
                    } else {
                        handleCTCLoss(ctcOrderId, amount, details)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling CTC result: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun handleCTCWin(ctcOrderId: String, amount: Long, details: Map<String, Any>) {
        totalWins++
        consecutiveWins++
        consecutiveLosses = 0
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"

        if (activeMartingaleOrderId == ctcOrderId && currentMartingaleOrder != null) {
            val martingaleOrder = currentMartingaleOrder!!
            val winAmount = details["win_amount"] as? Long ?: 0L
            val totalRecovered = winAmount
            val winningTrend = currentActiveTrend ?: "call"

            Log.d(TAG, "MARTINGALE WIN at step ${martingaleOrder.currentStep}")
            Log.d(TAG, "Winning trend: $winningTrend")
            Log.d(TAG, "KEEP this trend for next order (NO REVERSE)")

            val result = CTCMartingaleResult(
                isWin = true,
                step = martingaleOrder.currentStep,
                amount = amount,
                totalRecovered = totalRecovered,
                totalLoss = martingaleOrder.totalLoss,
                message = "CTC Martingale WIN - Continue with $winningTrend",
                ctcOrderId = ctcOrderId
            )

            stopCurrentMartingale()

            onCTCMartingaleResult(result)

            delay(500L)

            if (isCTCModeActive && !isMartingaleActive()) {
                Log.d(TAG, "Martingale WIN confirmed - Execute NEW ORDER with SAME trend: $winningTrend ($accountType)")
                executeImmediateOrderUltraFast()
            } else {
                Log.w(TAG, "Martingale still active after WIN - skipping execution to prevent duplicate")
            }

        } else {
            val winningTrend = currentActiveTrend ?: "call"
            Log.d(TAG, "Normal WIN - Continue with SAME trend: $winningTrend")

            if (isCTCModeActive) {
                executeImmediateOrderUltraFast()
            }
        }
    }

    private suspend fun handleCTCLoss(ctcOrderId: String, amount: Long, details: Map<String, Any>) {
        totalLosses++
        consecutiveLosses++
        consecutiveWins = 0
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"

        val settings = ctcMartingaleSettings
        if (settings != null && settings.isEnabled) {
            if (activeMartingaleOrderId == ctcOrderId && currentMartingaleOrder != null) {
                handleMartingaleLoss(ctcOrderId, amount, details)
            } else {
                startNewCTCMartingaleUltraFast(ctcOrderId, amount, details, settings)
            }
        } else {
            Log.d(TAG, "LOSE - Martingale disabled, continue with same trend")
            if (isCTCModeActive) {
                executeImmediateOrderUltraFast()
            }
        }
    }

    private suspend fun startNewCTCMartingaleUltraFast(
        ctcOrderId: String,
        lossAmount: Long,
        details: Map<String, Any>,
        settings: MartingaleState
    ) {
        try {
            val nextStep = 1
            val nextAmount = settings.getMartingaleAmountForStep(nextStep)

            val originalTrend = currentActiveTrend ?: currentCycleTrend ?: "call"
            val reversedTrend = getReverseTrend(originalTrend)

            Log.d(TAG, "STARTING MARTINGALE WITH REVERSE")
            Log.d(TAG, "  Original trend (that lost): $originalTrend")
            Log.d(TAG, "  REVERSED to: $reversedTrend")
            Log.d(TAG, "  All martingale steps will use: $reversedTrend")

            currentActiveTrend = reversedTrend

            currentMartingaleOrder = CTCMartingaleOrder(
                originalOrderId = ctcOrderId,
                currentStep = nextStep,
                maxSteps = settings.maxSteps,
                totalLoss = lossAmount,
                nextAmount = nextAmount,
                isActive = true
            )

            activeMartingaleOrderId = ctcOrderId
            isMartingalePendingExecution = true

            val result = CTCMartingaleResult(
                isWin = false,
                step = nextStep,
                amount = nextAmount,
                totalLoss = lossAmount,
                message = "CTC Martingale Step $nextStep - REVERSED to $reversedTrend",
                shouldContinue = true,
                ctcOrderId = ctcOrderId
            )

            onCTCMartingaleResult(result)
            executeImmediateOrderUltraFast()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting martingale: ${e.message}", e)
            if (isCTCModeActive) {
                executeImmediateOrderUltraFast()
            }
        }
    }

    private suspend fun handleMartingaleLoss(ctcOrderId: String, amount: Long, details: Map<String, Any>) {
        val martingaleOrder = currentMartingaleOrder ?: return
        val settings = ctcMartingaleSettings ?: return

        val newTotalLoss = martingaleOrder.totalLoss + amount
        val nextStep = martingaleOrder.currentStep + 1

        if (nextStep <= settings.maxSteps) {
            try {
                val nextAmount = settings.getMartingaleAmountForStep(nextStep)

                val currentTrend = currentActiveTrend ?: currentCycleTrend ?: "call"
                val reversedTrend = getReverseTrend(currentTrend)

                Log.d(TAG, "CONTINUING MARTINGALE WITH REVERSE")
                Log.d(TAG, "  Previous step: ${martingaleOrder.currentStep} - Trend: $currentTrend")
                Log.d(TAG, "  Next step: $nextStep - REVERSED to: $reversedTrend")

                currentActiveTrend = reversedTrend

                currentMartingaleOrder = martingaleOrder.copy(
                    currentStep = nextStep,
                    totalLoss = newTotalLoss,
                    nextAmount = nextAmount
                )

                isMartingalePendingExecution = true

                val result = CTCMartingaleResult(
                    isWin = false,
                    step = nextStep,
                    amount = nextAmount,
                    totalLoss = newTotalLoss,
                    message = "CTC Martingale Step $nextStep - REVERSED to $reversedTrend",
                    shouldContinue = true,
                    ctcOrderId = ctcOrderId
                )

                onCTCMartingaleResult(result)
                executeImmediateOrderUltraFast()

            } catch (e: Exception) {
                handleMartingaleFailure(newTotalLoss, ctcOrderId)
            }
        } else {
            handleMartingaleFailure(newTotalLoss, ctcOrderId)
        }
    }

    private suspend fun handleMartingaleFailure(totalLoss: Long, ctcOrderId: String) {
        val martingaleOrder = currentMartingaleOrder
        val finalStep = martingaleOrder?.currentStep ?: 0

        val losingTrend = currentActiveTrend ?: currentCycleTrend ?: "call"
        val reversedTrend = getReverseTrend(losingTrend)

        Log.d(TAG, "MARTINGALE FAILED at step $finalStep")
        Log.d(TAG, "Losing trend: $losingTrend")
        Log.d(TAG, "REVERSE to: $reversedTrend and CONTINUE IMMEDIATELY")

        val result = CTCMartingaleResult(
            isWin = false,
            step = finalStep,
            amount = 0L,
            totalLoss = totalLoss,
            message = "CTC Martingale failed - Continue with REVERSED trend: $reversedTrend",
            isMaxReached = true,
            ctcOrderId = ctcOrderId
        )

        currentActiveTrend = reversedTrend

        stopCurrentMartingale()
        onCTCMartingaleResult(result)

        delay(200L)
        if (isCTCModeActive) {
            Log.d(TAG, "Executing immediate order after max step with REVERSED trend: $reversedTrend")
            executeImmediateOrderUltraFast()
        }
    }

    private fun stopCurrentMartingale() {
        Log.d(TAG, "STOPPING MARTINGALE COMPLETELY")
        Log.d(TAG, "   Before: currentMartingaleOrder=${currentMartingaleOrder != null}, activeMartingaleOrderId=$activeMartingaleOrderId, isPending=$isMartingalePendingExecution")

        currentMartingaleOrder = null
        activeMartingaleOrderId = null
        isMartingalePendingExecution = false

        Log.d(TAG, "   After: currentMartingaleOrder=${currentMartingaleOrder != null}, activeMartingaleOrderId=$activeMartingaleOrderId, isPending=$isMartingalePendingExecution")
        Log.d(TAG, "   isMartingaleActive() = ${isMartingaleActive()}")
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isCTCModeActive) return

        if (::ctcOrderContinuousMonitor.isInitialized) {
            ctcOrderContinuousMonitor.handleWebSocketTradeUpdate(message)
        }
    }

    private fun createDummyCandle(): Candle {
        val currentUTC = getCurrentUTCTime()
        return Candle(
            open = java.math.BigDecimal("100.0"),
            close = java.math.BigDecimal("100.0"),
            high = java.math.BigDecimal("100.0"),
            low = java.math.BigDecimal("100.0"),
            createdAt = serverDateFormat.format(currentUTC)
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
        consecutiveWins = 0
        consecutiveLosses = 0
        ctcOrders.clear()
        onCTCOrdersUpdate(emptyList())
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
    }

    fun isActive(): Boolean = isCTCModeActive
    fun getCurrentMartingaleStep(): Int = currentMartingaleOrder?.currentStep ?: 0
    fun isMartingaleActive(): Boolean = activeMartingaleOrderId != null
    fun getCTCOrders(): List<CTCOrder> = ctcOrders.toList()

    fun getPerformanceStats(): Map<String, Any> {
        val successRate = if (totalExecutions > 0) {
            (totalWins.toDouble() / totalExecutions * 100)
        } else 0.0

        val accountType = if (currentIsDemoAccount) "demo" else "real"
        val monitoringStats = if (::ctcOrderContinuousMonitor.isInitialized) {
            ctcOrderContinuousMonitor.getMonitoringStats()
        } else {
            emptyMap()
        }

        return mapOf(
            "is_active" to isCTCModeActive,
            "account_type" to accountType,
            "execution_mode" to "CTC_ULTRA_FAST_FIXED_TIMING",
            "timing_method" to "SYNCED_TO_5_SECOND_BOUNDARIES",
            "trend_logic" to "WIN_KEEP_SAME | LOSE_REVERSE_IF_MARTINGALE",
            "execution_timing" to "INSTANT_MODE_ALL_TRADES",
            "current_cycle" to currentCycle,
            "cycle_in_progress" to isCycleInProgress,
            "current_active_trend" to (currentActiveTrend ?: "None"),
            "cached_cycle_trend" to (currentCycleTrend ?: "None"),
            "total_executions" to totalExecutions,
            "total_wins" to totalWins,
            "total_losses" to totalLosses,
            "success_rate" to String.format("%.1f%%", successRate),
            "current_martingale_step" to getCurrentMartingaleStep(),
            "martingale_active" to isMartingaleActive(),
            "ctc_orders_count" to ctcOrders.size,
            "ultra_fast_monitoring" to monitoringStats,
            "timing_config" to mapOf(
                "sync_to_boundary" to EXECUTION_SYNC_TO_BOUNDARY,
                "boundary_interval" to "5_SECONDS",
                "min_advance_ms" to EXECUTION_MIN_ADVANCE_MS,
                "wait_after_analysis" to EXECUTION_WAIT_AFTER_ANALYSIS
            ),
            "trend_rules" to mapOf(
                "normal_win" to "KEEP_SAME_TREND",
                "martingale_win" to "KEEP_WINNING_TREND",
                "first_lose_martingale_enabled" to "START_MARTINGALE_REVERSE_TREND",
                "martingale_lose_step_N" to "REVERSE_TREND_AGAIN",
                "martingale_max_step_reached" to "REVERSE_AND_CONTINUE_IMMEDIATELY",
                "no_martingale_lose" to "KEEP_SAME_TREND",
                "no_breaks" to "CONTINUOUS_EXECUTION_UNTIL_STOPPED"
            ),
            "bug_fixes" to mapOf(
                "martingale_win_continuation_bug" to "FIXED",
                "race_condition_protection" to "ENABLED",
                "duplicate_result_protection" to "ENABLED",
                "enhanced_state_checking" to "ENABLED",
                "extended_delays" to "ENABLED"
            )
        )
    }

    fun getModeStatus(): String {
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"
        val trend = currentActiveTrend ?: currentCycleTrend ?: "Unknown"

        return when {
            !isCTCModeActive -> "CTC Order tidak aktif"
            !isCycleInProgress -> "CTC Order - Mempersiapkan cycle baru ($accountType)"
            isWaitingForSecondMinute -> "CTC Order CYCLE $currentCycle - Fetching second minute ($accountType)"
            currentCycleTrend == null -> "CTC Order CYCLE $currentCycle - Analyzing data ($accountType)"
            isMartingalePendingExecution -> {
                "CTC Order - Martingale Step ${getCurrentMartingaleStep()} ready: $trend INSTANT ($accountType)"
            }
            isMartingaleActive() -> {
                "CTC Order - Martingale Step ${getCurrentMartingaleStep()}: $trend INSTANT ($accountType)"
            }
            else -> {
                "CTC Order - Ready INSTANT: $trend ($accountType)"
            }
        }
    }

    fun cleanup() {
        stopCTCMode()
        cycleJob?.cancel()
        preWarmJob?.cancel()
        if (::ctcOrderContinuousMonitor.isInitialized) {
            ctcOrderContinuousMonitor.cleanup()
        }
        ctcOrders.clear()
        resetStatistics()
        resetPriceData()
        currentActiveTrend = null
    }
}