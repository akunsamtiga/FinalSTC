package com.autotrade.finalstc.presentation.main.dashboard

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale

enum class IndicatorType {
    SMA,
    EMA,
    RSI
}

data class IndicatorSettings(
    val type: IndicatorType = IndicatorType.SMA,
    val period: Int = getDefaultPeriod(IndicatorType.SMA),
    val rsiOverbought: BigDecimal = BigDecimal("70.0"),
    val rsiOversold: BigDecimal = BigDecimal("30.0"),
    val isEnabled: Boolean = true,
    val sensitivity: BigDecimal = BigDecimal("0.5")
) {
    companion object {
        private val MATH_CONTEXT = MathContext(10, RoundingMode.HALF_UP)

        val SENSITIVITY_MIN = BigDecimal("0.001")
        val SENSITIVITY_MAX = BigDecimal("100.0")
        val SENSITIVITY_LOW = BigDecimal("0.1")
        val SENSITIVITY_MEDIUM = BigDecimal("1")
        val SENSITIVITY_HIGH = BigDecimal("5")
        val SENSITIVITY_VERY_HIGH = BigDecimal("10")
        val SENSITIVITY_MAX_LEVEL = BigDecimal("100.0")

        val RSI_DEFAULT_OVERBOUGHT = BigDecimal("70.0")
        val RSI_DEFAULT_OVERSOLD = BigDecimal("30.0")
        val RSI_MIN_OVERBOUGHT = BigDecimal("50.0")
        val RSI_MAX_OVERBOUGHT = BigDecimal("95.0")
        val RSI_MIN_OVERSOLD = BigDecimal("5.0")
        val RSI_MAX_OVERSOLD = BigDecimal("50.0")

        fun getDefaultPeriod(type: IndicatorType): Int = when (type) {
            IndicatorType.SMA -> 14
            IndicatorType.EMA -> 9
            IndicatorType.RSI -> 14
        }

        fun getDefaultSettings(type: IndicatorType): IndicatorSettings = when (type) {
            IndicatorType.SMA -> IndicatorSettings(
                type = IndicatorType.SMA,
                period = 14,
                sensitivity = SENSITIVITY_MEDIUM
            )
            IndicatorType.EMA -> IndicatorSettings(
                type = IndicatorType.EMA,
                period = 9,
                sensitivity = SENSITIVITY_MEDIUM
            )
            IndicatorType.RSI -> IndicatorSettings(
                type = IndicatorType.RSI,
                period = 14,
                rsiOverbought = RSI_DEFAULT_OVERBOUGHT,
                rsiOversold = RSI_DEFAULT_OVERSOLD,
                sensitivity = SENSITIVITY_MEDIUM
            )
        }
    }

    fun validate(): Result<Unit> {
        return try {
            when {
                !isEnabled -> Result.success(Unit)
                period < 2 -> Result.failure(IllegalArgumentException("Period minimal adalah 2"))
                period > 200 -> Result.failure(IllegalArgumentException("Period maksimal adalah 200"))
                type == IndicatorType.RSI && rsiOverbought <= rsiOversold ->
                    Result.failure(IllegalArgumentException("RSI Overbought harus lebih besar dari Oversold"))
                type == IndicatorType.RSI && (rsiOverbought <= RSI_MIN_OVERBOUGHT || rsiOverbought >= RSI_MAX_OVERBOUGHT) ->
                    Result.failure(IllegalArgumentException("RSI Overbought harus antara ${RSI_MIN_OVERBOUGHT.toPlainString()}-${RSI_MAX_OVERBOUGHT.toPlainString()}"))
                type == IndicatorType.RSI && (rsiOversold <= RSI_MIN_OVERSOLD || rsiOversold >= RSI_MAX_OVERSOLD) ->
                    Result.failure(IllegalArgumentException("RSI Oversold harus antara ${RSI_MIN_OVERSOLD.toPlainString()}-${RSI_MAX_OVERSOLD.toPlainString()}"))
                sensitivity < SENSITIVITY_MIN || sensitivity > SENSITIVITY_MAX ->
                    Result.failure(IllegalArgumentException("Sensitivity harus antara ${SENSITIVITY_MIN.toPlainString()}-${SENSITIVITY_MAX.toPlainString()}"))
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getParameterString(): String = when (type) {
        IndicatorType.SMA -> "Period: $period"
        IndicatorType.EMA -> "Period: $period"
        IndicatorType.RSI -> "Period: $period, OB: ${rsiOverbought.toPlainString()}, OS: ${rsiOversold.toPlainString()}"
    }

    fun getDisplayName(): String = when (type) {
        IndicatorType.SMA -> "SMA ($period)"
        IndicatorType.EMA -> "EMA ($period)"
        IndicatorType.RSI -> "RSI ($period)"
    }

    fun getDescription(): String = when (type) {
        IndicatorType.SMA -> "Simple Moving Average - rata-rata harga $period periode terakhir"
        IndicatorType.EMA -> "Exponential Moving Average - rata-rata berbobot dengan penekanan pada data terbaru"
        IndicatorType.RSI -> "Relative Strength Index - mengukur momentum dengan level ${rsiOversold.toPlainString()}/${rsiOverbought.toPlainString()}"
    }

    fun getSensitivityDisplayText(): String = when {
        sensitivity == SENSITIVITY_LOW -> "Low (${sensitivity.toPlainString()})"
        sensitivity == SENSITIVITY_MEDIUM -> "Medium (${sensitivity.toPlainString()})"
        sensitivity == SENSITIVITY_HIGH -> "High (${sensitivity.toPlainString()})"
        sensitivity == SENSITIVITY_VERY_HIGH -> "Very High (${sensitivity.toPlainString()})"
        sensitivity == SENSITIVITY_MAX_LEVEL -> "Maximum (${sensitivity.toPlainString()})"
        else -> sensitivity.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    fun getSensitivityLevel(): String = when {
        sensitivity <= SENSITIVITY_LOW -> "LOW"
        sensitivity <= SENSITIVITY_MEDIUM -> "MEDIUM"
        sensitivity <= SENSITIVITY_HIGH -> "HIGH"
        sensitivity <= SENSITIVITY_VERY_HIGH -> "VERY_HIGH"
        else -> "MAXIMUM"
    }
}

data class IndicatorValues(
    val primaryValue: BigDecimal,
    val secondaryValue: BigDecimal? = null,
    val trend: String,
    val strength: String
) {
    fun getFormattedPrimaryValue(): String = primaryValue.setScale(4, RoundingMode.HALF_UP).toPlainString()

    fun getFormattedSecondaryValue(): String = secondaryValue?.setScale(4, RoundingMode.HALF_UP)?.toPlainString() ?: "N/A"

    fun getTrendColor(): String = when (trend.uppercase()) {
        "BULLISH" -> "#4CAF50"
        "BEARISH" -> "#F44336"
        else -> "#FF9800"
    }

    fun getStrengthColor(): String = when (strength.uppercase()) {
        "STRONG" -> "#2196F3"
        "MODERATE" -> "#FF9800"
        else -> "#9E9E9E"
    }
}

data class IndicatorOrder(
    val id: String,
    val assetRic: String,
    val assetName: String,
    val trend: String,
    val amount: Long,
    val executionTime: Long,
    val triggerLevel: BigDecimal,
    val triggerType: String,
    val indicatorType: String,
    val indicatorValue: BigDecimal,
    val isExecuted: Boolean = false,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val martingaleState: IndicatorOrderMartingaleState = IndicatorOrderMartingaleState()
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

    fun getTriggerTypeDisplay(): String = when (triggerType) {
        "SUPPORT" -> "Support Hit"
        "RESISTANCE" -> "Resistance Hit"
        "MARTINGALE_SUPPORT" -> "Martingale Support"
        "MARTINGALE_RESISTANCE" -> "Martingale Resistance"
        else -> triggerType
    }

    fun getTrendDisplay(): String = when (trend.lowercase()) {
        "call" -> "CALL ↗"
        "put" -> "PUT ↘"
        else -> trend.uppercase()
    }

    fun getFormattedTriggerLevel(): String = triggerLevel.setScale(5, RoundingMode.HALF_UP).toPlainString()
    fun getFormattedIndicatorValue(): String = indicatorValue.setScale(4, RoundingMode.HALF_UP).toPlainString()

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }
}

data class IndicatorOrderMartingaleState(
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

data class IndicatorMartingaleOrder(
    val originalOrderId: String,
    val currentStep: Int,
    val maxSteps: Int,
    val totalLoss: Long,
    val nextAmount: Long,
    val isActive: Boolean,
    val indicatorType: String = "",
    val lastTriggerLevel: BigDecimal = BigDecimal.ZERO
)

data class IndicatorMartingaleResult(
    val isWin: Boolean,
    val step: Int,
    val amount: Long,
    val totalLoss: Long = 0L,
    val totalRecovered: Long = 0L,
    val message: String,
    val shouldContinue: Boolean = false,
    val isMaxReached: Boolean = false,
    val indicatorOrderId: String? = null,
    val triggerLevel: BigDecimal = BigDecimal.ZERO,
    val indicatorValue: BigDecimal = BigDecimal.ZERO,
    val indicatorType: String = "",
)

data class ConsecutiveLossSettings(
    val isEnabled: Boolean = false,
    val maxConsecutiveLosses: Int = 5
) {
    fun validate(): Result<Unit> {
        return try {
            when {
                !isEnabled -> Result.success(Unit)
                maxConsecutiveLosses < 1 -> Result.failure(IllegalArgumentException("Max consecutive losses minimal 1"))
                maxConsecutiveLosses > 20 -> Result.failure(IllegalArgumentException("Max consecutive losses maksimal 20"))
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getDisplayText(): String = if (isEnabled) {
        "Aktif - Maksimal $maxConsecutiveLosses kalah berturut"
    } else {
        "Tidak aktif"
    }
}

object IndicatorUtils {
    private val MATH_CONTEXT = MathContext(15, RoundingMode.HALF_UP)
    private val ONE_HUNDRED = BigDecimal("100")
    private val ONE_THOUSAND = BigDecimal("1000")

    fun isSignificantPriceChange(
        oldPrice: BigDecimal,
        newPrice: BigDecimal,
        sensitivity: BigDecimal
    ): Boolean {
        val percentChange = (newPrice - oldPrice).abs().divide(oldPrice, MATH_CONTEXT)
        val threshold = sensitivity.multiply(BigDecimal("0.001"), MATH_CONTEXT)
        return percentChange >= threshold
    }

    fun calculateSignalConfidence(
        indicatorValues: IndicatorValues,
        priceAction: String,
        volumeStrength: String = "MODERATE"
    ): BigDecimal {
        var confidence = BigDecimal("0.5")

        confidence = confidence.add(when (indicatorValues.strength) {
            "STRONG" -> BigDecimal("0.3")
            "MODERATE" -> BigDecimal("0.1")
            else -> BigDecimal.ZERO
        }, MATH_CONTEXT)

        val trendAlignment = when {
            indicatorValues.trend == "BULLISH" && priceAction == "HITTING_SUPPORT" -> BigDecimal("0.2")
            indicatorValues.trend == "BEARISH" && priceAction == "HITTING_RESISTANCE" -> BigDecimal("0.2")
            indicatorValues.trend == "NEUTRAL" -> BigDecimal.ZERO
            else -> BigDecimal("-0.1")
        }
        confidence = confidence.add(trendAlignment, MATH_CONTEXT)

        confidence = confidence.add(when (volumeStrength) {
            "HIGH" -> BigDecimal("0.1")
            "MODERATE" -> BigDecimal.ZERO
            else -> BigDecimal("-0.05")
        }, MATH_CONTEXT)

        return confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE)
    }

    fun formatPercentage(value: BigDecimal): String =
        "${value.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(2, RoundingMode.HALF_UP).toPlainString()}%"

    fun formatCurrency(amount: Long): String {
        return formatIndonesianCurrency(amount)
    }

    fun calculateRiskReward(
        entryPrice: BigDecimal,
        supportLevel: BigDecimal,
        resistanceLevel: BigDecimal,
        trend: String
    ): BigDecimal {
        return when (trend.lowercase()) {
            "call" -> {
                val risk = (entryPrice - supportLevel).abs()
                val reward = (resistanceLevel - entryPrice).abs()
                if (risk > BigDecimal.ZERO) reward.divide(risk, MATH_CONTEXT) else BigDecimal.ZERO
            }
            "put" -> {
                val risk = (resistanceLevel - entryPrice).abs()
                val reward = (entryPrice - supportLevel).abs()
                if (risk > BigDecimal.ZERO) reward.divide(risk, MATH_CONTEXT) else BigDecimal.ZERO
            }
            else -> BigDecimal.ZERO
        }
    }

    fun validateIndicatorParameters(
        type: IndicatorType,
        period: Int,
        rsiOverbought: BigDecimal = IndicatorSettings.RSI_DEFAULT_OVERBOUGHT,
        rsiOversold: BigDecimal = IndicatorSettings.RSI_DEFAULT_OVERSOLD
    ): Result<String> {
        return try {
            when (type) {
                IndicatorType.SMA, IndicatorType.EMA -> {
                    when {
                        period < 2 -> Result.failure(Exception("Period minimal 2"))
                        period > 200 -> Result.failure(Exception("Period maksimal 200"))
                        else -> Result.success("Parameters valid")
                    }
                }
                IndicatorType.RSI -> {
                    when {
                        period < 2 -> Result.failure(Exception("RSI period minimal 2"))
                        period > 50 -> Result.failure(Exception("RSI period maksimal 50"))
                        rsiOverbought <= rsiOversold -> Result.failure(Exception("Overbought harus > Oversold"))
                        rsiOverbought < IndicatorSettings.RSI_MIN_OVERBOUGHT || rsiOverbought > IndicatorSettings.RSI_MAX_OVERBOUGHT ->
                            Result.failure(Exception("Overbought harus ${IndicatorSettings.RSI_MIN_OVERBOUGHT.toPlainString()}-${IndicatorSettings.RSI_MAX_OVERBOUGHT.toPlainString()}"))
                        rsiOversold < IndicatorSettings.RSI_MIN_OVERSOLD || rsiOversold > IndicatorSettings.RSI_MAX_OVERSOLD ->
                            Result.failure(Exception("Oversold harus ${IndicatorSettings.RSI_MIN_OVERSOLD.toPlainString()}-${IndicatorSettings.RSI_MAX_OVERSOLD.toPlainString()}"))
                        else -> Result.success("RSI parameters valid")
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Validation error: ${e.message}"))
        }
    }

    fun getSensitivityPresets(): List<Pair<BigDecimal, String>> {
        return listOf(
            IndicatorSettings.SENSITIVITY_LOW to "Low",
            IndicatorSettings.SENSITIVITY_MEDIUM to "Medium",
            IndicatorSettings.SENSITIVITY_HIGH to "High",
            IndicatorSettings.SENSITIVITY_VERY_HIGH to "V.High",
            IndicatorSettings.SENSITIVITY_MAX_LEVEL to "Max"
        )
    }
}