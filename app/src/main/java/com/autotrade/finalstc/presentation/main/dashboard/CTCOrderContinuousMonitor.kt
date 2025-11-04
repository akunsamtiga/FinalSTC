package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.time.Instant
import kotlin.math.abs
import com.autotrade.finalstc.presentation.main.history.TradingHistoryApi
import com.autotrade.finalstc.presentation.main.history.TradingHistoryRaw

class CTCOrderContinuousMonitor(
    private val scope: CoroutineScope,
    private val getUserSession: suspend () -> UserSession?,
    private val onCTCTradeResult: (String, Boolean, Map<String, Any>) -> Unit,
    private val serverTimeService: ServerTimeService?
) {
    companion object {
        private const val TAG = "CTCOrderMonitor"
        private const val ULTRA_FAST_INTERVAL = 50L
        private const val FAST_POLL_INTERVAL = 100L
        private const val SLOW_POLL_INTERVAL = 2000L
        private const val CTC_ORDER_TIMEOUT = 90000L
        private const val WEBSOCKET_PRIORITY_WINDOW = 3000L
        private const val PRE_WARM_INTERVAL = 3000L
        private const val BALANCE_CHECK_INTERVAL = 200L
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val historyApi = retrofit.create(TradingHistoryApi::class.java)

    private var isMonitoringActive = false
    private var monitoringJob: Job? = null
    private var preWarmJob: Job? = null
    private var balanceMonitorJob: Job? = null
    private val monitoringMutex = Mutex()

    private val activeCTCOrders = mutableMapOf<String, CTCOrderMonitoringState>()
    private val processedResults = mutableMapOf<String, String>()

    private var lastWebSocketUpdate = 0L
    private var lastApiCall = 0L
    private var lastBalanceUpdate = 0L
    private var isPreWarmed = false
    private var lastBalance = 0L
    private val balanceHistory = mutableListOf<BalanceSnapshot>()

    data class CTCOrderMonitoringState(
        val ctcOrderId: String,
        val trend: String,
        val amount: Long,
        val assetRic: String,
        val isDemoAccount: Boolean,
        val executionTime: Long,
        val cycleNumber: Int,
        var isWebSocketDetected: Boolean = false,
        var isApiDetected: Boolean = false,
        var isBalanceDetected: Boolean = false,
        var isCompleted: Boolean = false,
        var lastChecked: Long = System.currentTimeMillis()
    )

    data class BalanceSnapshot(
        val balance: Long,
        val timestamp: Long,
        val change: Long = 0L
    )

    fun startMonitoring() {
        if (isMonitoringActive) {
            Log.d(TAG, "CTC Order monitoring already active")
            return
        }

        isMonitoringActive = true
        Log.d(TAG, "Starting ultra-fast CTC Order monitoring")
        Log.d(TAG, "  Ultra-fast interval: ${ULTRA_FAST_INTERVAL}ms")
        Log.d(TAG, "  Balance check interval: ${BALANCE_CHECK_INTERVAL}ms")
        Log.d(TAG, "  Pre-warm interval: ${PRE_WARM_INTERVAL}ms")
        Log.d(TAG, "  Mode: IDENTICAL to Follow Order monitoring")

        startMainMonitoringLoop()
        startPreWarmingLoop()
        startBalanceMonitoring()
    }

    fun stopMonitoring() {
        if (!isMonitoringActive) return

        isMonitoringActive = false

        monitoringJob?.cancel()
        preWarmJob?.cancel()
        balanceMonitorJob?.cancel()

        scope.launch {
            monitoringMutex.withLock {
                activeCTCOrders.clear()
                processedResults.clear()
                balanceHistory.clear()
            }
        }

        isPreWarmed = false
        Log.d(TAG, "CTC Order monitoring stopped")
    }

    fun addCTCOrderForMonitoring(
        ctcOrderId: String,
        trend: String,
        amount: Long,
        assetRic: String,
        isDemoAccount: Boolean,
        cycleNumber: Int
    ) {
        scope.launch {
            monitoringMutex.withLock {
                val executionTime = serverTimeService?.getCurrentServerTimeMillis()
                    ?: System.currentTimeMillis()

                val monitoringState = CTCOrderMonitoringState(
                    ctcOrderId = ctcOrderId,
                    trend = trend,
                    amount = amount,
                    assetRic = assetRic,
                    isDemoAccount = isDemoAccount,
                    executionTime = executionTime,
                    cycleNumber = cycleNumber
                )

                activeCTCOrders[ctcOrderId] = monitoringState

                Log.d(TAG, "Added CTC Order for ultra-fast monitoring:")
                Log.d(TAG, "  Order ID: $ctcOrderId")
                Log.d(TAG, "  Cycle: $cycleNumber")
                Log.d(TAG, "  Trend: $trend")
                Log.d(TAG, "  Amount: ${formatAmount(amount)}")
                Log.d(TAG, "  Account: ${if (isDemoAccount) "Demo" else "Real"}")
            }
        }
    }

    fun removeCTCOrderFromMonitoring(ctcOrderId: String) {
        scope.launch {
            monitoringMutex.withLock {
                activeCTCOrders.remove(ctcOrderId)
                processedResults.remove(ctcOrderId)
                Log.d(TAG, "Removed CTC Order from monitoring: $ctcOrderId")
            }
        }
    }

    private fun startMainMonitoringLoop() {
        monitoringJob = scope.launch {
            while (isMonitoringActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val ordersToCheck = mutableListOf<CTCOrderMonitoringState>()
                    val ordersToComplete = mutableListOf<String>()

                    monitoringMutex.withLock {
                        activeCTCOrders.values.forEach { orderState ->
                            when {
                                orderState.isCompleted -> {
                                    ordersToComplete.add(orderState.ctcOrderId)
                                }
                                currentTime - orderState.executionTime > CTC_ORDER_TIMEOUT -> {
                                    Log.w(TAG, "CTC Order timeout: ${orderState.ctcOrderId}")
                                    handleCTCOrderTimeout(orderState)
                                    ordersToComplete.add(orderState.ctcOrderId)
                                }
                                !orderState.isWebSocketDetected && !orderState.isApiDetected -> {
                                    ordersToCheck.add(orderState)
                                }
                            }
                        }

                        ordersToComplete.forEach { orderId ->
                            activeCTCOrders.remove(orderId)
                        }
                    }

                    if (ordersToCheck.isNotEmpty()) {
                        checkCTCOrdersViaAPI(ordersToCheck)
                    }

                    val delayInterval = when {
                        ordersToCheck.isNotEmpty() -> ULTRA_FAST_INTERVAL
                        currentTime - lastWebSocketUpdate < WEBSOCKET_PRIORITY_WINDOW -> FAST_POLL_INTERVAL
                        else -> SLOW_POLL_INTERVAL
                    }

                    delay(delayInterval)

                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.e(TAG, "Error in CTC Order monitoring loop: ${e.message}", e)
                    delay(1000L)
                }
            }
        }
    }

    private fun startPreWarmingLoop() {
        preWarmJob = scope.launch {
            while (isMonitoringActive) {
                try {
                    if (System.currentTimeMillis() - lastApiCall > PRE_WARM_INTERVAL) {
                        preWarmAPIConnections()
                    }
                    delay(PRE_WARM_INTERVAL)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pre-warming loop: ${e.message}", e)
                    delay(5000L)
                }
            }
        }
    }

    private fun startBalanceMonitoring() {
        balanceMonitorJob = scope.launch {
            while (isMonitoringActive) {
                try {
                    delay(BALANCE_CHECK_INTERVAL)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.e(TAG, "Error in balance monitoring: ${e.message}", e)
                    delay(1000L)
                }
            }
        }
    }

    private suspend fun preWarmAPIConnections() {
        if (!isMonitoringActive) return

        try {
            val userSession = getUserSession() ?: return

            val demoResponse = historyApi.getTradingHistoryRaw(
                type = "demo",
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType,
                deviceId = userSession.deviceId,
                timezone = "Asia/Jakarta",
                origin = "https://stockity.id",
                referer = "https://stockity.id",
                accept = "application/json, text/plain, */*"
            )

            if (demoResponse.isSuccessful) {
                if (!isPreWarmed) {
                    isPreWarmed = true
                    Log.d(TAG, "CTC Order API pre-warming completed")
                }
            }

            lastApiCall = System.currentTimeMillis()

        } catch (e: Exception) {
            isPreWarmed = false
            Log.e(TAG, "Pre-warming failed: ${e.message}", e)
        }
    }

    private suspend fun checkCTCOrdersViaAPI(ordersToCheck: List<CTCOrderMonitoringState>) {
        if (!isMonitoringActive || ordersToCheck.isEmpty()) return

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

            lastApiCall = System.currentTimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Error checking CTC orders via API: ${e.message}", e)
        }
    }

    private suspend fun checkOrdersForAccountType(
        orders: List<CTCOrderMonitoringState>,
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
                        val relevantTrade = findRelevantTradeForCTCOrder(historyData, orderState)
                        if (relevantTrade != null) {
                            processCTCOrderResult(orderState, relevantTrade, "API_DETECTION")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking $accountType orders: ${e.message}", e)
        }
    }

    private fun findRelevantTradeForCTCOrder(
        historyData: List<TradingHistoryRaw>,
        orderState: CTCOrderMonitoringState
    ): TradingHistoryRaw? {
        val searchWindowStart = orderState.executionTime - 15000L
        val searchWindowEnd = orderState.executionTime + 300000L
        val processedTradeId = processedResults[orderState.ctcOrderId]

        return historyData
            .filter { trade ->
                val tradeTime = parseTradeTime(trade.created_at)
                val isInTimeWindow = tradeTime in searchWindowStart..searchWindowEnd
                val matchesAmount = trade.amount == orderState.amount
                val matchesTrend = trade.trend.equals(orderState.trend, ignoreCase = true)
                val isCompleted = trade.status.lowercase() in listOf("won", "lost", "win", "lose")
                val hasFinished = !trade.finished_at.isNullOrEmpty()
                val matchesAccount = trade.deal_type.equals(
                    if (orderState.isDemoAccount) "demo" else "real",
                    ignoreCase = true
                )
                val notAlreadyProcessed = trade.uuid != processedTradeId

                isInTimeWindow && matchesAmount && matchesTrend &&
                        isCompleted && hasFinished && matchesAccount && notAlreadyProcessed
            }
            .sortedByDescending { parseTradeTime(it.created_at) }
            .firstOrNull()
    }

    private suspend fun processCTCOrderResult(
        orderState: CTCOrderMonitoringState,
        trade: TradingHistoryRaw,
        detectionMethod: String
    ) {
        monitoringMutex.withLock {
            if (orderState.isCompleted) return@withLock

            val isWin = trade.status.lowercase() in listOf("won", "win")
            processedResults[orderState.ctcOrderId] = trade.uuid

            activeCTCOrders[orderState.ctcOrderId] = orderState.copy(
                isApiDetected = detectionMethod.contains("API"),
                isWebSocketDetected = detectionMethod.contains("WEBSOCKET"),
                isBalanceDetected = detectionMethod.contains("BALANCE"),
                isCompleted = true
            )

            Log.d(TAG, "CTC Order result detected ($detectionMethod):")
            Log.d(TAG, "  Order ID: ${orderState.ctcOrderId}")
            Log.d(TAG, "  Cycle: ${orderState.cycleNumber}")
            Log.d(TAG, "  Trade ID: ${trade.uuid}")
            Log.d(TAG, "  Result: ${if (isWin) "WIN" else "LOSE"}")
            Log.d(TAG, "  Amount: ${formatAmount(trade.amount)}")
            Log.d(TAG, "  Detection: $detectionMethod")

            val resultDetails = mapOf(
                "trade_id" to trade.uuid,
                "amount" to trade.amount,
                "trend" to trade.trend,
                "asset_ric" to trade.asset_ric,
                "status" to trade.status,
                "win_amount" to (trade.win ?: 0L),
                "payment" to trade.payment,
                "detection_method" to detectionMethod.lowercase(),
                "detection_time" to System.currentTimeMillis(),
                "monitoring_duration" to (System.currentTimeMillis() - orderState.executionTime),
                "cycle_number" to orderState.cycleNumber,
                "account_type" to if (orderState.isDemoAccount) "demo" else "real"
            )

            onCTCTradeResult(orderState.ctcOrderId, isWin, resultDetails)
        }
    }

    fun handleWebSocketTradeUpdate(message: JSONObject) {
        if (!isMonitoringActive) return

        scope.launch {
            try {
                val event = message.optString("event", "")
                val payload = message.optJSONObject("payload")

                lastWebSocketUpdate = System.currentTimeMillis()

                when (event) {
                    "closed", "deal_result", "trade_update" -> {
                        val tradeId = payload?.optString("id", "") ?: ""
                        val uuid = payload?.optString("uuid", "") ?: ""
                        val status = payload?.optString("status", "") ?: ""
                        val amount = payload?.optLong("amount", 0L) ?: 0L
                        val trend = payload?.optString("trend", "") ?: ""

                        if ((tradeId.isNotEmpty() || uuid.isNotEmpty()) &&
                            status.lowercase() in listOf("won", "lost", "win", "lose")) {

                            processWebSocketCTCResult(tradeId, uuid, status, amount, trend, payload)
                        }
                    }
                    "balance_changed" -> {
                        handleBalanceUpdate(payload)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing WebSocket update: ${e.message}", e)
            }
        }
    }

    private suspend fun processWebSocketCTCResult(
        tradeId: String,
        uuid: String,
        status: String,
        amount: Long,
        trend: String,
        payload: JSONObject?
    ) {
        monitoringMutex.withLock {
            val matchingOrderId = activeCTCOrders.entries.find { (_, orderState) ->
                !orderState.isCompleted &&
                        orderState.amount == amount &&
                        orderState.trend.equals(trend, ignoreCase = true) &&
                        System.currentTimeMillis() - orderState.executionTime < 120000L
            }?.key

            if (matchingOrderId != null) {
                val orderState = activeCTCOrders[matchingOrderId] ?: return@withLock

                val isWin = status.lowercase() in listOf("won", "win")
                val finalTradeId = if (uuid.isNotEmpty()) uuid else tradeId
                processedResults[matchingOrderId] = finalTradeId

                activeCTCOrders[matchingOrderId] = orderState.copy(
                    isWebSocketDetected = true,
                    isCompleted = true
                )

                Log.d(TAG, "CTC Order WebSocket result (ULTRA-FAST):")
                Log.d(TAG, "  Order ID: $matchingOrderId")
                Log.d(TAG, "  Cycle: ${orderState.cycleNumber}")
                Log.d(TAG, "  Trade ID: $finalTradeId")
                Log.d(TAG, "  Result: ${if (isWin) "WIN" else "LOSE"}")
                Log.d(TAG, "  Amount: ${formatAmount(amount)}")

                val resultDetails = mapOf(
                    "trade_id" to finalTradeId,
                    "amount" to amount,
                    "trend" to trend,
                    "status" to status,
                    "win_amount" to (payload?.optLong("win", 0L) ?: 0L),
                    "payment" to (payload?.optLong("payment", 0L) ?: 0L),
                    "detection_method" to "websocket_ultra_fast",
                    "detection_time" to System.currentTimeMillis(),
                    "monitoring_duration" to (System.currentTimeMillis() - orderState.executionTime),
                    "cycle_number" to orderState.cycleNumber,
                    "account_type" to if (orderState.isDemoAccount) "demo" else "real"
                )

                onCTCTradeResult(matchingOrderId, isWin, resultDetails)
            }
        }
    }

    private suspend fun handleBalanceUpdate(payload: JSONObject?) {
        if (payload == null) return

        val newBalance = payload.optLong("balance", 0L)
        val currentTime = System.currentTimeMillis()

        if (lastBalance == 0L) {
            lastBalance = newBalance
            return
        }

        val balanceChange = newBalance - lastBalance
        val significantThreshold = 50000L

        if (abs(balanceChange) > significantThreshold) {
            balanceHistory.add(
                BalanceSnapshot(
                    balance = newBalance,
                    timestamp = currentTime,
                    change = balanceChange
                )
            )

            if (balanceHistory.size > 10) {
                balanceHistory.removeAt(0)
            }

            val recentOrderId = activeCTCOrders.entries
                .filter { (_, state) ->
                    !state.isCompleted &&
                            currentTime - state.executionTime < 60000L
                }
                .minByOrNull { (_, state) -> currentTime - state.executionTime }
                ?.key

            if (recentOrderId != null) {
                val orderState = activeCTCOrders[recentOrderId]!!
                val isWin = balanceChange > 0

                Log.d(TAG, "CTC Order balance detection:")
                Log.d(TAG, "  Order ID: $recentOrderId")
                Log.d(TAG, "  Balance change: ${formatAmount(abs(balanceChange))}")
                Log.d(TAG, "  Result: ${if (isWin) "WIN" else "LOSE"}")

                processedResults[recentOrderId] = "balance_${currentTime}"
                activeCTCOrders[recentOrderId] = orderState.copy(
                    isBalanceDetected = true,
                    isCompleted = true
                )

                val resultDetails = mapOf(
                    "trade_id" to "balance_detection_$currentTime",
                    "amount" to orderState.amount,
                    "trend" to orderState.trend,
                    "status" to if (isWin) "won" else "lost",
                    "win_amount" to if (isWin) maxOf(0L, balanceChange) else 0L,
                    "detection_method" to "balance_change_ultra_fast",
                    "detection_time" to currentTime,
                    "balance_change" to balanceChange,
                    "cycle_number" to orderState.cycleNumber,
                    "account_type" to if (orderState.isDemoAccount) "demo" else "real"
                )

                onCTCTradeResult(recentOrderId, isWin, resultDetails)
            }
        }

        lastBalance = newBalance
        lastBalanceUpdate = currentTime
    }

    private suspend fun handleCTCOrderTimeout(orderState: CTCOrderMonitoringState) {
        Log.w(TAG, "CTC Order timeout: ${orderState.ctcOrderId}")

        val resultDetails = mapOf(
            "trade_id" to "timeout_${System.currentTimeMillis()}",
            "amount" to orderState.amount,
            "trend" to orderState.trend,
            "status" to "timeout_assumed_loss",
            "detection_method" to "timeout_fallback",
            "detection_time" to System.currentTimeMillis(),
            "cycle_number" to orderState.cycleNumber,
            "account_type" to if (orderState.isDemoAccount) "demo" else "real"
        )

        onCTCTradeResult(orderState.ctcOrderId, false, resultDetails)
    }

    private fun parseTradeTime(timeString: String): Long {
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
                    TradingHistoryRaw(
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
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.created_at }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    fun isActive(): Boolean = isMonitoringActive

    fun getActiveOrdersCount(): Int = activeCTCOrders.size

    fun getMonitoringStats(): Map<String, Any> {
        return mapOf(
            "is_active" to isMonitoringActive,
            "active_orders" to activeCTCOrders.size,
            "is_prewarmed" to isPreWarmed,
            "monitoring_interval" to "${ULTRA_FAST_INTERVAL}ms",
            "balance_check_interval" to "${BALANCE_CHECK_INTERVAL}ms",
            "last_websocket_update" to lastWebSocketUpdate,
            "last_api_call" to lastApiCall,
            "last_balance_update" to lastBalanceUpdate,
            "processed_results" to processedResults.size,
            "balance_history_size" to balanceHistory.size,
            "performance_mode" to "ULTRA_FAST_CTC_ORDER",
            "identical_to" to "FOLLOW_ORDER_MONITORING"
        )
    }

    fun cleanup() {
        stopMonitoring()
        Log.d(TAG, "CTC Order continuous monitor cleaned up")
    }
}