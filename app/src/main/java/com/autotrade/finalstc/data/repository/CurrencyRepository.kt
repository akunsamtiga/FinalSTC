package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.api.CurrencyApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.CurrencyData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val currencyApiService: CurrencyApiService,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "CurrencyRepository"
    }

    suspend fun fetchUserCurrency(): Result<CurrencyData> {
        return try {
            val userSession = sessionManager.getUserSession()
                ?: return Result.failure(Exception("User session not found"))

            Log.d(TAG, "Fetching currency for user: ${userSession.email}")

            val response = currencyApiService.getCurrencies(
                deviceId = userSession.deviceId,
                authorizationToken = userSession.authtoken,
                userAgent = userSession.userAgent
            )

            if (!response.isSuccessful || response.body()?.data == null) {
                val errorMsg = "Failed to fetch currency: ${response.message()} (${response.code()})"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val currencyData = response.body()!!.data!!
            Log.d(TAG, "Currency fetched successfully")
            Log.d(TAG, "Current currency: ${currencyData.current}")
            Log.d(TAG, "Default currency: ${currencyData.default}")

            // Cari ISO dari currency yang sedang aktif (current)
            val currentCurrency = currencyData.list.find { it.iso == currencyData.current }
            val currencyIso = currentCurrency?.iso ?: currencyData.default

            Log.d(TAG, "Currency ISO found: $currencyIso")

            // Simpan currency dan ISO
            sessionManager.saveCurrencyWithIso(currencyData.current, currencyIso)
            Log.d(TAG, "Currency saved to session: ${currencyData.current}")
            Log.d(TAG, "Currency ISO saved to session: $currencyIso")

            Result.success(currencyData)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching currency: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getSavedCurrency(): String {
        val currency = sessionManager.getCurrency()
        Log.d(TAG, "Retrieved saved currency: $currency")
        return currency
    }

    fun getSavedCurrencyIso(): String {
        val iso = sessionManager.getCurrencyIso()
        Log.d(TAG, "Retrieved saved currency ISO: $iso")
        return iso
    }

    fun updateCurrency(currency: String) {
        Log.d(TAG, "Updating currency to: $currency")
        sessionManager.saveCurrency(currency)
    }

    fun updateCurrencyIso(iso: String) {
        Log.d(TAG, "Updating currency ISO to: $iso")
        sessionManager.saveCurrencyIso(iso)
    }

    fun updateCurrencyWithIso(currency: String, iso: String) {
        Log.d(TAG, "Updating currency to: $currency with ISO: $iso")
        sessionManager.saveCurrencyWithIso(currency, iso)
    }

    suspend fun getCurrencyWithFetch(): String {
        return try {
            val savedCurrency = getSavedCurrency()

            if (savedCurrency != "IDR") {
                return savedCurrency
            }

            Log.d(TAG, "Attempting to fetch currency from API")
            val result = fetchUserCurrency()

            if (result.isSuccess) {
                result.getOrNull()?.current ?: savedCurrency
            } else {
                Log.w(TAG, "Failed to fetch currency, using saved: $savedCurrency")
                savedCurrency
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrencyWithFetch: ${e.message}", e)
            getSavedCurrency()
        }
    }

    suspend fun getCurrencyIsoWithFetch(): String {
        return try {
            val savedIso = getSavedCurrencyIso()

            if (savedIso != "IDR") {
                return savedIso
            }

            Log.d(TAG, "Attempting to fetch currency ISO from API")
            val result = fetchUserCurrency()

            if (result.isSuccess) {
                val currencyData = result.getOrNull()
                val currentCurrency = currencyData?.list?.find { it.iso == currencyData.current }
                currentCurrency?.iso ?: savedIso
            } else {
                Log.w(TAG, "Failed to fetch currency ISO, using saved: $savedIso")
                savedIso
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrencyIsoWithFetch: ${e.message}", e)
            getSavedCurrencyIso()
        }
    }

    // Method untuk mendapatkan semua currency yang tersedia
    suspend fun getAvailableCurrencies(): List<String> {
        return try {
            val userSession = sessionManager.getUserSession()
                ?: return emptyList()

            val response = currencyApiService.getCurrencies(
                deviceId = userSession.deviceId,
                authorizationToken = userSession.authtoken,
                userAgent = userSession.userAgent
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val isoList = response.body()!!.data!!.list.map { it.iso }
                Log.d(TAG, "Available currencies: $isoList")
                isoList
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available currencies: ${e.message}", e)
            emptyList()
        }
    }
}
