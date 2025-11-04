package com.autotrade.finalstc.data.model

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

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