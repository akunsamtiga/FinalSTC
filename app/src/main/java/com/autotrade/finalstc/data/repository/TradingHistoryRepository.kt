package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.presentation.main.history.TradingHistoryApi
import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import com.autotrade.finalstc.presentation.main.history.TradingHistoryRaw
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradingHistoryRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "TradingHistoryRepository"
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val historyApi = retrofit.create(TradingHistoryApi::class.java)

    suspend fun getTradingHistory(isDemoAccount: Boolean): List<TradingHistoryNew> {
        return try {
            Log.d(TAG, "Mengambil riwayat trading")
            Log.d(TAG, "Jenis akun: ${if (isDemoAccount) "Demo" else "Real"}")

            val userSession = sessionManager.getUserSession()
            if (userSession?.authtoken == null) {
                Log.e(TAG, "Token auth tidak tersedia")
                return emptyList()
            }

            Log.d(TAG, "Membuat permintaan API")
            val response = historyApi.getTradingHistoryRaw(
                type = if (isDemoAccount) "demo" else "real",
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType ?: "web",
                deviceId = userSession.deviceId ?: "",
                timezone = "Asia/Jakarta",
                origin = "https://stockity.id",
                referer = "https://stockity.id",
                accept = "application/json, text/plain, */*"
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Respons API berhasil")
                val responseBody = response.body()!!
                val rawHistory = parseHistoryResponse(responseBody)
                val processedHistory = processHistoryData(rawHistory, isDemoAccount)

                Log.d(TAG, "Berhasil memproses ${processedHistory.size} riwayat")
                return processedHistory
            } else {
                Log.e(TAG, "Respons API gagal: ${response.code()} - ${response.message()}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil riwayat: ${e.message}", e)
            return emptyList()
        }
    }

    private fun parseHistoryResponse(responseBody: Any): List<TradingHistoryRaw> {
        return try {
            when (responseBody) {
                is List<*> -> parseHistoryArray(responseBody)
                is Map<*, *> -> {
                    when {
                        responseBody.containsKey("data") -> {
                            val data = responseBody["data"]
                            when (data) {
                                is Map<*, *> -> {
                                    when {
                                        data.containsKey("standard_trade_deals") -> {
                                            val deals = data["standard_trade_deals"]
                                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                                        }
                                        data.containsKey("deals") -> {
                                            val deals = data["deals"]
                                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                                        }
                                        else -> emptyList()
                                    }
                                }
                                is List<*> -> parseHistoryArray(data)
                                else -> emptyList()
                            }
                        }
                        responseBody.containsKey("standard_trade_deals") -> {
                            val deals = responseBody["standard_trade_deals"]
                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                        }
                        responseBody.containsKey("deals") -> {
                            val deals = responseBody["deals"]
                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                        }
                        responseBody.containsKey("id") && responseBody.containsKey("status") -> {
                            listOf(parseSingleHistoryItem(responseBody))
                        }
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing respons riwayat: ${e.message}")
            emptyList()
        }
    }

    private fun parseHistoryArray(array: List<*>): List<TradingHistoryRaw> {
        return array.mapNotNull { item ->
            try {
                if (item is Map<*, *>) {
                    parseSingleHistoryItem(item)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing item riwayat: ${e.message}")
                null
            }
        }
    }

    private fun parseSingleHistoryItem(item: Map<*, *>): TradingHistoryRaw {
        return TradingHistoryRaw(
            id = (item["id"] as? Number)?.toLong() ?: 0L,
            status = item["status"] as? String ?: "",
            amount = (item["amount"] as? Number)?.toLong() ?: 0L,
            deal_type = item["deal_type"] as? String ?: "",
            created_at = item["created_at"] as? String ?: "",
            uuid = item["uuid"] as? String ?: "",
            win = (item["win"] as? Number)?.toLong(),
            asset_id = (item["asset_id"] as? Number)?.toInt() ?: 0,
            close_rate = (item["close_rate"] as? Number)?.toDouble(),
            requested_by = item["requested_by"] as? String,
            finished_at = item["finished_at"] as? String,
            trend = item["trend"] as? String ?: "",
            payment = (item["payment"] as? Number)?.toLong() ?: 0L,
            payment_rate = (item["payment_rate"] as? Number)?.toInt() ?: 0,
            asset_name = item["asset_name"] as? String ?: "",
            asset_ric = item["asset_ric"] as? String ?: "",
            close_quote_created_at = item["close_quote_created_at"] as? String,
            open_quote_created_at = item["open_quote_created_at"] as? String,
            open_rate = (item["open_rate"] as? Number)?.toDouble() ?: 0.0,
            trade_type = item["trade_type"] as? String ?: ""
        )
    }

    private fun processHistoryData(rawHistory: List<TradingHistoryRaw>, isDemoAccount: Boolean): List<TradingHistoryNew> {
        return rawHistory.map { raw ->
            TradingHistoryNew(
                id = raw.id,
                status = raw.status,
                amount = raw.amount,
                dealType = raw.deal_type,
                createdAt = raw.created_at,
                uuid = raw.uuid,
                win = raw.win ?: 0L,
                assetId = raw.asset_id,
                closeRate = raw.close_rate,
                finishedAt = raw.finished_at,
                trend = raw.trend,
                payment = raw.payment,
                paymentRate = raw.payment_rate,
                assetName = raw.asset_name,
                assetRic = raw.asset_ric,
                closeQuoteCreatedAt = raw.close_quote_created_at,
                openQuoteCreatedAt = raw.open_quote_created_at,
                openRate = raw.open_rate,
                tradeType = raw.trade_type,
                accountType = if (isDemoAccount) "demo" else "real",
                isDemoAccount = isDemoAccount,
                isDemo = isDemoAccount
            )
        }.sortedByDescending { it.createdAt }
    }
}