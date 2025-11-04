package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName

data class CurrencyResponse(
    val success: Boolean,
    val data: CurrencyData?,
    val errors: List<String>?
)

data class CurrencyData(
    val default: String,
    val current: String,
    val list: List<Currency>
)

data class Currency(
    val unit: String,
    val iso: String,
    val summs: Summs?,
    val limits: Limits?,
    val pricing: List<Pricing>?,
    @SerializedName("demo_balance")
    val demoBalance: Long,
    @SerializedName("minimal_deposit")
    val minimalDeposit: Long
)

data class Summs(
    @SerializedName("binary_option")
    val binaryOption: List<Long>?,
    @SerializedName("standard_trade")
    val standardTrade: List<Long>?,
    val cfd: List<Long>?,
    @SerializedName("do")
    val digitalOption: List<Long>?
)

data class Limits(
    @SerializedName("binary_option")
    val binaryOption: LimitRange?,
    @SerializedName("standard_trade")
    val standardTrade: LimitRange?,
    @SerializedName("do")
    val digitalOption: LimitRange?,
    val cfd: LimitRange?
)

data class LimitRange(
    val min: Long,
    val max: Long
)

data class Pricing(
    val type: String,
    val deposit: Long
)