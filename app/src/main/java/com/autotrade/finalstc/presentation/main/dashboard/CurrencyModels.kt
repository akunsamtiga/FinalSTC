package com.autotrade.finalstc.presentation.main.dashboard

import java.text.NumberFormat
import java.util.Currency as JavaCurrency
import java.util.Locale

enum class CurrencyType(
    val code: String,
    val symbol: String,
    val flag: String,
    val locale: Locale,
    val minAmountInCents: Long,
    val decimalPlaces: Int
) {
    IDR(
        code = "IDR",
        symbol = "Rp",
        flag = "🇮🇩",
        locale = Locale("id", "ID"),
        minAmountInCents = 1_400_000L,
        decimalPlaces = 0
    ),
    JPY(
        code = "JPY",
        symbol = "¥",
        flag = "🇯🇵",
        locale = Locale.JAPAN,
        minAmountInCents = 15_000L,
        decimalPlaces = 0
    ),
    SGD(
        code = "SGD",
        symbol = "S$",
        flag = "🇸🇬",
        locale = Locale("en", "SG"),
        minAmountInCents = 135L,
        decimalPlaces = 2
    ),
    MYR(
        code = "MYR",
        symbol = "RM",
        flag = "🇲🇾",
        locale = Locale("ms", "MY"),
        minAmountInCents = 465L,
        decimalPlaces = 2
    ),
    THB(
        code = "THB",
        symbol = "฿",
        flag = "🇹🇭",
        locale = Locale("th", "TH"),
        minAmountInCents = 3_500L,
        decimalPlaces = 0
    ),
    PHP(
        code = "PHP",
        symbol = "₱",
        flag = "🇵🇭",
        locale = Locale("en", "PH"),
        minAmountInCents = 5_600L,
        decimalPlaces = 2
    ),
    VND(
        code = "VND",
        symbol = "₫",
        flag = "🇻🇳",
        locale = Locale("vi", "VN"),
        minAmountInCents = 2_450_000L,
        decimalPlaces = 0
    ),
    KRW(
        code = "KRW",
        symbol = "₩",
        flag = "🇰🇷",
        locale = Locale.KOREA,
        minAmountInCents = 133_000L,
        decimalPlaces = 0
    ),
    CNY(
        code = "CNY",
        symbol = "¥",
        flag = "🇨🇳",
        locale = Locale.CHINA,
        minAmountInCents = 725L,
        decimalPlaces = 2
    ),
    HKD(
        code = "HKD",
        symbol = "HK$",
        flag = "🇭🇰",
        locale = Locale("zh", "HK"),
        minAmountInCents = 780L,
        decimalPlaces = 2
    ),
    TWD(
        code = "TWD",
        symbol = "NT$",
        flag = "🇹🇼",
        locale = Locale("zh", "TW"),
        minAmountInCents = 3_150L,
        decimalPlaces = 2
    ),
    INR(
        code = "INR",
        symbol = "₹",
        flag = "🇮🇳",
        locale = Locale("en", "IN"),
        minAmountInCents = 8_300L,
        decimalPlaces = 2
    ),
    PKR(
        code = "PKR",
        symbol = "₨",
        flag = "🇵🇰",
        locale = Locale("en", "PK"),
        minAmountInCents = 28_000L,
        decimalPlaces = 2
    ),
    BDT(
        code = "BDT",
        symbol = "৳",
        flag = "🇧🇩",
        locale = Locale("bn", "BD"),
        minAmountInCents = 11_000L,
        decimalPlaces = 2
    ),
    LKR(
        code = "LKR",
        symbol = "Rs",
        flag = "🇱🇰",
        locale = Locale("si", "LK"),
        minAmountInCents = 32_500L,
        decimalPlaces = 2
    ),
    USD(
        code = "USD",
        symbol = "$",
        flag = "🇺🇸",
        locale = Locale.US,
        minAmountInCents = 100L,
        decimalPlaces = 2
    ),
    EUR(
        code = "EUR",
        symbol = "€",
        flag = "🇪🇺",
        locale = Locale.GERMANY,
        minAmountInCents = 95L,
        decimalPlaces = 2
    ),
    GBP(
        code = "GBP",
        symbol = "£",
        flag = "🇬🇧",
        locale = Locale.UK,
        minAmountInCents = 80L,
        decimalPlaces = 2
    ),
    CHF(
        code = "CHF",
        symbol = "Fr",
        flag = "🇨🇭",
        locale = Locale("de", "CH"),
        minAmountInCents = 90L,
        decimalPlaces = 2
    ),
    AUD(
        code = "AUD",
        symbol = "A$",
        flag = "🇦🇺",
        locale = Locale("en", "AU"),
        minAmountInCents = 150L,
        decimalPlaces = 2
    ),
    NZD(
        code = "NZD",
        symbol = "NZ$",
        flag = "🇳🇿",
        locale = Locale("en", "NZ"),
        minAmountInCents = 165L,
        decimalPlaces = 2
    ),
    CAD(
        code = "CAD",
        symbol = "C$",
        flag = "🇨🇦",
        locale = Locale.CANADA,
        minAmountInCents = 135L,
        decimalPlaces = 2
    ),
    MXN(
        code = "MXN",
        symbol = "Mex$",
        flag = "🇲🇽",
        locale = Locale("es", "MX"),
        minAmountInCents = 1_700L,
        decimalPlaces = 2
    ),
    BRL(
        code = "BRL",
        symbol = "R$",
        flag = "🇧🇷",
        locale = Locale("pt", "BR"),
        minAmountInCents = 500L,
        decimalPlaces = 2
    ),
    ARS(
        code = "ARS",
        symbol = "$",
        flag = "🇦🇷",
        locale = Locale("es", "AR"),
        minAmountInCents = 35_000L,
        decimalPlaces = 2
    ),
    CLP(
        code = "CLP",
        symbol = "$",
        flag = "🇨🇱",
        locale = Locale("es", "CL"),
        minAmountInCents = 90_000L,
        decimalPlaces = 0
    ),
    COP(
        code = "COP",
        symbol = "$",
        flag = "🇨🇴",
        locale = Locale("es", "CO"),
        minAmountInCents = 400_000L,
        decimalPlaces = 0
    ),
    AED(
        code = "AED",
        symbol = "د.إ",
        flag = "🇦🇪",
        locale = Locale("ar", "AE"),
        minAmountInCents = 367L,
        decimalPlaces = 2
    ),
    SAR(
        code = "SAR",
        symbol = "﷼",
        flag = "🇸🇦",
        locale = Locale("ar", "SA"),
        minAmountInCents = 375L,
        decimalPlaces = 2
    ),
    TRY(
        code = "TRY",
        symbol = "₺",
        flag = "🇹🇷",
        locale = Locale("tr", "TR"),
        minAmountInCents = 2_800L,
        decimalPlaces = 2
    ),
    EGP(
        code = "EGP",
        symbol = "£",
        flag = "🇪🇬",
        locale = Locale("ar", "EG"),
        minAmountInCents = 3_100L,
        decimalPlaces = 2
    ),
    ZAR(
        code = "ZAR",
        symbol = "R",
        flag = "🇿🇦",
        locale = Locale("en", "ZA"),
        minAmountInCents = 1_850L,
        decimalPlaces = 2
    ),
    NGN(
        code = "NGN",
        symbol = "₦",
        flag = "🇳🇬",
        locale = Locale("en", "NG"),
        minAmountInCents = 80_000L,
        decimalPlaces = 2
    ),
    RUB(
        code = "RUB",
        symbol = "₽",
        flag = "🇷🇺",
        locale = Locale("ru", "RU"),
        minAmountInCents = 9_200L,
        decimalPlaces = 2
    ),
    PLN(
        code = "PLN",
        symbol = "zł",
        flag = "🇵🇱",
        locale = Locale("pl", "PL"),
        minAmountInCents = 400L,
        decimalPlaces = 2
    ),
    CZK(
        code = "CZK",
        symbol = "Kč",
        flag = "🇨🇿",
        locale = Locale("cs", "CZ"),
        minAmountInCents = 2_300L,
        decimalPlaces = 2
    ),
    HUF(
        code = "HUF",
        symbol = "Ft",
        flag = "🇭🇺",
        locale = Locale("hu", "HU"),
        minAmountInCents = 36_000L,
        decimalPlaces = 0
    ),
    SEK(
        code = "SEK",
        symbol = "kr",
        flag = "🇸🇪",
        locale = Locale("sv", "SE"),
        minAmountInCents = 1_050L,
        decimalPlaces = 2
    ),
    NOK(
        code = "NOK",
        symbol = "kr",
        flag = "🇳🇴",
        locale = Locale("no", "NO"),
        minAmountInCents = 1_080L,
        decimalPlaces = 2
    ),
    DKK(
        code = "DKK",
        symbol = "kr",
        flag = "🇩🇰",
        locale = Locale("da", "DK"),
        minAmountInCents = 700L,
        decimalPlaces = 2
    ),
    KZT(
        code = "KZT",
        symbol = "₸",
        flag = "🇰🇿",
        locale = Locale("kk", "KZ"),
        minAmountInCents = 45_000L,
        decimalPlaces = 2
    );

    fun formatAmount(amountInCents: Long): String {
        val actualAmount = amountInCents.toDouble() / 100.0

        val format = NumberFormat.getCurrencyInstance(locale)
        format.maximumFractionDigits = decimalPlaces
        format.minimumFractionDigits = decimalPlaces

        try {
            format.currency = JavaCurrency.getInstance(code)
        } catch (e: Exception) {
        }

        return format.format(actualAmount)
    }

    fun formatCompact(amountInCents: Long): String {
        val actualAmount = amountInCents / 100.0

        return when {
            actualAmount >= 1_000_000_000 -> "$symbol${String.format("%.1f", actualAmount / 1_000_000_000)}B"
            actualAmount >= 1_000_000 -> "$symbol${String.format("%.1f", actualAmount / 1_000_000)}M"
            actualAmount >= 1_000 -> "$symbol${String.format("%.1f", actualAmount / 1_000)}K"
            else -> {
                if (decimalPlaces == 0) {
                    "$symbol${actualAmount.toLong()}"
                } else {
                    "$symbol${String.format("%.${decimalPlaces}f", actualAmount)}"
                }
            }
        }
    }

    fun parseUserInput(input: String): Long? {
        return try {
            val cleaned = input
                .replace(symbol, "")
                .replace(Regex("[,\\s]"), "")
                .trim()
                .uppercase()

            if (cleaned.isEmpty()) return null

            val multiplier = 100L

            val result = when {
                cleaned.endsWith("B") -> {
                    val value = cleaned.dropLast(1).toDoubleOrNull() ?: return null
                    if (value <= 0) return null
                    value * 1_000_000_000 * multiplier
                }
                cleaned.endsWith("M") -> {
                    val value = cleaned.dropLast(1).toDoubleOrNull() ?: return null
                    if (value <= 0) return null
                    value * 1_000_000 * multiplier
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

    fun isValidAmount(amountInCents: Long): Boolean {
        return amountInCents >= minAmountInCents
    }

    fun getValidationMessage(amountInCents: Long?): String? {
        return when {
            amountInCents == null -> "Invalid amount format"
            amountInCents <= 0 -> "Amount must be greater than 0"
            amountInCents < minAmountInCents -> "Minimum amount is ${formatAmount(minAmountInCents)}"
            else -> null
        }
    }

    fun getRegion(): String {
        return when (this) {
            IDR, JPY, SGD, MYR, THB, PHP, VND, KRW, CNY, HKD, TWD, INR, PKR, BDT, LKR -> "Asia"
            USD, EUR, GBP, CHF -> "Major Currencies"
            AUD, NZD -> "Oceania"
            CAD, MXN, BRL, ARS, CLP, COP -> "Americas"
            AED, SAR, TRY, EGP, ZAR, NGN -> "Middle East & Africa"
            RUB, KZT, PLN, CZK, HUF -> "Eastern Europe"
            SEK, NOK, DKK -> "Scandinavia"
        }
    }

    companion object {
        fun fromCode(code: String): CurrencyType {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: IDR
        }

        fun getCurrenciesByRegion(region: String): List<CurrencyType> {
            return values().filter { it.getRegion() == region }
        }

        fun getAllRegions(): List<String> {
            return values().map { it.getRegion() }.distinct().sorted()
        }
    }
}

data class CurrencySettings(
    val selectedCurrency: CurrencyType = CurrencyType.IDR,
    val baseAmountInCents: Long = CurrencyType.IDR.minAmountInCents
) {
    fun validate(): Result<Unit> {
        return try {
            when {
                !selectedCurrency.isValidAmount(baseAmountInCents) -> {
                    Result.failure(
                        IllegalArgumentException(
                            "Base amount ${selectedCurrency.formatAmount(baseAmountInCents)} is below minimum " +
                                    "${selectedCurrency.formatAmount(selectedCurrency.minAmountInCents)}"
                        )
                    )
                }
                baseAmountInCents > 100_000_000_000L -> {
                    Result.failure(IllegalArgumentException("Base amount too high"))
                }
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Validation error: ${e.message}"))
        }
    }

    fun getFormattedBaseAmount(): String {
        return selectedCurrency.formatAmount(baseAmountInCents)
    }

    fun getFormattedCompactAmount(): String {
        return selectedCurrency.formatCompact(baseAmountInCents)
    }

    fun adjustForCurrency(newCurrency: CurrencyType): CurrencySettings {
        val adjustedAmount = if (baseAmountInCents < newCurrency.minAmountInCents) {
            newCurrency.minAmountInCents
        } else {
            baseAmountInCents
        }

        return copy(
            selectedCurrency = newCurrency,
            baseAmountInCents = adjustedAmount
        )
    }
}

fun MartingaleState.withCurrency(currencySettings: CurrencySettings): MartingaleState {
    return copy(baseAmount = currencySettings.baseAmountInCents)
}

fun TradeOrder.withCurrency(currency: CurrencyType): TradeOrder {
    return copy(iso = currency.code)
}

object CurrencyFormatter {
    fun format(amountInCents: Long, currency: CurrencyType): String {
        return currency.formatAmount(amountInCents)
    }

    fun formatCompact(amountInCents: Long, currency: CurrencyType): String {
        return currency.formatCompact(amountInCents)
    }

    fun parse(input: String, currency: CurrencyType): Long? {
        return currency.parseUserInput(input)
    }

    fun validate(amountInCents: Long?, currency: CurrencyType): String? {
        return currency.getValidationMessage(amountInCents)
    }
}