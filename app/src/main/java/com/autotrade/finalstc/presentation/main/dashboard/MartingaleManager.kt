package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.autotrade.finalstc.presentation.main.history.TradingHistoryApi
import com.autotrade.finalstc.presentation.main.history.TradingHistoryRaw
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException

class MartingaleManager(
    private val scope: CoroutineScope,
    private val onMartingaleResult: (MartingaleResult) -> Unit,
    private val onExecuteNextTrade: (String, Long, String) -> Unit,
    private val getUserSession: suspend () -> UserSession?,
    private val webSocketManager: WebSocketManager? = null,
    private val serverTimeService: ServerTimeService? = null,
    private val onStepUpdate: ((String, Int) -> Unit)? = null
) {
    private var monitoringJob: Job? = null
    private var currentMartingaleOrder: MartingaleOrder? = null
    private var lastProcessedOrderId: String? = null
    private var isMonitoringActive = false
    private val resultLock = Mutex()
    private var isProcessingResult = false
    private var currentMartingaleSettings: MartingaleState? = null
    private var serverTimeOffset: Long = 0L
    private var currentExecutionInfo: ExecutionInfo? = null
    private var isWaitingForTradeResult = false
    private var tradeResultReceived = false
    private var totalExecutions = 0
    private var totalWins = 0
    private var totalLosses = 0
    private var currentCurrency: CurrencyType = CurrencyType.IDR

    init {
        serverTimeOffset = ServerTimeService.cachedServerTimeOffset
    }

    private data class ExecutionInfo(
        val tradeId: String?,
        val amount: Long,
        val trend: String,
        val step: Int,
        val executionTime: Long,
        val expectedExpireTime: Long,
        val scheduledOrderId: String
    )

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val historyApi = retrofit.create(TradingHistoryApi::class.java)

    private val RESULT_MONITORING_INTERVAL_MS = 25L
    private val TRADE_RESULT_TIMEOUT_MS = 90000L
    private val NEXT_STEP_DELAY_MS = 0L

    fun startMartingaleOrderInstant(
        initialTrade: TradeOrder,
        martingaleSettings: MartingaleState,
        trend: String,
        scheduledOrderId: String,
        initialStep: Int = 1
    ) {
        println("Starting martingale with currency: ${currentCurrency.code}")
        println("Starting martingale for order: $scheduledOrderId (step=$initialStep) - Ultra Fast Mode")

        val validation = martingaleSettings.validate()
        if (validation.isFailure) {
            println("Invalid martingale settings: ${validation.exceptionOrNull()?.message}")
            return
        }

        currentMartingaleSettings = martingaleSettings

        currentMartingaleOrder = MartingaleOrder(
            originalTrade = initialTrade,
            trend = trend,
            baseAmount = martingaleSettings.baseAmount,
            maxSteps = martingaleSettings.maxSteps,
            currentStep = maxOf(0, initialStep - 1),
            totalLoss = initialTrade.amount,
            isActive = true,
            accountType = initialTrade.dealType,
            startTime = System.currentTimeMillis(),
            scheduledOrderId = scheduledOrderId,
            multiplierType = martingaleSettings.multiplierType,
            multiplierValue = martingaleSettings.multiplierValue
        )

        resetExecutionTracking()
        totalExecutions++

        scope.launch {
            executeStepWithMonitoring(initialStep)
        }
    }

    fun updateCurrency(currency: CurrencyType) {
        currentCurrency = currency
        println("MartingaleManager: Currency updated to ${currency.code}")
    }

    fun startMartingaleOrder(
        initialTrade: TradeOrder,
        martingaleSettings: MartingaleState,
        trend: String,
        scheduledOrderId: String
    ) {
        startMartingaleOrderInstant(initialTrade, martingaleSettings, trend, scheduledOrderId)
    }

    private suspend fun executeStepWithMonitoring(martingaleStep: Int) {
        val currentOrder = currentMartingaleOrder ?: return
        val settings = currentMartingaleSettings ?: return

        if (martingaleStep > settings.maxSteps) {
            handleMaxStepsReached()
            return
        }

        val stepAmount = try {
            settings.getMartingaleAmountForStep(martingaleStep)
        } catch (e: IllegalArgumentException) {
            println("Invalid martingale step $martingaleStep: ${e.message}")
            return
        } catch (e: ArithmeticException) {
            println("Amount calculation overflow at step $martingaleStep: ${e.message}")
            return
        }

        println("Executing martingale step $martingaleStep - Amount: ${formatAmount(stepAmount)} (Ultra Fast)")
        println("Multiplier calculation: base(${formatAmount(settings.baseAmount)}) * multiplier^$martingaleStep = ${formatAmount(stepAmount)}")

        currentMartingaleOrder = currentOrder.copy(currentStep = martingaleStep)

        onStepUpdate?.invoke(currentOrder.scheduledOrderId, martingaleStep)

        println("MartingaleManager: Notified step update to $martingaleStep")

        lastProcessedOrderId = null
        resetExecutionTracking(preserveWaiting = true)
        isWaitingForTradeResult = true
        tradeResultReceived = false
        isProcessingResult = false

        currentExecutionInfo = ExecutionInfo(
            tradeId = null,
            amount = stepAmount,
            trend = currentOrder.trend,
            step = martingaleStep,
            executionTime = System.currentTimeMillis(),
            expectedExpireTime = calculateExpireTime(),
            scheduledOrderId = currentOrder.scheduledOrderId
        )

        println("Starting ultra-fast monitoring for martingale step $martingaleStep...")

        startResultMonitoring()

        val now = (serverTimeService?.getCurrentServerTimeMillis()
            ?: System.currentTimeMillis() + serverTimeOffset)

        if (now >= currentExecutionInfo!!.expectedExpireTime - 500) {
            println("Trade skipped: expire time too close (step $martingaleStep)")
            return
        }

        println("Sending martingale trade order for step $martingaleStep - Amount: ${formatAmount(stepAmount)}")
        onExecuteNextTrade(currentOrder.trend, stepAmount, currentOrder.scheduledOrderId)
    }

    private fun calculateExpireTime(): Long {
        val currentTime = (serverTimeService?.getCurrentServerTimeMillis()
            ?: System.currentTimeMillis() + serverTimeOffset)

        val seconds = (currentTime / 1000) % 60
        return if (seconds <= 10) {
            currentTime + ((60 - seconds) * 1000)
        } else {
            currentTime + ((120 - seconds) * 1000)
        }
    }

    private fun startResultMonitoring() {
        if (isMonitoringActive) {
            stopResultMonitoring()
        }

        isMonitoringActive = true
        val startTime = System.currentTimeMillis()
        val currentStep = currentMartingaleOrder?.currentStep ?: -1

        println("Starting ultra-fast result monitoring for step $currentStep (25ms intervals)...")

        monitoringJob = scope.launch {
            while (isMonitoringActive && isWaitingForTradeResult && !tradeResultReceived) {
                try {
                    val elapsed = System.currentTimeMillis() - startTime

                    if (elapsed > TRADE_RESULT_TIMEOUT_MS) {
                        println("Monitoring timeout for step $currentStep after ${elapsed}ms")
                        handleMonitoringTimeout()
                        break
                    }

                    checkTradeResult()
                    delay(25L)

                } catch (ce: CancellationException) {
                    println("Monitoring cancelled for step $currentStep")
                    throw ce
                } catch (e: Exception) {
                    println("Error in result monitoring for step $currentStep: ${e.message}")
                    delay(1000)
                }
            }

            println("Result monitoring completed for step $currentStep")
            isMonitoringActive = false
        }
    }

    private suspend fun checkTradeResult() {
        val userSession = getUserSession() ?: return
        val currentOrder = currentMartingaleOrder ?: return
        val executionInfo = currentExecutionInfo ?: return

        try {
            val response = historyApi.getTradingHistoryRaw(
                type = currentOrder.accountType,
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType,
                deviceId = userSession.deviceId,
                timezone = "Asia/Jakarta",
                origin = "https://stockity.id",
                referer = "https://stockity.id",
                accept = "application/json, text/plain, */*"
            )

            if (response.isSuccessful && response.body() != null) {
                val historyData = parseHistoryResponse(response.body()!!)
                val relevantTrade = findRelevantTrade(historyData, executionInfo)

                if (relevantTrade != null) {
                    resultLock.withLock {
                        if (tradeResultReceived || lastProcessedOrderId == relevantTrade.uuid || isProcessingResult) {
                            return@withLock
                        }

                        isProcessingResult = true
                        tradeResultReceived = true
                        lastProcessedOrderId = relevantTrade.uuid

                        println("Found relevant trade for step ${executionInfo.step}: ${relevantTrade.uuid}")
                        handleTradeResult(relevantTrade)
                    }
                }
            }
        } catch (ce: CancellationException) {
            println("Monitoring cancelled: ${ce.message}")
            throw ce
        } catch (e: Exception) {
            println("Error in checkTradeResult: ${e.message}")
            delay(1000)
        }
    }

    private fun findRelevantTrade(historyData: List<TradingHistoryRaw>, executionInfo: ExecutionInfo): TradingHistoryRaw? {
        val currentOrder = currentMartingaleOrder ?: return null
        val recentTimeThreshold = executionInfo.executionTime - 10000L

        return historyData
            .filter { trade ->
                val tradeTime = parseISO8601ToMillis(trade.created_at)
                val isRecent = tradeTime >= recentTimeThreshold

                val matchesAmount = kotlin.math.abs(trade.amount - executionInfo.amount) <= 1
                val matchesTrend = trade.trend.trim().equals(currentOrder.trend.trim(), ignoreCase = true)
                val normalizedStatus = trade.status.trim().lowercase()
                val isCompleted = normalizedStatus in listOf("won", "lost", "lose", "loss", "failed")
                val hasResult = !trade.finished_at.isNullOrEmpty()
                val matchesAccount = trade.deal_type.equals(currentOrder.accountType, ignoreCase = true)

                isRecent && matchesAmount && matchesTrend && isCompleted && hasResult && matchesAccount
            }
            .sortedByDescending { parseISO8601ToMillis(it.created_at) }
            .firstOrNull()
    }

    private suspend fun handleTradeResult(trade: TradingHistoryRaw) {
        val currentOrder = currentMartingaleOrder ?: return
        val settings = currentMartingaleSettings ?: return
        val currentStep = currentOrder.currentStep

        stopResultMonitoring()

        val normalizedStatus = trade.status.trim().lowercase()
        println("Martingale step $currentStep result: $normalizedStatus (Trade ID: ${trade.uuid})")
        println("Trade amount: ${formatAmount(trade.amount)}")

        when (normalizedStatus) {
            "won" -> {
                totalWins++
                val payout = trade.win ?: trade.payment ?: 0L
                val totalRecovered = payout - getTotalLoss(currentOrder.scheduledOrderId)

                println("MARTINGALE WIN!")
                println("Initial loss: ${formatAmount(currentOrder.totalLoss)}")
                println("Step $currentStep amount: ${formatAmount(trade.amount)}")
                println("Step $currentStep payout: ${formatAmount(payout)}")
                println("Net recovery: ${formatAmount(totalRecovered)}")

                val result = MartingaleResult(
                    isWin = true,
                    step = currentStep,
                    amount = trade.amount,
                    totalRecovered = maxOf(0L, totalRecovered),
                    totalLoss = getTotalLoss(currentOrder.scheduledOrderId),
                    message = "Martingale WIN at step $currentStep",
                    tradeId = trade.uuid,
                    scheduledOrderId = currentOrder.scheduledOrderId
                )

                updateTotalLoss(currentOrder.scheduledOrderId, 0L)
                stopMartingale()
                onMartingaleResult(result)
            }

            "lost", "lose", "loss", "failed" -> {
                totalLosses++
                val payout = trade.win ?: trade.payment ?: 0L
                val actualLoss = trade.amount - payout
                val newTotalLoss = getTotalLoss(currentOrder.scheduledOrderId) + maxOf(0L, actualLoss)
                updateTotalLoss(currentOrder.scheduledOrderId, newTotalLoss)

                val nextStep = currentStep + 1

                println("Martingale step $currentStep LOSS")
                println("Step amount: ${formatAmount(trade.amount)}")
                println("Step payout: ${formatAmount(payout)}")
                println("Step loss: ${formatAmount(actualLoss)}")
                println("New total loss: ${formatAmount(newTotalLoss)}")

                if (nextStep <= settings.maxSteps) {
                    val nextAmount = settings.getMartingaleAmountForStep(nextStep)

                    println("Preparing martingale step $nextStep (Ultra Fast Mode)")
                    println("Next amount: ${formatAmount(nextAmount)}")

                    currentMartingaleOrder = currentOrder.copy(totalLoss = newTotalLoss)

                    val result = MartingaleResult(
                        isWin = false,
                        step = nextStep,
                        amount = nextAmount,
                        totalLoss = newTotalLoss,
                        message = "Continue to martingale step $nextStep",
                        shouldContinue = true,
                        tradeId = trade.uuid,
                        scheduledOrderId = currentOrder.scheduledOrderId
                    )

                    onMartingaleResult(result)

                    scope.launch {
                        executeStepWithMonitoring(nextStep)
                    }
                } else {
                    println("Maximum martingale steps reached")
                    handleMartingaleFailure(newTotalLoss, trade.uuid)
                }
            }

            else -> {
                println("Unknown status: ${trade.status} - treating as loss")
                handleTradeResult(trade.copy(status = "lost"))
            }
        }
    }

    private fun resetProcessingFlags() {
        isProcessingResult = false
        tradeResultReceived = false
    }

    private suspend fun handleMartingaleFailure(totalLoss: Long, tradeId: String) {
        val currentOrder = currentMartingaleOrder ?: return
        val settings = currentMartingaleSettings ?: return

        totalLosses++

        val result = MartingaleResult(
            isWin = false,
            step = currentOrder.currentStep,
            amount = 0L,
            totalLoss = totalLoss,
            message = "Martingale failed - Max ${settings.maxSteps} steps reached",
            isMaxReached = true,
            tradeId = tradeId,
            scheduledOrderId = currentOrder.scheduledOrderId
        )

        stopMartingale()
        onMartingaleResult(result)
    }

    private suspend fun handleMonitoringTimeout() {
        val currentOrder = currentMartingaleOrder ?: return
        val settings = currentMartingaleSettings ?: return
        val executionInfo = currentExecutionInfo ?: return

        val currentStep = currentOrder.currentStep
        val nextStep = currentStep + 1

        println("Monitoring timeout for step $currentStep. Assuming LOSS and continuing...")

        if (nextStep <= settings.maxSteps) {
            val nextAmount = settings.getAmountForStep(nextStep)
            val newTotalLoss = currentOrder.totalLoss + executionInfo.amount

            currentMartingaleOrder = currentOrder.copy(totalLoss = newTotalLoss)

            val result = MartingaleResult(
                isWin = false,
                step = nextStep,
                amount = nextAmount,
                totalLoss = newTotalLoss,
                message = "Step $currentStep timeout - continue to step $nextStep",
                shouldContinue = true,
                tradeId = "TIMEOUT_${System.currentTimeMillis()}",
                scheduledOrderId = currentOrder.scheduledOrderId
            )

            onMartingaleResult(result)
            resetProcessingFlags()

            executeStepWithMonitoring(nextStep)

        } else {
            handleMartingaleFailure(
                currentOrder.totalLoss + executionInfo.amount,
                "TIMEOUT_MAX_${System.currentTimeMillis()}"
            )
        }
    }

    private fun handleMaxStepsReached() {
        val currentOrder = currentMartingaleOrder ?: return

        val result = MartingaleResult(
            isWin = false,
            step = currentOrder.currentStep,
            amount = 0L,
            totalLoss = currentOrder.totalLoss,
            message = "Maximum steps (${currentOrder.maxSteps}) reached",
            isMaxReached = true,
            scheduledOrderId = currentOrder.scheduledOrderId
        )

        stopMartingale()
        onMartingaleResult(result)
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isWaitingForTradeResult || tradeResultReceived) return

        scope.launch {
            processWebSocketResultImmediate(message)
        }
    }

    private suspend fun processWebSocketResultImmediate(message: JSONObject) {
        if (isProcessingResult) return

        try {
            val event = message.optString("event", "")
            val payload = message.optJSONObject("payload")
            val currentStep = currentMartingaleOrder?.currentStep ?: -1

            println("WebSocket update for step $currentStep: $event (Immediate Processing)")

            when (event) {
                "closed", "deal_result", "trade_update" -> {
                    val orderId = payload?.optString("id", "") ?: ""
                    val status = payload?.optString("status", "") ?: ""
                    val amount = payload?.optLong("amount", 0L) ?: 0L
                    val trend = payload?.optString("trend", "") ?: ""

                    if (isWebSocketTradeMatch(orderId, status, amount, trend) && status in listOf("won", "lost")) {
                        println("WebSocket result match for step $currentStep: $status (Immediate)")
                        isProcessingResult = true
                        tradeResultReceived = true

                        val mockTrade = createMockTradeFromWebSocket(orderId, status, amount, payload)
                        handleTradeResult(mockTrade)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error processing WebSocket update: ${e.message}")
        }
    }

    private fun isWebSocketTradeMatch(orderId: String, status: String, amount: Long, trend: String): Boolean {
        val executionInfo = currentExecutionInfo ?: return false
        val currentOrder = currentMartingaleOrder ?: return false

        val timeMatch = System.currentTimeMillis() - executionInfo.executionTime < 120000L
        val amountMatch = amount == executionInfo.amount
        val trendMatch = trend.isEmpty() || trend == currentOrder.trend
        val statusMatch = status in listOf("won", "lost")

        return timeMatch && amountMatch && trendMatch && statusMatch
    }

    private fun createMockTradeFromWebSocket(
        orderId: String,
        status: String,
        amount: Long,
        payload: JSONObject?
    ): TradingHistoryRaw {
        val currentOrder = currentMartingaleOrder ?: return createEmptyTrade()

        return TradingHistoryRaw(
            id = System.currentTimeMillis(),
            status = status,
            amount = amount,
            deal_type = currentOrder.accountType,
            created_at = Instant.now().toString(),
            uuid = orderId,
            win = if (status == "won") payload?.optLong("win", 0L) else null,
            asset_id = 0,
            close_rate = payload?.optDouble("close_rate"),
            requested_by = null,
            finished_at = Instant.now().toString(),
            trend = currentOrder.trend,
            payment = payload?.optLong("payment", 0L) ?: 0L,
            payment_rate = 0,
            asset_name = "",
            asset_ric = "",
            close_quote_created_at = null,
            open_quote_created_at = null,
            open_rate = payload?.optDouble("open_rate", 0.0) ?: 0.0,
            trade_type = ""
        )
    }

    private fun resetExecutionTracking(preserveWaiting: Boolean = false) {
        currentExecutionInfo = null
        if (!preserveWaiting) {
            isWaitingForTradeResult = false
        }
        tradeResultReceived = false
        isProcessingResult = false
    }

    private fun stopResultMonitoring() {
        isMonitoringActive = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private val totalLossMap = mutableMapOf<String, Long>()

    fun stopMartingale(resetLoss: Boolean = true) {
        currentMartingaleOrder?.let { order ->
            if (resetLoss) {
                totalLossMap[order.scheduledOrderId] = 0L
            }
        }
        currentMartingaleOrder = currentMartingaleOrder?.copy(isActive = false)
        resetExecutionTracking()
        stopResultMonitoring()
        currentMartingaleSettings = null
        println("Martingale stopped and total loss reset")
    }

    private fun updateTotalLoss(scheduledOrderId: String, newLoss: Long) {
        totalLossMap[scheduledOrderId] = newLoss
    }

    private fun getTotalLoss(scheduledOrderId: String): Long {
        return totalLossMap[scheduledOrderId] ?: 0L
    }

    fun isActive(): Boolean {
        return currentMartingaleOrder?.isActive == true
    }

    fun getCurrentOrder(): MartingaleOrder? {
        return currentMartingaleOrder
    }

    fun getCurrentScheduledOrderId(): String? {
        return currentMartingaleOrder?.scheduledOrderId
    }

    fun getCurrentStep(): Int {
        return currentMartingaleOrder?.currentStep ?: -1
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
        println("Server time offset updated: $offset ms")
    }

    fun getPerformanceStats(): Map<String, Any> {
        val successRate = if (totalExecutions > 0) {
            (totalWins.toDouble() / totalExecutions * 100)
        } else 0.0

        return mapOf(
            "monitoring_interval" to "${RESULT_MONITORING_INTERVAL_MS}ms (Ultra Fast)",
            "next_step_delay" to "${NEXT_STEP_DELAY_MS}ms (Instant)",
            "execution_method" to "ULTRA_FAST_INSTANT_WITH_MONITORING",
            "total_executions" to totalExecutions,
            "total_wins" to totalWins,
            "total_losses" to totalLosses,
            "success_rate" to String.format("%.1f%%", successRate),
            "is_active" to isActive(),
            "current_step" to getCurrentStep(),
            "waiting_for_result" to isWaitingForTradeResult,
            "monitoring_active" to isMonitoringActive,
            "performance_mode" to "optimized"
        )
    }

    fun getNextExecutionTime(): Long? {
        return null
    }

    fun cleanup() {
        stopResultMonitoring()
        currentMartingaleOrder = null
        lastProcessedOrderId = null
        resetExecutionTracking()
        currentMartingaleSettings = null
        totalExecutions = 0
        totalWins = 0
        totalLosses = 0
    }

    private fun parseHistoryResponse(responseBody: Any): List<TradingHistoryRaw> {
        return try {
            when (responseBody) {
                is List<*> -> parseHistoryArray(responseBody)
                is Map<*, *> -> {
                    val data = responseBody["data"]
                    when (data) {
                        is Map<*, *> -> {
                            val deals = data["standard_trade_deals"] ?: data["deals"]
                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                        }
                        is List<*> -> parseHistoryArray(data)
                        else -> {
                            val directDeals = responseBody["standard_trade_deals"] ?: responseBody["deals"]
                            if (directDeals is List<*>) parseHistoryArray(directDeals) else emptyList()
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHistoryArray(array: List<*>): List<TradingHistoryRaw> {
        return array.mapNotNull { item ->
            try {
                if (item is Map<*, *>) {
                    parseSingleHistoryItem(item)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.created_at }
    }

    private fun parseSingleHistoryItem(item: Map<*, *>): TradingHistoryRaw {
        return TradingHistoryRaw(
            id = (item["id"] as? Number)?.toLong() ?: 0L,
            status = item["status"] as? String ?: "",
            amount = (item["amount"] as? Number)?.toLong() ?: 0L,
            deal_type = item["deal_type"] as? String ?: "",
            created_at = item["created_at"] as? String ?: "",
            uuid = item["uuid"] as? String ?: "",
            win = (item["win"] as? Number)?.toLong(),
            asset_id = (item["asset_id"] as? Number)?.toInt() ?: 0,
            close_rate = (item["close_rate"] as? Number)?.toDouble(),
            requested_by = item["requested_by"] as? String,
            finished_at = item["finished_at"] as? String,
            trend = item["trend"] as? String ?: "",
            payment = (item["payment"] as? Number)?.toLong() ?: 0L,
            payment_rate = (item["payment_rate"] as? Number)?.toInt() ?: 0,
            asset_name = item["asset_name"] as? String ?: "",
            asset_ric = item["asset_ric"] as? String ?: "",
            close_quote_created_at = item["close_quote_created_at"] as? String,
            open_quote_created_at = item["open_quote_created_at"] as? String,
            open_rate = (item["open_rate"] as? Number)?.toDouble() ?: 0.0,
            trade_type = item["trade_type"] as? String ?: ""
        )
    }

    private fun parseISO8601ToMillis(timeString: String): Long {
        return try {
            when {
                timeString.endsWith("Z") -> Instant.parse(timeString).toEpochMilli()
                timeString.contains("T") -> Instant.parse("${timeString}Z").toEpochMilli()
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

    private fun createEmptyTrade(): TradingHistoryRaw {
        return TradingHistoryRaw(
            id = 0L, status = "", amount = 0L, deal_type = "", created_at = "", uuid = "",
            win = null, asset_id = 0, close_rate = null, requested_by = null, finished_at = null,
            trend = "", payment = 0L, payment_rate = 0, asset_name = "", asset_ric = "",
            close_quote_created_at = null, open_quote_created_at = null, open_rate = 0.0, trade_type = ""
        )
    }

    private fun formatAmount(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount / 100.0)
    }
}

data class MartingaleOrder(
    val originalTrade: TradeOrder,
    val trend: String,
    val baseAmount: Long,
    val maxSteps: Int,
    val currentStep: Int,
    val totalLoss: Long,
    val isActive: Boolean,
    val accountType: String,
    val startTime: Long = System.currentTimeMillis(),
    val scheduledOrderId: String,
    val multiplierType: MultiplierType = MultiplierType.FIXED,
    val multiplierValue: Double = 2.0
)

data class MartingaleResult(
    val isWin: Boolean,
    val step: Int,
    val amount: Long,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L,
    val message: String,
    val shouldContinue: Boolean = false,
    val isMaxReached: Boolean = false,
    val tradeId: String? = null,
    val scheduledOrderId: String? = null
)

data class UserSession(
    val authtoken: String,
    val deviceType: String,
    val deviceId: String,
    val email: String,
    val userAgent: String
)