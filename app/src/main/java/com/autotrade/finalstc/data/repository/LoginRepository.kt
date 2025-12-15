package com.autotrade.finalstc.data.repository

import android.content.Context
import android.util.Log
import com.autotrade.finalstc.data.api.LoginApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.LoginRequest
import com.autotrade.finalstc.data.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private val apiService: LoginApiService,
    private val sessionManager: SessionManager,
    private val firebaseRepository: FirebaseRepository,
    private val currencyRepository: CurrencyRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LoginRepository"
        private const val PREFS_NAME = "ai_signal_prefs"
        private const val KEY_AI_SIGNAL_ACTIVE = "is_ai_signal_active"
    }

    private fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    suspend fun login(email: String, password: String): Result<UserSession> {
        return try {
            Log.d(TAG, "Memulai proses login")
            Log.d(TAG, "Email: $email")

            val deviceId = generateDeviceId()
            val userAgent = generateUserAgent()
            val userTimezone = "Asia/Bangkok"
            val deviceType = "web"

            Log.d(TAG, "Autentikasi dengan API Stockity")
            val response = apiService.login(
                deviceId = deviceId,
                deviceType = deviceType,
                userTimezone = userTimezone,
                userAgent = userAgent,
                loginRequest = LoginRequest(email, password)
            )

            if (!response.isSuccessful || response.body()?.data == null) {
                val errorMsg = when (response.code()) {
                    401 -> "Email atau password salah untuk akun Stockity"
                    403 -> "Akun Stockity diblokir atau tidak memiliki akses"
                    404 -> "Endpoint tidak ditemukan"
                    422 -> "Data login tidak valid"
                    500 -> "Server Stockity error - coba lagi nanti"
                    else -> "Login Stockity gagal: ${response.message()} (${response.code()})"
                }
                Log.e(TAG, "Error API Stockity: $errorMsg")
                Log.e(TAG, "Kode respon: ${response.code()}")
                return Result.failure(Exception(errorMsg))
            }

            val loginData = response.body()!!.data!!
            Log.d(TAG, "Autentikasi Stockity berhasil")
            Log.d(TAG, "UserID dari Stockity: '${loginData.userId}'")

            Log.d(TAG, "Checking if user is admin...")
            val isAdmin = firebaseRepository.checkIsAdmin(email)

            if (isAdmin) {
                Log.d(TAG, "Admin detected: $email")

                var userSession = UserSession(
                    authtoken = loginData.authtoken,
                    userId = loginData.userId,
                    deviceId = deviceId,
                    email = email,
                    userTimezone = userTimezone,
                    userAgent = userAgent,
                    deviceType = deviceType,
                    currency = "IDR"
                )

                Log.d(TAG, "Menyimpan sesi admin")
                sessionManager.saveUserSession(userSession)

                try {
                    val admin = firebaseRepository.getAdminByEmail(email)
                    if (admin != null) {
                        firebaseRepository.updateAdminUser(admin.copy(lastLogin = System.currentTimeMillis()))
                        Log.d(TAG, "Admin last login updated")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update admin last login: ${e.message}")
                }

                try {
                    Log.d(TAG, "Mengambil data currency untuk admin...")
                    val currencyResult = currencyRepository.fetchUserCurrency()

                    if (currencyResult.isSuccess) {
                        val currencyData = currencyResult.getOrNull()
                        val currentCurrencyCode = currencyData?.current ?: "IDR"
                        val currentCurrency = currencyData?.list?.find { it.iso == currentCurrencyCode }
                        val unitSymbol = currentCurrency?.unit ?: "Rp"
                        val currencyIso = currentCurrency?.iso ?: currentCurrencyCode

                        userSession = userSession.copy(
                            currency = currentCurrencyCode,
                            currencyIso = unitSymbol
                        )
                        sessionManager.saveCurrencyWithIso(currentCurrencyCode, unitSymbol)
                        sessionManager.saveUserSession(userSession)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting currency for admin: ${e.message}")
                }

                Log.d(TAG, "Admin login successful")
                return Result.success(userSession)
            }

            Log.d(TAG, "Memeriksa whitelist aplikasi")

            val firestoreConnected = firebaseRepository.testFirestoreConnection()
            if (!firestoreConnected) {
                Log.e(TAG, "Koneksi Firestore gagal")
                return Result.failure(Exception(
                    "Login Stockity berhasil, tetapi tidak dapat terhubung ke database whitelist aplikasi. " +
                            "Periksa koneksi internet Anda dan coba lagi."
                ))
            }
            Log.d(TAG, "Koneksi Firestore berhasil")

            val allUsers = firebaseRepository.debugCheckAllDocuments()
            Log.d(TAG, "Total pengguna di whitelist: ${allUsers.size}")

            val isWhitelisted = firebaseRepository.checkUserInWhitelistByUserId(loginData.userId)
            Log.d(TAG, "Hasil pemeriksaan whitelist: $isWhitelisted")

            if (!isWhitelisted) {
                Log.e(TAG, "Pengguna tidak ada di whitelist atau tidak aktif")

                val userExists = firebaseRepository.debugCheckUserIdExists(loginData.userId)
                if (userExists) {
                    val inactiveMessage = "Login Stockity berhasil!\n\n" +
                            "Namun akun Anda terdaftar tetapi tidak aktif di aplikasi ini.\n" +
                            "UserID: ${loginData.userId}\n\n" +
                            "Silakan hubungi administrator untuk mengaktifkan akses aplikasi Anda."
                    return Result.failure(Exception(inactiveMessage))
                } else {
                    val notFoundMessage = "Login Stockity berhasil!\n\n" +
                            "Namun akun Anda belum terdaftar di aplikasi ini.\n" +
                            "UserID: ${loginData.userId}\n" +
                            "Email: $email\n\n" +
                            "Silakan hubungi administrator untuk mendaftarkan akses aplikasi Anda."
                    return Result.failure(Exception(notFoundMessage))
                }
            }

            Log.d(TAG, "Membuat sesi pengguna whitelist")
            var userSession = UserSession(
                authtoken = loginData.authtoken,
                userId = loginData.userId,
                deviceId = deviceId,
                email = email,
                userTimezone = userTimezone,
                userAgent = userAgent,
                deviceType = deviceType,
                currency = "IDR"
            )

            Log.d(TAG, "Menyimpan sesi")
            sessionManager.saveUserSession(userSession)

            try {
                Log.d(TAG, "Memperbarui waktu login terakhir")
                firebaseRepository.updateLastLogin(loginData.userId)
                Log.d(TAG, "Waktu login terakhir berhasil diperbarui")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memperbarui waktu login terakhir: ${e.message}")
            }

            try {
                Log.d(TAG, "Mengambil data currency pengguna dari Stockity API...")
                val currencyResult = currencyRepository.fetchUserCurrency()

                if (currencyResult.isSuccess) {
                    val currencyData = currencyResult.getOrNull()
                    val currentCurrencyCode = currencyData?.current ?: "IDR"
                    val currentCurrency = currencyData?.list?.find { it.iso == currentCurrencyCode }
                    val unitSymbol = currentCurrency?.unit ?: "Rp"
                    val currencyIso = currentCurrency?.iso ?: currentCurrencyCode

                    userSession = userSession.copy(
                        currency = currentCurrencyCode,
                        currencyIso = unitSymbol
                    )
                    sessionManager.saveCurrencyWithIso(currentCurrencyCode, unitSymbol)
                    sessionManager.saveUserSession(userSession)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting currency: ${e.message}")
            }

            Log.d(TAG, "Proses login berhasil")
            Result.success(userSession)

        } catch (e: Exception) {
            Log.e(TAG, "Proses login gagal: ${e.message}", e)
            val friendlyError = when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Koneksi timeout. Periksa jaringan internet Anda dan coba lagi."
                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("connection", ignoreCase = true) == true ->
                    "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                e.message?.contains("ssl", ignoreCase = true) == true ->
                    "Masalah keamanan koneksi. Coba lagi nanti."
                else -> e.message ?: "Terjadi kesalahan yang tidak diketahui"
            }
            Result.failure(Exception(friendlyError))
        }
    }

    fun logout() {
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "LOGOUT PROCESS STARTED")
        Log.d(TAG, "=" .repeat(60))

        Log.d(TAG, "Disabling AI Signal Mode notifications...")
        com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
        Log.d(TAG, "AI Signal Mode notifications disabled")

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AI_SIGNAL_ACTIVE, false).apply()
            Log.d(TAG, "AI Signal status cleared from prefs")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing AI Signal status: ${e.message}")
        }

        try {
            Log.d(TAG, "Unsubscribing from FCM topic...")
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("trading_signals")
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from trading_signals topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from topic: ${e.message}")
        }

        try {
            val userSession = getUserSession()
            if (userSession != null) {
                kotlinx.coroutines.runBlocking {
                    try {
                        val email = userSession.email
                        val userId = userSession.userId

                        val isAdmin = firebaseRepository.checkIsAdmin(email)

                        Log.d(TAG, "Clearing FCM token...")
                        Log.d(TAG, "   Email: $email")
                        Log.d(TAG, "   Is Admin: $isAdmin")

                        if (isAdmin) {
                            Log.d(TAG, "Clearing admin FCM token...")
                            firebaseRepository.clearAdminFCMToken(email)
                            Log.d(TAG, "Admin FCM token cleared")
                        } else {
                            Log.d(TAG, "Clearing user FCM token...")
                            firebaseRepository.clearUserFCMToken(userId)
                            Log.d(TAG, "User FCM token cleared")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing FCM token: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in FCM token cleanup: ${e.message}")
        }

        Log.d(TAG, "Clearing WebView data...")
        try {
            com.autotrade.finalstc.utils.WebViewCacheManager.clearAllWebViewData(context)
            Log.d(TAG, "WebView data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView data: ${e.message}", e)
        }

        Log.d(TAG, "Clearing user session...")
        sessionManager.logout()
        Log.d(TAG, "User session cleared")

        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "LOGOUT COMPLETED SUCCESSFULLY")
        Log.d(TAG, "=" .repeat(60))
    }

    fun isLoggedIn(): Boolean {
        val isLoggedIn = sessionManager.isLoggedIn()
        Log.d(TAG, "Memeriksa status login: $isLoggedIn")
        return isLoggedIn
    }

    fun getUserSession(): UserSession? {
        val session = sessionManager.getUserSession()
        Log.d(TAG, "Mengambil sesi pengguna: ${session?.email ?: "kosong"}")
        return session
    }
}