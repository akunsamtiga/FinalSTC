package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class AISignalOrderManager(
    private val scope: CoroutineScope,
    private val onAISignalOrdersUpdate: (List<AISignalOrder>) -> Unit,
    private val onExecuteAISignalTrade: (String, String, Long) -> Unit,
    private val onModeStatusUpdate: (String) -> Unit,
    private val telegramSignalService: TelegramSignalService,
    private val serverTimeService: ServerTimeService
) {
    private var pendingOrders = mutableListOf<AISignalOrder>()
    private var isActive = false
    private var executionJob: Job? = null
    private var selectedAsset: Asset? = null
    private var isDemoAccount = true
    private var baseAmount = 1_400_000L

    private val EXECUTION_CHECK_INTERVAL_MS = 100L // Ultra-fast checking
    private val EXECUTION_ADVANCE_MS = 1000L // Execute 1 second before target time

    fun startAISignalMode(
        asset: Asset,
        isDemoAccount: Boolean,
        baseAmount: Long
    ): Result<String> {
        return try {
            if (isActive) {
                return Result.failure(Exception("AI Signal mode already active"))
            }

            this.selectedAsset = asset
            this.isDemoAccount = isDemoAccount
            this.baseAmount = baseAmount
            this.isActive = true

            println("=" .repeat(60))
            println("🤖 AI SIGNAL MODE STARTED (FCM)")
            println("=" .repeat(60))
            println("📊 Asset: ${asset.name}")
            println("💰 Base Amount: ${formatAmount(baseAmount)}")
            println("🎯 Account: ${if (isDemoAccount) "Demo" else "Real"}")
            println("📡 Waiting for signals via FCM...")
            println("=" .repeat(60))

            // ✅ FIXED: Start monitoring with proper initialization
            telegramSignalService.startMonitoring()

            // Start execution monitoring
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

            // Stop Telegram monitoring
            telegramSignalService.stopMonitoring()

            // Stop execution monitoring
            stopExecutionMonitoring()

            // Clear pending orders
            pendingOrders.clear()
            onAISignalOrdersUpdate(emptyList())

            println("🛑 AI Signal Mode stopped")
            onModeStatusUpdate("AI Signal inactive")

            Result.success("AI Signal mode stopped")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Called when new signal received from Telegram
     */
    fun handleNewSignal(signal: TelegramSignal) {
        if (!isActive) return

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

        println("📩 NEW AI SIGNAL RECEIVED:")
        println("   ID: ${order.id}")
        println("   Trend: ${signal.trend.uppercase()}")
        println("   Original: ${signal.originalMessage}")
        println("   Received at: ${formatTime(signal.receivedAt)}")
        println("   Will execute at: ${formatTime(signal.executionTime)}")
        println("   Delay: ${signal.getDelaySeconds()}s")

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
        println("🚀 EXECUTING AI SIGNAL ORDER:")
        println("   ID: ${order.id}")
        println("   Trend: ${order.trend.uppercase()}")
        println("   Amount: ${formatAmount(order.amount)}")
        println("   Scheduled: ${formatTime(order.executionTime)}")
        println("   Actual: ${formatTime(serverTimeService.getCurrentServerTimeMillis())}")

        // Mark as executed
        val orderIndex = pendingOrders.indexOfFirst { it.id == order.id }
        if (orderIndex != -1) {
            pendingOrders[orderIndex] = order.copy(isExecuted = true)
            onAISignalOrdersUpdate(pendingOrders.toList())
        }

        // Execute trade
        onExecuteAISignalTrade(order.trend, order.id, order.amount)

        onModeStatusUpdate("🚀 Executing: ${order.trend.uppercase()} - ${formatAmount(order.amount)}")

        // Cleanup old executed orders after 5 minutes
        scope.launch {
            delay(300000L) // 5 minutes
            val fiveMinutesAgo = serverTimeService.getCurrentServerTimeMillis() - 300000L
            pendingOrders.removeAll { it.isExecuted && it.executionTime < fiveMinutesAgo }
            onAISignalOrdersUpdate(pendingOrders.toList())
        }
    }

    fun isActive(): Boolean = isActive

    fun getPendingOrders(): List<AISignalOrder> = pendingOrders.toList()

    fun getPerformanceStats(): Map<String, Any> {
        val totalOrders = pendingOrders.size
        val executedOrders = pendingOrders.count { it.isExecuted }
        val pendingCount = pendingOrders.count { !it.isExecuted }

        val telegramStatus = telegramSignalService.getStatus()

        return mapOf(
            "is_active" to isActive,
            "total_orders" to totalOrders,
            "executed_orders" to executedOrders,
            "pending_orders" to pendingCount,
            "execution_check_interval_ms" to EXECUTION_CHECK_INTERVAL_MS,
            "execution_advance_ms" to EXECUTION_ADVANCE_MS,
            "telegram_status" to telegramStatus,
            "asset" to (selectedAsset?.name ?: "None"),
            "base_amount" to baseAmount,
            "account_type" to if (isDemoAccount) "Demo" else "Real"
        )
    }

    fun getModeStatus(): String {
        return when {
            !isActive -> "INACTIVE"
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

    /**
     * Test function to inject signal manually
     */
    fun injectTestSignal(signalText: String) {
        telegramSignalService.injectTestSignal(signalText)
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
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
        stopAISignalMode()
        pendingOrders.clear()
    }
}

/**
 * AI Signal Order data class
 */
data class AISignalOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String, // "call" or "put"
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