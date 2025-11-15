package com.autotrade.finalstc.presentation.main.dashboard

import android.content.ContentValues.TAG
import android.util.Log
import com.autotrade.finalstc.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.filter
import kotlin.collections.isNotEmpty

class ScheduleManager(
    private val scope: CoroutineScope,
    private val onScheduledOrdersUpdate: (List<ScheduledOrder>) -> Unit,
    private val onExecuteScheduledTrade: (String, String) -> Unit,
    private val onAllSchedulesCompleted: () -> Unit,
    private val sessionManager: SessionManager,
    private val onMartingaleStepUpdate: ((String, Int) -> Unit)? = null,
    private val onMartingaleCompletion: ((orderId: String, isWin: Boolean, finalStep: Int) -> Unit)? = null
) {
    private var scheduledOrders = mutableListOf<ScheduledOrder>()
    private var scheduleMonitoringJob: Job? = null
    private var preWarmingJob: Job? = null
    private var activeMartingaleOrderId: String? = null
    private var botState: BotState = BotState.STOPPED

    private var completionCheckJob: Job? = null
    private var lastCompletionCheck = 0L
    private val COMPLETION_CHECK_INTERVAL = 5000L
    private val COMPLETION_CONFIRMATION_DELAY = 3000L

    // ========================================
    // 🔥 TIMING PRECISION CONFIGURATION
    // ========================================

    /**
     * EXECUTION_ADVANCE_SECONDS: Eksekusi order SEBELUM waktu jadwal
     *
     * Kenapa butuh advance?
     * 1. Processing time: 10-50ms untuk prepare request
     * 2. Network latency: 50-200ms untuk kirim ke server
     * 3. Server processing: 50-100ms untuk queue trade
     *
     * Nilai yang disarankan:
     * - 1L = Eksekusi 1 detik sebelum jadwal (untuk koneksi cepat)
     * - 2L = Eksekusi 2 detik sebelum jadwal (RECOMMENDED - paling stabil)
     * - 3L = Eksekusi 3 detik sebelum jadwal (untuk koneksi lambat)
     *
     * CATATAN: Server akan menjadwalkan trade untuk dimulai TEPAT pada waktu target
     */
    private val EXECUTION_ADVANCE_SECONDS = 2L  // ✅ CHANGED FROM 0L TO 2L

    private val PRECISION_CHECK_INTERVAL_MS = 50L  // Check setiap 50ms
    private val MIN_PREP_TIME_SECONDS = 10L  // Minimal 10 detik persiapan
    private val EXECUTION_WINDOW_MS = 2000L  // Window 2 detik untuk eksekusi

    // Pre-warming configuration
    private val PRE_WARM_SECONDS = 8L  // Pre-warm 8 detik sebelum eksekusi
    private val PRE_WARM_CHECK_INTERVAL_MS = 100L
    private val PRE_WARM_WEBSOCKET_DELAY_MS = 500L

    private var isPreWarmingActive = false
    private var preWarmStartTime = 0L
    private var preWarmTargetTime = 0L
    private var preWarmOrderId: String? = null

    // ========================================
    // 🎯 TIMING METRICS
    // ========================================

    private data class ExecutionMetrics(
        val scheduledTime: Long,
        val executionTime: Long,
        val advanceMs: Long,
        val accuracyMs: Long,
        val isOnTime: Boolean
    )

    private val executionHistory = mutableListOf<ExecutionMetrics>()
    private val MAX_HISTORY_SIZE = 20

    fun addScheduledOrders(input: String): Result<String> {
        return try {
            val (newOrders, errors) = parseScheduleInput(input)

            if (newOrders.isEmpty()) {
                return Result.failure(Exception(
                    if (errors.isNotEmpty()) errors.joinToString("\n") else "Tidak ada jadwal valid"
                ))
            }

            val currentTime = System.currentTimeMillis()
            val validOrders = newOrders.filter { order ->
                val timeDiff = (order.timeInMillis - currentTime) / 1000
                timeDiff >= MIN_PREP_TIME_SECONDS
            }

            if (validOrders.isEmpty()) {
                return Result.failure(Exception("Semua order terlalu dekat waktunya. Minimal ${MIN_PREP_TIME_SECONDS} detik ke depan."))
            }

            val existingKeys = scheduledOrders.map { "${it.time}_${it.trend}" }.toMutableSet()
            val nonDuplicateOrders = validOrders.filter { "${it.time}_${it.trend}" !in existingKeys }

            if (nonDuplicateOrders.isEmpty()) {
                return Result.failure(Exception("Semua jadwal yang dipaste sudah ada di list."))
            }

            scheduledOrders.addAll(nonDuplicateOrders)
            scheduledOrders.sortBy { it.timeInMillis }
            onScheduledOrdersUpdate(scheduledOrders.toList())

            saveScheduledOrdersToStorage()
            scheduleNextPreWarming()

            val message = "Jadwal ditambahkan: ${nonDuplicateOrders.size} valid, " +
                    "dilewati: ${errors.size} tidak valid, " +
                    "${validOrders.size - nonDuplicateOrders.size} duplikat."
            Result.success(message)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseScheduleInput(input: String): Pair<List<ScheduledOrder>, List<String>> {
        val orders = mutableListOf<ScheduledOrder>()
        val errors = mutableListOf<String>()

        val lines = input.trim().lines().map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }

        for ((lineIndex, line) in lines.withIndex()) {
            val parts = line.split(" ")
            if (parts.size != 2) {
                errors.add("Baris ${lineIndex + 1} format tidak valid: '$line'")
                continue
            }

            val timeStr = parts[0]
            val trendStr = parts[1].uppercase()

            if (!timeStr.matches(Regex("\\d{1,2}[:.]{1}\\d{2}"))) {
                errors.add("Baris ${lineIndex + 1} jam tidak valid: '$timeStr'")
                continue
            }

            if (trendStr !in listOf("B", "S", "BUY", "SELL", "CALL", "PUT")) {
                errors.add("Baris ${lineIndex + 1} arah/trend tidak valid: '$trendStr'")
                continue
            }

            val trend = when (trendStr) {
                "B", "BUY", "CALL" -> "call"
                "S", "SELL", "PUT" -> "put"
                else -> trendStr.lowercase()
            }

            val (hour, minute) = timeStr.split(Regex("[:.]")).map { it.toInt() }
            if (hour !in 0..23 || minute !in 0..59) {
                errors.add("Baris ${lineIndex + 1} waktu di luar rentang: '$timeStr'")
                continue
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            orders.add(
                ScheduledOrder(
                    id = UUID.randomUUID().toString(),
                    time = String.format("%02d:%02d", hour, minute),
                    trend = trend,
                    timeInMillis = calendar.timeInMillis,
                    martingaleState = ScheduledOrderMartingaleState()
                )
            )
        }

        return orders.sortedBy { it.timeInMillis } to errors
    }

    fun startBot() {
        if (botState == BotState.RUNNING) return
        botState = BotState.RUNNING

        println("=" .repeat(60))
        println("🚀 SCHEDULE MANAGER STARTED")
        println("=" .repeat(60))
        println("⏰ Timing Configuration:")
        println("   • Execution Advance: ${EXECUTION_ADVANCE_SECONDS}s BEFORE schedule")
        println("   • Check Interval: ${PRECISION_CHECK_INTERVAL_MS}ms (ultra-precise)")
        println("   • Execution Window: ${EXECUTION_WINDOW_MS}ms")
        println("   • Pre-warm: ${PRE_WARM_SECONDS}s before execution")
        println("=" .repeat(60))

        startUltraPrecisionMonitoring()
        startCompletionMonitoring()
        scheduleNextPreWarming()
    }

    fun pauseBot() {
        if (botState != BotState.RUNNING) return
        botState = BotState.PAUSED
        stopMonitoring()
        stopCompletionMonitoring()
        stopPreWarming()

        println("⏸️  Schedule Manager PAUSED")
    }

    fun resumeBot() {
        if (botState != BotState.PAUSED) return
        botState = BotState.RUNNING

        println("▶️  Schedule Manager RESUMED")

        startUltraPrecisionMonitoring()
        startCompletionMonitoring()
        scheduleNextPreWarming()
    }

    fun stopBot() {
        botState = BotState.STOPPED
        activeMartingaleOrderId = null
        stopMonitoring()
        stopCompletionMonitoring()
        stopPreWarming()

        println("⏹️  Schedule Manager STOPPED")
    }

    private fun scheduleNextPreWarming() {
        if (botState != BotState.RUNNING) return

        val nextOrder = getNextActiveOrder()
        if (nextOrder == null) {
            stopPreWarming()
            return
        }

        val executionTime = nextOrder.timeInMillis - (EXECUTION_ADVANCE_SECONDS * 1000)
        val preWarmTime = executionTime - (PRE_WARM_SECONDS * 1000)
        val currentTime = System.currentTimeMillis()
        val delayUntilPreWarm = preWarmTime - currentTime

        if (delayUntilPreWarm > 0) {
            scope.launch {
                delay(delayUntilPreWarm)
                if (botState == BotState.RUNNING) {
                    startPreWarmingForOrder(nextOrder, executionTime)
                }
            }
        }
    }

    private fun startPreWarmingForOrder(order: ScheduledOrder, executionTime: Long) {
        if (isPreWarmingActive) stopPreWarming()

        isPreWarmingActive = true
        preWarmStartTime = System.currentTimeMillis()
        preWarmTargetTime = executionTime
        preWarmOrderId = order.id

        val advanceInfo = "Advance: ${EXECUTION_ADVANCE_SECONDS}s before schedule"

        println("🔥 PRE-WARMING: Order ${order.time} - ${PRE_WARM_SECONDS}s before execution ($advanceInfo)")

        preWarmWebSocketConnection()

        preWarmingJob = scope.launch {
            val endTime = executionTime + 1000L

            while (isPreWarmingActive && System.currentTimeMillis() < endTime && botState == BotState.RUNNING) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val timeUntilExecution = executionTime - currentTime

                    when {
                        timeUntilExecution > 2000 -> delay(PRE_WARM_CHECK_INTERVAL_MS * 2)
                        timeUntilExecution > 0 -> delay(PRE_WARM_CHECK_INTERVAL_MS / 2)
                        timeUntilExecution >= -500 -> delay(25L)
                        else -> delay(PRE_WARM_CHECK_INTERVAL_MS)
                    }
                } catch (e: Exception) {
                    delay(1000)
                }
            }

            stopPreWarming()
            scheduleNextPreWarming()
        }
    }

    private fun preWarmWebSocketConnection() {
        scope.launch {
            delay(PRE_WARM_WEBSOCKET_DELAY_MS)
            println("   ✅ WebSocket connection pre-warmed and ready")
        }
    }

    private fun stopPreWarming() {
        isPreWarmingActive = false
        preWarmingJob?.cancel()
        preWarmingJob = null
        preWarmStartTime = 0L
        preWarmTargetTime = 0L
        preWarmOrderId = null
    }

    private fun getNextActiveOrder(): ScheduledOrder? {
        return scheduledOrders.filter { !it.isExecuted && !it.isSkipped }
            .minByOrNull { it.timeInMillis }
    }

    private fun startCompletionMonitoring() {
        stopCompletionMonitoring()

        completionCheckJob = scope.launch {
            while (botState == BotState.RUNNING) {
                try {
                    checkForCompletionEnhanced()
                    delay(COMPLETION_CHECK_INTERVAL)
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    private fun stopCompletionMonitoring() {
        completionCheckJob?.cancel()
        completionCheckJob = null
    }

    private suspend fun checkForCompletionEnhanced() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCompletionCheck < COMPLETION_CHECK_INTERVAL) {
            return
        }
        lastCompletionCheck = currentTime

        val activeOrders = scheduledOrders.filter { !it.isExecuted && !it.isSkipped }
        val hasPendingSchedules = activeOrders.isNotEmpty()
        val hasActiveMartingale = activeMartingaleOrderId != null
        val hasIncompleteMaringale = scheduledOrders.any { order ->
            order.martingaleState.isActive && !order.martingaleState.isCompleted
        }

        val allActivitiesComplete = !hasPendingSchedules &&
                !hasActiveMartingale &&
                !hasIncompleteMaringale &&
                !isPreWarmingActive

        if (allActivitiesComplete && scheduledOrders.isNotEmpty()) {
            delay(COMPLETION_CONFIRMATION_DELAY)
            stopPreWarming()
            onAllSchedulesCompleted()
            stopBot()
        }
    }

    // ========================================
    // 🎯 ULTRA PRECISION MONITORING
    // ========================================

    private fun startUltraPrecisionMonitoring() {
        stopMonitoring()

        scheduleMonitoringJob = scope.launch {
            while (botState == BotState.RUNNING && hasActiveOrders()) {
                try {
                    val currentTime = System.currentTimeMillis()
                    var hasChanges = false

                    scheduledOrders.forEachIndexed { index, order ->
                        if (!order.isExecuted && !order.isSkipped) {
                            // ✅ CALCULATE EXECUTION TIME WITH ADVANCE
                            val executionTime = when {
                                activeMartingaleOrderId != null -> order.timeInMillis
                                else -> order.timeInMillis - (EXECUTION_ADVANCE_SECONDS * 1000)
                            }

                            val timeUntilExecution = executionTime - currentTime

                            // 📊 DETAILED LOGGING FOR TIMING
                            if (timeUntilExecution in -2000..5000) {
                                println("⏰ Order ${order.time} ${order.trend.uppercase()}:")
                                println("   • Scheduled at: ${order.timeInMillis} (${formatTime(order.timeInMillis)})")
                                println("   • Execution advance: ${EXECUTION_ADVANCE_SECONDS}s")
                                println("   • Will execute at: $executionTime (${formatTime(executionTime)})")
                                println("   • Time until execution: ${timeUntilExecution}ms")
                                println("   • Current time: $currentTime (${formatTime(currentTime)})")
                            }

                            // ✅ EXECUTE WHEN TIME COMES (with advance applied)
                            if (timeUntilExecution <= 0 && timeUntilExecution >= -EXECUTION_WINDOW_MS) {
                                if (shouldSkipDueToMartingale(order)) {
                                    scheduledOrders[index] = order.copy(
                                        isSkipped = true,
                                        skipReason = "Dilewati karena martingale aktif"
                                    )
                                    hasChanges = true
                                } else {
                                    if (isPreWarmingActive && preWarmOrderId == order.id) {
                                        stopPreWarming()
                                    }

                                    // 📊 EXECUTION METRICS
                                    val actualExecutionTime = System.currentTimeMillis()
                                    val scheduledTime = order.timeInMillis
                                    val advanceMs = (EXECUTION_ADVANCE_SECONDS * 1000)
                                    val differenceMs = actualExecutionTime - scheduledTime
                                    val absoluteDiff = kotlin.math.abs(differenceMs + advanceMs)

                                    println("=" .repeat(60))
                                    println("🚀 EXECUTING ORDER: ${order.time} ${order.trend.uppercase()}")
                                    println("=" .repeat(60))
                                    println("⏰ TIMING ANALYSIS:")
                                    println("   • Scheduled time: $scheduledTime (${formatTime(scheduledTime)})")
                                    println("   • Actual exec time: $actualExecutionTime (${formatTime(actualExecutionTime)})")
                                    println("   • Advance setting: ${EXECUTION_ADVANCE_SECONDS}s (${advanceMs}ms)")
                                    println("   • Raw difference: ${differenceMs}ms")
                                    println("   • Accuracy: ${absoluteDiff}ms from target")
                                    println("")

                                    // ✅ TIMING QUALITY ASSESSMENT
                                    val quality = when {
                                        absoluteDiff <= 500 -> "🟢 EXCELLENT (within 500ms)"
                                        absoluteDiff <= 1000 -> "🟡 GOOD (within 1 second)"
                                        absoluteDiff <= 2000 -> "🟠 ACCEPTABLE (within 2 seconds)"
                                        else -> "🔴 NEEDS IMPROVEMENT (over 2 seconds)"
                                    }
                                    println("   📊 Quality: $quality")

                                    // Store metrics
                                    val metrics = ExecutionMetrics(
                                        scheduledTime = scheduledTime,
                                        executionTime = actualExecutionTime,
                                        advanceMs = advanceMs,
                                        accuracyMs = absoluteDiff,
                                        isOnTime = absoluteDiff <= 1000
                                    )
                                    addExecutionMetrics(metrics)

                                    println("=" .repeat(60))

                                    executeScheduledOrderPrecise(order)
                                    scheduledOrders[index] = order.copy(isExecuted = true)
                                    hasChanges = true
                                    scheduleNextPreWarming()
                                }
                            }
                        }
                    }

                    // Cleanup old executed orders
                    val twoHoursAgo = currentTime - (2 * 60 * 60 * 1000)
                    val sizeBefore = scheduledOrders.size
                    scheduledOrders.removeAll { order ->
                        (order.isExecuted || order.isSkipped) && order.timeInMillis < twoHoursAgo
                    }

                    if (scheduledOrders.size != sizeBefore) {
                        hasChanges = true
                        scheduleNextPreWarming()
                    }

                    if (hasChanges) {
                        onScheduledOrdersUpdate(scheduledOrders.toList())
                    }

                    delay(PRECISION_CHECK_INTERVAL_MS)

                } catch (e: Exception) {
                    println("❌ Error in precision monitoring: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    private fun shouldSkipDueToMartingale(order: ScheduledOrder): Boolean {
        return activeMartingaleOrderId != null && activeMartingaleOrderId != order.id
    }

    private fun executeScheduledOrderPrecise(order: ScheduledOrder) {
        onExecuteScheduledTrade(order.trend, order.id)
    }

    // ========================================
    // 📊 EXECUTION METRICS
    // ========================================

    private fun addExecutionMetrics(metrics: ExecutionMetrics) {
        executionHistory.add(metrics)
        if (executionHistory.size > MAX_HISTORY_SIZE) {
            executionHistory.removeAt(0)
        }
    }

    fun getExecutionMetrics(): Map<String, Any> {
        if (executionHistory.isEmpty()) {
            return mapOf(
                "total_executions" to 0,
                "message" to "No executions yet"
            )
        }

        val onTimeCount = executionHistory.count { it.isOnTime }
        val averageAccuracy = executionHistory.map { it.accuracyMs }.average()
        val bestAccuracy = executionHistory.minOf { it.accuracyMs }
        val worstAccuracy = executionHistory.maxOf { it.accuracyMs }

        return mapOf(
            "total_executions" to executionHistory.size,
            "on_time_executions" to onTimeCount,
            "on_time_percentage" to String.format("%.1f%%", (onTimeCount.toDouble() / executionHistory.size) * 100),
            "average_accuracy_ms" to String.format("%.0f", averageAccuracy),
            "best_accuracy_ms" to bestAccuracy,
            "worst_accuracy_ms" to worstAccuracy,
            "execution_advance_seconds" to EXECUTION_ADVANCE_SECONDS,
            "recent_executions" to executionHistory.takeLast(5).map { metrics ->
                mapOf(
                    "scheduled_time" to formatTime(metrics.scheduledTime),
                    "execution_time" to formatTime(metrics.executionTime),
                    "accuracy_ms" to metrics.accuracyMs,
                    "is_on_time" to metrics.isOnTime
                )
            }
        )
    }

    // ========================================
    // 🛠️ HELPER FUNCTIONS
    // ========================================

    private fun formatTime(timeMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        return String.format(
            "%02d:%02d:%02d.%03d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            calendar.get(Calendar.MILLISECOND)
        )
    }

    fun skipOrder(orderId: String, reason: String) {
        val orderIndex = scheduledOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            scheduledOrders[orderIndex] = scheduledOrders[orderIndex].copy(
                isSkipped = true,
                skipReason = reason
            )
            onScheduledOrdersUpdate(scheduledOrders.toList())

            if (preWarmOrderId == orderId) {
                stopPreWarming()
                scheduleNextPreWarming()
            }
        }
    }

    fun startMartingaleForOrder(orderId: String) {
        activeMartingaleOrderId = orderId

        val orderIndex = scheduledOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            scheduledOrders[orderIndex] = scheduledOrders[orderIndex].copy(
                martingaleState = scheduledOrders[orderIndex].martingaleState.copy(
                    isActive = true,
                    currentStep = 1,
                    isCompleted = false,
                    finalResult = null
                )
            )
            onScheduledOrdersUpdate(scheduledOrders.toList())
        }

        if (preWarmOrderId == orderId) {
            stopPreWarming()
        }
    }

    fun completeOrder(orderId: String, isWin: Boolean) {
        val orderIndex = scheduledOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            val order = scheduledOrders[orderIndex]
            val wasActiveMartingale = activeMartingaleOrderId == orderId

            if (wasActiveMartingale) {
                activeMartingaleOrderId = null

                onMartingaleCompletion?.invoke(
                    orderId,
                    isWin,
                    order.martingaleState.currentStep
                )

                println("ScheduleManager: Notified martingale completion - Order: $orderId, Win: $isWin, Step: ${order.martingaleState.currentStep}")
            }

            val finalMartingaleState = if (order.martingaleState.isActive || wasActiveMartingale) {
                order.martingaleState.copy(
                    isActive = false,
                    isCompleted = true,
                    finalResult = if (isWin) "MENANG" else "KALAH"
                )
            } else {
                ScheduledOrderMartingaleState(
                    isActive = false,
                    isCompleted = true,
                    finalResult = if (isWin) "MENANG" else "KALAH"
                )
            }

            scheduledOrders[orderIndex] = order.copy(
                martingaleState = finalMartingaleState
            )

            onScheduledOrdersUpdate(scheduledOrders.toList())
            saveScheduledOrdersToStorage()
            scheduleNextPreWarming()
        }
    }

    fun updateMartingaleStep(orderId: String, step: Int, totalLoss: Long = 0L) {
        val orderIndex = scheduledOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            val order = scheduledOrders[orderIndex]

            scheduledOrders[orderIndex] = order.copy(
                martingaleState = order.martingaleState.copy(
                    currentStep = step,
                    totalLoss = totalLoss
                )
            )

            onScheduledOrdersUpdate(scheduledOrders.toList())
        }
    }

    fun loadScheduledOrdersFromStorage() {
        try {
            val savedOrders = sessionManager.getScheduledOrders()

            if (savedOrders.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                val relevantOrders = savedOrders.filter { order ->
                    !order.isExecuted && !order.isSkipped &&
                            order.timeInMillis > currentTime
                }

                if (relevantOrders.isNotEmpty()) {
                    scheduledOrders.clear()
                    scheduledOrders.addAll(relevantOrders)
                    scheduledOrders.sortBy { it.timeInMillis }
                    onScheduledOrdersUpdate(scheduledOrders.toList())

                    Log.d(TAG, "Loaded ${relevantOrders.size} scheduled orders from storage")
                } else {
                    sessionManager.clearScheduledOrders()
                    Log.d(TAG, "All saved orders expired, cleared storage")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading scheduled orders: ${e.message}")
        }
    }

    fun removeScheduledOrder(orderId: String) {
        val sizeBefore = scheduledOrders.size
        scheduledOrders.removeAll { it.id == orderId }

        if (scheduledOrders.size != sizeBefore) {
            if (activeMartingaleOrderId == orderId) {
                activeMartingaleOrderId = null
            }

            if (preWarmOrderId == orderId) {
                stopPreWarming()
            }

            onScheduledOrdersUpdate(scheduledOrders.toList())
            saveScheduledOrdersToStorage()
            scheduleNextPreWarming()
        }

        if (scheduledOrders.isEmpty()) {
            stopBot()
            onAllSchedulesCompleted()
        }
    }

    fun clearAllScheduledOrders() {
        val hadOrders = scheduledOrders.isNotEmpty()
        scheduledOrders.clear()
        activeMartingaleOrderId = null

        if (hadOrders) {
            stopBot()
            onScheduledOrdersUpdate(emptyList())
            sessionManager.clearScheduledOrders()
            onAllSchedulesCompleted()
        }
    }

    private fun stopMonitoring() {
        scheduleMonitoringJob?.cancel()
        scheduleMonitoringJob = null
    }

    fun getTimingStatus(): String {
        val currentTime = System.currentTimeMillis()
        val activeOrders = scheduledOrders.filter { !it.isExecuted && !it.isSkipped }
        val nextOrder = activeOrders.minByOrNull { it.timeInMillis }

        return when {
            activeMartingaleOrderId != null -> {
                val martingaleOrder = scheduledOrders.find { it.id == activeMartingaleOrderId }
                val stepInfo = martingaleOrder?.let { "Langkah ${it.martingaleState.currentStep}" } ?: "Langkah tidak diketahui"
                "[>] Martingale Aktif: ${martingaleOrder?.time} ($stepInfo)"
            }
            botState != BotState.RUNNING -> "[X] Bot ${botState.name.lowercase()}"
            isPreWarmingActive && nextOrder != null -> {
                val timeUntilExecution = (preWarmTargetTime - currentTime) / 1000.0
                "[PRE] PRA-PEMANASAN: ${nextOrder.time} ${nextOrder.trend.uppercase()} (${String.format("%.1f", timeUntilExecution)}d)"
            }
            nextOrder != null -> {
                val executionTime = nextOrder.timeInMillis - (EXECUTION_ADVANCE_SECONDS * 1000)
                val executeIn = executionTime - currentTime

                when {
                    executeIn <= 0 -> "[!] MENJALANKAN: ${nextOrder.time} ${nextOrder.trend.uppercase()}"
                    executeIn <= 5000 -> "[~] SIAP: ${nextOrder.time} ${nextOrder.trend.uppercase()} dalam ${String.format("%.1f", executeIn/1000.0)}d"
                    executeIn <= (PRE_WARM_SECONDS * 1000) -> "[PRE] PRA-PEMANASAN SEGERA: ${nextOrder.time} ${nextOrder.trend.uppercase()}"
                    executeIn <= 30000 -> "[+] BERIKUTNYA: ${nextOrder.time} ${nextOrder.trend.uppercase()} dalam ${executeIn/1000}d"
                    else -> "[#] Terjadwal: ${nextOrder.time} ${nextOrder.trend.uppercase()}"
                }
            }
            scheduledOrders.isNotEmpty() -> {
                val completedCount = scheduledOrders.count { it.isExecuted }
                val skippedCount = scheduledOrders.count { it.isSkipped }
                val martingaleCount = scheduledOrders.count { it.martingaleState.isCompleted }
                val winCount = scheduledOrders.count { it.martingaleState.finalResult == "MENANG" }

                "[OK] Selesai: $completedCount, Martingale: $martingaleCount ($winCount menang), Dilewati: $skippedCount"
            }
            else -> "[-] Tidak ada jadwal"
        }
    }

    fun hasActiveSchedules(): Boolean {
        return hasActiveOrders() || activeMartingaleOrderId != null || isPreWarmingActive
    }

    private fun hasActiveOrders(): Boolean {
        return scheduledOrders.any { !it.isExecuted && !it.isSkipped }
    }

    fun getScheduledOrders(): List<ScheduledOrder> {
        return scheduledOrders.toList()
    }

    fun getTimingConfig(): Map<String, Any> {
        return mapOf(
            "precision_check_ms" to PRECISION_CHECK_INTERVAL_MS,
            "execution_advance_seconds" to EXECUTION_ADVANCE_SECONDS,
            "execution_mode" to when (EXECUTION_ADVANCE_SECONDS) {
                0L -> "EXACT_TIMING (⚠️ May be late)"
                1L -> "EARLY_1S (Good for fast connection)"
                2L -> "EARLY_2S (Recommended - most stable)"
                else -> "EARLY_${EXECUTION_ADVANCE_SECONDS}S (Custom)"
            },
            "pre_warm_seconds" to PRE_WARM_SECONDS,
            "execution_window_ms" to EXECUTION_WINDOW_MS,
            "min_prep_time_seconds" to MIN_PREP_TIME_SECONDS,
            "timing_accuracy" to "HIGH_PRECISION",
            "advance_explanation" to "Orders execute ${EXECUTION_ADVANCE_SECONDS}s BEFORE scheduled time to compensate for network latency"
        )
    }

    fun getNextExecutionTime(): Long? {
        val nextOrder = getNextActiveOrder()
        return nextOrder?.let { it.timeInMillis - (EXECUTION_ADVANCE_SECONDS * 1000) }
    }

    fun cleanup() {
        stopBot()
        stopCompletionMonitoring()
        stopPreWarming()
        scheduledOrders.clear()
        activeMartingaleOrderId = null
        lastCompletionCheck = 0L
        executionHistory.clear()
    }

    private fun saveScheduledOrdersToStorage() {
        try {
            sessionManager.saveScheduledOrders(scheduledOrders.toList())
            Log.d(TAG, "Auto-saved ${scheduledOrders.size} orders to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-saving orders: ${e.message}")
        }
    }

    fun updateMartingaleStepRealtime(orderId: String, step: Int) {
        val orderIndex = scheduledOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            val order = scheduledOrders[orderIndex]

            scheduledOrders[orderIndex] = order.copy(
                martingaleState = order.martingaleState.copy(
                    currentStep = step,
                    isActive = true
                )
            )

            onScheduledOrdersUpdate(scheduledOrders.toList())

            println("ScheduleManager: Updated order $orderId to step $step")
        }
    }
}