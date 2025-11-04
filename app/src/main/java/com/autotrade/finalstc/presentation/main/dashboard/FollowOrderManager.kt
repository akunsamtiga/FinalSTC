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

class FollowOrderManager(
    private val scope: CoroutineScope,
    private val onFollowOrdersUpdate: (List<FollowOrder>) -> Unit,
    private val onExecuteFollowTrade: (String, String, Long, Boolean) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val getUserSession: suspend () -> UserSession?,
    private val serverTimeService: ServerTimeService?,
    private val onFollowMartingaleResult: (FollowMartingaleResult) -> Unit,
    private val tradeManager: TradeManager,
    private val onFollowTradeStatsUpdate: (tradeId: String, orderId: String, result: String) -> Unit
) {
    companion object {
        private const val TAG = "FollowOrderManager"
        private const val FIRST_FETCH_OFFSET_MS = 300L
        private const val SECOND_FETCH_OFFSET_MS = 300L
        private const val IMMEDIATE_EXECUTION_DELAY = 0L
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

    private var isFollowModeActive = false
    private val executionMutex = Mutex()
    private var followOrders = mutableListOf<FollowOrder>()
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

    private var currentMartingaleOrder: FollowMartingaleOrder? = null
    private var followMartingaleSettings: MartingaleState? = null
    private var activeMartingaleOrderId: String? = null
    private var isMartingalePendingExecution = false

    private var totalExecutions = 0
    private var totalWins = 0
    private var totalLosses = 0
    private var consecutiveWins = 0
    private var consecutiveLosses = 0

    private var isApiPreWarmed = false
    private var preWarmJob: Job? = null
    private var lastSuccessfulPreWarm = 0L

    private lateinit var followOrderContinuousMonitor: FollowOrderContinuousMonitor

    private fun getCurrentServerTime(): Long {
        return serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + serverTimeOffset)
    }

    private fun getCurrentUTCTime(): Date {
        return Date(getCurrentServerTime())
    }

    private fun initializeUltraFastMonitoring() {
        followOrderContinuousMonitor = FollowOrderContinuousMonitor(
            scope = scope,
            getUserSession = getUserSession,
            onFollowTradeResult = { followOrderId, isWin, details ->
                handleFollowTradeResult(followOrderId, isWin, details)
            },
            serverTimeService = serverTimeService
        )

        Log.d(TAG, "Ultra-fast monitoring initialized")
    }

    fun startFollowMode(
        selectedAsset: Asset,
        isDemoAccount: Boolean,
        martingaleSettings: MartingaleState
    ): Result<String> {
        if (isFollowModeActive) {
            return Result.failure(Exception("Follow Order mode sudah aktif"))
        }

        return try {
            initializeUltraFastMonitoring()

            currentSelectedAsset = selectedAsset
            currentIsDemoAccount = isDemoAccount
            currentMartingaleSettings = martingaleSettings
            followMartingaleSettings = martingaleSettings

            isFollowModeActive = true
            isCycleInProgress = false
            currentCycle = 0
            currentCycleTrend = null
            resetStatistics()
            resetPriceData()

            val accountType = if (isDemoAccount) "Demo" else "Real"
            Log.d(TAG, "Follow Order Mode Starting with Optimized Execution")
            Log.d(TAG, "Asset: ${selectedAsset.name}")
            Log.d(TAG, "Account: $accountType")
            Log.d(TAG, "Execution Rules:")
            Log.d(TAG, "  1. Normal WIN → Execute immediate (same trend)")
            Log.d(TAG, "  2. Martingale WIN → Fetch candle baru")
            Log.d(TAG, "  3. Normal LOSE (no martingale) → Fetch candle baru")
            Log.d(TAG, "  4. Martingale MAX STEP → Fetch candle baru")
            Log.d(TAG, "  5. Martingale step LOSE → Execute immediate (same trend)")

            followOrderContinuousMonitor.startMonitoring()
            startApiPreWarming()
            startNewCycleWithInstantExecution()

            onModeStatusUpdate("Follow Order OPTIMIZED aktif ($accountType)")

            Result.success("Follow Order mode dimulai untuk ${selectedAsset.name} ($accountType)")
        } catch (e: Exception) {
            isFollowModeActive = false
            Result.failure(Exception("Gagal memulai Follow Order: ${e.message}"))
        }
    }

    private fun startApiPreWarming() {
        preWarmJob?.cancel()
        preWarmJob = scope.launch {
            while (isFollowModeActive) {
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
                Log.d(TAG, "API pre-warmed successfully")
            } else {
                isApiPreWarmed = false
            }

        } catch (e: Exception) {
            isApiPreWarmed = false
            Log.e(TAG, "Pre-warming error: ${e.message}", e)
        }
    }

    fun stopFollowMode(): Result<String> {
        if (!isFollowModeActive) {
            return Result.failure(Exception("Follow Order mode tidak aktif"))
        }

        return try {
            isFollowModeActive = false
            isCycleInProgress = false

            preWarmJob?.cancel()
            preWarmJob = null
            isApiPreWarmed = false

            if (::followOrderContinuousMonitor.isInitialized) {
                followOrderContinuousMonitor.stopMonitoring()
            }

            cycleJob?.cancel()
            cycleJob = null

            currentCycleTrend = null
            currentCycle = 0
            currentMartingaleOrder = null
            activeMartingaleOrderId = null
            followMartingaleSettings = null
            isMartingalePendingExecution = false
            currentSelectedAsset = null
            currentMartingaleSettings = null
            resetPriceData()

            onModeStatusUpdate("Follow Order dihentikan")

            Log.d(TAG, "Follow Order mode stopped")
            Log.d(TAG, "Total cycles: $currentCycle")
            Log.d(TAG, "Total executions: $totalExecutions (Win: $totalWins, Loss: $totalLosses)")

            Result.success("Follow Order mode berhasil dihentikan")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghentikan Follow Order: ${e.message}"))
        }
    }

    private fun startNewCycleWithInstantExecution() {
        cycleJob?.cancel()
        currentCycle++
        currentCycleTrend = null
        isCycleInProgress = true
        isWaitingForSecondMinute = false
        resetPriceData()

        cycleJob = scope.launch {
            try {
                val accountType = if (currentIsDemoAccount) "Demo" else "Real"
                Log.d(TAG, "CYCLE $currentCycle: Starting NEW CYCLE ($accountType)")

                val serverNow = getCurrentServerTime()
                val firstMinuteStart = calculateNextMinuteStart(serverNow)
                val waitToFirstMinute = firstMinuteStart - serverNow

                Log.d(TAG, "CYCLE $currentCycle: Wait ${waitToFirstMinute}ms to first fetch")
                onModeStatusUpdate("Follow Order CYCLE $currentCycle: Wait ${waitToFirstMinute}ms ($accountType)")

                if (waitToFirstMinute > 0) {
                    delay(waitToFirstMinute)
                }

                if (!isFollowModeActive) return@launch

                delay(FIRST_FETCH_OFFSET_MS)

                Log.d(TAG, "CYCLE $currentCycle: FIRST fetch (optimized)")
                priceDataFirstMinute = performPrecisePriceFetch("FIRST_MINUTE")

                if (priceDataFirstMinute == null) {
                    Log.w(TAG, "CYCLE $currentCycle: First minute fetch failed")
                    delay(2000L)
                    if (isFollowModeActive) {
                        startNewCycleWithInstantExecution()
                    }
                    return@launch
                }

                Log.d(TAG, "CYCLE $currentCycle: First minute OK")
                isWaitingForSecondMinute = true
                onModeStatusUpdate("Follow Order CYCLE $currentCycle: First minute OK ($accountType)")

                val secondMinuteStart = firstMinuteStart + 60000L
                val waitToSecondMinute = secondMinuteStart - getCurrentServerTime()

                Log.d(TAG, "CYCLE $currentCycle: Wait ${waitToSecondMinute}ms to second minute")
                if (waitToSecondMinute > 0) {
                    delay(waitToSecondMinute)
                }

                if (!isFollowModeActive) return@launch

                delay(SECOND_FETCH_OFFSET_MS)

                Log.d(TAG, "CYCLE $currentCycle: SECOND fetch (optimized)")
                priceDataSecondMinute = performPrecisePriceFetch("SECOND_MINUTE")

                if (priceDataSecondMinute == null) {
                    Log.w(TAG, "CYCLE $currentCycle: Second minute fetch failed")
                    delay(2000L)
                    if (isFollowModeActive) {
                        startNewCycleWithInstantExecution()
                    }
                    return@launch
                }

                val trend = compareFullMinutesAndDetermineTrend()
                if (trend == null) {
                    Log.w(TAG, "CYCLE $currentCycle: Comparison failed")
                    delay(2000L)
                    if (isFollowModeActive) {
                        startNewCycleWithInstantExecution()
                    }
                    return@launch
                }

                currentCycleTrend = trend
                isWaitingForSecondMinute = false

                Log.d(TAG, "CYCLE $currentCycle: Comparison complete - Trend: $trend")
                onModeStatusUpdate("Follow Order CYCLE $currentCycle: Analysis complete - $trend ($accountType)")

                if (IMMEDIATE_EXECUTION_DELAY > 0) {
                    delay(IMMEDIATE_EXECUTION_DELAY)
                }

                if (!isFollowModeActive) return@launch

                Log.d(TAG, "CYCLE $currentCycle: INSTANT EXECUTION - NO DELAY!")
                executeFirstOrderInstantly(trend)

            } catch (ce: CancellationException) {
                Log.d(TAG, "CYCLE $currentCycle: Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "CYCLE $currentCycle: Error: ${e.message}", e)
                if (isFollowModeActive) {
                    delay(3000L)
                    startNewCycleWithInstantExecution()
                }
            }
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

            Log.d(TAG, "CYCLE $currentCycle: $fetchType at $timeString")

            val latestCandles = fetchPriceDataWithPreWarming(selectedAsset)
            if (latestCandles == null || latestCandles.size < 2) {
                Log.w(TAG, "CYCLE $currentCycle: Insufficient data for $fetchType")
                return null
            }

            val fetchTime = getCurrentServerTime() - startTime
            Log.d(TAG, "CYCLE $currentCycle: $fetchType completed in ${fetchTime}ms")

            latestCandles
        } catch (e: Exception) {
            Log.e(TAG, "CYCLE $currentCycle: Error in $fetchType: ${e.message}", e)
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

            Log.d(TAG, "CYCLE $currentCycle: Minute comparison:")
            Log.d(TAG, "  Price first: $priceFirstMinute")
            Log.d(TAG, "  Price second: $priceSecondMinute")
            Log.d(TAG, "  Change: $priceChange")
            Log.d(TAG, "  Trend: $trend")

            trend
        } catch (e: Exception) {
            Log.e(TAG, "CYCLE $currentCycle: Error comparing: ${e.message}", e)
            null
        }
    }

    private suspend fun executeFirstOrderInstantly(trend: String) {
        executionMutex.withLock {
            if (!isFollowModeActive || !isCycleInProgress) return@withLock

            val martingaleSettings = currentMartingaleSettings
            val selectedAsset = currentSelectedAsset

            if (martingaleSettings == null || selectedAsset == null) {
                return@withLock
            }

            val accountType = if (currentIsDemoAccount) "Demo" else "Real"

            try {
                val executionServerTime = getCurrentServerTime()

                Log.d(TAG, "CYCLE $currentCycle: INSTANT EXECUTION ($accountType)")
                Log.d(TAG, "  Trend: $trend")
                Log.d(TAG, "  Amount: ${formatAmount(martingaleSettings.baseAmount)}")
                Log.d(TAG, "  NO DELAY - EXECUTING NOW!")

                val followOrder = FollowOrder(
                    id = UUID.randomUUID().toString(),
                    assetRic = selectedAsset.ric,
                    assetName = selectedAsset.name,
                    trend = trend,
                    amount = martingaleSettings.baseAmount,
                    executionTime = executionServerTime,
                    sourceCandle = createDummyCandle()
                )

                followOrders.add(followOrder)
                onFollowOrdersUpdate(followOrders.toList())

                followOrderContinuousMonitor.addFollowOrderForMonitoring(
                    followOrderId = followOrder.id,
                    trend = trend,
                    amount = martingaleSettings.baseAmount,
                    assetRic = selectedAsset.ric,
                    isDemoAccount = currentIsDemoAccount,
                    cycleNumber = currentCycle
                )

                onModeStatusUpdate("Follow Order CYCLE $currentCycle: INSTANT execution $trend - ${selectedAsset.name} ($accountType)")

                onExecuteFollowTrade(trend, followOrder.id, martingaleSettings.baseAmount, false)
                totalExecutions++

                Log.d(TAG, "CYCLE $currentCycle: Order executed INSTANTLY!")

            } catch (e: Exception) {
                Log.e(TAG, "CYCLE $currentCycle: Error executing: ${e.message}", e)
                delay(3000L)
                if (isFollowModeActive) {
                    startNewCycleWithInstantExecution()
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

    fun executeImmediateOrderUltraFast() {
        scope.launch {
            try {
                delay(50L)

                if (!isFollowModeActive || !isCycleInProgress) return@launch

                val trend = currentCycleTrend
                if (trend == null) {
                    Log.w(TAG, "CYCLE $currentCycle: No cached trend")
                    return@launch
                }

                executionMutex.withLock {
                    val selectedAsset = currentSelectedAsset ?: return@withLock
                    val martingaleSettings = currentMartingaleSettings ?: return@withLock
                    val accountType = if (currentIsDemoAccount) "Demo" else "Real"

                    try {
                        if (isMartingalePendingExecution && currentMartingaleOrder != null) {
                            executeImmediateMartingaleUltraFast(selectedAsset, accountType)
                            return@withLock
                        }

                        val currentServerTime = getCurrentServerTime()

                        Log.d(TAG, "Executing IMMEDIATE order with trend: $trend")

                        val followOrder = FollowOrder(
                            id = UUID.randomUUID().toString(),
                            assetRic = selectedAsset.ric,
                            assetName = selectedAsset.name,
                            trend = trend,
                            amount = martingaleSettings.baseAmount,
                            executionTime = currentServerTime,
                            sourceCandle = createDummyCandle()
                        )

                        followOrders.add(followOrder)
                        onFollowOrdersUpdate(followOrders.toList())

                        followOrderContinuousMonitor.addFollowOrderForMonitoring(
                            followOrderId = followOrder.id,
                            trend = trend,
                            amount = martingaleSettings.baseAmount,
                            assetRic = selectedAsset.ric,
                            isDemoAccount = currentIsDemoAccount,
                            cycleNumber = currentCycle
                        )

                        onExecuteFollowTrade(trend, followOrder.id, martingaleSettings.baseAmount, false)
                        totalExecutions++

                        Log.d(TAG, "IMMEDIATE order executed: trend=$trend")

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
        accountType: String
    ) {
        val martingaleOrder = currentMartingaleOrder ?: return
        val martingaleTrend = currentCycleTrend ?: return

        try {
            Log.d(TAG, "EXECUTING MARTINGALE")
            Log.d(TAG, "  Step: ${martingaleOrder.currentStep}")
            Log.d(TAG, "  Trend: $martingaleTrend")
            Log.d(TAG, "  Amount: ${formatAmount(martingaleOrder.nextAmount)}")

            val martingaleFollowOrder = FollowOrder(
                id = UUID.randomUUID().toString(),
                assetRic = selectedAsset.ric,
                assetName = selectedAsset.name,
                trend = martingaleTrend,
                amount = martingaleOrder.nextAmount,
                executionTime = getCurrentServerTime(),
                sourceCandle = createDummyCandle()
            )

            followOrders.add(martingaleFollowOrder)
            onFollowOrdersUpdate(followOrders.toList())

            followOrderContinuousMonitor.addFollowOrderForMonitoring(
                followOrderId = martingaleFollowOrder.id,
                trend = martingaleTrend,
                amount = martingaleOrder.nextAmount,
                assetRic = selectedAsset.ric,
                isDemoAccount = currentIsDemoAccount,
                cycleNumber = currentCycle
            )

            onExecuteFollowTrade(martingaleTrend, martingaleFollowOrder.id, martingaleOrder.nextAmount, false)
            totalExecutions++

            activeMartingaleOrderId = martingaleFollowOrder.id
            isMartingalePendingExecution = false

            onModeStatusUpdate("Follow Martingale Step ${martingaleOrder.currentStep}: $martingaleTrend - ${formatAmount(martingaleOrder.nextAmount)} ($accountType)")

        } catch (e: Exception) {
            Log.e(TAG, "Error immediate martingale: ${e.message}", e)
            isMartingalePendingExecution = false
        }
    }

    fun handleFollowTradeResult(
        followOrderId: String,
        isWin: Boolean,
        details: Map<String, Any>
    ) {
        scope.launch {
            executionMutex.withLock {
                try {
                    val followOrder = followOrders.find { it.id == followOrderId }
                    if (followOrder == null) {
                        Log.w(TAG, "Follow order not found: $followOrderId")
                        return@withLock
                    }

                    if (followOrder.isExecuted) {
                        Log.w(TAG, "Follow order already processed: $followOrderId")
                        return@withLock
                    }

                    if (::followOrderContinuousMonitor.isInitialized) {
                        followOrderContinuousMonitor.removeFollowOrderFromMonitoring(followOrderId)
                    }

                    val updatedOrder = followOrder.copy(isExecuted = true)
                    val index = followOrders.indexOfFirst { it.id == followOrderId }
                    if (index >= 0) {
                        followOrders[index] = updatedOrder
                        onFollowOrdersUpdate(followOrders.toList())
                    }

                    val amount = details["amount"] as? Long ?: followOrder.amount
                    val tradeId = details["trade_id"] as? String ?: followOrderId

                    val resultStr = if (isWin) "WIN"
                    else if (details["status"]?.toString()?.equals("draw", ignoreCase = true) == true) "DRAW"
                    else "LOSE"

                    onFollowTradeStatsUpdate(tradeId, followOrderId, resultStr)

                    Log.d(TAG, "Follow Trade Result: $followOrderId = $resultStr")

                    if (isWin) {
                        handleFollowWin(followOrderId, amount, details)
                    } else {
                        handleFollowLoss(followOrderId, amount, details)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling follow result: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun handleFollowWin(followOrderId: String, amount: Long, details: Map<String, Any>) {
        totalWins++
        consecutiveWins++
        consecutiveLosses = 0
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"

        if (activeMartingaleOrderId == followOrderId && currentMartingaleOrder != null) {
            val martingaleOrder = currentMartingaleOrder!!
            val winAmount = details["win_amount"] as? Long ?: 0L
            val totalRecovered = winAmount

            Log.d(TAG, "MARTINGALE WIN at step ${martingaleOrder.currentStep}")
            Log.d(TAG, "Rule #2: Martingale WIN → Fetch candle baru")

            val result = FollowMartingaleResult(
                isWin = true,
                step = martingaleOrder.currentStep,
                amount = amount,
                totalRecovered = totalRecovered,
                totalLoss = martingaleOrder.totalLoss,
                message = "Martingale WIN - Fetch candle baru",
                followOrderId = followOrderId
            )

            stopCurrentMartingale()
            onFollowMartingaleResult(result)
            delay(500L)

            if (isFollowModeActive && !isMartingaleActive()) {
                Log.d(TAG, "Starting NEW CYCLE after martingale WIN ($accountType)")
                startNewCycleWithInstantExecution()
            }

        } else {
            Log.d(TAG, "NORMAL WIN")
            Log.d(TAG, "Rule #1: Normal WIN → Execute immediate (same trend)")

            if (isFollowModeActive) {
                Log.d(TAG, "Executing IMMEDIATE order after normal WIN (same trend)")
                executeImmediateOrderUltraFast()
            }
        }
    }

    private suspend fun handleFollowLoss(followOrderId: String, amount: Long, details: Map<String, Any>) {
        totalLosses++
        consecutiveLosses++
        consecutiveWins = 0
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"

        val settings = followMartingaleSettings
        if (settings != null && settings.isEnabled) {
            if (activeMartingaleOrderId == followOrderId && currentMartingaleOrder != null) {
                handleMartingaleLoss(followOrderId, amount, details)
            } else {
                Log.d(TAG, "NORMAL LOSE")
                Log.d(TAG, "Rule #3: Normal LOSE → Start martingale (fetch candle baru if martingale also loses)")

                startNewFollowMartingaleUltraFast(followOrderId, amount, details, settings)
            }
        } else {
            Log.d(TAG, "LOSE - Martingale disabled")
            Log.d(TAG, "Rule #3: Normal LOSE (no martingale) → Fetch candle baru")

            if (isFollowModeActive) {
                Log.d(TAG, "Starting NEW CYCLE after normal LOSE")
                startNewCycleWithInstantExecution()
            }
        }
    }

    private suspend fun startNewFollowMartingaleUltraFast(
        followOrderId: String,
        lossAmount: Long,
        details: Map<String, Any>,
        settings: MartingaleState
    ) {
        try {
            val nextStep = 1
            val nextAmount = settings.getMartingaleAmountForStep(nextStep)

            currentMartingaleOrder = FollowMartingaleOrder(
                originalOrderId = followOrderId,
                currentStep = nextStep,
                maxSteps = settings.maxSteps,
                totalLoss = lossAmount,
                nextAmount = nextAmount,
                isActive = true
            )

            activeMartingaleOrderId = followOrderId
            isMartingalePendingExecution = true

            val result = FollowMartingaleResult(
                isWin = false,
                step = nextStep,
                amount = nextAmount,
                totalLoss = lossAmount,
                message = "Martingale Step $nextStep",
                shouldContinue = true,
                followOrderId = followOrderId
            )

            onFollowMartingaleResult(result)

            Log.d(TAG, "Starting martingale step $nextStep")
            Log.d(TAG, "Rule #5: Martingale step LOSE → Execute immediate (same trend)")
            executeImmediateOrderUltraFast()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting martingale: ${e.message}", e)
            if (isFollowModeActive) {
                Log.d(TAG, "Martingale start failed - starting NEW CYCLE")
                startNewCycleWithInstantExecution()
            }
        }
    }

    private suspend fun handleMartingaleLoss(followOrderId: String, amount: Long, details: Map<String, Any>) {
        val martingaleOrder = currentMartingaleOrder ?: return
        val settings = followMartingaleSettings ?: return

        val newTotalLoss = martingaleOrder.totalLoss + amount
        val nextStep = martingaleOrder.currentStep + 1

        if (nextStep <= settings.maxSteps) {
            try {
                val nextAmount = settings.getMartingaleAmountForStep(nextStep)

                Log.d(TAG, "Martingale LOSE at step ${martingaleOrder.currentStep}")
                Log.d(TAG, "Rule #5: Martingale step LOSE → Execute immediate (same trend)")

                currentMartingaleOrder = martingaleOrder.copy(
                    currentStep = nextStep,
                    totalLoss = newTotalLoss,
                    nextAmount = nextAmount
                )

                isMartingalePendingExecution = true

                val result = FollowMartingaleResult(
                    isWin = false,
                    step = nextStep,
                    amount = nextAmount,
                    totalLoss = newTotalLoss,
                    message = "Martingale Step $nextStep",
                    shouldContinue = true,
                    followOrderId = followOrderId
                )

                onFollowMartingaleResult(result)

                Log.d(TAG, "Martingale continues to step $nextStep - execute immediate (same trend)")
                executeImmediateOrderUltraFast()

            } catch (e: Exception) {
                handleMartingaleFailure(newTotalLoss, followOrderId)
            }
        } else {
            handleMartingaleFailure(newTotalLoss, followOrderId)
        }
    }

    private suspend fun handleMartingaleFailure(totalLoss: Long, followOrderId: String) {
        val martingaleOrder = currentMartingaleOrder
        val finalStep = martingaleOrder?.currentStep ?: 0

        Log.d(TAG, "Martingale FAILED at step $finalStep")
        Log.d(TAG, "Rule #4: Martingale MAX STEP → Fetch candle baru")

        val result = FollowMartingaleResult(
            isWin = false,
            step = finalStep,
            amount = 0L,
            totalLoss = totalLoss,
            message = "Martingale failed - Fetch candle baru",
            isMaxReached = true,
            followOrderId = followOrderId
        )

        stopCurrentMartingale()
        onFollowMartingaleResult(result)

        delay(200L)

        if (isFollowModeActive) {
            Log.d(TAG, "Starting NEW CYCLE after martingale MAX STEP")
            startNewCycleWithInstantExecution()
        }
    }

    private fun stopCurrentMartingale() {
        currentMartingaleOrder = null
        activeMartingaleOrderId = null
        isMartingalePendingExecution = false
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isFollowModeActive) return

        if (::followOrderContinuousMonitor.isInitialized) {
            followOrderContinuousMonitor.handleWebSocketTradeUpdate(message)
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
        followOrders.clear()
        onFollowOrdersUpdate(emptyList())
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
    }

    fun isActive(): Boolean = isFollowModeActive
    fun getCurrentMartingaleStep(): Int = currentMartingaleOrder?.currentStep ?: 0
    fun isMartingaleActive(): Boolean = activeMartingaleOrderId != null
    fun getFollowOrders(): List<FollowOrder> = followOrders.toList()

    fun getPerformanceStats(): Map<String, Any> {
        val successRate = if (totalExecutions > 0) {
            (totalWins.toDouble() / totalExecutions * 100)
        } else 0.0

        val accountType = if (currentIsDemoAccount) "demo" else "real"
        val monitoringStats = if (::followOrderContinuousMonitor.isInitialized) {
            followOrderContinuousMonitor.getMonitoringStats()
        } else {
            emptyMap()
        }

        return mapOf(
            "is_active" to isFollowModeActive,
            "account_type" to accountType,
            "execution_mode" to "OPTIMIZED_FOLLOW_ORDER",
            "execution_rules" to mapOf(
                "rule_1" to "Normal WIN → Execute immediate (same trend)",
                "rule_2" to "Martingale WIN → Fetch candle baru",
                "rule_3" to "Normal LOSE (no martingale) → Fetch candle baru",
                "rule_4" to "Martingale MAX STEP → Fetch candle baru",
                "rule_5" to "Martingale step LOSE → Execute immediate (same trend)"
            ),
            "current_cycle" to currentCycle,
            "cycle_in_progress" to isCycleInProgress,
            "cached_trend" to (currentCycleTrend ?: "None"),
            "total_executions" to totalExecutions,
            "total_wins" to totalWins,
            "total_losses" to totalLosses,
            "success_rate" to String.format("%.1f%%", successRate),
            "current_martingale_step" to getCurrentMartingaleStep(),
            "martingale_active" to isMartingaleActive(),
            "follow_orders_count" to followOrders.size,
            "ultra_fast_monitoring" to monitoringStats,
            "performance_mode" to "OPTIMIZED_WITH_RULES"
        )
    }

    fun getModeStatus(): String {
        val accountType = if (currentIsDemoAccount) "Demo" else "Real"
        val trend = currentCycleTrend ?: "Unknown"

        return when {
            !isFollowModeActive -> "Follow Order tidak aktif"
            !isCycleInProgress -> "Follow Order - Mempersiapkan cycle ($accountType)"
            isWaitingForSecondMinute -> "Follow Order CYCLE $currentCycle - Second minute ($accountType)"
            currentCycleTrend == null -> "Follow Order CYCLE $currentCycle - Analyzing ($accountType)"
            isMartingalePendingExecution -> "Follow Order - Martingale Step ${getCurrentMartingaleStep()} ready: $trend INSTANT ($accountType)"
            isMartingaleActive() -> "Follow Order - Martingale Step ${getCurrentMartingaleStep()}: $trend INSTANT ($accountType)"
            else -> "Follow Order - Ready OPTIMIZED: $trend ($accountType)"
        }
    }

    fun cleanup() {
        stopFollowMode()
        cycleJob?.cancel()
        preWarmJob?.cancel()
        if (::followOrderContinuousMonitor.isInitialized) {
            followOrderContinuousMonitor.cleanup()
        }
        followOrders.clear()
        resetStatistics()
        resetPriceData()
    }
}