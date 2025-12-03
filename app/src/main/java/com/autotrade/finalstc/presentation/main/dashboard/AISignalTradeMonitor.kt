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
import org.json.JSONObject
import java.time.Instant

class AISignalTradeMonitor(
    private val scope: CoroutineScope,
    private val getUserSession: suspend () -> UserSession?,
    private val onTradeResultDetected: (AISignalTradeResult) -> Unit,
    private val serverTimeService: ServerTimeService
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val historyApi = retrofit.create(TradingHistoryApi::class.java)

    private var monitoringJob: Job? = null
    private var isActive = false

    private val activeMonitoring = mutableMapOf<String, AISignalOrderMonitoring>()
    private val monitoringLock = Mutex()
    private val processedResults = mutableMapOf<String, String>()

    private val MONITORING_INTERVAL_MS = 50L
    private val MONITORING_TIMEOUT_MS = 90000L
    private val WEBSOCKET_PRIORITY_WINDOW_MS = 2000L

    private var lastWebSocketUpdateTime = System.currentTimeMillis()

    data class AISignalOrderMonitoring(
        val parentOrderId: String,
        val monitoringOrderId: String, // Full ID with martingale suffix if applicable
        val trend: String,
        val amount: Long,
        val assetRic: String,
        val isDemoAccount: Boolean,
        val isMartingale: Boolean,
        val martingaleStep: Int,
        val startTime: Long,
        val executionTime: Long,
        var lastCheckedTime: Long = 0L,
        var webSocketResultReceived: Boolean = false,
        var isCompleted: Boolean = false
    )

    data class AISignalTradeResult(
        val parentOrderId: String,
        val monitoringOrderId: String,
        val isWin: Boolean,
        val isMartingale: Boolean,
        val martingaleStep: Int,
        val details: Map<String, Any>
    )

    companion object {
        private const val TAG = "AISignalTradeMonitor"
    }

    fun startMonitoring() {
        if (isActive) return

        isActive = true
        println("$TAG: Starting AI Signal monitoring")

        startMonitoringLoop()
    }

    fun stopMonitoring() {
        isActive = false
        monitoringJob?.cancel()
        monitoringJob = null

        scope.launch {
            monitoringLock.withLock {
                activeMonitoring.clear()
                processedResults.clear()
            }
        }

        println("$TAG: Monitoring stopped")
    }

    fun startMonitoringOrder(
        parentOrderId: String,
        trend: String,
        amount: Long,
        assetRic: String,
        isDemoAccount: Boolean,
        isMartingale: Boolean,
        martingaleStep: Int
    ) {
        scope.launch {
            monitoringLock.withLock {
                val monitoringOrderId = if (isMartingale) {
                    "${parentOrderId}_martingale_${martingaleStep}"
                } else {
                    parentOrderId
                }

                val serverTime = serverTimeService.getCurrentServerTimeMillis()

                val monitoring = AISignalOrderMonitoring(
                    parentOrderId = parentOrderId,
                    monitoringOrderId = monitoringOrderId,
                    trend = trend,
                    amount = amount,
                    assetRic = assetRic,
                    isDemoAccount = isDemoAccount,
                    isMartingale = isMartingale,
                    martingaleStep = martingaleStep,
                    startTime = serverTime,
                    executionTime = serverTime
                )

                activeMonitoring[monitoringOrderId] = monitoring

                println("=" .repeat(60))
                println("$TAG: MONITORING STARTED")
                println("=" .repeat(60))
                println("   Parent Order: $parentOrderId")
                println("   Monitoring ID: $monitoringOrderId")
                println("   Is Martingale: $isMartingale")
                println("   Step: $martingaleStep")
                println("   Amount: ${formatAmount(amount)}")
                println("=" .repeat(60))
            }
        }
    }

    fun stopMonitoringOrder(orderId: String) {
        scope.launch {
            monitoringLock.withLock {
                activeMonitoring.remove(orderId)
                processedResults.remove(orderId)
                println("$TAG: Stopped monitoring $orderId")
            }
        }
    }

    private fun startMonitoringLoop() {
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val ordersToCheck = mutableListOf<AISignalOrderMonitoring>()
                    val ordersToComplete = mutableListOf<String>()

                    monitoringLock.withLock {
                        activeMonitoring.values.forEach { monitoring ->
                            when {
                                monitoring.isCompleted -> {
                                    ordersToComplete.add(monitoring.monitoringOrderId)
                                }
                                currentTime - monitoring.startTime > MONITORING_TIMEOUT_MS -> {
                                    println("$TAG: Timeout for ${monitoring.monitoringOrderId}")
                                    ordersToComplete.add(monitoring.monitoringOrderId)
                                }
                                !monitoring.webSocketResultReceived ||
                                        (currentTime - monitoring.executionTime > WEBSOCKET_PRIORITY_WINDOW_MS) -> {
                                    ordersToCheck.add(monitoring)
                                }
                            }
                        }

                        ordersToComplete.forEach { orderId ->
                            activeMonitoring.remove(orderId)
                            processedResults.remove(orderId)
                        }
                    }

                    if (ordersToCheck.isNotEmpty()) {
                        checkOrdersViaApi(ordersToCheck)
                    }

                    val delayInterval = if (currentTime - lastWebSocketUpdateTime > WEBSOCKET_PRIORITY_WINDOW_MS) {
                        MONITORING_INTERVAL_MS
                    } else {
                        200L
                    }

                    delay(delayInterval)

                } catch (e: Exception) {
                    println("$TAG: Error in monitoring loop: ${e.message}")
                    delay(1000L)
                }
            }
        }
    }

    private suspend fun checkOrdersViaApi(ordersToCheck: List<AISignalOrderMonitoring>) {
        if (!isActive || ordersToCheck.isEmpty()) return

        try {
            val userSession = getUserSession() ?: return

            val demoOrders = ordersToCheck.filter { it.isDemoAccount }
            val realOrders = ordersToCheck.filter { !it.isDemoAccount }

            if (demoOrders.isNotEmpty()) {
                checkOrdersForAccountType(demoOrders, "demo", userSession)
            }

            if (realOrders.isNotEmpty()) {
                checkOrdersForAccountType(realOrders, "real", userSession)
            }

        } catch (e: Exception) {
            println("$TAG: Error checking orders via API: ${e.message}")
        }
    }

    private suspend fun checkOrdersForAccountType(
        orders: List<AISignalOrderMonitoring>,
        accountType: String,
        userSession: UserSession
    ) {
        try {
            val response = historyApi.getTradingHistoryRaw(
                type = accountType,
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

                orders.forEach { monitoring ->
                    if (!monitoring.isCompleted) {
                        val relevantTrade = findRelevantTrade(historyData, monitoring)
                        if (relevantTrade != null) {
                            processTradeResult(monitoring, relevantTrade)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("$TAG: Error checking $accountType orders: ${e.message}")
        }
    }

    private fun findRelevantTrade(
        historyData: List<TradingHistoryRaw>,
        monitoring: AISignalOrderMonitoring
    ): TradingHistoryRaw? {
        val recentTimeThreshold = monitoring.executionTime - 10000L
        val lastProcessedTradeId = processedResults[monitoring.monitoringOrderId]

        return historyData
            .filter { trade ->
                val tradeTime = parseISO8601ToMillis(trade.created_at)
                val isRecent = tradeTime >= recentTimeThreshold
                val matchesAmount = trade.amount == monitoring.amount
                val matchesTrend = trade.trend == monitoring.trend
                val isCompleted = trade.status in listOf("won", "lost")
                val hasResult = !trade.finished_at.isNullOrEmpty()
                val matchesAccount = trade.deal_type == if (monitoring.isDemoAccount) "demo" else "real"
                val isNotProcessed = trade.uuid != lastProcessedTradeId

                isRecent && matchesAmount && matchesTrend &&
                        isCompleted && hasResult && matchesAccount && isNotProcessed
            }
            .sortedByDescending { parseISO8601ToMillis(it.created_at) }
            .firstOrNull()
    }

    private suspend fun processTradeResult(
        monitoring: AISignalOrderMonitoring,
        trade: TradingHistoryRaw
    ) {
        monitoringLock.withLock {
            if (monitoring.isCompleted) return@withLock

            val isWin = trade.status == "won"
            processedResults[monitoring.monitoringOrderId] = trade.uuid

            activeMonitoring[monitoring.monitoringOrderId] = monitoring.copy(
                isCompleted = true
            )

            println("=" .repeat(60))
            println("$TAG: TRADE RESULT DETECTED (API)")
            println("=" .repeat(60))
            println("   Parent Order: ${monitoring.parentOrderId}")
            println("   Monitoring ID: ${monitoring.monitoringOrderId}")
            println("   Trade ID: ${trade.uuid}")
            println("   Is Martingale: ${monitoring.isMartingale}")
            println("   Step: ${monitoring.martingaleStep}")
            println("   Result: ${if (isWin) "WIN ✅" else "LOSE ❌"}")
            println("   Amount: ${formatAmount(trade.amount)}")
            println("=" .repeat(60))

            val resultDetails = mapOf(
                "trade_id" to trade.uuid,
                "amount" to trade.amount,
                "trend" to trade.trend,
                "status" to trade.status,
                "win_amount" to (trade.win ?: 0L),
                "payment" to trade.payment,
                "detection_method" to "ai_signal_monitor_api",
                "detection_time" to System.currentTimeMillis(),
                "monitoring_duration" to (System.currentTimeMillis() - monitoring.startTime)
            )

            val result = AISignalTradeResult(
                parentOrderId = monitoring.parentOrderId,
                monitoringOrderId = monitoring.monitoringOrderId,
                isWin = isWin,
                isMartingale = monitoring.isMartingale,
                martingaleStep = monitoring.martingaleStep,
                details = resultDetails
            )

            onTradeResultDetected(result)
        }
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isActive) return

        scope.launch {
            try {
                val event = message.optString("event", "")
                val payload = message.optJSONObject("payload")

                when (event) {
                    "closed", "deal_result", "trade_update" -> {
                        lastWebSocketUpdateTime = System.currentTimeMillis()

                        val orderId = payload?.optString("id", "") ?: ""
                        val status = payload?.optString("status", "") ?: ""
                        val amount = payload?.optLong("amount", 0L) ?: 0L
                        val trend = payload?.optString("trend", "") ?: ""

                        if (orderId.isNotEmpty() && status in listOf("won", "lost")) {
                            processWebSocketResult(orderId, status, amount, trend, payload)
                        }
                    }
                }
            } catch (e: Exception) {
                println("$TAG: Error processing WebSocket update: ${e.message}")
            }
        }
    }

    private suspend fun processWebSocketResult(
        tradeId: String,
        status: String,
        amount: Long,
        trend: String,
        payload: JSONObject?
    ) {
        monitoringLock.withLock {
            val matchingMonitoring = activeMonitoring.values.find { monitoring ->
                !monitoring.isCompleted &&
                        monitoring.amount == amount &&
                        monitoring.trend == trend &&
                        System.currentTimeMillis() - monitoring.executionTime < 120000L
            }

            if (matchingMonitoring != null) {
                activeMonitoring[matchingMonitoring.monitoringOrderId] = matchingMonitoring.copy(
                    webSocketResultReceived = true,
                    isCompleted = true
                )

                val isWin = status == "won"
                processedResults[matchingMonitoring.monitoringOrderId] = tradeId

                println("=" .repeat(60))
                println("$TAG: TRADE RESULT DETECTED (WebSocket)")
                println("=" .repeat(60))
                println("   Parent Order: ${matchingMonitoring.parentOrderId}")
                println("   Monitoring ID: ${matchingMonitoring.monitoringOrderId}")
                println("   Trade ID: $tradeId")
                println("   Is Martingale: ${matchingMonitoring.isMartingale}")
                println("   Step: ${matchingMonitoring.martingaleStep}")
                println("   Result: ${if (isWin) "WIN ✅" else "LOSE ❌"}")
                println("=" .repeat(60))

                val resultDetails = mapOf(
                    "trade_id" to tradeId,
                    "amount" to amount,
                    "trend" to trend,
                    "status" to status,
                    "win_amount" to (payload?.optLong("win", 0L) ?: 0L),
                    "payment" to (payload?.optLong("payment", 0L) ?: 0L),
                    "detection_method" to "ai_signal_monitor_websocket",
                    "detection_time" to System.currentTimeMillis(),
                    "monitoring_duration" to (System.currentTimeMillis() - matchingMonitoring.startTime)
                )

                val result = AISignalTradeResult(
                    parentOrderId = matchingMonitoring.parentOrderId,
                    monitoringOrderId = matchingMonitoring.monitoringOrderId,
                    isWin = isWin,
                    isMartingale = matchingMonitoring.isMartingale,
                    martingaleStep = matchingMonitoring.martingaleStep,
                    details = resultDetails
                )

                onTradeResultDetected(result)
            }
        }
    }

    fun isActive(): Boolean = isActive

    fun getMonitoringStatus(): Map<String, Any> {
        return mapOf(
            "is_active" to isActive,
            "active_monitoring_count" to activeMonitoring.size,
            "monitoring_interval_ms" to MONITORING_INTERVAL_MS,
            "timeout_ms" to MONITORING_TIMEOUT_MS,
            "processed_results_count" to processedResults.size
        )
    }

    fun cleanup() {
        stopMonitoring()
        println("$TAG: Cleanup completed")
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
                        else -> emptyList()
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
                } else null
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

    private fun formatAmount(amount: Long): String {
        return String.format("%,d", amount)
    }
}