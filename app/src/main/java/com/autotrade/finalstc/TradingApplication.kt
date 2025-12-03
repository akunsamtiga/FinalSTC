package com.autotrade.finalstc

import android.app.Application
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "TradingApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize Firebase first
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "✅ Firebase initialized")

        // ❌ REMOVED: Auto-save FCM token on app start
        // ✅ Token will ONLY be saved when user explicitly starts AI Signal Mode
        Log.d(TAG, "⚠️ FCM Token auto-save DISABLED")
        Log.d(TAG, "💡 Token will be saved ONLY when AI Signal Mode is started")
    }

    // ✅ NEW: Method to manually save FCM token (called from DashboardViewModel when AI Signal starts)
    fun saveFCMTokenManually() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "📤 MANUAL FCM TOKEN SAVE (AI Signal Mode Started)")
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
                Log.d(TAG, "🔍 Is Admin: $isAdmin")

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "❌ Failed to get FCM token: ${task.exception?.message}")
                        task.exception?.printStackTrace()
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
                    Log.d(TAG, "Email: $email")
                    Log.d(TAG, "User Type: ${if (isAdmin) "ADMIN" else "USER"}")
                    Log.d(TAG, "Token length: ${fcmToken.length}")
                    Log.d(TAG, "Token preview: ${fcmToken.take(20)}...${fcmToken.takeLast(20)}")
                    Log.d(TAG, "=" .repeat(60))

                    applicationScope.launch(Dispatchers.IO) {
                        var retryCount = 0
                        val maxRetries = 3

                        while (retryCount < maxRetries) {
                            try {
                                Log.d(TAG, "📤 Attempt ${retryCount + 1}/$maxRetries: Saving FCM token to Firestore...")

                                // ✅ SAVE TO CORRECT COLLECTION BASED ON USER TYPE
                                val success = if (isAdmin) {
                                    Log.d(TAG, "👑 Saving to admin_users collection...")
                                    firebaseRepository.updateAdminFCMToken(email, fcmToken)
                                } else {
                                    Log.d(TAG, "👤 Saving to whitelist_users collection...")
                                    firebaseRepository.updateUserFCMToken(userSession.userId, fcmToken)
                                }

                                if (success) {
                                    Log.d(TAG, "=" .repeat(60))
                                    Log.d(TAG, "✅ FCM TOKEN SAVED SUCCESSFULLY")
                                    Log.d(TAG, "=" .repeat(60))
                                    Log.d(TAG, "Email: $email")
                                    Log.d(TAG, "User Type: ${if (isAdmin) "ADMIN" else "USER"}")
                                    Log.d(TAG, "Collection: ${if (isAdmin) "admin_users" else "whitelist_users"}")
                                    Log.d(TAG, "Token saved to: ${if (isAdmin) "admin_users/$email/fcmToken" else "whitelist_users/${userSession.userId}/fcmToken"}")
                                    Log.d(TAG, "=" .repeat(60))
                                    break
                                } else {
                                    Log.w(TAG, "⚠️ Failed to save FCM token (attempt ${retryCount + 1})")
                                    retryCount++
                                    if (retryCount < maxRetries) {
                                        delay(2000L * retryCount)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error saving FCM token (attempt ${retryCount + 1}): ${e.message}", e)
                                e.printStackTrace()
                                retryCount++
                                if (retryCount < maxRetries) {
                                    delay(2000L * retryCount)
                                }
                            }
                        }

                        if (retryCount >= maxRetries) {
                            Log.e(TAG, "❌ FAILED to save FCM token after $maxRetries attempts")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Critical error in saveFCMTokenManually: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

}