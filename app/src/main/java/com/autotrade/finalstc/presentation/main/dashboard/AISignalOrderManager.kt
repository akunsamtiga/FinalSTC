package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class AISignalOrderManager(
    private val onAISignalOrdersUpdate: (List<AISignalOrder>) -> Unit,
    private val onExecuteAISignalTrade: (String, String, Long, Boolean, Int) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val telegramSignalService: TelegramSignalService,
    private val serverTimeService: ServerTimeService,
    private val scope: CoroutineScope,
    private val aiSignalTradeMonitor: AISignalTradeMonitor,
    private val onAISignalTradeStatsUpdate: ((tradeId: String, orderId: String, result: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "AISignalOrderManager"
    }

    data class AlwaysSignalLossState(
        val hasOutstandingLoss: Boolean = false,
        val currentMartingaleStep: Int = 0,
        val originalOrderId: String = "",
        val totalLoss: Long = 0L,
        val currentTrend: String = ""
    )

    data class MartingaleSequenceInfo(
        val orderId: String,
        val currentStep: Int,
        val maxSteps: Int,
        val totalLoss: Long,
        val isActive: Boolean,
        val originalTrend: String,
        val lastExecutionTime: Long = System.currentTimeMillis()
    )

    private val backgroundScope = kotlinx.coroutines.GlobalScope
    private var pendingOrders = mutableListOf<AISignalOrder>()
    private var isActive = false
    private var executionJob: Job? = null
    private var selectedAsset: Asset? = null
    private var isDemoAccount = true
    private var baseAmount = 1_400_000L
    private var alwaysSignalLossTracking: AlwaysSignalLossState? = null
    private var currentCurrency: CurrencyType = CurrencyType.IDR
    private val executedOrdersMap = mutableMapOf<String, AISignalOrder>()
    private var martingaleSettings: MartingaleState? = null
    private val activeMartingaleOrders = mutableMapOf<String, MartingaleSequenceInfo>()

    private val EXECUTION_CHECK_INTERVAL_MS = 100L
    private val EXECUTION_ADVANCE_MS = 1000L

    fun isInitialized(): Boolean = isActive

    fun isActive(): Boolean = isActive

    fun startAISignalMode(
        asset: Asset,
        isDemoAccount: Boolean,
        baseAmount: Long,
        martingaleSettings: MartingaleState
    ): Result<String> {
        return try {
            if (isActive) {
                Log.d(TAG, "AI Signal already active, updating settings...")
                this.selectedAsset = asset
                this.isDemoAccount = isDemoAccount
                this.baseAmount = baseAmount
                this.martingaleSettings = martingaleSettings
                return Result.success("AI Signal mode settings updated")
            }

            this.selectedAsset = asset
            this.isDemoAccount = isDemoAccount
            this.baseAmount = baseAmount
            this.martingaleSettings = martingaleSettings
            this.isActive = true

            println("=" .repeat(60))
            println("🤖 AI SIGNAL MODE STARTED")
            println("=" .repeat(60))
            println("📊 Asset: ${asset.name}")
            println("💰 Base Amount: ${currentCurrency.formatAmount(baseAmount)}")
            println("💱 Currency: ${currentCurrency.code}")
            println("🦾 Account: ${if (isDemoAccount) "Demo" else "Real"}")
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

    fun stopAISignalMode(): Result<String> {
        return try {
            if (!isActive) {
                return Result.failure(Exception("AI Signal mode not active"))
            }

            isActive = false

            Log.d(TAG, "🔇 Disabling AI Signal Mode notifications...")
            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
            Log.d(TAG, "✅ Notifications disabled")

            try {
                Log.d(TAG, "📕 Unsubscribing from trading_signals topic...")
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

    fun cleanup() {
        Log.d(TAG, "🔇 Disabling notifications in cleanup...")
        com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)

        stopAISignalMode()
        pendingOrders.clear()
        executedOrdersMap.clear()
        activeMartingaleOrders.clear()
        alwaysSignalLossTracking = null
        martingaleSettings = null

        aiSignalTradeMonitor.stopMonitoring()
    }

    fun updateCurrency(currency: CurrencyType) {
        currentCurrency = currency
        println("$TAG: Currency updated to ${currency.code}")
    }

    fun restorePendingOrders(orders: List<AISignalOrder>) {
        if (!isActive) return

        pendingOrders.clear()
        pendingOrders.addAll(orders.filter { !it.isExecuted })
        pendingOrders.sortBy { it.executionTime }

        onAISignalOrdersUpdate(pendingOrders.toList())

        Log.d(TAG, "✅ Restored ${pendingOrders.size} pending AI Signal orders")
    }

    fun handleNewSignal(signal: TelegramSignal) {
        if (!isActive) {
            println("⚠️ AI Signal inactive - signal ignored")
            return
        }

        val settings = martingaleSettings
        val lossState = alwaysSignalLossTracking

        if (activeMartingaleOrders.isNotEmpty() && settings?.isAlwaysSignal == false) {
            println("⚠️ Signal skipped - Standard Martingale active")
            onModeStatusUpdate("Signal skipped - Martingale in progress")
            return
        }

        val asset = selectedAsset ?: return

        if (settings != null && settings.isAlwaysSignal && lossState != null && lossState.hasOutstandingLoss) {
            val nextStep = lossState.currentMartingaleStep + 1

            if (nextStep > settings.maxSteps) {
                println("⚠️ Always Signal: Max steps reached - RESET")
                alwaysSignalLossTracking = null
                return
            }

            try {
                val nextAmount = settings.getMartingaleAmountForStep(nextStep, currentCurrency)

                val order = AISignalOrder(
                    id = UUID.randomUUID().toString(),
                    assetRic = asset.ric,
                    assetName = asset.name,
                    trend = signal.trend,
                    amount = nextAmount,
                    executionTime = signal.executionTime,
                    receivedAt = signal.receivedAt,
                    originalMessage = "${signal.originalMessage} (Step $nextStep)",
                    isExecuted = false,
                    status = AISignalOrderStatus.MARTINGALE_STEP,
                    martingaleStep = nextStep,
                    maxMartingaleSteps = settings.maxSteps
                )

                pendingOrders.add(order)
                pendingOrders.sortBy { it.executionTime }
                onAISignalOrdersUpdate(pendingOrders.toList())

                alwaysSignalLossTracking = lossState.copy(
                    currentMartingaleStep = nextStep
                )

                val formattedAmount = currentCurrency.formatAmount(nextAmount)
                onModeStatusUpdate("Martingale Step $nextStep/${settings.maxSteps} - Amount: $formattedAmount")

                backgroundScope.launch {
                    delay(100)
                    checkAndExecutePendingOrders()
                }

            } catch (e: Exception) {
                println("❌ Error calculating Always Signal amount: ${e.message}")
                alwaysSignalLossTracking = null
            }

            return
        }

        val order = AISignalOrder(
            id = UUID.randomUUID().toString(),
            assetRic = asset.ric,
            assetName = asset.name,
            trend = signal.trend,
            amount = baseAmount,
            executionTime = signal.executionTime,
            receivedAt = signal.receivedAt,
            originalMessage = signal.originalMessage,
            isExecuted = false,
            status = AISignalOrderStatus.PENDING,
            martingaleStep = 0,
            maxMartingaleSteps = settings?.maxSteps ?: 0
        )

        pendingOrders.add(order)
        pendingOrders.sortBy { it.executionTime }

        println("=".repeat(60))
        println("🎯 NEW AI SIGNAL RECEIVED")
        println("=".repeat(60))
        println("   Trend: ${signal.trend.uppercase()}")
        println("   Execute at: ${formatTime(signal.executionTime)}")
        println("   Delay: ${(signal.executionTime - signal.receivedAt) / 1000}s")
        println("   Current time: ${formatTime(serverTimeService.getCurrentServerTimeMillis())}")
        println("=".repeat(60))

        onAISignalOrdersUpdate(pendingOrders.toList())
        onModeStatusUpdate("Signal received: ${signal.trend.uppercase()} at ${signal.getFormattedExecutionTime()}")

        backgroundScope.launch {
            delay(100)
            checkAndExecutePendingOrders()
        }
    }

    fun handleAISignalTradeResultFromMonitor(
        parentOrderId: String,
        isWin: Boolean,
        isMartingale: Boolean,
        martingaleStep: Int,
        details: Map<String, Any>
    ) {
        println("AI Signal Result: ${if (isWin) "WIN" else "LOSE"} - Step: $martingaleStep")

        val resultStatus = if (isWin) AISignalOrderStatus.WIN else AISignalOrderStatus.LOSE
        val resultText = if (isWin) "WIN" else "LOSE"

        executedOrdersMap[parentOrderId]?.let { order ->
            executedOrdersMap[parentOrderId] = order.copy(
                result = resultText,
                status = resultStatus
            )
        }

        val orderIndex = pendingOrders.indexOfFirst { it.id == parentOrderId }
        if (orderIndex != -1) {
            pendingOrders[orderIndex] = pendingOrders[orderIndex].copy(
                result = resultText,
                status = resultStatus
            )
            onAISignalOrdersUpdate(pendingOrders.toList())
        }

        scope.launch {
            delay(3000)

            val waitingIndex = pendingOrders.indexOfFirst { it.id == parentOrderId }
            if (waitingIndex != -1) {
                pendingOrders[waitingIndex] = pendingOrders[waitingIndex].copy(
                    status = AISignalOrderStatus.WAITING
                )
                onAISignalOrdersUpdate(pendingOrders.toList())
            }
        }

        val settings = martingaleSettings
        val lossState = alwaysSignalLossTracking

        if (settings != null && settings.isAlwaysSignal && lossState != null && lossState.hasOutstandingLoss) {
            if (isWin) {
                println("Always Signal WIN - Reset tracking")
                alwaysSignalLossTracking = null

                if (onAISignalTradeStatsUpdate != null) {
                    val tradeId = details["trade_id"] as? String
                        ?: "ai_signal_always_win_${parentOrderId}_${System.currentTimeMillis()}"

                    val enhancedDetails = details + mapOf(
                        "order_source" to "AI_SIGNAL",
                        "mode" to "ALWAYS_SIGNAL",
                        "martingale_step" to martingaleStep
                    )

                    onAISignalTradeStatsUpdate(tradeId, parentOrderId, "WIN")
                }

                val winAmount = details["win_amount"] as? Long ?: 0L
                val totalRecovered = winAmount - lossState.totalLoss
                val formattedRecovery = currentCurrency.formatAmount(totalRecovered)
                onModeStatusUpdate("Always Signal WIN at Step ${lossState.currentMartingaleStep} - Recovery: $formattedRecovery")
            } else {
                val nextStep = lossState.currentMartingaleStep + 1

                if (nextStep > settings.maxSteps) {
                    println("Max steps reached - RESET")
                    alwaysSignalLossTracking = null

                    if (onAISignalTradeStatsUpdate != null) {
                        val tradeId = details["trade_id"] as? String
                            ?: "ai_signal_always_fail_${parentOrderId}_${System.currentTimeMillis()}"
                        onAISignalTradeStatsUpdate(tradeId, parentOrderId, "LOSE")
                    }

                    val formattedLoss = currentCurrency.formatAmount(lossState.totalLoss)
                    onModeStatusUpdate("Always Signal: Max steps reached - Total Loss: $formattedLoss")
                } else {
                    println("Always Signal will continue on next signal (Step $nextStep/${settings.maxSteps})")

                    val order = executedOrdersMap[parentOrderId]
                        ?: pendingOrders.find { it.id == parentOrderId }
                    val currentAmount = order?.amount ?: 0L

                    alwaysSignalLossTracking = lossState.copy(
                        currentMartingaleStep = nextStep,
                        totalLoss = lossState.totalLoss + currentAmount
                    )

                    val formattedLoss = currentCurrency.formatAmount(alwaysSignalLossTracking!!.totalLoss)
                    onModeStatusUpdate("Always Signal LOSE - Continue on next signal - Total Loss: $formattedLoss")
                }
            }

            return
        }

        if (isMartingale) {
            val martingaleInfo = activeMartingaleOrders[parentOrderId]

            if (martingaleInfo != null) {
                handleMartingaleResult(parentOrderId, martingaleInfo, isWin, details)
            }
        } else {
            handleInitialTradeResult(parentOrderId, isWin, details)
        }
    }

    fun getPendingOrders(): List<AISignalOrder> = pendingOrders.toList()

    fun getPerformanceStats(): Map<String, Any> {
        val totalOrders = pendingOrders.size
        val executedOrders = pendingOrders.count { it.isExecuted }
        val pendingCount = pendingOrders.count { !it.isExecuted }
        val activeMartingaleCount = activeMartingaleOrders.size

        val telegramStatus = telegramSignalService.getStatus()
        val monitorStatus = aiSignalTradeMonitor.getMonitoringStatus()

        val alwaysSignalStatus = if (martingaleSettings?.isAlwaysSignal == true) {
            val lossState = alwaysSignalLossTracking
            if (lossState != null && lossState.hasOutstandingLoss) {
                mapOf(
                    "is_active" to true,
                    "current_step" to lossState.currentMartingaleStep,
                    "max_steps" to (martingaleSettings?.maxSteps ?: 0),
                    "total_loss" to currentCurrency.formatAmount(lossState.totalLoss),
                    "status" to "Waiting for next signal (Step ${lossState.currentMartingaleStep}/${martingaleSettings?.maxSteps ?: 0})"
                )
            } else {
                mapOf("is_active" to false, "status" to "No outstanding loss")
            }
        } else {
            mapOf("is_active" to false, "mode" to "standard_martingale")
        }

        return mapOf(
            "is_active" to isActive,
            "total_orders" to totalOrders,
            "executed_orders" to executedOrders,
            "pending_orders" to pendingCount,
            "active_martingale_sequences" to activeMartingaleCount,
            "martingale_enabled" to (martingaleSettings?.isEnabled ?: false),
            "martingale_max_steps" to (martingaleSettings?.maxSteps ?: 0),
            "martingale_always_signal" to (martingaleSettings?.isAlwaysSignal ?: false),
            "always_signal_status" to alwaysSignalStatus,
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

    fun getExecutionDebugInfo(): Map<String, Any> {
        val currentTime = serverTimeService.getCurrentServerTimeMillis()

        return mapOf(
            "is_active" to isActive,
            "is_initialized" to isInitialized(),
            "pending_orders" to pendingOrders.size,
            "executed_orders" to pendingOrders.count { it.isExecuted },
            "current_server_time" to formatTime(currentTime),
            "execution_advance_ms" to EXECUTION_ADVANCE_MS,
            "execution_check_interval_ms" to EXECUTION_CHECK_INTERVAL_MS,
            "execution_job_active" to (executionJob?.isActive == true),
            "next_pending_orders" to pendingOrders.filter { !it.isExecuted }.map { order ->
                mapOf(
                    "id" to order.id,
                    "trend" to order.trend,
                    "execution_time" to formatTime(order.executionTime),
                    "time_until_execution" to (order.executionTime - currentTime) / 1000,
                    "status" to order.status.name,
                    "is_executed" to order.isExecuted
                )
            }
        )
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

    private fun startExecutionMonitoring() {
        executionJob = backgroundScope.launch {
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

        println("✅ Execution monitoring started with background scope")
        println("   This will survive activity lifecycle changes")
    }

    private fun stopExecutionMonitoring() {
        executionJob?.cancel()
        executionJob = null
    }

    private suspend fun checkAndExecutePendingOrders() {
        val currentTime = serverTimeService.getCurrentServerTimeMillis()

        val pendingCount = pendingOrders.count { !it.isExecuted }
        if (pendingCount > 0) {
            println("🔍 Checking $pendingCount pending orders (current: ${formatTime(currentTime)})")
        }

        val ordersToExecute = pendingOrders.filter { order ->
            val shouldExecute = !order.isExecuted &&
                    currentTime >= (order.executionTime - EXECUTION_ADVANCE_MS)

            if (shouldExecute) {
                println("=".repeat(60))
                println("⏰ ORDER READY FOR EXECUTION")
                println("=".repeat(60))
                println("   Order ID: ${order.id}")
                println("   Trend: ${order.trend}")
                println("   Execution time: ${formatTime(order.executionTime)}")
                println("   Current time: ${formatTime(currentTime)}")
                println("   Advance window: ${EXECUTION_ADVANCE_MS}ms")
                println("=".repeat(60))
            }

            shouldExecute
        }

        if (ordersToExecute.isNotEmpty()) {
            println("📋 Executing ${ordersToExecute.size} orders NOW")
        }

        ordersToExecute.forEach { order ->
            try {
                executeOrder(order)
            } catch (e: Exception) {
                println("❌ Error executing order ${order.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun executeOrder(order: AISignalOrder) {
        println("=".repeat(60))
        println("🚀 EXECUTING AI SIGNAL ORDER")
        println("=".repeat(60))
        println("   Order ID: ${order.id}")
        println("   Trend: ${order.trend}")
        println("   Amount: ${currentCurrency.formatAmount(order.amount)}")
        println("   Martingale Step: ${order.martingaleStep}")
        println("=".repeat(60))

        val orderIndex = pendingOrders.indexOfFirst { it.id == order.id }
        if (orderIndex != -1) {
            val executingOrder = order.copy(
                isExecuted = true,
                status = AISignalOrderStatus.EXECUTING
            )
            pendingOrders[orderIndex] = executingOrder
            executedOrdersMap[order.id] = executingOrder
            onAISignalOrdersUpdate(pendingOrders.toList())
        }

        var executionAttempt = 0
        val maxAttempts = 3

        fun attemptExecution() {
            executionAttempt++
            println("📤 Execution attempt $executionAttempt/$maxAttempts")

            try {
                onExecuteAISignalTrade(
                    order.trend,
                    order.id,
                    order.amount,
                    order.martingaleStep > 0,
                    order.martingaleStep
                )

                println("✅ Trade execution call successful")

                backgroundScope.launch {
                    delay(2000)

                    val monitoringIndex = pendingOrders.indexOfFirst { it.id == order.id }
                    if (monitoringIndex != -1) {
                        val monitoringOrder = pendingOrders[monitoringIndex].copy(
                            status = AISignalOrderStatus.MONITORING
                        )
                        pendingOrders[monitoringIndex] = monitoringOrder
                        executedOrdersMap[order.id] = monitoringOrder
                        onAISignalOrdersUpdate(pendingOrders.toList())
                    }
                }

            } catch (e: Exception) {
                println("❌ Execution attempt $executionAttempt failed: ${e.message}")

                if (executionAttempt < maxAttempts) {
                    println("🔄 Retrying in ${executionAttempt}s...")
                    backgroundScope.launch {
                        delay(executionAttempt * 1000L)
                        attemptExecution()
                    }
                } else {
                    println("❌ All execution attempts failed for order ${order.id}")
                    onModeStatusUpdate("Execution failed: ${order.trend.uppercase()} - All retries exhausted")
                }
            }
        }

        attemptExecution()

        onModeStatusUpdate("Executing: ${order.trend.uppercase()} - ${currentCurrency.formatAmount(order.amount)}")

        backgroundScope.launch {
            delay(300000L)
            val fiveMinutesAgo = serverTimeService.getCurrentServerTimeMillis() - 300000L
            val beforeCount = pendingOrders.size

            pendingOrders.removeAll { it.isExecuted && it.executionTime < fiveMinutesAgo }
            executedOrdersMap.entries.removeIf { (_, order) ->
                order.isExecuted && order.executionTime < fiveMinutesAgo
            }

            val afterCount = pendingOrders.size
            if (beforeCount != afterCount) {
                println("🧹 Cleaned ${beforeCount - afterCount} old orders")
                onAISignalOrdersUpdate(pendingOrders.toList())
            }
        }
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

        val settings = martingaleSettings

        if (isWin) {
            println("   ✅ AI Signal Initial Trade WIN")

            alwaysSignalLossTracking = null

            if (onAISignalTradeStatsUpdate != null) {
                val tradeId = details["trade_id"] as? String ?: "ai_signal_${orderId}_${System.currentTimeMillis()}"
                onAISignalTradeStatsUpdate(tradeId, orderId, "WIN")
            }

            val winAmount = details["win_amount"] as? Long ?: 0L
            val formattedWin = currentCurrency.formatAmount(winAmount)
            onModeStatusUpdate("✅ AI Signal WIN - Profit: $formattedWin - Ready for next signal")

        } else {
            if (settings != null && settings.isEnabled) {
                if (settings.isAlwaysSignal) {
                    println("   ❌ AI Signal LOSE - Always Signal: Continue on next signal")

                    alwaysSignalLossTracking = AlwaysSignalLossState(
                        hasOutstandingLoss = true,
                        currentMartingaleStep = 0,
                        originalOrderId = orderId,
                        totalLoss = order.amount,
                        currentTrend = order.trend
                    )

                    if (onAISignalTradeStatsUpdate != null) {
                        val tradeId = details["trade_id"] as? String ?: "ai_signal_${orderId}_${System.currentTimeMillis()}"
                        onAISignalTradeStatsUpdate(tradeId, orderId, "LOSE")
                    }

                    val lossAmount = order.amount
                    val formattedLoss = currentCurrency.formatAmount(lossAmount)
                    onModeStatusUpdate("❌ AI Signal LOSE - Loss: $formattedLoss - Always Signal: Will continue on next signal (Step 1/${settings.maxSteps})")
                } else {
                    println("   ❌ AI Signal LOSE - Starting standard martingale...")

                    val lossAmount = order.amount
                    val formattedLoss = currentCurrency.formatAmount(lossAmount)
                    onModeStatusUpdate("❌ AI Signal LOSE - Loss: $formattedLoss - Starting Martingale Step 1")

                    startMartingaleSequence(orderId, order, settings)
                }
            } else {
                println("   ❌ AI Signal Direct LOSE (martingale disabled)")

                if (onAISignalTradeStatsUpdate != null) {
                    val tradeId = details["trade_id"] as? String ?: "ai_signal_${orderId}_${System.currentTimeMillis()}"
                    onAISignalTradeStatsUpdate(tradeId, orderId, "LOSE")
                }

                val lossAmount = order.amount
                val formattedLoss = currentCurrency.formatAmount(lossAmount)
                onModeStatusUpdate("❌ AI Signal LOSE - Loss: $formattedLoss - Waiting for next signal")
            }
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
            isExecuted = true,
            result = null
        )

        pendingOrders.add(martingaleOrder)
        executedOrdersMap[martingaleOrderId] = martingaleOrder
        onAISignalOrdersUpdate(pendingOrders.toList())

        onExecuteAISignalTrade(trend, parentOrderId, amount, true, step)

        println("   ✅ Martingale trade execution triggered")
        println("=" .repeat(60))
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

            if (onAISignalTradeStatsUpdate != null) {
                val tradeId = details["trade_id"] as? String ?: "ai_signal_mart_win_${parentOrderId}_${System.currentTimeMillis()}"
                onAISignalTradeStatsUpdate(tradeId, parentOrderId, "WIN")
                println("📊 Stats updated: WIN counted (martingale recovery)")
            }

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

        if (onAISignalTradeStatsUpdate != null) {
            val tradeId = details["trade_id"] as? String ?: "ai_signal_mart_fail_${parentOrderId}_${System.currentTimeMillis()}"
            onAISignalTradeStatsUpdate(tradeId, parentOrderId, "LOSE")
            println("📊 Stats updated: LOSE counted (martingale failed)")
        }

        val formattedLoss = currentCurrency.formatAmount(martingaleInfo.totalLoss)
        onModeStatusUpdate("❌ Martingale FAILED at Step ${martingaleInfo.currentStep} - Total Loss: $formattedLoss - Waiting for next signal")
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
    val skipReason: String? = null,
    val result: String? = null,
    val status: AISignalOrderStatus = AISignalOrderStatus.WAITING,
    val martingaleStep: Int = 0,
    val maxMartingaleSteps: Int = 0
) {
    fun getStatusDisplay(): String {
        return when (status) {
            AISignalOrderStatus.WAITING -> "Waiting for signal"
            AISignalOrderStatus.PENDING -> {
                val delay = (executionTime - System.currentTimeMillis()) / 1000
                "Execute in ${delay}s"
            }
            AISignalOrderStatus.EXECUTING -> "Executing trade..."
            AISignalOrderStatus.MONITORING -> "Monitoring result..."
            AISignalOrderStatus.WIN -> "WIN"
            AISignalOrderStatus.LOSE -> "LOSE"
            AISignalOrderStatus.MARTINGALE_STEP -> "Martingale Step $martingaleStep/$maxMartingaleSteps"
            AISignalOrderStatus.COMPLETED -> "Completed"
        }
    }

    fun getExecutionTimeFormatted(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = executionTime
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )
    }
}