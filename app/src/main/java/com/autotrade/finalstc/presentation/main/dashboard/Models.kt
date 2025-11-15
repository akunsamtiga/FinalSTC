package com.autotrade.finalstc.presentation.main.dashboard

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

enum class TradingMode {
    SCHEDULE,
    FOLLOW_ORDER,
    INDICATOR_ORDER,
    CTC_ORDER,
    MULTI_MOMENTUM
}

enum class BotState {
    STOPPED, RUNNING, PAUSED
}

enum class MultiplierType {
    FIXED, PERCENTAGE
}

data class Asset(
    val ric: String,
    val name: String,
    val typeName: String,
    val profitRate: Double,
    val isActive: Boolean = true
)

data class TradeOrder(
    val amount: Long,
    val createdAt: Long,
    val dealType: String = "demo",
    val expireAt: Long,
    val iso: String = "IDR",
    val optionType: String = "turbo",
    val ric: String,
    val trend: String,
    val duration: Int,
    val scheduledOrderId: String? = null,
    val isMartingaleAttempt: Boolean = false,
    val martingaleStep: Int = 0
)

data class TradeResult(
    val success: Boolean,
    val message: String,
    val orderId: String? = null,
    val details: Map<String, Any>? = null,
    val rawResponse: String? = null,
    val isMartingaleAttempt: Boolean = false,
    val martingaleStep: Int = 0,
    val martingaleTotalLoss: Long = 0L,
    val martingaleStatus: String? = null,
    val scheduledOrderId: String? = null,
    val isScheduledOrder: Boolean = false,
    val executionType: String? = null
) {
    fun isExecutionSuccess(): Boolean = executionType == "EXECUTION_SUCCESS"
    fun isExecutionFailure(): Boolean = executionType in listOf("EXECUTION_FAILED", "WEBSOCKET_SEND_FAILED", "TIMEOUT")
}

data class MartingaleState(
    val isEnabled: Boolean = true,
    val maxSteps: Int = 3,
    val multiplierType: MultiplierType = MultiplierType.FIXED,
    val baseAmount: Long = 1_400_000L,
    val multiplierValue: Double = 2.0,
) {
    fun validate(currency: CurrencyType = CurrencyType.IDR): Result<Unit> {
        return try {
            when {
                maxSteps < 1 -> Result.failure(IllegalArgumentException("Max steps must be at least 1"))
                maxSteps > 10 -> Result.failure(IllegalArgumentException("Max steps cannot exceed 10"))

                baseAmount < currency.minAmountInCents -> Result.failure(
                    IllegalArgumentException(
                        "Base amount must be at least ${currency.formatAmount(currency.minAmountInCents)}"
                    )
                )
                baseAmount > 100_000_000_000L -> Result.failure(
                    IllegalArgumentException("Base amount too high")
                )

                multiplierType == MultiplierType.FIXED && multiplierValue <= 1.0 ->
                    Result.failure(IllegalArgumentException("Fixed multiplier must be greater than 1.0"))
                multiplierType == MultiplierType.FIXED && multiplierValue > 15.0 ->
                    Result.failure(IllegalArgumentException("Fixed multiplier too high"))

                multiplierType == MultiplierType.PERCENTAGE && multiplierValue <= 0.0 ->
                    Result.failure(IllegalArgumentException("Percentage multiplier must be greater than 0%"))
                multiplierType == MultiplierType.PERCENTAGE && multiplierValue > 1000.0 ->
                    Result.failure(IllegalArgumentException("Percentage too high"))

                else -> {
                    try {
                        val testSequence = getAmountSequence()
                        val testTotalRisk = testSequence.sum()

                        when {
                            testSequence.size < maxSteps + 1 ->
                                Result.failure(ArithmeticException("Calculation overflow at step ${testSequence.size + 1}"))
                            testTotalRisk > 1000000000000000000L ->
                                Result.failure(IllegalArgumentException("Total risk exceeds maximum"))
                            testSequence.last() > 5000000000000000000L ->
                                Result.failure(IllegalArgumentException("Final step amount exceeds maximum"))
                            else -> Result.success(Unit)
                        }
                    } catch (e: ArithmeticException) {
                        Result.failure(e)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getAmountForStep(step: Int): Long {
        if (step < 1 || step > maxSteps + 1) {
            throw IllegalArgumentException("Step must be between 1 and ${maxSteps + 1}")
        }

        return try {
            when (multiplierType) {
                MultiplierType.FIXED -> {
                    if (step == 1) {
                        baseAmount
                    } else {
                        val martingaleStep = step - 1
                        val result = baseAmount.toDouble() * multiplierValue.pow(martingaleStep)

                        if (result > Long.MAX_VALUE || result.isInfinite() || result.isNaN()) {
                            throw ArithmeticException("Amount calculation overflow at step $step")
                        }

                        result.toLong()
                    }
                }
                MultiplierType.PERCENTAGE -> {
                    val multiplier = 1.0 + (multiplierValue / 100.0)
                    if (step == 1) {
                        baseAmount
                    } else {
                        val martingaleStep = step - 1
                        val result = baseAmount.toDouble() * multiplier.pow(martingaleStep)

                        if (result > Long.MAX_VALUE || result.isInfinite() || result.isNaN()) {
                            throw ArithmeticException("Amount calculation overflow at step $step")
                        }

                        result.toLong()
                    }
                }
            }
        } catch (e: ArithmeticException) {
            throw e
        } catch (e: Exception) {
            throw ArithmeticException("Amount calculation failed at step $step: ${e.message}")
        }
    }

    fun getAmountSequence(): List<Long> {
        return try {
            (1..maxSteps + 1).map { step -> getAmountForStep(step) }
        } catch (e: ArithmeticException) {
            val sequence = mutableListOf<Long>()
            for (step in 1..maxSteps + 1) {
                try {
                    sequence.add(getAmountForStep(step))
                } catch (overflow: ArithmeticException) {
                    break
                }
            }
            sequence
        }
    }

    fun getMartingaleAmountForStep(martingaleStep: Int): Long {
        if (martingaleStep < 1 || martingaleStep > maxSteps) {
            throw IllegalArgumentException("Martingale step must be between 1 and $maxSteps")
        }

        val rawAmount = when (multiplierType) {
            MultiplierType.FIXED -> {
                val result = baseAmount.toDouble() * multiplierValue.pow(martingaleStep)
                if (result > Long.MAX_VALUE || result.isInfinite() || result.isNaN()) {
                    throw ArithmeticException("Amount calculation overflow at step $martingaleStep")
                }
                result.toLong()
            }
            MultiplierType.PERCENTAGE -> {
                val multiplier = 1.0 + (multiplierValue / 100.0)
                val result = baseAmount.toDouble() * multiplier.pow(martingaleStep)
                if (result > Long.MAX_VALUE || result.isInfinite() || result.isNaN()) {
                    throw ArithmeticException("Amount calculation overflow at step $martingaleStep")
                }
                result.toLong()
            }
        }

        val adjustedAmount = roundToNearestValidAmount(rawAmount)
        if (!isValidTradingAmount(adjustedAmount)) {
            println("Adjusted martingale step $martingaleStep amount to $adjustedAmount to fit broker rules")
        }

        return adjustedAmount
    }

    private fun roundToNearestValidAmount(amount: Long): Long {
        val minTradeAmount = 1_000L
        return (amount / minTradeAmount) * minTradeAmount
    }

    fun getFormattedSequence(): String {
        return try {
            getAmountSequence().mapIndexed { index, amount ->
                val stepLabel = when (index) {
                    0 -> "Initial"
                    else -> "Martingale ${index}"
                }
                "$stepLabel: ${formatCompactAmount(amount)}"
            }.joinToString(" â†’ ")
        } catch (e: ArithmeticException) {
            "Calculation overflow"
        }
    }

    fun getTotalRisk(): Long {
        return try {
            getAmountSequence().sum()
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }
    }

    fun getFormattedTotalRisk(): String {
        val totalRisk = getTotalRisk()
        return if (totalRisk == Long.MAX_VALUE) {
            "Overflow"
        } else {
            formatCompactAmount(totalRisk)
        }
    }

    fun validate(): Result<Unit> {
        return try {
            when {
                maxSteps < 1 -> Result.failure(IllegalArgumentException("Max steps must be at least 1"))
                maxSteps > 10 -> Result.failure(IllegalArgumentException("Max steps cannot exceed 10"))

                baseAmount < 1_400_000L -> Result.failure(IllegalArgumentException("Base amount must be at least 1,400,000 IDR"))
                baseAmount > 100_000_000_000L -> Result.failure(IllegalArgumentException("Base amount too high"))

                multiplierType == MultiplierType.FIXED && multiplierValue <= 1.0 ->
                    Result.failure(IllegalArgumentException("Fixed multiplier must be greater than 1.0"))
                multiplierType == MultiplierType.FIXED && multiplierValue > 15.0 ->
                    Result.failure(IllegalArgumentException("Fixed multiplier too high"))

                multiplierType == MultiplierType.PERCENTAGE && multiplierValue <= 0.0 ->
                    Result.failure(IllegalArgumentException("Percentage multiplier must be greater than 0%"))
                multiplierType == MultiplierType.PERCENTAGE && multiplierValue > 1000.0 ->
                    Result.failure(IllegalArgumentException("Percentage too high"))

                else -> {
                    try {
                        val testSequence = getAmountSequence()
                        val testTotalRisk = testSequence.sum()

                        when {
                            testSequence.size < maxSteps + 1 ->
                                Result.failure(ArithmeticException("Calculation overflow at step ${testSequence.size + 1}"))
                            testTotalRisk > 1000000000000000000L ->
                                Result.failure(IllegalArgumentException("Total risk exceeds IDR"))
                            testSequence.last() > 5000000000000000000L ->
                                Result.failure(IllegalArgumentException("Final step amount exceeds IDR"))
                            else -> Result.success(Unit)
                        }
                    } catch (e: ArithmeticException) {
                        Result.failure(e)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun canCalculateAllSteps(): Boolean {
        return try {
            getAmountSequence().size == maxSteps + 1
        } catch (e: Exception) {
            false
        }
    }

    fun getOverflowStep(): Int? {
        for (step in 1..maxSteps + 1) {
            try {
                getAmountForStep(step)
            } catch (e: ArithmeticException) {
                return step
            }
        }
        return null
    }

    private fun formatCompactAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class ScheduledOrderMartingaleState(
    val isActive: Boolean = false,
    val currentStep: Int = 0,
    val maxSteps: Int = 10,
    val isCompleted: Boolean = false,
    val finalResult: String? = null,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L
) {
    fun getStatusDisplay(): String {
        return when {
            !isActive && !isCompleted -> "Ready"
            isActive -> "Step ${currentStep + 1}/$maxSteps (Loss: ${formatCompactAmount(totalLoss)})"
            isCompleted -> when (finalResult) {
                "WIN" -> "WIN (Recovered: ${formatCompactAmount(totalRecovered)})"
                "LOSS" -> "LOSS (Total: ${formatCompactAmount(totalLoss)})"
                else -> "Completed"
            }
            else -> "Unknown"
        }
    }

    private fun formatCompactAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class ScheduledOrder(
    val id: String,
    val time: String,
    val trend: String,
    val timeInMillis: Long,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val martingaleState: ScheduledOrderMartingaleState = ScheduledOrderMartingaleState(),
    val result: String? = null
) {
    fun getStatusDisplay(): String {
        return when {
            isSkipped -> "Skipped: ${skipReason ?: "Unknown reason"}"
            isExecuted && martingaleState.isCompleted -> {
                when (martingaleState.finalResult) {
                    "WIN", "MENANG" -> "WIN (${formatCompactAmount(martingaleState.totalRecovered)})"
                    "LOSS", "KALAH" -> "LOSE (${formatCompactAmount(martingaleState.totalLoss)})"
                    else -> when (result?.uppercase()) {
                        "WON", "WIN" -> "WIN"
                        "LOST", "LOSE", "LOSS", "FAILED" -> "LOSE"
                        "STAND", "DRAW", "TIE" -> "DRAW"
                        else -> "Monitoring..."
                    }
                }
            }
            isExecuted && martingaleState.isActive -> "Martingale ${martingaleState.getStatusDisplay()}"
            isExecuted && result != null -> {
                when (result.uppercase()) {
                    "WON", "WIN" -> "WIN"
                    "LOST", "LOSE", "LOSS", "FAILED" -> "LOSE"
                    "STAND", "DRAW", "TIE" -> "DRAW"
                    else -> "Monitoring..."
                }
            }
            isExecuted -> "Monitoring..."
            else -> "Pending"
        }
    }

    private fun formatCompactAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}



data class FollowMartingaleOrder(
    val originalOrderId: String,
    val currentStep: Int,
    val maxSteps: Int,
    val totalLoss: Long,
    val nextAmount: Long,
    val isActive: Boolean
)

data class FollowMartingaleResult(
    val isWin: Boolean,
    val step: Int,
    val amount: Long,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L,
    val message: String,
    val shouldContinue: Boolean = false,
    val isMaxReached: Boolean = false,
    val followOrderId: String? = null
)

data class TodayStats(
    val winCount: Int = 0,
    val loseCount: Int = 0,
    val drawCount: Int = 0,
    val totalTrades: Int = 0
) {
    fun getWinRate(): Double {
        return if (totalTrades > 0) {
            (winCount.toDouble() / totalTrades) * 100
        } else {
            0.0
        }
    }

    fun getLoseRate(): Double {
        return if (totalTrades > 0) {
            (loseCount.toDouble() / totalTrades) * 100
        } else {
            0.0
        }
    }

    fun getDrawRate(): Double {
        return if (totalTrades > 0) {
            (drawCount.toDouble() / totalTrades) * 100
        } else {
            0.0
        }
    }

    fun getSummaryText(): String {
        return if (totalTrades > 0) {
            "Today: ${totalTrades} trades (${winCount}W/${loseCount}L/${drawCount}D) - ${String.format("%.1f", getWinRate())}% win rate"
        } else {
            "No trades today"
        }
    }

    fun getFormattedStats(): String {
        return "${winCount}W/${loseCount}L/${drawCount}D"
    }

    fun isEmpty(): Boolean = totalTrades == 0

    fun hasWins(): Boolean = winCount > 0
    fun hasLosses(): Boolean = loseCount > 0
    fun hasDraws(): Boolean = drawCount > 0
}

data class FollowOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String,
    val amount: Long,
    val executionTime: Long,
    val sourceCandle: Candle,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val martingaleState: FollowOrderMartingaleState = FollowOrderMartingaleState()
) {
    fun getStatusDisplay(): String {
        return when {
            isSkipped -> "Skipped: ${skipReason ?: "Unknown reason"}"
            isExecuted && martingaleState.isCompleted -> {
                when (martingaleState.finalResult) {
                    "WIN", "MENANG" -> "WIN (${formatAmount(martingaleState.totalRecovered)})"
                    "LOSS", "KALAH" -> "LOSE (${formatAmount(martingaleState.totalLoss)})"
                    else -> "Monitoring..."
                }
            }
            isExecuted && martingaleState.isActive -> "Martingale ${martingaleState.getStatusDisplay()}"
            isExecuted -> "Monitoring..."
            else -> "Pending"
        }
    }

    fun getExecutionTimeFormatted(): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(java.util.Date(executionTime))
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}


data class FollowOrderMartingaleState(
    val isActive: Boolean = false,
    val currentStep: Int = 0,
    val isCompleted: Boolean = false,
    val finalResult: String? = null,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L
) {
    fun getStatusDisplay(): String {
        return when {
            !isActive && !isCompleted -> "Siap"
            isActive -> "Langkah ${currentStep + 1} (Loss: ${formatAmount(totalLoss)})"
            isCompleted -> when (finalResult) {
                "WIN" -> "MENANG (Pulih: ${formatAmount(totalRecovered)})"
                "LOSS" -> "KALAH (Total: ${formatAmount(totalLoss)})"
                else -> "Selesai"
            }
            else -> "Tidak diketahui"
        }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class CTCOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String,
    val amount: Long,
    val executionTime: Long,
    val sourceCandle: Candle,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val martingaleState: CTCOrderMartingaleState = CTCOrderMartingaleState()
) {
    fun getStatusDisplay(): String {
        return when {
            isSkipped -> "Skipped: ${skipReason ?: "Unknown reason"}"
            isExecuted && martingaleState.isCompleted -> {
                when (martingaleState.finalResult) {
                    "WIN", "MENANG" -> "WIN (${formatAmount(martingaleState.totalRecovered)})"
                    "LOSS", "KALAH" -> "LOSE (${formatAmount(martingaleState.totalLoss)})"
                    else -> "Monitoring..."
                }
            }
            isExecuted && martingaleState.isActive -> "Martingale ${martingaleState.getStatusDisplay()}"
            isExecuted -> "Monitoring..."
            else -> "Pending"
        }
    }

    fun getExecutionTimeFormatted(): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(java.util.Date(executionTime))
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class CTCOrderMartingaleState(
    val isActive: Boolean = false,
    val currentStep: Int = 0,
    val isCompleted: Boolean = false,
    val finalResult: String? = null,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L
) {
    fun getStatusDisplay(): String {
        return when {
            !isActive && !isCompleted -> "Siap"
            isActive -> "Langkah ${currentStep + 1} (Loss: ${formatAmount(totalLoss)})"
            isCompleted -> when (finalResult) {
                "WIN" -> "MENANG (Pulih: ${formatAmount(totalRecovered)})"
                "LOSS" -> "KALAH (Total: ${formatAmount(totalLoss)})"
                else -> "Selesai"
            }
            else -> "Tidak diketahui"
        }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class CTCMartingaleOrder(
    val originalOrderId: String,
    val currentStep: Int,
    val maxSteps: Int,
    val totalLoss: Long,
    val nextAmount: Long,
    val isActive: Boolean
)

data class CTCMartingaleResult(
    val isWin: Boolean,
    val step: Int,
    val amount: Long,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L,
    val message: String,
    val shouldContinue: Boolean = false,
    val isMaxReached: Boolean = false,
    val ctcOrderId: String? = null
)

data class Candle(
    val open: java.math.BigDecimal,
    val close: java.math.BigDecimal,
    val high: java.math.BigDecimal,
    val low: java.math.BigDecimal,
    val createdAt: String
) {
    fun getTrend(): String {
        return if (close > open) "buy" else "sell"
    }

    fun getPriceChange(): java.math.BigDecimal {
        return close - open
    }

    fun isValidCandle(): Boolean {
        return open > java.math.BigDecimal.ZERO &&
                close > java.math.BigDecimal.ZERO &&
                high >= maxOf(open, close) &&
                low <= minOf(open, close)
    }

    fun getFormattedPriceChange(): String {
        val change = getPriceChange()
        val trend = if (change >= java.math.BigDecimal.ZERO) "NAIK" else "TURUN"
        return "$trend (${change.toPlainString()})"
    }

    companion object {
        fun fromApiResponse(response: CandleApiResponse): Candle? {
            return try {
                val last = response.data.lastOrNull() ?: return null
                Candle(
                    open = java.math.BigDecimal(last.open),
                    close = java.math.BigDecimal(last.close),
                    high = java.math.BigDecimal(last.high),
                    low = java.math.BigDecimal(last.low),
                    createdAt = last.created_at
                )
            } catch (e: Exception) {
                println("Error parsing candle data: ${e.message}")
                null
            }
        }
    }
}

data class CandleApiResponse(
    val data: List<CandleData>
)

data class CandleData(
    val open: String,
    val close: String,
    val high: String,
    val low: String,
    val created_at: String
)

data class AssetApiResponse(val data: AssetData)
data class AssetData(val assets: List<AssetRaw>)
data class AssetRaw(
    val ric: String,
    val name: String,
    val type: Int,
    val personal_user_payment_rates: List<PaymentRate>? = null,
    val trading_tools_settings: TradingToolsSettings? = null
)
data class PaymentRate(val trading_type: String, val payment_rate: Double)
data class TradingToolsSettings(
    val ftt: FttSettings? = null,
    val bo: BoSettings? = null,
    val payment_rate_turbo: Double? = null
)
data class FttSettings(val user_statuses: UserStatuses? = null)
data class UserStatuses(val vip: VipSettings? = null)
data class VipSettings(val payment_rate_turbo: Double? = null)
data class BoSettings(val payment_rate_turbo: Double? = null)

data class WebSocketMessage(
    val topic: String,
    val event: String,
    val payload: Map<String, Any>,
    val ref: Int
)

fun parseUserInputToServerAmount(input: String): Long? {
    return try {
        val cleaned = input
            .replace(Regex("[.,\\s]"), "")
            .trim()
            .uppercase()

        if (cleaned.isEmpty()) return null

        val multiplier = 100L

        val result = when {
            cleaned.endsWith("M") -> {
                val value = cleaned.dropLast(1).toDoubleOrNull() ?: return null
                if (value <= 0) return null
                value * 1_000_000 * multiplier
            }
            cleaned.endsWith("JT") -> {
                val value = cleaned.dropLast(2).toDoubleOrNull() ?: return null
                if (value <= 0) return null
                value * 1_000_000 * multiplier
            }
            cleaned.endsWith("RB") -> {
                val value = cleaned.dropLast(2).toDoubleOrNull() ?: return null
                if (value <= 0) return null
                value * 1_000 * multiplier
            }
            cleaned.endsWith("K") -> {
                val value = cleaned.dropLast(1).toDoubleOrNull() ?: return null
                if (value <= 0) return null
                value * 1_000 * multiplier
            }
            else -> {
                val value = cleaned.toDoubleOrNull() ?: return null
                if (value <= 0) return null
                value * multiplier
            }
        }

        if (result > Long.MAX_VALUE || result.isInfinite() || result.isNaN()) {
            return null
        }

        result.toLong()
    } catch (e: Exception) {
        null
    }
}

fun formatIndonesianCurrency(amount: Long): String {
    return try {
        val symbols = java.text.DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

        val formatter = java.text.DecimalFormat("#,##0.00", symbols)
        "Rp ${formatter.format(amount / 100.0)}"
    } catch (e: Exception) {
        "Rp ${String.format("%,d", amount / 100)}"
    }
}

fun formatIndonesianCompact(amount: Long): String {
    return try {
        when {
            amount >= 1_000_000_000_000L -> "${String.format("%.1f", amount / 1_000_000_000_000.0)}T"
            amount >= 1_000_000_000L -> "${String.format("%.1f", amount / 1_000_000_000.0)}B"
            amount >= 1_000_000L -> "${String.format("%.1f", amount / 1_000_000.0)}Jt"
            amount >= 1_000L -> "${String.format("%.1f", amount / 1_000.0)}Rb"
            else -> amount.toString()
        }
    } catch (e: Exception) {
        amount.toString()
    }
}

fun isValidTradingAmount(amount: Long): Boolean {
    return try {
        amount >= 1_400_000L && amount <= 100_000_000_000L && amount > 0
    } catch (e: Exception) {
        false
    }
}

fun getAmountValidationMessage(amount: Long?): String? {
    return when {
        amount == null -> "Invalid amount format"
        amount <= 0 -> "Amount must be greater than 0"
        amount < 1_400_000L -> "Minimum amount is Rp 14.000,00"
        amount > 100_000_000_000L -> "Maximum amount is Rp 1.000.000.000,00"
        else -> null
    }
}

fun validateMultiplier(type: MultiplierType, value: Double): String? {
    return try {
        when (type) {
            MultiplierType.FIXED -> when {
                value <= 1.0 -> "Fixed multiplier must be greater than 1.0"
                value > 5.0 -> "Fixed multiplier too high (max 5.0x)"
                value.isNaN() || value.isInfinite() -> "Invalid multiplier value"
                else -> null
            }
            MultiplierType.PERCENTAGE -> when {
                value <= 0.0 -> "Percentage multiplier must be greater than 0%"
                value > 1000.0 -> "Percentage too high (max 1000%)"
                value.isNaN() || value.isInfinite() -> "Invalid percentage value"
                else -> null
            }
        }
    } catch (e: Exception) {
        "Multiplier validation error: ${e.message}"
    }
}

data class MultiMomentumOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String,
    val amount: Long,
    val executionTime: Long,
    val momentumType: String,
    val confidence: Double,
    val sourceCandle: Candle,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val martingaleState: MultiMomentumOrderMartingaleState = MultiMomentumOrderMartingaleState()
) {
    fun getStatusDisplay(): String {
        return when {
            isSkipped -> "Skipped: ${skipReason ?: "Unknown reason"}"
            isExecuted && martingaleState.isCompleted -> {
                when (martingaleState.finalResult) {
                    "WIN", "MENANG" -> "WIN (${formatAmount(martingaleState.totalRecovered)})"
                    "LOSS", "KALAH" -> "LOSE (${formatAmount(martingaleState.totalLoss)})"
                    else -> "Monitoring..."
                }
            }
            isExecuted && martingaleState.isActive -> "Martingale ${martingaleState.getStatusDisplay()}"
            isExecuted -> "Monitoring..."
            else -> "Pending"
        }
    }

    fun getExecutionTimeFormatted(): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(java.util.Date(executionTime))
    }

    fun getMomentumDisplayName(): String {
        return when (momentumType) {
            "CANDLE_SABIT" -> "Candle Sabit"
            "DOJI_TERJEPIT" -> "Doji Terjepit"
            "DOJI_PEMBATALAN" -> "Doji Pembatalan"
            "BB_SAR_BREAK" -> "BB/SAR Break"
            else -> momentumType
        }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class MultiMomentumOrderMartingaleState(
    val isActive: Boolean = false,
    val currentStep: Int = 0,
    val isCompleted: Boolean = false,
    val finalResult: String? = null,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L
) {
    fun getStatusDisplay(): String {
        return when {
            !isActive && !isCompleted -> "Siap"
            isActive -> "Langkah ${currentStep + 1} (Loss: ${formatAmount(totalLoss)})"
            isCompleted -> when (finalResult) {
                "WIN" -> "MENANG (Pulih: ${formatAmount(totalRecovered)})"
                "LOSS" -> "KALAH (Total: ${formatAmount(totalLoss)})"
                else -> "Selesai"
            }
            else -> "Tidak diketahui"
        }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}