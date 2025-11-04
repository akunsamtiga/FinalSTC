package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName

data class LoginData(
    val authtoken: String,
    @SerializedName("user_id")
    val userId: String
)