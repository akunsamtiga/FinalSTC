package com.autotrade.finalstc.data.api

import com.autotrade.finalstc.data.model.LoginRequest
import com.autotrade.finalstc.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.*

interface LoginApiService {
    @POST("passport/v2/sign_in")
    suspend fun login(
        @Query("locale") locale: String = "id",
        @Header("device-id") deviceId: String,
        @Header("device-type") deviceType: String = "web",
        @Header("user-timezone") userTimezone: String = "Asia/Bangkok",
        @Header("User-Agent") userAgent: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/json",
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>
}