package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.api.LoginApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.LoginRequest
import com.autotrade.finalstc.data.model.UserSession
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private val apiService: LoginApiService,
    private val sessionManager: SessionManager,
    private val firebaseRepository: FirebaseRepository,
    private val currencyRepository: CurrencyRepository
) {
    companion object {
        private const val TAG = "LoginRepository"
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

            if (allUsers.isNotEmpty()) {
                Log.d(TAG, "Daftar pengguna whitelist:")
                allUsers.forEach { user ->
                    Log.d(TAG, "Pengguna: ${user.name} - Email: ${user.email} - UserID: '${user.userId}' - Aktif: ${user.isActive}")
                }
            } else {
                Log.w(TAG, "Tidak ada pengguna ditemukan di koleksi whitelist")
            }

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

            Log.d(TAG, "Membuat sesi pengguna")
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
                    Log.d(TAG, "Currency API response: default=${currencyData?.default}, current=${currencyData?.current}")

                    val currentCurrencyCode = currencyData?.current ?: "IDR"
                    val currentCurrency = currencyData?.list?.find { it.iso == currentCurrencyCode }
                    val unitSymbol = currentCurrency?.unit ?: "Rp"
                    val currencyIso = currentCurrency?.iso ?: currentCurrencyCode

                    Log.d(TAG, "✅ Currency terdeteksi: $currencyIso ($unitSymbol)")

                    // Update session agar realtimeTodayProfit ikut berubah
                    userSession = userSession.copy(
                        currency = currentCurrencyCode, // "USD"
                        currencyIso = unitSymbol        // "$"
                    )
                    sessionManager.saveCurrencyWithIso(currentCurrencyCode, unitSymbol)
                    sessionManager.saveUserSession(userSession)

                    Log.d(TAG, "✅ User session disimpan dengan currency: ${userSession.currency} (${userSession.currencyIso})")
                } else {
                    Log.w(TAG, "❌ Gagal mengambil currency, fallback ke IDR")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saat mengambil currency: ${e.message}", e)
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
        Log.d(TAG, "Pengguna logout")
        sessionManager.logout()
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
