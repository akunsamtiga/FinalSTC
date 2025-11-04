package com.autotrade.finalstc.data.model

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class AdminUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "admin", // "super_admin" or "admin"

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val lastLogin: Long = 0L
)