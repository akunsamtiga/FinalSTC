package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName

data class BalanceResponse(
    val data: List<BalanceData>
)

data class BalanceData(
    val amount: Long,
    val balance: Long,
    val currency: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("balance_version")
    val balanceVersion: Int
)

data class BalanceInfo(
    val realBalance: Long = 0L,
    val demoBalance: Long = 0L,
    val currency: String = "IDR",
    val lastUpdate: Long = 0L
)