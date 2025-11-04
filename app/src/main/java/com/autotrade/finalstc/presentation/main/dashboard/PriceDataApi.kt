package com.autotrade.finalstc.presentation.main.dashboard

import retrofit2.Response
import retrofit2.http.*

interface PriceDataApi {
    @GET("candles/v1/{symbol}/{date}/5")
    suspend fun getLastCandle(
        @Path(value = "symbol", encoded = true) symbol: String,
        @Path("date") date: String,
        @Query("locale") locale: String = "id",
        @Header("authorization-token") authToken: String,
        @Header("device-id") deviceId: String,
        @Header("device-type") deviceType: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Accept-Language") lang: String = "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
        @Header("Origin") origin: String = "https://stockity.id",
        @Header("Referer") referer: String = "https://stockity.id/",
        @Header("Cache-Control") cacheControl: String = "no-cache, no-store, must-revalidate",
        @Header("Content-Type") contentType: String = "application/json",
        @Header("user-timezone") userTimezone: String = "Asia/Bangkok"
    ): Response<CandleApiResponse>
}
