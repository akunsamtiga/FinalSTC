package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class AISignalOrderManager(
    private val scope: CoroutineScope,
    private val onAISignalOrdersUpdate: (List<AISignalOrder>) -> Unit,
    private val onExecuteAISignalTrade: (String, String, Long, Boolean, Int) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val telegramSignalService: TelegramSignalService,
    private val serverTimeService: ServerTimeService,
    private val aiSignalTradeMonitor: AISignalTradeMonitor,
    private val onAISignalTradeStatsUpdate: ((tradeId: String, orderId: String, result: String) -> Unit)? = null
) {

    private var pendingOrders = mutableListOf<AISignalOrder>()
    private var isActive = false
    private var executionJob: Job? = null
    private var selectedAsset: Asset? = null
    private var isDemoAccount = true
    private var baseAmount = 1_400_000L

    // ✅ FIXED: Add currency tracking
    private var currentCurrency: CurrencyType = CurrencyType.IDR

    private val EXECUTION_CHECK_INTERVAL_MS = 100L
    private val EXECUTION_ADVANCE_MS = 1000L

    private val executedOrdersMap = mutableMapOf<String, AISignalOrder>()
    private var martingaleSettings: MartingaleState? = null

    // Martingale tracking
    private val activeMartingaleOrders = mutableMapOf<String, MartingaleSequenceInfo>()

    data class MartingaleSequenceInfo(
        val orderId: String,
        val currentStep: Int,
        val maxSteps: Int,
        val totalLoss: Long,
        val isActive: Boolean,
        val originalTrend: String,
        val lastExecutionTime: Long = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "AISignalOrderManager"
    }

    // ✅ FIXED: Update currency method
    fun updateCurrency(currency: CurrencyType) {
        currentCurrency = currency
        println("$TAG: Currency updated to ${currency.code}")
    }

    fun handleAISignalTradeResultFromMonitor(
        parentOrderId: String,
        isWin: Boolean,
        isMartingale: Boolean,
        martingaleStep: Int,
        details: Map<String, Any>
    ) {
        println("=" .repeat(60))
        println("🎯 AI SIGNAL RESULT PROCESSING")
        println("=" .repeat(60))
        println("   Parent Order: $parentOrderId")
        println("   Is Martingale: $isMartingale")
        println("   Step: $martingaleStep")
        println("   Result: ${if (isWin) "WIN ✅" else "LOSE ❌"}")

        if (isMartingale) {
            val martingaleInfo = activeMartingaleOrders[parentOrderId]

            if (martingaleInfo != null) {
                handleMartingaleResult(parentOrderId, martingaleInfo, isWin, details)
            } else {
                println("   ⚠️ WARNING: Martingale info not found for $parentOrderId")
            }
        } else {
            handleInitialTradeResult(parentOrderId, isWin, details)
        }

        println("=" .repeat(60))
    }

    private fun handleInitialTradeResult(
        orderId: String,
        isWin: Boolean,
        details: Map<String, Any>
    ) {
        val order = executedOrdersMap[orderId] ?: pendingOrders.find { it.id == orderId }

        if (order == null) {
            println("   ⚠️ Order not found: $orderId")
            return
        }

        if (isWin) {
            println("   ✅ AI Signal Initial Trade WIN")

            // ✅ UPDATE: Add WIN stats tracking
            if (onAISignalTradeStatsUpdate != null) {
                val tradeId = details["trade_id"] as? String ?: "ai_signal_${orderId}_${System.currentTimeMillis()}"
                onAISignalTradeStatsUpdate(tradeId, orderId, "WIN")
            }

            // ✅ NEW: Update status with WIN info
            val winAmount = details["win_amount"] as? Long ?: 0L
            val formattedWin = currentCurrency.formatAmount(winAmount)
            onModeStatusUpdate("✅ AI Signal WIN - Profit: $formattedWin - Ready for next signal")

        } else {
            val settings = martingaleSettings

            if (settings != null && settings.isEnabled) {
                println("   ❌ AI Signal LOSE - Starting martingale...")

                // ✅ NEW: Show LOSE with martingale info
                val lossAmount = order.amount
                val formattedLoss = currentCurrency.formatAmount(lossAmount)
                onModeStatusUpdate("❌ AI Signal LOSE - Loss: $formattedLoss - Starting Martingale Step 1")

                startMartingaleSequence(orderId, order, settings)
            } else {
                println("   ❌ AI Signal Direct LOSE (martingale disabled)")

                // ✅ UPDATE: Add direct LOSE stats tracking
                if (onAISignalTradeStatsUpdate != null) {
                    val tradeId = details["trade_id"] as? String ?: "ai_signal_${orderId}_${System.currentTimeMillis()}"
                    onAISignalTradeStatsUpdate(tradeId, orderId, "LOSE")
                }

                // ✅ NEW: Show direct LOSE info
                val lossAmount = order.amount
                val formattedLoss = currentCurrency.formatAmount(lossAmount)
                onModeStatusUpdate("❌ AI Signal LOSE - Loss: $formattedLoss - Waiting for next signal")
            }
        }
    }

    fun startAISignalMode(
        asset: Asset,
        isDemoAccount: Boolean,
        baseAmount: Long,
        martingaleSettings: MartingaleState
    ): Result<String> {
        return try {
            if (isActive) {
                return Result.failure(Exception("AI Signal mode already active"))
            }

            this.selectedAsset = asset
            this.isDemoAccount = isDemoAccount
            this.baseAmount = baseAmount
            this.martingaleSettings = martingaleSettings
            this.isActive = true

            println("=" .repeat(60))
            println("🤖 AI SIGNAL MODE STARTED (DEDICATED MONITORING)")
            println("=" .repeat(60))
            println("📊 Asset: ${asset.name}")
            println("💰 Base Amount: ${currentCurrency.formatAmount(baseAmount)}")
            println("💱 Currency: ${currentCurrency.code}")
            println("🦾 Account: ${if (isDemoAccount) "Demo" else "Real"}")
            println("📡 Connection: FCM")
            println("📍 Monitoring: AISignalTradeMonitor (Dedicated)")
            println("   - Independent monitoring system")
            println("   - WebSocket Priority Detection")
            println("   - API Polling Fallback")
            println("   - 50ms ultra-fast intervals")
            println("📊 Stats Tracking: ${if (onAISignalTradeStatsUpdate != null) "ENABLED" else "DISABLED"}")
            println("🎲 Martingale: ${if (martingaleSettings.isEnabled) "ENABLED" else "DISABLED"}")
            if (martingaleSettings.isEnabled) {
                println("   Max Steps: ${martingaleSettings.maxSteps}")
                println("   Multiplier: ${martingaleSettings.multiplierValue}${if (martingaleSettings.multiplierType == MultiplierType.PERCENTAGE) "%" else "x"}")
                println("   Sequence: ${martingaleSettings.getFormattedSequence()}")
            }
            println("=" .repeat(60))

            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(true)

            telegramSignalService.startMonitoring()
            aiSignalTradeMonitor.startMonitoring()
            startExecutionMonitoring()

            onModeStatusUpdate("🤖 AI Signal active - Listening via FCM...")

            Result.success("AI Signal mode started successfully")

        } catch (e: Exception) {
            isActive = false
            Result.failure(e)
        }
    }

    // AISignalOrderManager.kt - UPDATE stopAISignalMode()

    fun stopAISignalMode(): Result<String> {
        return try {
            if (!isActive) {
                return Result.failure(Exception("AI Signal mode not active"))
            }

            isActive = false

            Log.d(TAG, "🔇 Disabling AI Signal Mode notifications...")
            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
            Log.d(TAG, "✅ Notifications disabled")

            // ✅ TAMBAH: Unsubscribe from FCM topic
            try {
                Log.d(TAG, "🔕 Unsubscribing from trading_signals topic...")
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("trading_signals")
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Unsubscribed successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Unsubscribe failed: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing: ${e.message}")
            }

            telegramSignalService.stopMonitoring()
            aiSignalTradeMonitor.stopMonitoring()
            stopExecutionMonitoring()

            // Clear all tracking
            pendingOrders.clear()
            executedOrdersMap.clear()
            activeMartingaleOrders.clear()
            onAISignalOrdersUpdate(emptyList())

            println("🛑 AI Signal Mode stopped completely")
            onModeStatusUpdate("AI Signal inactive")

            Result.success("AI Signal mode stopped")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun handleNewSignal(signal: TelegramSignal) {
        if (!isActive) return

        // ✅ CHECK: Skip signal if martingale is active
        if (activeMartingaleOrders.isNotEmpty()) {
            println("=" .repeat(60))
            println("⚠️ SIGNAL SKIPPED - MARTINGALE ACTIVE")
            println("=" .repeat(60))
            println("   Active martingale sequences: ${activeMartingaleOrders.size}")
            activeMartingaleOrders.forEach { (orderId, info) ->
                println("   - Order: $orderId, Step: ${info.currentStep}/${info.maxSteps}")
            }
            println("   New signal: ${signal.trend.uppercase()} - IGNORED")
            println("=" .repeat(60))

            onModeStatusUpdate("⚠️ Signal skipped - Martingale in progress")
            return
        }

        val asset = selectedAsset ?: return

        val order = AISignalOrder(
            id = UUID.randomUUID().toString(),
            assetRic = asset.ric,
            assetName = asset.name,
            trend = signal.trend,
            amount = baseAmount,
            executionTime = signal.executionTime,
            receivedAt = signal.receivedAt,
            originalMessage = signal.originalMessage,
            isExecuted = false
        )

        pendingOrders.add(order)
        pendingOrders.sortBy { it.executionTime }

        println("=" .repeat(60))
        println("📩 NEW AI SIGNAL RECEIVED")
        println("=" .repeat(60))
        println("   ID: ${order.id}")
        println("   Trend: ${signal.trend.uppercase()}")
        println("   Original: ${signal.originalMessage}")
        println("   Received at: ${formatTime(signal.receivedAt)}")
        println("   Will execute at: ${formatTime(signal.executionTime)}")
        println("   Delay: ${signal.getDelaySeconds()}s")
        println("=" .repeat(60))

        onAISignalOrdersUpdate(pendingOrders.toList())
        onModeStatusUpdate("📩 Signal received: ${signal.trend.uppercase()} at ${signal.getFormattedExecutionTime()}")
    }

    private fun startExecutionMonitoring() {
        executionJob = scope.launch {
            while (isActive) {
                try {
                    checkAndExecutePendingOrders()
                    delay(EXECUTION_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    println("❌ Error in execution monitoring: ${e.message}")
                    delay(1000L)
                }
            }
        }
    }

    private fun stopExecutionMonitoring() {
        executionJob?.cancel()
        executionJob = null
    }

    private suspend fun checkAndExecutePendingOrders() {
        val currentTime = serverTimeService.getCurrentServerTimeMillis()
        val ordersToExecute = pendingOrders.filter { order ->
            !order.isExecuted &&
                    currentTime >= (order.executionTime - EXECUTION_ADVANCE_MS)
        }

        ordersToExecute.forEach { order ->
            executeOrder(order)
        }
    }

    private fun executeOrder(order: AISignalOrder) {
        println("=" .repeat(60))
        println("🚀 EXECUTING AI SIGNAL ORDER")
        println("=" .repeat(60))
        println("   ID: ${order.id}")
        println("   Trend: ${order.trend.uppercase()}")
        println("   Amount: ${currentCurrency.formatAmount(order.amount)}")
        println("   Currency: ${currentCurrency.code}")
        println("   Scheduled: ${formatTime(order.executionTime)}")
        println("   Actual: ${formatTime(serverTimeService.getCurrentServerTimeMillis())}")
        println("   Monitoring: AISignalTradeMonitor will start")
        println("=" .repeat(60))

        val orderIndex = pendingOrders.indexOfFirst { it.id == order.id }
        if (orderIndex != -1) {
            val executedOrder = order.copy(isExecuted = true)
            pendingOrders[orderIndex] = executedOrder
            executedOrdersMap[order.id] = executedOrder
            onAISignalOrdersUpdate(pendingOrders.toList())
        }

        // ✅ EXECUTE INITIAL TRADE (not martingale)
        onExecuteAISignalTrade(order.trend, order.id, order.amount, false, 0)

        onModeStatusUpdate("🚀 Executing: ${order.trend.uppercase()} - ${currentCurrency.formatAmount(order.amount)}")

        scope.launch {
            delay(300000L)
            val fiveMinutesAgo = serverTimeService.getCurrentServerTimeMillis() - 300000L
            pendingOrders.removeAll { it.isExecuted && it.executionTime < fiveMinutesAgo }
            executedOrdersMap.entries.removeIf { (_, order) ->
                order.isExecuted && order.executionTime < fiveMinutesAgo
            }
            onAISignalOrdersUpdate(pendingOrders.toList())
        }
    }

    private fun startMartingaleSequence(
        orderId: String,
        order: AISignalOrder,
        settings: MartingaleState
    ) {
        try {
            val nextStep = 1
            val nextAmount = settings.getMartingaleAmountForStep(nextStep, currentCurrency)

            println("=" .repeat(60))
            println("🔄 STARTING MARTINGALE SEQUENCE")
            println("=" .repeat(60))
            println("   Order ID: $orderId")
            println("   Step: $nextStep")
            println("   Amount: ${currentCurrency.formatAmount(nextAmount)}")
            println("   Currency: ${currentCurrency.code}")
            println("   Total Loss: ${currentCurrency.formatAmount(order.amount)}")

            activeMartingaleOrders[orderId] = MartingaleSequenceInfo(
                orderId = orderId,
                currentStep = nextStep,
                maxSteps = settings.maxSteps,
                totalLoss = order.amount,
                isActive = true,
                originalTrend = order.trend,
                lastExecutionTime = System.currentTimeMillis()
            )

            executeMartingaleTrade(orderId, order.trend, nextAmount, nextStep)

            onModeStatusUpdate("🔄 Martingale Step $nextStep - ${currentCurrency.formatAmount(nextAmount)}")

            println("✅ Martingale sequence started")
            println("=" .repeat(60))

        } catch (e: Exception) {
            println("❌ Error starting martingale: ${e.message}")
            e.printStackTrace()

            if (onAISignalTradeStatsUpdate != null) {
                val tradeId = "ai_signal_fail_${orderId}_${System.currentTimeMillis()}"
                onAISignalTradeStatsUpdate(tradeId, orderId, "LOSE")
                println("📊 Stats updated: LOSE counted (calculation error)")
            }

            onModeStatusUpdate("❌ Martingale calculation error - Waiting for next signal")
        }
    }

    private fun handleMartingaleResult(
        parentOrderId: String,
        martingaleInfo: MartingaleSequenceInfo,
        isWin: Boolean,
        details: Map<String, Any>
    ) {
        println("📊 MARTINGALE RESULT")
        println("   Parent Order: $parentOrderId")
        println("   Current Step: ${martingaleInfo.currentStep}")
        println("   Result: ${if (isWin) "WIN ✅" else "LOSE ❌"}")

        if (isWin) {
            println("✅ AI Signal Martingale WIN at step ${martingaleInfo.currentStep}")
            println("   Total Loss Recovered: ${currentCurrency.formatAmount(martingaleInfo.totalLoss)}")

            activeMartingaleOrders.remove(parentOrderId)

            // ✅ UPDATE: Add martingale WIN stats
            if (onAISignalTradeStatsUpdate != null) {
                val tradeId = details["trade_id"] as? String ?: "ai_signal_mart_win_${parentOrderId}_${System.currentTimeMillis()}"
                onAISignalTradeStatsUpdate(tradeId, parentOrderId, "WIN")
                println("📊 Stats updated: WIN counted (martingale recovery)")
            }

            // ✅ NEW: Show martingale WIN with recovery info
            val winAmount = details["win_amount"] as? Long ?: 0L
            val totalRecovered = winAmount - martingaleInfo.totalLoss
            val formattedRecovery = currentCurrency.formatAmount(totalRecovered)
            onModeStatusUpdate("✅ Martingale WIN at Step ${martingaleInfo.currentStep} - Recovery: $formattedRecovery - Ready for next signal")

        } else {
            val settings = martingaleSettings

            if (settings != null) {
                val nextStep = martingaleInfo.currentStep + 1

                println("❌ Martingale Step ${martingaleInfo.currentStep} LOSE")
                println("   Next Step: $nextStep")
                println("   Max Steps: ${settings.maxSteps}")

                if (nextStep <= settings.maxSteps) {
                    try {
                        val nextAmount = settings.getMartingaleAmountForStep(nextStep, currentCurrency)
                        val newTotalLoss = martingaleInfo.totalLoss + nextAmount

                        println("🔄 Continuing martingale to step $nextStep")
                        println("   Next Amount: ${currentCurrency.formatAmount(nextAmount)}")
                        println("   New Total Loss: ${currentCurrency.formatAmount(newTotalLoss)}")

                        activeMartingaleOrders[parentOrderId] = martingaleInfo.copy(
                            currentStep = nextStep,
                            totalLoss = newTotalLoss,
                            lastExecutionTime = System.currentTimeMillis()
                        )

                        // ✅ NEW: Show next step info
                        val formattedAmount = currentCurrency.formatAmount(nextAmount)
                        val formattedLoss = currentCurrency.formatAmount(newTotalLoss)
                        onModeStatusUpdate("🔄 Martingale Step $nextStep - Amount: $formattedAmount - Total Loss: $formattedLoss")

                        executeMartingaleTrade(parentOrderId, martingaleInfo.originalTrend, nextAmount, nextStep)

                        println("✅ Martingale step $nextStep queued for execution")

                    } catch (e: Exception) {
                        println("❌ Error calculating next martingale: ${e.message}")
                        e.printStackTrace()
                        finalizeMartingaleFailure(parentOrderId, martingaleInfo, details)
                    }

                } else {
                    finalizeMartingaleFailure(parentOrderId, martingaleInfo, details)
                }
            } else {
                println("❌ Martingale settings null - cannot continue")
                finalizeMartingaleFailure(parentOrderId, martingaleInfo, details)
            }
        }

    }



    private fun finalizeMartingaleFailure(
        parentOrderId: String,
        martingaleInfo: MartingaleSequenceInfo,
        details: Map<String, Any>
    ) {
        println("❌ AI Signal Martingale FAILED at step ${martingaleInfo.currentStep}")
        println("   Final Total Loss: ${currentCurrency.formatAmount(martingaleInfo.totalLoss)}")

        activeMartingaleOrders.remove(parentOrderId)

        // ✅ UPDATE: Add martingale failure stats
        if (onAISignalTradeStatsUpdate != null) {
            val tradeId = details["trade_id"] as? String ?: "ai_signal_mart_fail_${parentOrderId}_${System.currentTimeMillis()}"
            onAISignalTradeStatsUpdate(tradeId, parentOrderId, "LOSE")
            println("📊 Stats updated: LOSE counted (martingale failed)")
        }

        // ✅ NEW: Show martingale failure with total loss
        val formattedLoss = currentCurrency.formatAmount(martingaleInfo.totalLoss)
        onModeStatusUpdate("❌ Martingale FAILED at Step ${martingaleInfo.currentStep} - Total Loss: $formattedLoss - Waiting for next signal")
    }

    fun getCurrentStatusInfo(): Map<String, String> {
        return when {
            activeMartingaleOrders.isNotEmpty() -> {
                val info = activeMartingaleOrders.values.first()
                mapOf(
                    "status" to "MARTINGALE_ACTIVE",
                    "step" to "${info.currentStep}/${info.maxSteps}",
                    "total_loss" to currentCurrency.formatAmount(info.totalLoss),
                    "trend" to info.originalTrend.uppercase()
                )
            }
            pendingOrders.any { !it.isExecuted } -> {
                val nextOrder = pendingOrders.filter { !it.isExecuted }.minByOrNull { it.executionTime }
                if (nextOrder != null) {
                    val delay = (nextOrder.executionTime - serverTimeService.getCurrentServerTimeMillis()) / 1000
                    mapOf(
                        "status" to "WAITING_SIGNAL",
                        "next_signal" to nextOrder.trend.uppercase(),
                        "execute_in" to "${delay}s"
                    )
                } else {
                    mapOf("status" to "LISTENING")
                }
            }
            else -> mapOf("status" to "LISTENING")
        }
    }

    private fun executeMartingaleTrade(parentOrderId: String, trend: String, amount: Long, step: Int) {
        println("=" .repeat(60))
        println("🔥 EXECUTING AI SIGNAL MARTINGALE TRADE")
        println("=" .repeat(60))
        println("   Parent Order ID: $parentOrderId")
        println("   Step: $step")
        println("   Trend: ${trend.uppercase()}")
        println("   Amount: ${currentCurrency.formatAmount(amount)}")
        println("   Currency: ${currentCurrency.code}")

        val currentTime = serverTimeService.getCurrentServerTimeMillis()
        val martingaleOrderId = "${parentOrderId}_martingale_$step"

        val martingaleOrder = AISignalOrder(
            id = martingaleOrderId,
            assetRic = selectedAsset?.ric ?: "",
            assetName = selectedAsset?.name ?: "",
            trend = trend,
            amount = amount,
            executionTime = currentTime,
            receivedAt = currentTime,
            originalMessage = "Martingale Step $step",
            isExecuted = true
        )

        pendingOrders.add(martingaleOrder)
        executedOrdersMap[martingaleOrderId] = martingaleOrder
        onAISignalOrdersUpdate(pendingOrders.toList())

        // ✅ EXECUTE WITH MARTINGALE FLAG
        onExecuteAISignalTrade(trend, parentOrderId, amount, true, step)

        println("   ✅ Martingale trade execution triggered")
        println("=" .repeat(60))
    }

    fun isActive(): Boolean = isActive

    fun getPendingOrders(): List<AISignalOrder> = pendingOrders.toList()

    fun getPerformanceStats(): Map<String, Any> {
        val totalOrders = pendingOrders.size
        val executedOrders = pendingOrders.count { it.isExecuted }
        val pendingCount = pendingOrders.count { !it.isExecuted }
        val activeMartingaleCount = activeMartingaleOrders.size

        val telegramStatus = telegramSignalService.getStatus()
        val monitorStatus = aiSignalTradeMonitor.getMonitoringStatus()

        return mapOf(
            "is_active" to isActive,
            "total_orders" to totalOrders,
            "executed_orders" to executedOrders,
            "pending_orders" to pendingCount,
            "active_martingale_sequences" to activeMartingaleCount,
            "martingale_enabled" to (martingaleSettings?.isEnabled ?: false),
            "martingale_max_steps" to (martingaleSettings?.maxSteps ?: 0),
            "execution_check_interval_ms" to EXECUTION_CHECK_INTERVAL_MS,
            "execution_advance_ms" to EXECUTION_ADVANCE_MS,
            "telegram_status" to telegramStatus,
            "monitoring_status" to monitorStatus,
            "asset" to (selectedAsset?.name ?: "None"),
            "base_amount" to baseAmount,
            "current_currency" to currentCurrency.code,
            "account_type" to if (isDemoAccount) "Demo" else "Real",
            "stats_tracking" to (onAISignalTradeStatsUpdate != null),
            "monitoring_method" to "AISignalTradeMonitor (Dedicated)",
            "detection_speed" to "50ms ultra-fast",
            "martingale_sequences" to activeMartingaleOrders.values.map { info ->
                mapOf(
                    "order_id" to info.orderId,
                    "current_step" to info.currentStep,
                    "max_steps" to info.maxSteps,
                    "total_loss" to currentCurrency.formatAmount(info.totalLoss),
                    "is_active" to info.isActive,
                    "trend" to info.originalTrend
                )
            }
        )
    }

    fun getModeStatus(): String {
        return when {
            !isActive -> "INACTIVE"
            activeMartingaleOrders.isNotEmpty() -> {
                val info = activeMartingaleOrders.values.first()
                "MARTINGALE - Step ${info.currentStep}/${info.maxSteps}"
            }
            pendingOrders.any { !it.isExecuted } -> {
                val nextOrder = pendingOrders.filter { !it.isExecuted }.minByOrNull { it.executionTime }
                if (nextOrder != null) {
                    val delay = (nextOrder.executionTime - serverTimeService.getCurrentServerTimeMillis()) / 1000
                    "WAITING - Next: ${nextOrder.trend.uppercase()} in ${delay}s"
                } else {
                    "ACTIVE - No pending signals"
                }
            }
            else -> "ACTIVE - Listening for signals"
        }
    }

    fun injectTestSignal(signalText: String) {
        telegramSignalService.injectTestSignal(signalText)
    }

    private fun formatAmount(amount: Long): String {
        return currentCurrency.formatAmount(amount)
    }

    private fun formatTime(timeMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }

    fun cleanup() {
        Log.d(TAG, "🔇 Disabling notifications in cleanup...")
        com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)

        stopAISignalMode()
        pendingOrders.clear()
        executedOrdersMap.clear()
        activeMartingaleOrders.clear()
        martingaleSettings = null

        aiSignalTradeMonitor.stopMonitoring()
    }
}

data class AISignalOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String,
    val amount: Long,
    val executionTime: Long,
    val receivedAt: Long,
    val originalMessage: String,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null
) {
    fun getStatusDisplay(): String {
        return when {
            isSkipped -> "Skipped: ${skipReason ?: "Unknown"}"
            isExecuted -> "Executed"
            else -> {
                val delay = (executionTime - System.currentTimeMillis()) / 1000
                if (delay > 0) {
                    "Pending - Execute in ${delay}s"
                } else {
                    "Ready to execute"
                }
            }
        }
    }

    fun getExecutionTimeFormatted(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = executionTime
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }
}