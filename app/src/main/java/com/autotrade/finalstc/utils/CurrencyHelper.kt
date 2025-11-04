package com.autotrade.finalstc.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyHelper {

    fun formatCurrency(amount: Long, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)
        val formatter = when (currencyIso) {
            "IDR" -> NumberFormat.getNumberInstance(Locale("id", "ID"))
            "USD" -> NumberFormat.getNumberInstance(Locale.US)
            "EUR" -> NumberFormat.getNumberInstance(Locale.GERMANY)
            else -> NumberFormat.getNumberInstance(Locale.getDefault())
        }
        return "$unit ${formatter.format(amount)}"
    }

    fun formatCurrency(amount: Double, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)
        val formatter = when (currencyIso) {
            "IDR" -> NumberFormat.getNumberInstance(Locale("id", "ID"))
            "USD" -> NumberFormat.getNumberInstance(Locale.US)
            "EUR" -> NumberFormat.getNumberInstance(Locale.GERMANY)
            else -> NumberFormat.getNumberInstance(Locale.getDefault())
        }
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return "$unit ${formatter.format(amount)}"
    }

    fun getCurrencySymbol(currencyIso: String): String {
        return when (currencyIso.uppercase()) {
            "IDR" -> "Rp"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "CHF" -> "Fr"
            "CNY" -> "¥"
            "SGD" -> "S$"
            "MYR" -> "RM"
            "THB" -> "฿"
            "PHP" -> "₱"
            "VND" -> "₫"
            else -> currencyIso
        }
    }

    fun getCurrencyName(currencyIso: String): String {
        return when (currencyIso.uppercase()) {
            "IDR" -> "Indonesian Rupiah"
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "GBP" -> "British Pound"
            "JPY" -> "Japanese Yen"
            "AUD" -> "Australian Dollar"
            "CAD" -> "Canadian Dollar"
            "CHF" -> "Swiss Franc"
            "CNY" -> "Chinese Yuan"
            "SGD" -> "Singapore Dollar"
            "MYR" -> "Malaysian Ringgit"
            "THB" -> "Thai Baht"
            "PHP" -> "Philippine Peso"
            "VND" -> "Vietnamese Dong"
            else -> currencyIso
        }
    }

    fun formatCompactCurrency(amount: Long, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)

        return when {
            amount >= 1_000_000_000 -> {
                val billions = amount / 1_000_000_000.0
                "$unit %.1fB".format(billions)
            }
            amount >= 1_000_000 -> {
                val millions = amount / 1_000_000.0
                "$unit %.1fM".format(millions)
            }
            amount >= 1_000 -> {
                val thousands = amount / 1_000.0
                "$unit %.1fK".format(thousands)
            }
            else -> formatCurrency(amount, currencyIso)
        }
    }

    fun parseCurrency(formattedAmount: String): Long? {
        return try {
            val cleanAmount = formattedAmount.replace(Regex("[^0-9]"), "")
            cleanAmount.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

fun Long.toCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCurrency(this, currencyIso)
}

fun Double.toCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCurrency(this, currencyIso)
}

fun Long.toCompactCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCompactCurrency(this, currencyIso)
}