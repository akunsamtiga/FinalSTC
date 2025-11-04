package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

interface ServerTimeApi {
    @GET("api/v1/server_time")
    suspend fun getServerTime(
        @Header("authorization-token") authToken: String,
        @Header("device-type") deviceType: String,
        @Header("device-id") deviceId: String,
        @Header("user-timezone") timezone: String,
        @Header("origin") origin: String,
        @Header("referer") referer: String,
        @Header("accept") accept: String
    ): Response<ServerTimeResponse>
}

data class ServerTimeResponse(
    val timestamp: Long,
    val server_time: Long?,
    val timezone: String?
)

class ServerTimeService(
    private val scope: CoroutineScope,
    private val onServerTimeSync: (Long) -> Unit
) {
    companion object {
        private const val TAG = "ServerTimeService"
        var cachedServerTimeOffset: Long = 0L

        private val dateParser = SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val serverTimeApi = retrofit.create(ServerTimeApi::class.java)

    fun synchronizeServerTime(authToken: String, deviceType: String, deviceId: String) {
        scope.launch {
            try {
                val localTimeBeforeRequest = System.currentTimeMillis()

                val response = serverTimeApi.getServerTime(
                    authToken = authToken,
                    deviceType = deviceType,
                    deviceId = deviceId,
                    timezone = "Asia/Jakarta",
                    origin = "https://stockity.id",
                    referer = "https://stockity.id",
                    accept = "application/json, text/plain, */*"
                )

                val localTimeAfterRequest = System.currentTimeMillis()
                val networkLatency = (localTimeAfterRequest - localTimeBeforeRequest) / 2

                if (response.isSuccessful && response.body() != null) {
                    val serverTimeResponse = response.body()!!
                    val serverTimestamp = serverTimeResponse.server_time
                        ?: serverTimeResponse.timestamp
                        ?: System.currentTimeMillis()

                    updateOffset(serverTimestamp, localTimeAfterRequest, networkLatency)

                } else {
                    val dateHeader = response.headers()["Date"]
                    if (!dateHeader.isNullOrBlank()) {
                        try {
                            val serverTime = dateParser.parse(dateHeader)?.time ?: System.currentTimeMillis()
                            updateOffset(serverTime, localTimeAfterRequest, networkLatency)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing header Date: ${e.message}")
                            fallbackToMinimalOffset()
                        }
                    } else {
                        Log.w(TAG, "Header Date tidak ditemukan pada respons server_time")
                        fallbackToMinimalOffset()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Gagal sinkronisasi waktu server: ${e.message}", e)
                fallbackToMinimalOffset()
            }
        }
    }

    private fun updateOffset(serverTimestamp: Long, localTimeAfterRequest: Long, networkLatency: Long) {
        val adjustedLocalTime = localTimeAfterRequest - networkLatency
        val serverTimeOffset = serverTimestamp - adjustedLocalTime

        if (abs(serverTimeOffset - cachedServerTimeOffset) > 5000) {
            Log.w(TAG, "Offset waktu server berubah signifikan: $serverTimeOffset ms")
        }

        cachedServerTimeOffset = serverTimeOffset
        Log.i(TAG, "Offset waktu server berhasil disinkronkan: $serverTimeOffset ms")
        onServerTimeSync(serverTimeOffset)
    }

    private fun fallbackToMinimalOffset() {
        Log.w(TAG, "Gagal sinkronisasi waktu server, menggunakan offset 0")
        cachedServerTimeOffset = 0L
        onServerTimeSync(0L)
    }

    fun syncUsingAssetEndpoint(authToken: String, deviceType: String, deviceId: String) {
        scope.launch {
            try {
                val localTimeBeforeRequest = System.currentTimeMillis()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.stockity.id/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val stockityApi = retrofit.create(StockityApi::class.java)

                val response = stockityApi.getAssets(
                    authToken = authToken,
                    deviceType = deviceType,
                    deviceId = deviceId,
                    timezone = "Asia/Jakarta",
                    origin = "https://stockity.id",
                    referer = "https://stockity.id",
                    accept = "application/json, text/plain, */*"
                )

                val localTimeAfterRequest = System.currentTimeMillis()
                val networkLatency = (localTimeAfterRequest - localTimeBeforeRequest) / 2

                if (response.isSuccessful) {
                    val dateHeader = response.headers()["Date"]
                    if (!dateHeader.isNullOrBlank()) {
                        try {
                            val serverTime = dateParser.parse(dateHeader)?.time ?: System.currentTimeMillis()
                            updateOffset(serverTime, localTimeAfterRequest, networkLatency)
                            return@launch
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing header Date: ${e.message}")
                        }
                    }
                }

                fallbackToMinimalOffset()

            } catch (e: Exception) {
                Log.e(TAG, "Gagal sinkronisasi asset endpoint: ${e.message}", e)
                fallbackToMinimalOffset()
            }
        }
    }

    fun getCurrentServerTimeMillis(): Long {
        return System.currentTimeMillis() + cachedServerTimeOffset
    }
}