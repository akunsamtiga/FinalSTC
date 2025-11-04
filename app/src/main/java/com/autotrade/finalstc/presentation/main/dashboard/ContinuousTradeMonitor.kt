package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import com.autotrade.finalstc.presentation.main.history.TradingHistoryApi
import com.autotrade.finalstc.presentation.main.history.TradingHistoryRaw
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.time.Instant

class ContinuousTradeMonitor(
    private val scope: CoroutineScope,
    private val getUserSession: suspend () -> UserSession?,
    private val onTradeResultDetected: (String, Boolean, Map<String, Any>) -> Unit,
    private val serverTimeService: ServerTimeService
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val historyApi = retrofit.create(TradingHistoryApi::class.java)

    private var globalMonitoringJob: Job? = null
    private var isGlobalMonitoringActive = false
    private var serverTimeOffset: Long = 0L

    private val activeOrderMonitoring = mutableMapOf<String, OrderMonitoringState>()
    private val monitoringLock = Mutex()
    private var lastProcessedResults = mutableMapOf<String, String>()

    private var isPreWarmed = false
    private var lastApiCallTime = 0L
    private val preWarmInterval = 5000L

    private val CONTINUOUS_MONITORING_INTERVAL = 100L
    private val ORDER_MONITORING_TIMEOUT = 90000L
    private val WEBSOCKET_PRIORITY_WINDOW = 2000L
    private val PRE_WARM_API_CALLS = true

    private var lastWebSocketUpdateTime = System.currentTimeMillis()
    private val FAST_POLL_INTERVAL = 100L
    private val SLOW_POLL_INTERVAL = 5000L

    data class OrderMonitoringState(
        val scheduledOrderId: String,
        val trend: String,
        val amount: Long,
        val assetRic: String,
        val isDemoAccount: Boolean,
        val martingaleSettings: MartingaleState,
        val startTime: Long,
        val executionTime: Long,
        var lastCheckedTime: Long = 0L,
        var webSocketResultReceived: Boolean = false,
        var apiResultConfirmed: Boolean = false,
        var isCompleted: Boolean = false
    )

    fun notifyWebSocketUpdate() {
        lastWebSocketUpdateTime = System.currentTimeMillis()
    }

    fun startMonitoring() {
        if (isGlobalMonitoringActive) return

        isGlobalMonitoringActive = true
        println("Monitoring berkelanjutan dimulai")
        println("Interval: ${CONTINUOUS_MONITORING_INTERVAL}ms")
        println("Timeout: ${ORDER_MONITORING_TIMEOUT / 1000}s")
        println("Pre-warming: $PRE_WARM_API_CALLS")

        startGlobalMonitoringLoop()
        if (PRE_WARM_API_CALLS) {
            startPreWarmingLoop()
        }
    }

    fun pauseMonitoring() {
        isGlobalMonitoringActive = false
        globalMonitoringJob?.cancel()
        println("Monitoring berkelanjutan dijeda")
    }

    fun resumeMonitoring() {
        if (isGlobalMonitoringActive) return

        isGlobalMonitoringActive = true
        println("Monitoring berkelanjutan dilanjutkan")
        startGlobalMonitoringLoop()
        if (PRE_WARM_API_CALLS) {
            startPreWarmingLoop()
        }
    }

    fun stopMonitoring() {
        isGlobalMonitoringActive = false
        globalMonitoringJob?.cancel()
        globalMonitoringJob = null

        scope.launch {
            monitoringLock.withLock {
                activeOrderMonitoring.clear()
                lastProcessedResults.clear()
            }
        }

        isPreWarmed = false
        println("Monitoring berkelanjutan dihentikan")
    }

    fun startMonitoringScheduledOrder(
        scheduledOrderId: String,
        trend: String,
        amount: Long,
        assetRic: String,
        isDemoAccount: Boolean,
        martingaleSettings: MartingaleState,
        startTimeMillis: Long
    ) {
        scope.launch {
            monitoringLock.withLock {
                val serverTime = serverTimeService?.getCurrentServerTimeMillis()
                    ?: (System.currentTimeMillis() + serverTimeOffset)

                val monitoringState = OrderMonitoringState(
                    scheduledOrderId = scheduledOrderId,
                    trend = trend,
                    amount = amount,
                    assetRic = assetRic,
                    isDemoAccount = isDemoAccount,
                    martingaleSettings = martingaleSettings,
                    startTime = serverTime,
                    executionTime = serverTime
                )
                activeOrderMonitoring[scheduledOrderId] = monitoringState

                println("Memulai monitoring order dengan server time:")
                println("Order ID: $scheduledOrderId")
                println("Start Time: $startTimeMillis")
            }
        }
    }

    fun stopMonitoringOrder(orderId: String) {
        scope.launch {
            monitoringLock.withLock {
                activeOrderMonitoring.remove(orderId)
                lastProcessedResults.remove(orderId)
                println("Berhenti monitoring order: $orderId")
            }
        }
    }

    private fun startGlobalMonitoringLoop() {
        globalMonitoringJob = scope.launch {
            while (isGlobalMonitoringActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val ordersToCheck = mutableListOf<OrderMonitoringState>()
                    val ordersToComplete = mutableListOf<String>()

                    monitoringLock.withLock {
                        activeOrderMonitoring.values.forEach { orderState ->
                            when {
                                orderState.isCompleted -> {
                                    ordersToComplete.add(orderState.scheduledOrderId)
                                }
                                currentTime - orderState.startTime > ORDER_MONITORING_TIMEOUT -> {
                                    println("Timeout monitoring order: ${orderState.scheduledOrderId}")
                                    ordersToComplete.add(orderState.scheduledOrderId)
                                }
                                !orderState.webSocketResultReceived ||
                                        (currentTime - orderState.executionTime > WEBSOCKET_PRIORITY_WINDOW) -> {
                                    ordersToCheck.add(orderState)
                                }
                            }
                        }

                        ordersToComplete.forEach { orderId ->
                            activeOrderMonitoring.remove(orderId)
                            lastProcessedResults.remove(orderId)
                        }
                    }

                    if (ordersToCheck.isNotEmpty()) {
                        checkOrdersViaApi(ordersToCheck)
                    }

                    val delayInterval = if (System.currentTimeMillis() - lastWebSocketUpdateTime > WEBSOCKET_PRIORITY_WINDOW) {
                        FAST_POLL_INTERVAL
                    } else {
                        SLOW_POLL_INTERVAL
                    }
                    delay(delayInterval)

                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    println("Error dalam monitoring berkelanjutan: ${e.message}")
                    delay(1000L)
                }
            }
        }
    }

    private fun startPreWarmingLoop() {
        scope.launch {
            while (isGlobalMonitoringActive) {
                try {
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastApiCallTime > preWarmInterval) {
                        preWarmApiConnection()
                        lastApiCallTime = currentTime
                    }

                    delay(preWarmInterval)

                } catch (e: Exception) {
                    delay(10000L)
                }
            }
        }
    }

    private suspend fun preWarmApiConnection() {
        if (!isGlobalMonitoringActive) return

        try {
            val userSession = getUserSession() ?: return

            val response = historyApi.getTradingHistoryRaw(
                type = "demo",
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType,
                deviceId = userSession.deviceId,
                timezone = "Asia/Jakarta",
                origin = "https://stockity.id",
                referer = "https://stockity.id",
                accept = "application/json, text/plain, */*"
            )

            if (response.isSuccessful) {
                if (!isPreWarmed) {
                    isPreWarmed = true
                    println("Koneksi API berhasil di-prewarming")
                }
            }

        } catch (e: Exception) {
            isPreWarmed = false
        }
    }

    private suspend fun checkOrdersViaApi(ordersToCheck: List<OrderMonitoringState>) {
        if (!isGlobalMonitoringActive || ordersToCheck.isEmpty()) return

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
            println("Error memeriksa order via API: ${e.message}")
        }
    }

    private suspend fun checkOrdersForAccountType(
        orders: List<OrderMonitoringState>,
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

                orders.forEach { orderState ->
                    if (!orderState.isCompleted) {
                        val relevantTrade = findRelevantTradeForOrder(historyData, orderState)
                        if (relevantTrade != null) {
                            processTradeResult(orderState, relevantTrade)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("Error memeriksa order $accountType: ${e.message}")
        }
    }

    private fun findRelevantTradeForOrder(
        historyData: List<TradingHistoryRaw>,
        orderState: OrderMonitoringState
    ): TradingHistoryRaw? {
        val recentTimeThreshold = orderState.executionTime - 10000L
        val lastProcessedTradeId = lastProcessedResults[orderState.scheduledOrderId]

        return historyData
            .filter { trade ->
                val tradeTime = parseISO8601ToMillis(trade.created_at)
                val isRecent = tradeTime >= recentTimeThreshold
                val matchesAmount = trade.amount == orderState.amount
                val matchesTrend = trade.trend == orderState.trend
                val matchesAsset = trade.asset_ric == orderState.assetRic || orderState.assetRic.isEmpty()
                val isCompleted = trade.status in listOf("won", "lost")
                val hasResult = !trade.finished_at.isNullOrEmpty()
                val matchesAccount = trade.deal_type == if (orderState.isDemoAccount) "demo" else "real"
                val isNotProcessed = trade.uuid != lastProcessedTradeId

                isRecent && matchesAmount && matchesTrend && matchesAsset &&
                        isCompleted && hasResult && matchesAccount && isNotProcessed
            }
            .sortedByDescending { parseISO8601ToMillis(it.created_at) }
            .firstOrNull()
    }

    private suspend fun processTradeResult(orderState: OrderMonitoringState, trade: TradingHistoryRaw) {
        monitoringLock.withLock {
            if (orderState.isCompleted) return@withLock

            val isWin = trade.status == "won"
            lastProcessedResults[orderState.scheduledOrderId] = trade.uuid

            activeOrderMonitoring[orderState.scheduledOrderId] = orderState.copy(
                apiResultConfirmed = true,
                isCompleted = true
            )

            println("Hasil trade terdeteksi (Monitoring Berkelanjutan):")
            println("Order ID: ${orderState.scheduledOrderId}")
            println("Trade ID: ${trade.uuid}")
            println("Hasil: ${if (isWin) "MENANG" else "KALAH"}")
            println("Jumlah: ${formatAmount(trade.amount)} IDR")
            println("Deteksi: API Monitoring Berkelanjutan")

            val resultDetails = mapOf(
                "trade_id" to trade.uuid,
                "amount" to trade.amount,
                "trend" to trade.trend,
                "asset_ric" to trade.asset_ric,
                "status" to trade.status,
                "win_amount" to (trade.win ?: 0L),
                "payment" to trade.payment,
                "detection_method" to "continuous_api_monitoring",
                "detection_time" to System.currentTimeMillis(),
                "monitoring_duration" to (System.currentTimeMillis() - orderState.startTime)
            )

            onTradeResultDetected(orderState.scheduledOrderId, isWin, resultDetails)
        }
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isGlobalMonitoringActive) return

        scope.launch {
            try {
                val event = message.optString("event", "")
                val payload = message.optJSONObject("payload")

                when (event) {
                    "closed", "deal_result", "trade_update" -> {
                        notifyWebSocketUpdate()

                        val orderId = payload?.optString("id", "") ?: ""
                        val status = payload?.optString("status", "") ?: ""
                        val amount = payload?.optLong("amount", 0L) ?: 0L
                        val trend = payload?.optString("trend", "") ?: ""

                        if (orderId.isNotEmpty() && status in listOf("won", "lost")) {
                            processWebSocketTradeResult(orderId, status, amount, trend, payload)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error memproses update WebSocket trade: ${e.message}")
            }
        }
    }

    private suspend fun processWebSocketTradeResult(
        tradeId: String,
        status: String,
        amount: Long,
        trend: String,
        payload: JSONObject?
    ) {
        monitoringLock.withLock {
            val matchingOrderId = activeOrderMonitoring.entries.find { (_, orderState) ->
                !orderState.isCompleted &&
                        orderState.amount == amount &&
                        orderState.trend == trend &&
                        System.currentTimeMillis() - orderState.executionTime < 120000L
            }?.key

            if (matchingOrderId != null) {
                val orderState = activeOrderMonitoring[matchingOrderId] ?: return@withLock

                activeOrderMonitoring[matchingOrderId] = orderState.copy(
                    webSocketResultReceived = true,
                    isCompleted = true
                )

                val isWin = status == "won"
                lastProcessedResults[matchingOrderId] = tradeId

                println("Hasil trade terdeteksi (WebSocket - Prioritas):")
                println("Order ID: $matchingOrderId")
                println("Trade ID: $tradeId")
                println("Hasil: ${if (isWin) "MENANG" else "KALAH"}")
                println("Jumlah: ${formatAmount(amount)} IDR")
                println("Deteksi: WebSocket (Prioritas)")

                val resultDetails = mapOf(
                    "trade_id" to tradeId,
                    "amount" to amount,
                    "trend" to trend,
                    "status" to status,
                    "win_amount" to (payload?.optLong("win", 0L) ?: 0L),
                    "payment" to (payload?.optLong("payment", 0L) ?: 0L),
                    "detection_method" to "websocket_priority",
                    "detection_time" to System.currentTimeMillis(),
                    "monitoring_duration" to (System.currentTimeMillis() - orderState.startTime)
                )

                onTradeResultDetected(matchingOrderId, isWin, resultDetails)
            }
        }
    }

    fun isActive(): Boolean {
        return isGlobalMonitoringActive
    }

    fun isMonitoringOrder(orderId: String): Boolean {
        return activeOrderMonitoring.containsKey(orderId)
    }

    fun getMonitoringStatus(): String {
        val activeCount = activeOrderMonitoring.size
        val preWarmStatus = if (isPreWarmed) "Siap" else "Dingin"

        return when {
            !isGlobalMonitoringActive -> "Tidak Aktif"
            activeCount == 0 -> "$preWarmStatus Pre-warmed & Siap"
            activeCount == 1 -> "$preWarmStatus Monitoring 1 order"
            else -> "$preWarmStatus Monitoring $activeCount orders"
        }
    }

    fun getConfiguration(): Map<String, Any> {
        return mapOf(
            "monitoring_interval" to "${CONTINUOUS_MONITORING_INTERVAL}ms",
            "timeout_seconds" to (ORDER_MONITORING_TIMEOUT / 1000),
            "prewarm_enabled" to PRE_WARM_API_CALLS,
            "websocket_integration" to true,
            "websocket_priority_window_ms" to WEBSOCKET_PRIORITY_WINDOW,
            "active_orders" to activeOrderMonitoring.size,
            "is_prewarmed" to isPreWarmed,
            "is_active" to isGlobalMonitoringActive
        )
    }

    fun updateServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
    }

    fun cleanup() {
        stopMonitoring()
        isPreWarmed = false
        lastApiCallTime = 0L
        println("Continuous monitoring dibersihkan")
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

    private fun formatAmount(amount: Long): String {
        return String.format("%,d", amount)
    }
}