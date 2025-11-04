package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName

data class UserSession(
    val authtoken: String,
    val userId: String,
    val deviceId: String,
    val email: String,
    val userTimezone: String,
    val userAgent: String,
    val deviceType: String,
    val currency: String,
    val currencyIso: String = "IDR"
)