package com.autotrade.finalstc.data.model

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class WhitelistUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val userId: String = "",
    val deviceId: String = "",

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0L,

    val addedBy: String = "",
    val addedAt: Long = System.currentTimeMillis()
)