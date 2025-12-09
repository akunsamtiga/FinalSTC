package com.autotrade.finalstc

import android.app.Application
import android.content.Context
import android.util.Log
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.repository.LoginRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TradingApplication : Application() {

    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    @Inject
    lateinit var loginRepository: LoginRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "TradingApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize Firebase first
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "✅ Firebase initialized")

        // ✅ REMOVE auto-save on app start
        Log.d(TAG, "⚠️ FCM Token auto-save DISABLED")
        Log.d(TAG, "💡 Token will be saved ONLY when AI Signal Mode is started")

        // ✅ ADD: Check and restore AI Signal on app restart
        checkAndRestoreAISignalMode()
    }

    private fun checkAndRestoreAISignalMode() {
        applicationScope.launch {
            try {
                val prefs = getSharedPreferences("ai_signal_prefs", Context.MODE_PRIVATE)
                val wasActive = prefs.getBoolean("is_ai_signal_active", false)

                if (wasActive) {
                    Log.d(TAG, "=" .repeat(60))
                    Log.d(TAG, "🔄 AI SIGNAL WAS ACTIVE - ENSURING FCM TOKEN")
                    Log.d(TAG, "=" .repeat(60))

                    // Re-save FCM token to ensure it's current
                    saveFCMTokenManually()

                    // Re-enable notifications
                    com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(true)

                    Log.d(TAG, "✅ AI Signal Mode preserved across app restart")
                    Log.d(TAG, "=" .repeat(60))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring AI Signal: ${e.message}", e)
            }
        }
    }


    // ✅ NEW: Method to manually save FCM token (called from DashboardViewModel when AI Signal starts)
    fun saveFCMTokenManually() {
        // ✅ CHANGE: Use applicationScope instead of temporary scope
        applicationScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "📤 MANUAL FCM TOKEN SAVE (AI Signal Mode)")
                Log.d(TAG, "=" .repeat(60))

                val userSession = loginRepository.getUserSession()
                if (userSession == null) {
                    Log.w(TAG, "⚠️ No user session - skip FCM token save")
                    return@launch
                }

                val email = userSession.email
                Log.d(TAG, "👤 User Email: $email")

                // ✅ CHECK IF USER IS ADMIN
                val isAdmin = firebaseRepository.checkIsAdmin(email)
                Log.d(TAG, "🔐 Is Admin: $isAdmin")

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "❌ Failed to get FCM token: ${task.exception?.message}")
                        return@addOnCompleteListener
                    }

                    val fcmToken = task.result
                    if (fcmToken.isNullOrEmpty()) {
                        Log.e(TAG, "❌ FCM token is empty or null")
                        return@addOnCompleteListener
                    }

                    Log.d(TAG, "=" .repeat(60))
                    Log.d(TAG, "📱 FCM TOKEN OBTAINED")
                    Log.d(TAG, "=" .repeat(60))
                    Log.d(TAG, "Token preview: ${fcmToken.take(20)}...${fcmToken.takeLast(20)}")

                    // ✅ Use applicationScope for saving
                    applicationScope.launch(Dispatchers.IO) {
                        var retryCount = 0
                        val maxRetries = 5 // ✅ Increase retries

                        while (retryCount < maxRetries) {
                            try {
                                Log.d(TAG, "📤 Attempt ${retryCount + 1}/$maxRetries: Saving FCM token...")

                                val success = if (isAdmin) {
                                    firebaseRepository.updateAdminFCMToken(email, fcmToken)
                                } else {
                                    firebaseRepository.updateUserFCMToken(userSession.userId, fcmToken)
                                }

                                if (success) {
                                    Log.d(TAG, "=" .repeat(60))
                                    Log.d(TAG, "✅ FCM TOKEN SAVED SUCCESSFULLY")
                                    Log.d(TAG, "=" .repeat(60))
                                    break
                                } else {
                                    retryCount++
                                    if (retryCount < maxRetries) {
                                        delay(3000L * retryCount) // ✅ Longer delay
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error saving: ${e.message}", e)
                                retryCount++
                                if (retryCount < maxRetries) {
                                    delay(3000L * retryCount)
                                }
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Critical error: ${e.message}", e)
            }
        }
    }
}