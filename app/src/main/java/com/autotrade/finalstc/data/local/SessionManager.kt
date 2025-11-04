package com.autotrade.finalstc.data.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.autotrade.finalstc.data.model.UserSession
import com.autotrade.finalstc.presentation.main.dashboard.ScheduledOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "trading_session"
        private const val KEY_AUTHTOKEN = "authtoken"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_TIMEZONE = "user_timezone"
        private const val KEY_USER_AGENT = "user_agent"
        private const val KEY_DEVICE_TYPE = "device_type"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_CURRENCY_ISO = "currency_iso"

        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"

        private const val KEY_SCHEDULED_ORDERS = "scheduled_orders"

    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserSession(userSession: UserSession) {
        sharedPreferences.edit().apply {
            putString(KEY_AUTHTOKEN, userSession.authtoken)
            putString(KEY_USER_ID, userSession.userId)
            putString(KEY_DEVICE_ID, userSession.deviceId)
            putString(KEY_EMAIL, userSession.email)
            putString(KEY_USER_TIMEZONE, userSession.userTimezone)
            putString(KEY_USER_AGENT, userSession.userAgent)
            putString(KEY_DEVICE_TYPE, userSession.deviceType)
            putString(KEY_CURRENCY, userSession.currency)
            putString(KEY_CURRENCY_ISO, userSession.currencyIso)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun saveCredentials(email: String, password: String, rememberMe: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_SAVED_EMAIL, email)
                putString(KEY_SAVED_PASSWORD, password)
            } else {
                remove(KEY_SAVED_EMAIL)
                remove(KEY_SAVED_PASSWORD)
            }
            apply()
        }
    }

    fun getSavedCredentials(): Triple<String, String, Boolean> {
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
        val savedEmail = if (rememberMe) sharedPreferences.getString(KEY_SAVED_EMAIL, "") ?: "" else ""
        val savedPassword = if (rememberMe) sharedPreferences.getString(KEY_SAVED_PASSWORD, "") ?: "" else ""
        return Triple(savedEmail, savedPassword, rememberMe)
    }

    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clearSavedCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_REMEMBER_ME)
            remove(KEY_SAVED_EMAIL)
            remove(KEY_SAVED_PASSWORD)
            apply()
        }
    }

    fun getUserSession(): UserSession? {
        return if (isLoggedIn()) {
            UserSession(
                authtoken = sharedPreferences.getString(KEY_AUTHTOKEN, "") ?: "",
                userId = sharedPreferences.getString(KEY_USER_ID, "") ?: "",
                deviceId = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: "",
                email = sharedPreferences.getString(KEY_EMAIL, "") ?: "",
                userTimezone = sharedPreferences.getString(KEY_USER_TIMEZONE, "") ?: "",
                userAgent = sharedPreferences.getString(KEY_USER_AGENT, "") ?: "",
                deviceType = sharedPreferences.getString(KEY_DEVICE_TYPE, "") ?: "",
                currency = sharedPreferences.getString(KEY_CURRENCY, null) ?: "",
                currencyIso = sharedPreferences.getString(KEY_CURRENCY_ISO, null) ?: ""
            )
        } else null
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTHTOKEN, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getDeviceId(): String? {
        return sharedPreferences.getString(KEY_DEVICE_ID, null)
    }

    fun saveCurrency(currency: String) {
        sharedPreferences.edit().apply {
            putString(KEY_CURRENCY, currency)
            apply()
        }
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "IDR") ?: "IDR"
    }

    fun saveCurrencyIso(iso: String) {
        sharedPreferences.edit().apply {
            putString(KEY_CURRENCY_ISO, iso)
            apply()
        }
    }

    fun getCurrencyIso(): String {
        return sharedPreferences.getString(KEY_CURRENCY_ISO, "IDR") ?: "IDR"
    }

    fun saveCurrencyWithIso(currency: String, iso: String) {
        sharedPreferences.edit().apply {
            putString(KEY_CURRENCY, currency)
            putString(KEY_CURRENCY_ISO, iso)
            apply()
        }
    }

    fun logout() {
        val (savedEmail, savedPassword, rememberMe) = getSavedCredentials()
        sharedPreferences.edit().clear().apply()
        if (rememberMe && savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
            saveCredentials(savedEmail, savedPassword, true)
        }
    }

    fun observeLoginStatus(): Flow<Boolean> = flow {
        emit(isLoggedIn())
    }

    fun saveScheduledOrders(orders: List<ScheduledOrder>) {
        try {
            val gson = com.google.gson.Gson()
            val jsonString = gson.toJson(orders)

            sharedPreferences.edit().apply {
                putString(KEY_SCHEDULED_ORDERS, jsonString)
                apply()
            }

            Log.d("SessionManager", "Saved ${orders.size} scheduled orders")
        } catch (e: Exception) {
            Log.e("SessionManager", "Error saving scheduled orders: ${e.message}", e)
        }
    }

    fun getScheduledOrders(): List<ScheduledOrder> {
        return try {
            val jsonString = sharedPreferences.getString(KEY_SCHEDULED_ORDERS, null)

            if (jsonString.isNullOrEmpty()) {
                Log.d("SessionManager", "No saved scheduled orders found")
                return emptyList()
            }

            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<ScheduledOrder>>() {}.type
            val orders: List<ScheduledOrder> = gson.fromJson(jsonString, type)

            Log.d("SessionManager", "Loaded ${orders.size} scheduled orders")
            orders
        } catch (e: Exception) {
            Log.e("SessionManager", "Error loading scheduled orders: ${e.message}", e)
            emptyList()
        }
    }

    fun clearScheduledOrders() {
        sharedPreferences.edit().apply {
            remove(KEY_SCHEDULED_ORDERS)
            apply()
        }
        Log.d("SessionManager", "Cleared scheduled orders")
    }
}