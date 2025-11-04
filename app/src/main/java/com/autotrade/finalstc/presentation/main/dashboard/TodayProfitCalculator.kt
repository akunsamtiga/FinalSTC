package com.autotrade.finalstc.presentation.main.dashboard

import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TodayProfitCalculator(
    private val serverTimeService: ServerTimeService?
) {
    companion object {
        private const val TAG = "TodayProfitCalculator"

        private val JAKARTA_TIMEZONE = TimeZone.getTimeZone("Asia/Jakarta")

        private val TODAY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = JAKARTA_TIMEZONE
        }

        private val WIN_STATUSES = setOf("won", "win")
        private val LOSS_STATUSES = setOf("lost", "lose", "loss", "failed")
        private val DRAW_STATUSES = setOf("stand", "draw", "tie", "draw_trade")
        private val RUNNING_STATUSES = setOf("opened", "open", "pending", "running", "active", "executing")
        private val ALL_COMPLETED_STATUSES = WIN_STATUSES + LOSS_STATUSES + DRAW_STATUSES
    }

    private var currentAccountType: String? = null
    private var currentCurrency: String = "IDR"

    private val processedTrades = ConcurrentHashMap<String, ProcessedTrade>()
    private var lastCalculatedDate = ""
    private var cachedTotalProfit = 0L
    private var cachedStats = TodayStats()
    private var lastCalculationTime = 0L

    private val calculationHistory = mutableListOf<CalculationSnapshot>()

    data class ProcessedTrade(
        val tradeId: String,
        val status: String,
        val amount: Long,
        val profit: Long,
        val processedTime: Long,
        val accountType: String,
        val tradeDate: String,
        val currency: String = "IDR"
    )

    data class CalculationSnapshot(
        val timestamp: Long,
        val totalProfit: Long,
        val newTrades: Int,
        val changedTrades: Int,
        val totalProcessed: Int,
        val trigger: String,
        val currency: String = "IDR"
    )

    data class TodayProfitResult(
        val totalProfit: Long,
        val stats: TodayStats,
        val isIncremental: Boolean = false,
        val debugInfo: Map<String, Any> = emptyMap()
    )

    fun exportState(): Map<String, Any> {
        return mapOf(
            "last_calculated_date" to lastCalculatedDate,
            "cached_total_profit" to cachedTotalProfit,
            "cached_stats" to cachedStats,
            "processed_trades" to processedTrades.toMap(),
            "current_account_type" to (currentAccountType ?: "unknown"),
            "current_currency" to currentCurrency,
            "last_calculation_time" to lastCalculationTime,
            "processed_trades_count" to processedTrades.size
        )
    }

    fun importState(
        restoredDate: String,
        restoredProfit: Long,
        restoredStats: TodayStats,
        restoredProcessedTrades: Map<String, ProcessedTrade>,
        restoredAccountType: String,
        restoredCurrency: String = "IDR"
    ): Result<Unit> {
        return try {
            println("=== IMPORTING TODAY PROFIT STATE ===")
            println("Restoring from persistence:")
            println("  Date: $restoredDate")
            println("  Profit: ${restoredProfit / 100.0} $restoredCurrency")
            println("  Currency: $restoredCurrency")
            println("  Stats: Win=${restoredStats.winCount}, Lose=${restoredStats.loseCount}, Draw=${restoredStats.drawCount}")
            println("  Account: $restoredAccountType")
            println("  Processed Trades: ${restoredProcessedTrades.size}")

            val todayDateString = getTodayDateString(getCurrentJakartaTime())
            if (restoredDate != todayDateString) {
                println("⚠️ WARNING: Restored date ($restoredDate) doesn't match today ($todayDateString)")
                println("Clearing restored data and starting fresh")
                return Result.failure(IllegalStateException("Date mismatch: restored date is not today"))
            }

            lastCalculatedDate = restoredDate
            cachedTotalProfit = restoredProfit
            cachedStats = restoredStats
            currentAccountType = restoredAccountType
            currentCurrency = restoredCurrency
            lastCalculationTime = System.currentTimeMillis()

            processedTrades.clear()
            processedTrades.putAll(restoredProcessedTrades)

            calculationHistory.clear()
            calculationHistory.add(CalculationSnapshot(
                timestamp = System.currentTimeMillis(),
                totalProfit = cachedTotalProfit,
                newTrades = 0,
                changedTrades = 0,
                totalProcessed = processedTrades.size,
                trigger = "STATE_IMPORTED_FROM_PERSISTENCE",
                currency = restoredCurrency
            ))

            println("✅ State imported successfully")
            println("   Current Profit: ${cachedTotalProfit / 100.0} $currentCurrency")
            println("   Total Trades: ${cachedStats.totalTrades}")
            println("=======================================")

            Result.success(Unit)

        } catch (e: Exception) {
            println("❌ Error importing state: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun validateImportedState(): List<String> {
        val warnings = mutableListOf<String>()

        try {
            val todayDateString = getTodayDateString(getCurrentJakartaTime())
            if (lastCalculatedDate != todayDateString) {
                warnings.add("Date mismatch: cached=$lastCalculatedDate, today=$todayDateString")
            }

            val processedCount = processedTrades.values.count { it.tradeDate == lastCalculatedDate }
            if (processedCount != cachedStats.totalTrades) {
                warnings.add("Trade count mismatch: processed=$processedCount, stats=${cachedStats.totalTrades}")
            }

            val profitDisplay = cachedTotalProfit / 100.0
            if (kotlin.math.abs(profitDisplay) > 100_000_000) {
                warnings.add("Profit seems unrealistic: $profitDisplay $currentCurrency")
            }

            if (currentAccountType.isNullOrBlank()) {
                warnings.add("Account type is not set")
            }

            val currencies = processedTrades.values.map { it.currency }.distinct()
            if (currencies.size > 1) {
                warnings.add("Multiple currencies detected: $currencies")
            }

            val calculatedProfit = processedTrades.values
                .filter { it.tradeDate == lastCalculatedDate }
                .sumOf { it.profit }

            if (calculatedProfit != cachedTotalProfit) {
                warnings.add("Profit mismatch: calculated=$calculatedProfit, cached=$cachedTotalProfit (diff=${calculatedProfit - cachedTotalProfit})")
            }

        } catch (e: Exception) {
            warnings.add("Validation error: ${e.message}")
        }

        return warnings
    }

    fun canImportState(
        restoredDate: String,
        restoredAccountType: String,
        currentIsDemoAccount: Boolean,
        restoredCurrency: String = "IDR",
        currentCurrency: String = "IDR"
    ): Boolean {
        val todayDateString = getTodayDateString(getCurrentJakartaTime())
        val expectedAccountType = if (currentIsDemoAccount) "demo" else "real"

        return restoredDate == todayDateString &&
                restoredAccountType == expectedAccountType &&
                restoredCurrency == currentCurrency
    }

    fun calculateTodayProfit(
        historyList: List<TradingHistoryNew>,
        isDemoAccount: Boolean,
        forceFull: Boolean = false,
        currencyCode: String = "IDR"
    ): TodayProfitResult {
        val debugInfo = mutableMapOf<String, Any>()
        val currentTime = System.currentTimeMillis()

        try {
            val jakartaNow = getCurrentJakartaTime()
            val todayDateString = getTodayDateString(jakartaNow)
            val accountType = if (isDemoAccount) "demo" else "real"

            debugInfo["jakarta_time"] = jakartaNow
            debugInfo["today_date"] = todayDateString
            debugInfo["account_type"] = accountType
            debugInfo["currency_code"] = currencyCode
            debugInfo["calculation_time"] = currentTime

            if (currentCurrency != currencyCode) {
                println("=== CURRENCY CHANGED - RESETTING TODAY PROFIT ===")
                println("Previous currency: $currentCurrency")
                println("Current currency: $currencyCode")
                resetForCurrencyChange(currencyCode, accountType, todayDateString)
                debugInfo["currency_reset"] = true
            }

            if (currentAccountType != null && currentAccountType != accountType) {
                println("=== ACCOUNT TYPE CHANGED - RESETTING TODAY PROFIT ===")
                println("Previous account: $currentAccountType")
                println("Current account: $accountType")
                resetForAccountChange(accountType, todayDateString)
                debugInfo["account_type_reset"] = true
            } else if (currentAccountType == null) {
                currentAccountType = accountType
                debugInfo["account_type_init"] = true
            }

            if (todayDateString != lastCalculatedDate) {
                println("=== NEW DAY DETECTED - RESETTING TODAY PROFIT ===")
                println("Previous date: $lastCalculatedDate")
                println("Current date: $todayDateString")
                resetForNewDay(todayDateString)
                debugInfo["new_day_reset"] = true
            }

            val (newTrades, changedTrades, unchangedCount) = if (forceFull) {
                processedTrades.clear()
                cachedTotalProfit = 0L
                cachedStats = TodayStats()
                analyzeTradeChanges(historyList, todayDateString, accountType, currencyCode, true)
            } else {
                analyzeTradeChanges(historyList, todayDateString, accountType, currencyCode, false)
            }

            debugInfo["new_trades_count"] = newTrades.size
            debugInfo["changed_trades_count"] = changedTrades.size
            debugInfo["unchanged_count"] = unchangedCount
            debugInfo["force_full"] = forceFull

            println("=== STABLE TODAY PROFIT CALCULATION ===")
            println("Date: $todayDateString | Account: $accountType | Currency: $currencyCode")
            println("New trades: ${newTrades.size} | Changed: ${changedTrades.size} | Unchanged: $unchangedCount")

            var profitDelta = 0L
            var statsDelta = TodayStats()

            (newTrades + changedTrades).forEach { trade ->
                val oldProcessed = processedTrades[trade.uuid]
                val newProfit = calculateSingleTradeProfit(trade)
                val oldProfit = oldProcessed?.profit ?: 0L

                processedTrades[trade.uuid] = ProcessedTrade(
                    tradeId = trade.uuid,
                    status = trade.status,
                    amount = trade.amount,
                    profit = newProfit,
                    processedTime = currentTime,
                    accountType = accountType,
                    tradeDate = todayDateString,
                    currency = currencyCode
                )

                profitDelta += (newProfit - oldProfit)

                val isNewTrade = oldProcessed == null
                val statusChanged = oldProcessed?.status != trade.status

                if (isNewTrade || statusChanged) {
                    if (statusChanged && oldProcessed != null) {
                        when (oldProcessed.status.lowercase()) {
                            in WIN_STATUSES -> statsDelta = statsDelta.copy(winCount = statsDelta.winCount - 1)
                            in LOSS_STATUSES -> statsDelta = statsDelta.copy(loseCount = statsDelta.loseCount - 1)
                            in DRAW_STATUSES -> statsDelta = statsDelta.copy(drawCount = statsDelta.drawCount - 1)
                        }
                        statsDelta = statsDelta.copy(totalTrades = statsDelta.totalTrades - 1)
                    }

                    when (trade.status.lowercase()) {
                        in WIN_STATUSES -> statsDelta = statsDelta.copy(winCount = statsDelta.winCount + 1)
                        in LOSS_STATUSES -> statsDelta = statsDelta.copy(loseCount = statsDelta.loseCount + 1)
                        in DRAW_STATUSES -> statsDelta = statsDelta.copy(drawCount = statsDelta.drawCount + 1)
                    }

                    if (isNewTrade) {
                        statsDelta = statsDelta.copy(totalTrades = statsDelta.totalTrades + 1)
                    }
                }

                println("${if (isNewTrade) "NEW" else "CHANGED"}: ${trade.uuid.take(8)} | ${trade.status} | Profit: ${oldProfit / 100.0} → ${newProfit / 100.0} $currencyCode")
            }

            cachedTotalProfit += profitDelta
            cachedStats = TodayStats(
                winCount = cachedStats.winCount + statsDelta.winCount,
                loseCount = cachedStats.loseCount + statsDelta.loseCount,
                drawCount = cachedStats.drawCount + statsDelta.drawCount,
                totalTrades = cachedStats.totalTrades + statsDelta.totalTrades
            )

            lastCalculatedDate = todayDateString
            lastCalculationTime = currentTime
            currentCurrency = currencyCode

            val snapshot = CalculationSnapshot(
                timestamp = currentTime,
                totalProfit = cachedTotalProfit,
                newTrades = newTrades.size,
                changedTrades = changedTrades.size,
                totalProcessed = processedTrades.size,
                trigger = if (forceFull) "FORCE_FULL" else "INCREMENTAL",
                currency = currencyCode
            )
            calculationHistory.add(snapshot)

            if (calculationHistory.size > 10) {
                calculationHistory.removeAt(0)
            }

            debugInfo["profit_delta"] = profitDelta
            debugInfo["final_profit"] = cachedTotalProfit
            debugInfo["processed_trades_total"] = processedTrades.size
            debugInfo["calculation_method"] = "STABLE_INCREMENTAL_WITH_CURRENCY_SUPPORT"

            println("Profit delta: ${profitDelta / 100.0} $currencyCode")
            println("Final profit: ${cachedTotalProfit / 100.0} $currencyCode")
            println("Stats: ${cachedStats.winCount}W/${cachedStats.loseCount}L/${cachedStats.drawCount}D")
            println("=======================================")

            return TodayProfitResult(
                totalProfit = cachedTotalProfit,
                stats = cachedStats,
                isIncremental = !forceFull && (newTrades.isNotEmpty() || changedTrades.isNotEmpty()),
                debugInfo = debugInfo
            )

        } catch (e: Exception) {
            println("ERROR in stable calculation: ${e.message}")
            e.printStackTrace()

            debugInfo["error"] = e.message ?: "Unknown error"
            debugInfo["calculation_method"] = "ERROR_FALLBACK"

            return TodayProfitResult(
                totalProfit = cachedTotalProfit,
                stats = cachedStats,
                debugInfo = debugInfo
            )
        }
    }

    private fun resetForCurrencyChange(newCurrency: String, accountType: String, todayDateString: String) {
        processedTrades.clear()
        cachedTotalProfit = 0L
        cachedStats = TodayStats()
        currentCurrency = newCurrency
        currentAccountType = accountType
        lastCalculatedDate = todayDateString
        calculationHistory.clear()

        println("Today profit reset for currency change: $newCurrency")
        println("All cached data cleared")
    }

    private fun resetForAccountChange(newAccountType: String, todayDateString: String) {
        processedTrades.clear()
        cachedTotalProfit = 0L
        cachedStats = TodayStats()
        currentAccountType = newAccountType
        lastCalculatedDate = todayDateString
        calculationHistory.clear()

        println("Today profit reset for account change: $newAccountType")
    }

    private fun analyzeTradeChanges(
        historyList: List<TradingHistoryNew>,
        todayDateString: String,
        accountType: String,
        currencyCode: String,
        forceFull: Boolean
    ): Triple<List<TradingHistoryNew>, List<TradingHistoryNew>, Int> {

        val todayCompletedTrades = historyList.filter { trade ->
            val tradeDate = parseTradeDate(trade.createdAt)
            val tradeDateString = getTodayDateString(tradeDate)
            val isToday = tradeDateString == todayDateString
            val isCorrectAccount = detectAccountType(trade, accountType == "demo")
            val isCompleted = trade.status.lowercase() in ALL_COMPLETED_STATUSES

            isToday && isCorrectAccount && isCompleted
        }

        println("   Total filtered trades for $accountType account ($currencyCode): ${todayCompletedTrades.size}")

        val newTrades = mutableListOf<TradingHistoryNew>()
        val changedTrades = mutableListOf<TradingHistoryNew>()
        var unchangedCount = 0

        todayCompletedTrades.forEach { trade ->
            val existingProcessed = processedTrades[trade.uuid]

            when {
                forceFull || existingProcessed == null -> {
                    newTrades.add(trade)
                }

                existingProcessed.status != trade.status ||
                        existingProcessed.amount != trade.amount ||
                        existingProcessed.accountType != accountType ||
                        existingProcessed.currency != currencyCode -> {
                    changedTrades.add(trade)
                }

                else -> {
                    unchangedCount++
                }
            }
        }

        return Triple(newTrades, changedTrades, unchangedCount)
    }

    private fun detectTradeAccountType(trade: TradingHistoryNew): String {
        return when {
            trade.isDemoAccount != null -> if (trade.isDemoAccount) "demo" else "real"
            !trade.dealType.isNullOrBlank() -> trade.dealType.lowercase().trim()
            !trade.accountType.isNullOrBlank() -> trade.accountType.lowercase().trim()
            else -> if (trade.isDemo) "demo" else "real"
        }
    }

    private fun resetForNewDay(newDate: String) {
        processedTrades.clear()
        cachedTotalProfit = 0L
        cachedStats = TodayStats()
        lastCalculatedDate = newDate
        calculationHistory.clear()

        println("Today profit reset for new day: $newDate (Currency: $currentCurrency)")
    }

    fun forceFullRecalculation(
        historyList: List<TradingHistoryNew>,
        isDemoAccount: Boolean,
        currencyCode: String = "IDR"
    ): TodayProfitResult {
        println("=== FORCE FULL RECALCULATION ===")
        println("Currency: $currencyCode")
        return calculateTodayProfit(historyList, isDemoAccount, forceFull = true, currencyCode = currencyCode)
    }

    fun getCalculationHistory(): List<CalculationSnapshot> {
        return calculationHistory.toList()
    }

    fun getProcessedTrades(): Map<String, ProcessedTrade> {
        return processedTrades.toMap()
    }

    fun manualCorrection(profitAdjustment: Long, reason: String) {
        cachedTotalProfit += profitAdjustment

        val snapshot = CalculationSnapshot(
            timestamp = System.currentTimeMillis(),
            totalProfit = cachedTotalProfit,
            newTrades = 0,
            changedTrades = 0,
            totalProcessed = processedTrades.size,
            trigger = "MANUAL_CORRECTION: $reason",
            currency = currentCurrency
        )
        calculationHistory.add(snapshot)

        println("Manual correction applied: ${profitAdjustment / 100.0} $currentCurrency - Reason: $reason")
        println("New total: ${cachedTotalProfit / 100.0} $currentCurrency")
    }

    private fun getCurrentJakartaTime(): Long {
        val baseTime = serverTimeService?.getCurrentServerTimeMillis()
            ?: (System.currentTimeMillis() + ServerTimeService.cachedServerTimeOffset)

        val calendar = Calendar.getInstance(JAKARTA_TIMEZONE)
        calendar.timeInMillis = baseTime
        return calendar.timeInMillis
    }

    private fun getTodayDateString(jakartaTime: Long): String {
        return TODAY_DATE_FORMAT.format(Date(jakartaTime))
    }

    private fun detectAccountType(trade: TradingHistoryNew, isDemoAccount: Boolean): Boolean {
        if (trade.isDemoAccount != null) {
            return trade.isDemoAccount == isDemoAccount
        }

        if (!trade.dealType.isNullOrBlank()) {
            val dealType = trade.dealType.lowercase().trim()
            return if (isDemoAccount) dealType == "demo" else dealType == "real"
        }

        if (!trade.accountType.isNullOrBlank()) {
            val accountType = trade.accountType.lowercase().trim()
            return if (isDemoAccount) accountType == "demo" else accountType == "real"
        }

        return trade.isDemo == isDemoAccount
    }

    private fun parseTradeDate(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) {
            return getCurrentJakartaTime()
        }

        try {
            if (createdAt.contains("T")) {
                val isoString = if (createdAt.endsWith("Z")) createdAt else "${createdAt}Z"
                val utcTime = java.time.Instant.parse(isoString).toEpochMilli()

                val jakartaCalendar = Calendar.getInstance(JAKARTA_TIMEZONE)
                jakartaCalendar.timeInMillis = utcTime
                return jakartaCalendar.timeInMillis
            }

            val numericPart = createdAt.filter { it.isDigit() }
            if (numericPart.isNotEmpty()) {
                val timestamp = numericPart.toLong()
                val finalTimestamp = if (timestamp > 9999999999L) timestamp else timestamp * 1000

                val jakartaCalendar = Calendar.getInstance(JAKARTA_TIMEZONE)
                jakartaCalendar.timeInMillis = finalTimestamp
                return jakartaCalendar.timeInMillis
            }

        } catch (e: Exception) {
            println("Date parsing error for '$createdAt': ${e.message}")
        }

        return getCurrentJakartaTime()
    }

    private fun calculateSingleTradeProfit(trade: TradingHistoryNew): Long {
        val status = trade.status.lowercase().trim()

        return when (status) {
            in WIN_STATUSES -> {
                val payout = when {
                    trade.payment > 0 -> trade.payment
                    trade.win > 0 -> trade.win
                    else -> trade.amount
                }

                val profit = payout - trade.amount

                if (profit <= 0 && payout == trade.amount) {
                    return 0L
                }

                profit
            }

            in LOSS_STATUSES -> -trade.amount
            in DRAW_STATUSES -> 0L

            else -> {
                println("Unknown status '$status' for trade ${trade.uuid} - treating as loss")
                -trade.amount
            }
        }
    }

    fun validateResults(result: TodayProfitResult): List<String> {
        val warnings = mutableListOf<String>()

        val profitDisplay = result.totalProfit / 100.0
        if (kotlin.math.abs(profitDisplay) > 100_000_000) {
            warnings.add("Today profit seems unrealistic: ${String.format("%.2f", profitDisplay)} $currentCurrency")
        }

        val stats = result.stats
        if (stats.totalTrades > 0) {
            val winRate = stats.winCount.toDouble() / stats.totalTrades
            if (winRate > 0.95 && stats.totalTrades > 5) {
                warnings.add("Win rate unusually high: ${String.format("%.1f", winRate * 100)}%")
            }
        }

        val processedCount = processedTrades.values.count { it.tradeDate == lastCalculatedDate }
        if (processedCount != stats.totalTrades) {
            warnings.add("Mismatch between processed trades ($processedCount) and stats (${stats.totalTrades})")
        }

        return warnings
    }

    fun getCalculatorStatus(): Map<String, Any> {
        return mapOf(
            "last_calculated_date" to lastCalculatedDate,
            "current_account_type" to (currentAccountType ?: "unknown"),
            "current_currency" to currentCurrency,
            "cached_total_profit" to cachedTotalProfit,
            "cached_stats" to cachedStats,
            "processed_trades_count" to processedTrades.size,
            "last_calculation_time" to lastCalculationTime,
            "calculation_history_size" to calculationHistory.size,
            "is_stable_mode" to true,
            "account_change_detection" to "ENABLED",
            "currency_change_detection" to "ENABLED",
            "persistence_support" to "ENABLED",
            "currency_aware" to "ENABLED",
            "currencies_processed" to processedTrades.values.map { it.currency }.distinct(),
            "currency_breakdown" to processedTrades.values
                .groupBy { it.currency }
                .mapValues { (_, trades) ->
                    mapOf(
                        "count" to trades.size,
                        "total_profit" to trades.sumOf { it.profit }
                    )
                },
            "calculator_version" to "STABLE_V2.2_CURRENCY_AWARE"
        )
    }
}