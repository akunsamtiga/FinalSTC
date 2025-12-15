package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.api.BalanceApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.BalanceInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepository @Inject constructor(
    private val balanceApiService: BalanceApiService,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "BalanceRepository"
    }

    suspend fun fetchBalance(): Result<BalanceInfo> {
        return try {
            val userSession = sessionManager.getUserSession()
                ?: return Result.failure(Exception("No user session"))

            Log.d(TAG, "Fetching balance...")

            val response = balanceApiService.getBalance(
                deviceId = userSession.deviceId,
                deviceType = userSession.deviceType,
                userTimezone = userSession.userTimezone,
                authorizationToken = userSession.authtoken,
                userAgent = userSession.userAgent
            )

            if (response.isSuccessful && response.body() != null) {
                val balanceResponse = response.body()!!

                val realBalance = balanceResponse.data
                    .find { it.accountType == "real" }
                    ?.balance ?: 0L

                val demoBalance = balanceResponse.data
                    .find { it.accountType == "demo" }
                    ?.balance ?: 0L

                val currency = balanceResponse.data
                    .firstOrNull()?.currency ?: "IDR"

                val balanceInfo = BalanceInfo(
                    realBalance = realBalance,
                    demoBalance = demoBalance,
                    currency = currency,
                    lastUpdate = System.currentTimeMillis()
                )

                Log.d(TAG, "Balance fetched successfully:")
                Log.d(TAG, "  Real: ${realBalance / 100.0} $currency")
                Log.d(TAG, "  Demo: ${demoBalance / 100.0} $currency")

                Result.success(balanceInfo)
            } else {
                val error = "Failed to fetch balance: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching balance: ${e.message}", e)
            Result.failure(e)
        }
    }
}