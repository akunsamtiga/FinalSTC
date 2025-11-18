package com.autotrade.finalstc.data.api

import com.autotrade.finalstc.data.model.BalanceResponse
import retrofit2.Response
import retrofit2.http.*

interface BalanceApiService {
    @GET("bank/v1/read")
    suspend fun getBalance(
        @Query("locale") locale: String = "id",
        @Header("device-id") deviceId: String,
        @Header("device-type") deviceType: String = "web",
        @Header("user-timezone") userTimezone: String = "Asia/Bangkok",
        @Header("authorization-token") authorizationToken: String,
        @Header("User-Agent") userAgent: String,
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Cache-Control") cacheControl: String = "no-cache, no-store, must-revalidate",
        @Header("Origin") origin: String = "https://stockity.id",
        @Header("Referer") referer: String = "https://stockity.id/"
    ): Response<BalanceResponse>
}