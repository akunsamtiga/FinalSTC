package com.autotrade.finalstc.data.model

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class WhitelistUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val userId: String = "",
    val deviceId: String = "",

    // Firestore mapping untuk field "isActive"
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0L,

    // Field baru untuk tracking admin yang menambahkan user
    val addedBy: String = "",  // Email atau ID admin yang menambahkan user
    val addedAt: Long = System.currentTimeMillis()  // Timestamp kapan user ditambahkan
)