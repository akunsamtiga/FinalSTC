package com.autotrade.finalstc.data.api

import com.autotrade.finalstc.data.model.UserProfileResponse
import retrofit2.Response
import retrofit2.http.*

interface UserProfileApiService {
    @GET("passport/v1/user_profile")
    suspend fun getUserProfile(
        @Query("locale") locale: String = "id",
        @Header("device-id") deviceId: String,
        @Header("device-type") deviceType: String = "web",
        @Header("user-timezone") userTimezone: String = "Asia/Bangkok",
        @Header("authorization-token") authorizationToken: String,
        @Header("User-Agent") userAgent: String,
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Origin") origin: String = "https://stockity.id",
        @Header("Referer") referer: String = "https://stockity.id/"
    ): Response<UserProfileResponse>
}