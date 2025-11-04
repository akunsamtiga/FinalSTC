package com.autotrade.finalstc.data.api

import com.autotrade.finalstc.data.model.CurrencyResponse
import retrofit2.Response
import retrofit2.http.*

interface CurrencyApiService {
    @GET("platform/private/v2/currencies")
    suspend fun getCurrencies(
        @Query("locale") locale: String = "id",
        @Header("device-id") deviceId: String,
        @Header("device-type") deviceType: String = "web",
        @Header("user-timezone") userTimezone: String = "Asia/Bangkok",
        @Header("authorization-token") authorizationToken: String,
        @Header("User-Agent") userAgent: String,
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Origin") origin: String = "https://stockity.id",
        @Header("Referer") referer: String = "https://stockity.id/",
        @Header("cache-control") cacheControl: String = "no-cache, no-store, must-revalidate"
    ): Response<CurrencyResponse>
}