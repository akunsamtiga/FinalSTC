package com.autotrade.finalstc.presentation.main.dashboard

import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class DashboardUiState(
    val todayProfit: Long = 0L,
    val isLoading: Boolean = false,
    val userEmail: String = "",
    val isWebSocketConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val selectedAsset: Asset? = null,
    val isDemoAccount: Boolean = true,
    val error: String? = null,
    val assetsLoading: Boolean = false,
    val botState: BotState = BotState.STOPPED,
    val scheduleInput: String = "",
    val martingaleSettings: MartingaleState = MartingaleState(),
    val stopLossSettings: StopLossSettings = StopLossSettings(),
    val stopProfitSettings: StopProfitSettings = StopProfitSettings(),
    val tradingSession: TradingSession = TradingSession(),
    val botStatus: String = "Bot Stopped",
    val activeOrderId: String? = null,
    val activeMartingaleStep: Int = 0,
    val lastTradeResult: TradeResult? = null,
    val tradingMode: TradingMode = TradingMode.SCHEDULE,
    val isFollowModeActive: Boolean = false,
    val followOrderStatus: String = "Follow Order tidak aktif",
    val isTradingModeSelected: Boolean = false,
    val activeFollowOrderId: String? = null,
    val followMartingaleStep: Int = 0,
    val isIndicatorModeActive: Boolean = false,
    val indicatorSettings: IndicatorSettings = IndicatorSettings(),
    val indicatorOrderStatus: String = "Indicator Order tidak aktif",
    val activeIndicatorOrderId: String? = null,
    val indicatorMartingaleStep: Int = 0,
    val consecutiveLossSettings: ConsecutiveLossSettings = ConsecutiveLossSettings(),
    val currentSupportLevel: Double = 0.0,
    val currentResistanceLevel: Double = 0.0,
    val lastIndicatorValues: IndicatorValues? = null,
    val isCTCModeActive: Boolean = false,
    val ctcOrderStatus: String = "CTC Order tidak aktif",
    val activeCTCOrderId: String? = null,
    val ctcMartingaleStep: Int = 0,
    val currencySettings: CurrencySettings = CurrencySettings(),
    val isMultiMomentumModeActive: Boolean = false,
    val multiMomentumOrderStatus: String = "Multi-Momentum Order tidak aktif",
    val activeMultiMomentumOrderId: String? = null,
    val multiMomentumMartingaleSteps: Map<String, Int> = emptyMap(),
    val multiMomentumCandleCount: Int = 0,
    ) {
    fun canStartMultiMomentumMode(): Boolean {
        return botState == BotState.STOPPED &&
                !isFollowModeActive &&
                !isIndicatorModeActive &&
                !isCTCModeActive &&
                !isMultiMomentumModeActive &&
                selectedAsset != null &&
                isWebSocketConnected &&
                martingaleSettings.validate(currencySettings.selectedCurrency).isSuccess &&
                stopLossSettings.validate().isSuccess &&
                stopProfitSettings.validate().isSuccess &&
                getMartingaleValidationError() == null
    }

    fun canStopMultiMomentumMode(): Boolean = isMultiMomentumModeActive

    fun canModifySettings(): Boolean = botState == BotState.STOPPED &&
            !isFollowModeActive &&
            !isIndicatorModeActive &&
            !isCTCModeActive &&
            !isMultiMomentumModeActive

    fun canStartBot(): Boolean {
        return botState == BotState.STOPPED &&
                !isFollowModeActive &&
                !isIndicatorModeActive &&
                !isCTCModeActive &&
                !isMultiMomentumModeActive &&
                selectedAsset != null &&
                isWebSocketConnected &&
                martingaleSettings.validate(currencySettings.selectedCurrency).isSuccess &&
                currencySettings.validate().isSuccess &&
                stopLossSettings.validate().isSuccess &&
                stopProfitSettings.validate().isSuccess &&
                getMartingaleValidationError() == null
    }

    fun canStartFollowMode(): Boolean {
        return botState == BotState.STOPPED &&
                !isFollowModeActive &&
                !isIndicatorModeActive &&
                !isCTCModeActive &&
                !isMultiMomentumModeActive &&
                selectedAsset != null &&
                isWebSocketConnected &&
                martingaleSettings.validate(currencySettings.selectedCurrency).isSuccess &&
                stopLossSettings.validate().isSuccess &&
                stopProfitSettings.validate().isSuccess &&
                getMartingaleValidationError() == null
    }



    fun canStartIndicatorMode(): Boolean {
        return botState == BotState.STOPPED &&
                !isFollowModeActive &&
                !isIndicatorModeActive &&
                !isCTCModeActive &&
                !isMultiMomentumModeActive &&
                selectedAsset != null &&
                isWebSocketConnected &&
                martingaleSettings.validate(currencySettings.selectedCurrency).isSuccess &&
                stopLossSettings.validate().isSuccess &&
                stopProfitSettings.validate().isSuccess &&
                indicatorSettings.validate().isSuccess &&
                consecutiveLossSettings.validate().isSuccess &&
                getMartingaleValidationError() == null
    }

    fun canStartCTCMode(): Boolean {
        return botState == BotState.STOPPED &&
                !isFollowModeActive &&
                !isIndicatorModeActive &&
                !isCTCModeActive &&
                !isMultiMomentumModeActive &&
                selectedAsset != null &&
                isWebSocketConnected &&
                martingaleSettings.validate(currencySettings.selectedCurrency).isSuccess &&
                stopLossSettings.validate().isSuccess &&
                stopProfitSettings.validate().isSuccess &&
                getMartingaleValidationError() == null
    }

    fun canStopCTCMode(): Boolean = isCTCModeActive

    fun canStopFollowMode(): Boolean = isFollowModeActive

    fun canStopIndicatorMode(): Boolean = isIndicatorModeActive

    fun canPauseBot(): Boolean = botState == BotState.RUNNING
    fun canResumeBot(): Boolean = botState == BotState.PAUSED
    fun canStopBot(): Boolean = botState == BotState.RUNNING || botState == BotState.PAUSED

    fun getBotStatusText(): String {
        return when {
            isMultiMomentumModeActive -> "Multi-Momentum Order Active"
            isCTCModeActive -> "CTC Order Active"
            isIndicatorModeActive -> "Indicator Order Active"
            isFollowModeActive -> "Follow Order Active"
            else -> when (botState) {
                BotState.STOPPED -> "Bot Stopped"
                BotState.RUNNING -> "Bot Running"
                BotState.PAUSED -> "Bot Paused"
            }
        }
    }


    fun getBotStatusColor(): String {
        return when {
            isMultiMomentumModeActive -> "#00BCD4"
            isCTCModeActive -> "#FF9800"
            isIndicatorModeActive -> "#9C27B0"
            isFollowModeActive -> "#4ECDC4"
            else -> when (botState) {
                BotState.STOPPED -> "#FF6B6B"
                BotState.RUNNING -> "#51CF66"
                BotState.PAUSED -> "#FFD43B"
            }
        }
    }

    fun getSessionInfo(): Map<String, String> {
        return mapOf(
            "net_profit" to formatIndonesianCurrency(tradingSession.getNetProfit()),
            "total_trades" to "${tradingSession.totalTrades}",
            "win_rate" to "${String.format("%.1f", tradingSession.getWinRate())}%",
            "consecutive_wins" to "${tradingSession.consecutiveWins}",
            "consecutive_losses" to "${tradingSession.consecutiveLosses}",
            "session_duration" to formatDuration(System.currentTimeMillis() - tradingSession.startTime)
        )
    }

    fun shouldStopBot(): Pair<Boolean, String?> {
        val stopOnLoss = tradingSession.shouldStopOnLoss(stopLossSettings)
        val stopOnProfit = tradingSession.shouldStopOnProfit(stopProfitSettings)

        return when {
            stopOnLoss -> {
                val reason = tradingSession.getStopLossReason(stopLossSettings)
                Pair(true, "STOP LOSS: $reason")
            }
            stopOnProfit -> {
                val reason = tradingSession.getStopProfitReason(stopProfitSettings)
                Pair(true, "STOP PROFIT: $reason")
            }
            else -> Pair(false, null)
        }
    }

    fun getMartingaleValidationError(): String? {
        return try {
            val validationResult = martingaleSettings.validate(currencySettings.selectedCurrency)
            if (validationResult.isFailure) {
                validationResult.exceptionOrNull()?.message
            } else {
                when {
                    !martingaleSettings.canCalculateAllSteps() -> {
                        val overflowStep = martingaleSettings.getOverflowStep()
                        "Calculation overflow at step $overflowStep"
                    }
                    martingaleSettings.getTotalRisk() == Long.MAX_VALUE -> {
                        "Total risk calculation overflow"
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            "Validation error: ${e.message}"
        }
    }

    fun getMartingaleDisplayInfo(): Map<String, String> {
        return try {
            val settings = martingaleSettings
            mapOf(
                "sequence" to settings.getFormattedSequence(),
                "total_risk" to settings.getFormattedTotalRisk(),
                "max_steps" to "${settings.maxSteps}",
                "base_amount" to formatIndonesianCurrency(settings.baseAmount),
                "multiplier_type" to when (settings.multiplierType) {
                    MultiplierType.FIXED -> "Fixed (${settings.multiplierValue}x)"
                    MultiplierType.PERCENTAGE -> "Percentage (+${settings.multiplierValue}%)"
                },
                "validation_status" to if (getMartingaleValidationError() == null) "Valid" else "Invalid",
                "can_calculate_all" to if (settings.canCalculateAllSteps()) "Yes" else "No"
            )
        } catch (e: Exception) {
            mapOf(
                "sequence" to "Calculation error",
                "total_risk" to "Unknown",
                "validation_status" to "Error: ${e.message}",
                "can_calculate_all" to "No"
            )
        }
    }

    fun getIndicatorDisplayInfo(): Map<String, String> {
        return try {
            val settings = indicatorSettings
            mapOf(
                "type" to settings.type.name,
                "display_name" to settings.getDisplayName(),
                "parameters" to settings.getParameterString(),
                "description" to settings.getDescription(),
                "validation_status" to if (settings.validate().isSuccess) "Valid" else "Invalid",
                "sensitivity" to "${String.format("%.1f", settings.sensitivity)}",
                "current_value" to (lastIndicatorValues?.getFormattedPrimaryValue() ?: "N/A"),
                "trend" to (lastIndicatorValues?.trend ?: "UNKNOWN"),
                "strength" to (lastIndicatorValues?.strength ?: "UNKNOWN"),
                "support_level" to if (currentSupportLevel > 0) String.format("%.5f", currentSupportLevel) else "N/A",
                "resistance_level" to if (currentResistanceLevel > 0) String.format("%.5f", currentResistanceLevel) else "N/A"
            )
        } catch (e: Exception) {
            mapOf(
                "type" to "ERROR",
                "display_name" to "Error calculating",
                "validation_status" to "Error: ${e.message}"
            )
        }
    }

    fun getCurrentModeInfo(): Map<String, String> {
        return when {
            isCTCModeActive -> mapOf(
                "mode" to "CTC Order",
                "status" to ctcOrderStatus,
                "martingale_step" to if (ctcMartingaleStep > 0) "$ctcMartingaleStep" else "Tidak aktif",
                "description" to "Eksekusi otomatis CTC mode (identik dengan Follow Order)"
            )
            isIndicatorModeActive -> mapOf(
                "mode" to "Indicator Order",
                "status" to indicatorOrderStatus,
                "martingale_step" to if (indicatorMartingaleStep > 0) "$indicatorMartingaleStep" else "Tidak aktif",
                "description" to "Eksekusi otomatis berdasarkan ${indicatorSettings.type.name}",
                "indicator_info" to "${indicatorSettings.getDisplayName()} - ${lastIndicatorValues?.trend ?: "UNKNOWN"}"
            )
            isFollowModeActive -> mapOf(
                "mode" to "Follow Order",
                "status" to followOrderStatus,
                "martingale_step" to if (followMartingaleStep > 0) "$followMartingaleStep" else "Tidak aktif",
                "description" to "Eksekusi otomatis berdasarkan harga data"
            )
            botState != BotState.STOPPED -> mapOf(
                "mode" to "Schedule",
                "status" to botStatus,
                "martingale_step" to if (activeMartingaleStep > 0) "$activeMartingaleStep" else "Tidak aktif",
                "description" to "Eksekusi berdasarkan jadwal waktu"
            )
            else -> mapOf(
                "mode" to "None",
                "status" to "Tidak ada mode aktif",
                "martingale_step" to "Tidak aktif",
                "description" to "Pilih mode dan mulai trading"
            )
        }
    }


    fun getIndicatorValidationError(): String? {
        return try {
            val indicatorResult = indicatorSettings.validate()
            val consecutiveResult = consecutiveLossSettings.validate()

            when {
                indicatorResult.isFailure -> indicatorResult.exceptionOrNull()?.message
                consecutiveResult.isFailure -> consecutiveResult.exceptionOrNull()?.message
                else -> null
            }
        } catch (e: Exception) {
            "Validation error: ${e.message}"
        }
    }
}

data class StopLossSettings(
    val isEnabled: Boolean = false,
    val maxLossAmount: Long = 0L
) {
    fun validate(): Result<Unit> {
        return try {
            when {
                !isEnabled -> Result.success(Unit)
                isEnabled && maxLossAmount <= 0 ->
                    Result.failure(IllegalArgumentException("Stop loss harus memiliki batas amount yang valid"))
                isEnabled && maxLossAmount < 0 ->
                    Result.failure(IllegalArgumentException("Max loss amount tidak boleh negatif"))
                isEnabled && maxLossAmount > 1_000_000_000L ->
                    Result.failure(IllegalArgumentException("Max loss amount terlalu tinggi"))
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getFormattedMaxLoss(): String {
        return if (maxLossAmount > 0) {
            formatIndonesianCurrency(maxLossAmount)
        } else "Tidak dibatasi"
    }
}

data class StopProfitSettings(
    val isEnabled: Boolean = false,
    val targetProfitAmount: Long = 0L
) {
    fun validate(): Result<Unit> {
        return try {
            when {
                !isEnabled -> Result.success(Unit)
                isEnabled && targetProfitAmount <= 0 ->
                    Result.failure(IllegalArgumentException("Stop profit harus memiliki target amount yang valid"))
                isEnabled && targetProfitAmount < 0 ->
                    Result.failure(IllegalArgumentException("Target profit amount tidak boleh negatif"))
                isEnabled && targetProfitAmount > 10_000_000_000L ->
                    Result.failure(IllegalArgumentException("Target profit amount terlalu tinggi"))
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getFormattedTargetProfit(): String {
        return if (targetProfitAmount > 0) {
            formatIndonesianCurrency(targetProfitAmount)
        } else "Tidak dibatasi"
    }
}

data class TradingSession(
    val startTime: Long = System.currentTimeMillis(),
    var totalTrades: Int = 0,
    var totalWins: Int = 0,
    var totalLosses: Int = 0,
    var consecutiveWins: Int = 0,
    var consecutiveLosses: Int = 0,
    var isStopLossTriggered: Boolean = false,
    var isStopProfitTriggered: Boolean = false,
    var stopReason: String? = null,
    var netProfitCents: Long = 0L
) {

    fun getNetProfit(): Long = netProfitCents

    val totalLoss: Long
        get() = if (netProfitCents < 0) -netProfitCents else 0L

    val totalProfit: Long
        get() = if (netProfitCents > 0) netProfitCents else 0L

    fun getWinRate(): Double = if (totalTrades > 0) (totalWins.toDouble() / totalTrades * 100) else 0.0

    fun reset() {
        totalTrades = 0
        totalWins = 0
        totalLosses = 0
        consecutiveWins = 0
        consecutiveLosses = 0
        isStopLossTriggered = false
        isStopProfitTriggered = false
        stopReason = null
        netProfitCents = 0L

        println("TradingSession direset - semua nilai diatur ke 0")
    }

    fun updateFromHistory(
        historyTotalTrades: Int,
        historyTotalWins: Int,
        historyTotalLosses: Int,
        historyConsecutiveWins: Int,
        historyConsecutiveLosses: Int,
        historyNetProfitCents: Long
    ) {
        val oldNetProfit = netProfitCents

        totalTrades = historyTotalTrades
        totalWins = historyTotalWins
        totalLosses = historyTotalLosses
        consecutiveWins = historyConsecutiveWins
        consecutiveLosses = historyConsecutiveLosses
        netProfitCents = historyNetProfitCents

        println("TradingSession diperbarui dari history:")
        println("   Net Profit Lama: ${oldNetProfit / 100.0} IDR")
        println("   Net Profit Baru: ${netProfitCents / 100.0} IDR")
        println("   Total Loss: ${totalLoss / 100.0} IDR")
        println("   Total Profit: ${totalProfit / 100.0} IDR")
        println("   Total Trades: $totalTrades (Menang: $totalWins, Kalah: $totalLosses)")
    }

    fun shouldStopOnLoss(settings: StopLossSettings): Boolean {
        if (!settings.isEnabled) return false

        val shouldStop = settings.maxLossAmount > 0 && totalLoss >= settings.maxLossAmount

        if (shouldStop) {
            println("Kondisi stop loss tercapai: Loss Saat Ini=${totalLoss / 100.0} IDR >= Max Loss=${settings.maxLossAmount / 100.0} IDR")
        }

        return shouldStop
    }

    fun shouldStopOnProfit(settings: StopProfitSettings): Boolean {
        if (!settings.isEnabled) return false

        val shouldStop = settings.targetProfitAmount > 0 && netProfitCents >= settings.targetProfitAmount

        if (shouldStop) {
            println("Kondisi stop profit tercapai: Profit Saat Ini=${netProfitCents / 100.0} IDR >= Target=${settings.targetProfitAmount / 100.0} IDR")
        }

        return shouldStop
    }

    fun getStopLossReason(settings: StopLossSettings): String? {
        if (!settings.isEnabled || !shouldStopOnLoss(settings)) return null
        return "Loss maksimum ${formatIndonesianCurrency(settings.maxLossAmount)} tercapai (${formatIndonesianCurrency(totalLoss)})"
    }

    fun getStopProfitReason(settings: StopProfitSettings): String? {
        if (!settings.isEnabled || !shouldStopOnProfit(settings)) return null
        return "Target profit ${formatIndonesianCurrency(settings.targetProfitAmount)} tercapai (${formatIndonesianCurrency(netProfitCents)})"
    }
}

class StopLossProfitManager(
    private val scope: CoroutineScope,
    private val onStopTriggered: (String, String) -> Unit
) {
    private var currentSession = TradingSession()

    fun startNewSession() {
        currentSession = TradingSession()
        println("Sesi baru dimulai pada ${currentSession.startTime}")
    }

    fun calculateSessionFromHistory(
        historyList: List<TradingHistoryNew>,
        isDemoAccount: Boolean,
        sessionStartTime: Long = currentSession.startTime
    ): TradingSession {
        println("StopLossProfitManager menghitung sesi:")
        println("   Tipe Akun: ${if (isDemoAccount) "Demo" else "Real"}")
        println("   Waktu Mulai Sesi: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date(sessionStartTime))}")
        println("   Total Jumlah History: ${historyList.size}")

        val SESSION_TOLERANCE_MS = 5000L

        fun parseCreatedAtToMillis(createdAt: String?): Long {
            if (createdAt.isNullOrBlank()) return 0L

            try {
                val inst = java.time.Instant.parse(createdAt)
                return inst.toEpochMilli()
            } catch (_: Exception) { }

            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )

            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.getDefault())
                    if (p.endsWith("'Z'") || p.contains("Z")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val d = sdf.parse(createdAt)
                    if (d != null) return d.time
                } catch (_: Exception) { }
            }

            try {
                val n = createdAt.filter { it.isDigit() || it == '-' || it == '+' }
                val v = n.toLong()
                return if (kotlin.math.abs(v) > 999_999_999_999L) v else v * 1000L
            } catch (_: Exception) { }

            return 0L
        }

        val sessionHistories = historyList.filter { history ->
            try {
                val historyTime = parseCreatedAtToMillis(history.createdAt)
                if (historyTime == 0L) {
                    println("   createdAt tidak dapat diparse untuk trade ${history.id}: '${history.createdAt}'")
                    return@filter false
                }

                val isInSession = historyTime >= (sessionStartTime - SESSION_TOLERANCE_MS)

                val isCorrectAccountType = when {
                    history.isDemoAccount != null -> history.isDemoAccount == isDemoAccount
                    history.accountType != null -> {
                        val accountType = history.accountType.lowercase()
                        if (isDemoAccount) accountType == "demo" else accountType == "real"
                    }
                    else -> {
                        val dealType = history.dealType.lowercase()
                        if (isDemoAccount) dealType == "demo" else dealType == "real"
                    }
                }

                val isValidResult = isInSession && isCorrectAccountType

                if (isValidResult) {
                    println("   Menyertakan trade: ${history.id} | Waktu: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date(historyTime))} | Status: ${history.status} | Amount: ${history.amount} | Win: ${history.win}")
                } else {
                    if (!isInSession) println("   Dilewati (waktu sebelum sesi): ${history.id} @ ${history.createdAt}")
                    if (!isCorrectAccountType) println("   Dilewati (tipe akun salah): ${history.id} acct=${history.accountType} isDemoFlag=${history.isDemoAccount}")
                }

                isValidResult

            } catch (e: Exception) {
                println("   Error parsing tanggal untuk trade ${history.id}: ${e.message}")
                false
            }
        }

        println("   Jumlah Sesi Terfilter: ${sessionHistories.size}")

        val sessionStats = calculateSessionStats(sessionHistories)

        val previousNetProfit = currentSession.netProfitCents
        currentSession.updateFromHistory(
            historyTotalTrades = sessionStats.totalTrades,
            historyTotalWins = sessionStats.totalWins,
            historyTotalLosses = sessionStats.totalLosses,
            historyConsecutiveWins = sessionStats.consecutiveWins,
            historyConsecutiveLosses = sessionStats.consecutiveLosses,
            historyNetProfitCents = sessionStats.netProfitCents
        )

        println("   Sesi Diperbarui:")
        println("     Net Profit Sebelumnya: ${previousNetProfit / 100.0} IDR")
        println("     Net Profit Baru: ${sessionStats.netProfitCents / 100.0} IDR")
        println("     Total Loss: ${currentSession.totalLoss / 100.0} IDR")
        println("     Total Profit: ${currentSession.totalProfit / 100.0} IDR")
        println("     Total Trades: ${sessionStats.totalTrades}")
        println("     Win Rate: ${currentSession.getWinRate()}%")

        return currentSession.copy()
    }

    private fun calculateSessionStats(sessionHistories: List<TradingHistoryNew>): SessionStats {
        var totalTrades = 0
        var totalWins = 0
        var totalLosses = 0
        var consecutiveWins = 0
        var consecutiveLosses = 0
        var netProfitCents = 0L

        println("   Memproses ${sessionHistories.size} trades sesi:")

        sessionHistories.forEach { history ->
            totalTrades++

            when (history.status.lowercase()) {
                "won", "win" -> {
                    totalWins++
                    consecutiveWins++
                    consecutiveLosses = 0

                    val actualProfit = history.win - history.amount
                    netProfitCents += actualProfit
                    println("     Trade MENANG ${history.id}: Amount=${history.amount}, Win=${history.win}, Profit=${actualProfit}")
                }
                "lost", "lose", "loss" -> {
                    totalLosses++
                    consecutiveLosses++
                    consecutiveWins = 0

                    val loss = -history.amount
                    netProfitCents += loss
                    println("     Trade KALAH ${history.id}: Amount=${history.amount}, Loss=${loss}")
                }
                "stand", "draw", "tie" -> {
                    println("     Trade SERI ${history.id}: Amount=${history.amount}, Profit=0")
                }
                else -> {
                    println("     Status UNKNOWN '${history.status}' untuk trade ${history.id}")
                }
            }
        }

        println("   Statistik Sesi Akhir: NetProfit=${netProfitCents / 100.0} IDR, Trades=${totalTrades}, Menang=${totalWins}, Kalah=${totalLosses}")

        return SessionStats(
            totalTrades = totalTrades,
            totalWins = totalWins,
            totalLosses = totalLosses,
            consecutiveWins = consecutiveWins,
            consecutiveLosses = consecutiveLosses,
            netProfitCents = netProfitCents
        )
    }

    fun checkStopConditions(
        stopLossSettings: StopLossSettings,
        stopProfitSettings: StopProfitSettings
    ) {
        scope.launch {
            delay(100)

            println("Memeriksa kondisi stop:")
            println("   Net Profit Saat Ini: ${currentSession.netProfitCents / 100.0} IDR")
            println("   Total Loss: ${currentSession.totalLoss / 100.0} IDR")
            println("   Stop Loss Aktif: ${stopLossSettings.isEnabled}")
            println("   Stop Loss Max: ${stopLossSettings.maxLossAmount / 100.0} IDR")
            println("   Stop Profit Aktif: ${stopProfitSettings.isEnabled}")
            println("   Stop Profit Target: ${stopProfitSettings.targetProfitAmount / 100.0} IDR")

            if (currentSession.shouldStopOnLoss(stopLossSettings)) {
                val reason = currentSession.getStopLossReason(stopLossSettings) ?: "Stop loss tercapai"
                currentSession.isStopLossTriggered = true
                currentSession.stopReason = reason

                println("STOP LOSS TRIGGERED: $reason")
                onStopTriggered("STOP_LOSS", reason)
                return@launch
            }

            if (currentSession.shouldStopOnProfit(stopProfitSettings)) {
                val reason = currentSession.getStopProfitReason(stopProfitSettings) ?: "Stop profit tercapai"
                currentSession.isStopProfitTriggered = true
                currentSession.stopReason = reason

                println("STOP PROFIT TRIGGERED: $reason")
                onStopTriggered("STOP_PROFIT", reason)
                return@launch
            }

            println("Tidak ada kondisi stop yang tercapai")
        }
    }

    fun getCurrentSession(): TradingSession {
        return currentSession.copy()
    }

    fun resetSession() {
        println("Mereset sesi")
        currentSession.reset()
    }

    fun shouldPreventNewTrade(
        stopLossSettings: StopLossSettings,
        stopProfitSettings: StopProfitSettings
    ): Pair<Boolean, String?> {
        return when {
            currentSession.shouldStopOnLoss(stopLossSettings) -> {
                val reason = currentSession.getStopLossReason(stopLossSettings)
                Pair(true, "STOP LOSS: $reason")
            }
            currentSession.shouldStopOnProfit(stopProfitSettings) -> {
                val reason = currentSession.getStopProfitReason(stopProfitSettings)
                Pair(true, "STOP PROFIT: $reason")
            }
            else -> Pair(false, null)
        }
    }

    fun getSessionStats(): Map<String, Any> {
        return mapOf(
            "session_start" to currentSession.startTime as Any,
            "total_trades" to currentSession.totalTrades as Any,
            "total_wins" to currentSession.totalWins as Any,
            "total_losses" to currentSession.totalLosses as Any,
            "win_rate" to currentSession.getWinRate() as Any,
            "net_profit" to currentSession.getNetProfit() as Any,
            "total_loss" to currentSession.totalLoss as Any,
            "total_profit" to currentSession.totalProfit as Any,
            "is_stop_loss_triggered" to currentSession.isStopLossTriggered as Any,
            "is_stop_profit_triggered" to currentSession.isStopProfitTriggered as Any,
            "stop_reason" to (currentSession.stopReason ?: "") as Any,
            "session_duration" to (System.currentTimeMillis() - currentSession.startTime) as Any
        )
    }

    fun cleanup() {
        currentSession.reset()
    }
}

private data class SessionStats(
    val totalTrades: Int,
    val totalWins: Int,
    val totalLosses: Int,
    val consecutiveWins: Int,
    val consecutiveLosses: Int,
    val netProfitCents: Long
)

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}j ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}