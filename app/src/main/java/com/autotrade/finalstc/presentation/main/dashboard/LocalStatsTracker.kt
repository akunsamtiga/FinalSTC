package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LocalStatsTracker(
    private val serverTimeService: ServerTimeService?
) {
    companion object {
        private const val TAG = "LocalStatsTracker"
        private val JAKARTA_TIMEZONE = TimeZone.getTimeZone("Asia/Jakarta")

        private val TODAY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = JAKARTA_TIMEZONE
        }
    }

    private val _localStats = MutableStateFlow(LocalTradingStats())
    val localStats: StateFlow<LocalTradingStats> = _localStats.asStateFlow()

    private var currentDate = ""
    private var currentAccountType = ""

    private val activeMartingaleSequences = ConcurrentHashMap<String, MartingaleSequence>()
    private val processedTrades = ConcurrentHashMap<String, ProcessedTradeLocal>()

    data class LocalTradingStats(
        val winCount: Int = 0,
        val loseCount: Int = 0,
        val drawCount: Int = 0,
        val totalTrades: Int = 0,
        val currentDate: String = "",
        val accountType: String = ""
    ) {
        fun getWinRate(): Double {
            return if (totalTrades > 0) {
                (winCount.toDouble() / totalTrades) * 100
            } else 0.0
        }
    }

    data class MartingaleSequence(
        val orderId: String,
        val startTime: Long,
        val currentStep: Int,
        val maxSteps: Int,
        val isActive: Boolean = true
    )

    data class ProcessedTradeLocal(
        val tradeId: String,
        val orderId: String,
        val result: String,
        val processedTime: Long,
        val martingaleStep: Int = 0
    )

    fun initializeOrReset(isDemoAccount: Boolean) {
        val accountType = if (isDemoAccount) "demo" else "real"
        val todayDate = getCurrentDateString()

        if (currentDate != todayDate || currentAccountType != accountType) {
            println("=== LOCAL STATS RESET ===")
            println("Previous: $currentDate ($currentAccountType)")
            println("Current: $todayDate ($accountType)")

            resetStats(todayDate, accountType)
        }
    }

    private fun resetStats(newDate: String, newAccountType: String) {
        currentDate = newDate
        currentAccountType = newAccountType

        activeMartingaleSequences.clear()
        processedTrades.clear()

        _localStats.value = LocalTradingStats(
            currentDate = newDate,
            accountType = newAccountType
        )

        println("Local stats reset for $newDate ($newAccountType)")
    }

    fun handleTradeResult(
        tradeId: String,
        orderId: String,
        result: String,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0,
        maxMartingaleSteps: Int = 5
    ) {
        if (processedTrades.containsKey(tradeId)) {
            println("⚠️ LocalStats: Trade $tradeId already processed, skipping")
            return
        }

        val currentTime = System.currentTimeMillis()

        when {
            result.uppercase() == "WIN" -> {
                handleWinResult(tradeId, orderId, currentTime, martingaleStep)
            }

            result.uppercase() == "DRAW" -> {
                handleDrawResult(tradeId, orderId, currentTime, martingaleStep)
            }

            result.uppercase() == "LOSE" || result.uppercase() == "LOSS" -> {
                if (isMartingaleAttempt) {
                    handleMartingaleLoseResult(tradeId, orderId, martingaleStep, maxMartingaleSteps, currentTime)
                } else {
                    if (maxMartingaleSteps == 1) {
                        handleDirectLoss(tradeId, orderId, currentTime)
                    } else {
                        startMartingaleSequence(orderId, maxMartingaleSteps, currentTime)
                        println("📊 LocalStats: Initial LOSS - Starting martingale (NOT counted yet)")
                    }
                }
            }
        }

        println("LocalStats: Processed trade $tradeId | Result: $result | Martingale: $isMartingaleAttempt (step $martingaleStep)")
        logCurrentStats()
    }

    private fun handleDirectLoss(tradeId: String, orderId: String, currentTime: Long) {
        processedTrades[tradeId] = ProcessedTradeLocal(
            tradeId = tradeId,
            orderId = orderId,
            result = "LOSE",
            processedTime = currentTime,
            martingaleStep = 1
        )

        val current = _localStats.value
        _localStats.value = current.copy(
            loseCount = current.loseCount + 1,
            totalTrades = current.totalTrades + 1
        )

        println("📊 LocalStats: Direct LOSS counted (no martingale)")
    }

    fun handleMartingaleCompletion(
        orderId: String,
        isWin: Boolean,
        finalStep: Int,
        tradeId: String? = null
    ) {
        val sequence = activeMartingaleSequences[orderId]
        if (sequence == null) {
            println("WARNING: Martingale completion for unknown sequence: $orderId")
        }

        val currentTime = System.currentTimeMillis()

        if (isWin) {
            val safeTradeId = tradeId ?: "martingale_win_$orderId"

            println("✅ MARTINGALE WIN - Counting to stats:")
            println("   Order ID: $orderId")
            println("   Trade ID: $safeTradeId")
            println("   Final Step: $finalStep")

            handleWinResult(safeTradeId, orderId, currentTime, finalStep)
            println("✅ Martingale WIN counted at step $finalStep")
        } else {
            val safeTradeId = tradeId ?: "martingale_fail_$orderId"

            println("❌ MARTINGALE FAILED - Counting to stats:")
            println("   Order ID: $orderId")
            println("   Trade ID: $safeTradeId")
            println("   Final Step: $finalStep")

            handleMartingaleMaxStepReached(orderId, finalStep, currentTime)
            println("❌ Martingale FAILED counted at step $finalStep")
        }

        activeMartingaleSequences.remove(orderId)

        println("Martingale completion: $orderId | Win: $isWin | Final step: $finalStep")
        logCurrentStats()
    }

    private fun handleWinResult(tradeId: String, orderId: String, currentTime: Long, martingaleStep: Int) {
        processedTrades[tradeId] = ProcessedTradeLocal(
            tradeId = tradeId,
            orderId = orderId,
            result = "WIN",
            processedTime = currentTime,
            martingaleStep = martingaleStep
        )

        activeMartingaleSequences.remove(orderId)

        val current = _localStats.value
        _localStats.value = current.copy(
            winCount = current.winCount + 1,
            totalTrades = current.totalTrades + 1
        )
    }

    private fun handleDrawResult(tradeId: String, orderId: String, currentTime: Long, martingaleStep: Int) {
        processedTrades[tradeId] = ProcessedTradeLocal(
            tradeId = tradeId,
            orderId = orderId,
            result = "DRAW",
            processedTime = currentTime,
            martingaleStep = martingaleStep
        )

        activeMartingaleSequences.remove(orderId)

        val current = _localStats.value
        _localStats.value = current.copy(
            drawCount = current.drawCount + 1,
            totalTrades = current.totalTrades + 1
        )
    }

    private fun handleMartingaleLoseResult(
        tradeId: String,
        orderId: String,
        martingaleStep: Int,
        maxSteps: Int,
        currentTime: Long
    ) {
        if (martingaleStep >= maxSteps) {
            handleMartingaleMaxStepReached(orderId, martingaleStep, currentTime)
        } else {
            updateMartingaleSequence(orderId, martingaleStep + 1, currentTime)
        }

        processedTrades[tradeId] = ProcessedTradeLocal(
            tradeId = tradeId,
            orderId = orderId,
            result = if (martingaleStep >= maxSteps) "LOSE" else "MARTINGALE_CONTINUE",
            processedTime = currentTime,
            martingaleStep = martingaleStep
        )
    }

    private fun startMartingaleSequence(orderId: String, maxSteps: Int, currentTime: Long) {
        activeMartingaleSequences[orderId] = MartingaleSequence(
            orderId = orderId,
            startTime = currentTime,
            currentStep = 1,
            maxSteps = maxSteps
        )

        println("Started martingale sequence for order: $orderId (max steps: $maxSteps)")
    }

    private fun updateMartingaleSequence(orderId: String, newStep: Int, currentTime: Long) {
        val existing = activeMartingaleSequences[orderId]
        if (existing != null) {
            activeMartingaleSequences[orderId] = existing.copy(
                currentStep = newStep
            )
        }

        println("Updated martingale sequence: $orderId to step $newStep")
    }

    private fun handleMartingaleMaxStepReached(orderId: String, finalStep: Int, currentTime: Long) {
        val current = _localStats.value
        _localStats.value = current.copy(
            loseCount = current.loseCount + 1,
            totalTrades = current.totalTrades + 1
        )

        activeMartingaleSequences.remove(orderId)

        println("Martingale FAILED for order $orderId at step $finalStep - counted as LOSE")
    }

    fun manualAddWin() {
        val current = _localStats.value
        _localStats.value = current.copy(
            winCount = current.winCount + 1,
            totalTrades = current.totalTrades + 1
        )
        println("Manual WIN added")
        logCurrentStats()
    }

    fun manualAddDraw() {
        val current = _localStats.value
        _localStats.value = current.copy(
            drawCount = current.drawCount + 1,
            totalTrades = current.totalTrades + 1
        )
        println("Manual DRAW added")
        logCurrentStats()
    }

    fun manualAddLose() {
        val current = _localStats.value
        _localStats.value = current.copy(
            loseCount = current.loseCount + 1,
            totalTrades = current.totalTrades + 1
        )
        println("Manual LOSE added")
        logCurrentStats()
    }

    fun getCurrentStats(): LocalTradingStats {
        return _localStats.value
    }

    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "current_date" to currentDate,
            "account_type" to currentAccountType,
            "active_martingale_sequences" to activeMartingaleSequences.size,
            "processed_trades" to processedTrades.size,
            "local_stats" to _localStats.value,
            "martingale_details" to activeMartingaleSequences.values.map { seq ->
                mapOf(
                    "order_id" to seq.orderId,
                    "current_step" to seq.currentStep,
                    "max_steps" to seq.maxSteps,
                    "start_time" to Date(seq.startTime).toString()
                )
            }
        )
    }

    private fun getCurrentDateString(): String {
        val jakartaTime = getCurrentJakartaTime()
        return TODAY_DATE_FORMAT.format(Date(jakartaTime))
    }

    private fun getCurrentJakartaTime(): Long {
        val baseTime = serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + ServerTimeService.cachedServerTimeOffset)

        val calendar = Calendar.getInstance(JAKARTA_TIMEZONE)
        calendar.timeInMillis = baseTime
        return calendar.timeInMillis
    }

    private fun logCurrentStats() {
        val stats = _localStats.value
        println("=== LOCAL STATS ===")
        println("Win: ${stats.winCount} | Draw: ${stats.drawCount} | Lose: ${stats.loseCount}")
        println("Total: ${stats.totalTrades} | Win Rate: ${String.format("%.1f", stats.getWinRate())}%")
        println("Date: ${stats.currentDate} | Account: ${stats.accountType}")
        println("==================")
    }

    fun forceReset(isDemoAccount: Boolean) {
        val accountType = if (isDemoAccount) "demo" else "real"
        val todayDate = getCurrentDateString()
        resetStats(todayDate, accountType)
    }
}