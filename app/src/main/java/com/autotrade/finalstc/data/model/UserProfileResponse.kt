package com.autotrade.finalstc.data.model

import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    val data: UserProfileData?
)

data class UserProfileData(
    val id: Long,
    val email: String,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    val nickname: String?,
    val phone: String?,
    @SerializedName("email_verified")
    val emailVerified: Boolean,
    @SerializedName("phone_verified")
    val phoneVerified: Boolean,
    val gender: String?,
    val country: String?,
    val birthday: String?,
    @SerializedName("registered_at")
    val registeredAt: String?,
    @SerializedName("registration_country_iso")
    val registrationCountryIso: String?
) {
    // Helper function untuk mendapatkan full name
    fun getFullName(): String {
        val first = firstName?.trim() ?: ""
        val last = lastName?.trim() ?: ""

        return when {
            first.isNotEmpty() && last.isNotEmpty() -> "$first $last"
            first.isNotEmpty() -> first
            last.isNotEmpty() -> last
            nickname?.isNotEmpty() == true -> nickname
            email.isNotEmpty() -> email.substringBefore("@")
            else -> "User $id"
        }
    }
}