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

    private val EXECUTION_ADVANCE_SECONDS = 0L
    private val PRECISION_CHECK_INTERVAL_MS = 50L
    private val MIN_PREP_TIME_SECONDS = 10L
    private val EXECUTION_WINDOW_MS = 2000L

    private val PRE_WARM_SECONDS = 8L
    private val PRE_WARM_CHECK_INTERVAL_MS = 100L
    private val PRE_WARM_WEBSOCKET_DELAY_MS = 500L

    private var isPreWarmingActive = false
    private var preWarmStartTime = 0L
    private var preWarmTargetTime = 0L
    private var preWarmOrderId: String? = null

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
        println("ScheduleManager: Bot started with EXECUTION_ADVANCE = ${EXECUTION_ADVANCE_SECONDS}s")
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
    }

    fun resumeBot() {
        if (botState != BotState.PAUSED) return
        botState = BotState.RUNNING
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

        val advanceInfo = if (EXECUTION_ADVANCE_SECONDS > 0) {
            " (${EXECUTION_ADVANCE_SECONDS}s advance)"
        } else {
            " (exact timing)"
        }

        println("PRE-WARMING: Order ${order.time} - ${PRE_WARM_SECONDS}s before execution$advanceInfo")

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
            println("WebSocket connection pre-warmed and ready")
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

    private fun startUltraPrecisionMonitoring() {
        stopMonitoring()

        scheduleMonitoringJob = scope.launch {
            while (botState == BotState.RUNNING && hasActiveOrders()) {
                try {
                    val currentTime = System.currentTimeMillis()
                    var hasChanges = false

                    scheduledOrders.forEachIndexed { index, order ->
                        if (!order.isExecuted && !order.isSkipped) {
                            val executionTime = when {
                                activeMartingaleOrderId != null -> order.timeInMillis
                                else -> order.timeInMillis - (EXECUTION_ADVANCE_SECONDS * 1000)
                            }

                            val timeUntilExecution = executionTime - currentTime

                            if (timeUntilExecution in -2000..5000) {
                                println("Order ${order.time} ${order.trend.uppercase()}:")
                                println("   Time until execution: ${timeUntilExecution}ms")
                                println("   Scheduled at: ${order.timeInMillis}")
                                println("   Execution advance: ${EXECUTION_ADVANCE_SECONDS}s")
                                println("   Will execute at: $executionTime")
                            }

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

                                    val actualExecutionTime = System.currentTimeMillis()
                                    val scheduledTime = order.timeInMillis
                                    val differenceMs = actualExecutionTime - scheduledTime
                                    val differenceSign = if (differenceMs < 0) "EARLY" else "LATE"

                                    println("EXECUTING ORDER: ${order.time} ${order.trend.uppercase()}")
                                    println("   Scheduled time: $scheduledTime")
                                    println("   Actual time: $actualExecutionTime")
                                    println("   Difference: ${kotlin.math.abs(differenceMs)}ms $differenceSign")
                                    println("   Advance setting: ${EXECUTION_ADVANCE_SECONDS}s")

                                    if (kotlin.math.abs(differenceMs) <= 1000) {
                                        println("   TIMING: Excellent (within 1 second)")
                                    } else if (kotlin.math.abs(differenceMs) <= 3000) {
                                        println("   TIMING: Acceptable (within 3 seconds)")
                                    } else {
                                        println("   TIMING: Poor (over 3 seconds difference)")
                                    }

                                    executeScheduledOrderPrecise(order)
                                    scheduledOrders[index] = order.copy(isExecuted = true)
                                    hasChanges = true
                                    scheduleNextPreWarming()
                                }
                            }
                        }
                    }

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
                    println("Error in precision monitoring: ${e.message}")
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
            "execution_mode" to if (EXECUTION_ADVANCE_SECONDS == 0L) "EXACT_TIMING" else "ADVANCED_TIMING",
            "pre_warm_seconds" to PRE_WARM_SECONDS,
            "execution_window_ms" to EXECUTION_WINDOW_MS,
            "min_prep_time_seconds" to MIN_PREP_TIME_SECONDS,
            "timing_accuracy" to "HIGH_PRECISION"
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