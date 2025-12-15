package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.api.UserProfileApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.UserProfileData
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: UserProfileApiService,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "ProfileRepository"
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private var cachedProfile: UserProfileData? = null
    private var cacheTimestamp: Long = 0L

    suspend fun getUserProfile(forceRefresh: Boolean = false): Result<UserProfileData> {
        if (!forceRefresh && isCacheValid()) {
            Log.d(TAG, "Returning cached profile data (age: ${System.currentTimeMillis() - cacheTimestamp}ms)")
            return Result.success(cachedProfile!!)
        }

        return try {
            val session = sessionManager.getUserSession()
                ?: return Result.failure(Exception("Session not found"))

            Log.d(TAG, "Fetching user profile from API (force: $forceRefresh)")

            var lastException: Exception? = null

            repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                try {
                    Log.d(TAG, "Fetch attempt ${attempt + 1}/$MAX_RETRY_ATTEMPTS")

                    val response = apiService.getUserProfile(
                        locale = "id",
                        deviceId = session.deviceId,
                        deviceType = session.deviceType,
                        userTimezone = session.userTimezone,
                        authorizationToken = session.authtoken,
                        userAgent = session.userAgent
                    )

                    if (response.isSuccessful && response.body()?.data != null) {
                        val profileData = response.body()!!.data!!

                        cachedProfile = profileData
                        cacheTimestamp = System.currentTimeMillis()

                        Log.d(TAG, "Profile fetched successfully: ${profileData.email}")
                        Log.d(TAG, "   Avatar URL: ${profileData.avatar ?: "null"}")
                        Log.d(TAG, "   Cache updated at: $cacheTimestamp")

                        return Result.success(profileData)
                    } else {
                        val errorMsg = "Failed to fetch profile: ${response.code()} - ${response.message()}"
                        Log.e(TAG, errorMsg)
                        lastException = Exception(errorMsg)

                        if (response.code() in 400..499) {
                            return Result.failure(lastException!!)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error on attempt ${attempt + 1}: ${e.message}", e)
                    lastException = e

                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        val delayMs = 1000L * (1 shl attempt)
                        Log.d(TAG, "Retrying after ${delayMs}ms...")
                        delay(delayMs)
                    }
                }
            }

            if (cachedProfile != null) {
                Log.w(TAG, "Using stale cached data (age: ${System.currentTimeMillis() - cacheTimestamp}ms)")
                return Result.success(cachedProfile!!)
            }

            Result.failure(lastException ?: Exception("Failed to fetch profile after $MAX_RETRY_ATTEMPTS attempts"))

        } catch (e: Exception) {
            Log.e(TAG, "Critical error fetching profile: ${e.message}", e)

            if (cachedProfile != null) {
                Log.w(TAG, "Returning cached data due to error")
                return Result.success(cachedProfile!!)
            }

            Result.failure(e)
        }
    }

    private fun isCacheValid(): Boolean {
        if (cachedProfile == null) return false
        val age = System.currentTimeMillis() - cacheTimestamp
        val isValid = age < CACHE_DURATION_MS

        if (!isValid) {
            Log.d(TAG, "Cache expired (age: ${age}ms, max: ${CACHE_DURATION_MS}ms)")
        }

        return isValid
    }

    fun clearCache() {
        Log.d(TAG, "Clearing profile cache")
        cachedProfile = null
        cacheTimestamp = 0L
    }

    fun getCachedProfile(): UserProfileData? {
        return if (isCacheValid()) cachedProfile else null
    }
}