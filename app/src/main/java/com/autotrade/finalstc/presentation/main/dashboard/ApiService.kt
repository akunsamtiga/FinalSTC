package com.autotrade.finalstc.presentation.main.dashboard

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface StockityApi {
    @GET("bo-assets/v6/assets?locale=id")
    suspend fun getAssets(
        @Header("authorization-token") authToken: String,
        @Header("device-type") deviceType: String,
        @Header("device-id") deviceId: String,
        @Header("user-timezone") timezone: String,
        @Header("origin") origin: String,
        @Header("referer") referer: String,
        @Header("accept") accept: String
    ): Response<AssetApiResponse>
}