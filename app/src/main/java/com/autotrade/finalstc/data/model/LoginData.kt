package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName
import com.google.firebase.firestore.PropertyName

data class LoginData(
    val authtoken: String,
    @SerializedName("user_id")
    val userId: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val data: LoginData?
)

data class RegistrationConfig(
    val id: String = "registration_config",
    val registrationUrl: String = "https://stockity.id/registered?a=25db72fbbc00",
    val whatsappHelpNumber: String = "6285959860015",
    val isActive: Boolean = true,
    val description: String = "Default registration link",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
)