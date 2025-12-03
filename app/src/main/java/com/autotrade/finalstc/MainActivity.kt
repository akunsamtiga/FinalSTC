package com.autotrade.finalstc

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.presentation.login.LoginScreen
import com.autotrade.finalstc.presentation.main.MainScreen
import com.autotrade.finalstc.presentation.register.RegisterScreen
import com.autotrade.finalstc.ui.theme.FinalSTCTheme
import com.autotrade.finalstc.utils.PermissionsHandler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = loginRepository.isLoggedIn()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var loginRepository: LoginRepository

    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    private lateinit var permissionsHandler: PermissionsHandler

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "ai_signal_prefs"
        private const val KEY_AI_SIGNAL_ACTIVE = "is_ai_signal_active"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionsHandler = PermissionsHandler(this)
        permissionsHandler.checkAndRequestPermissions()

        setContent {
            FinalSTCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TradingApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ Check AI Signal status and restore notifications if needed
        val isAISignalActive = getAISignalStatus()

        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "📱 APP RESUMED")
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "   AI Signal Status: ${if (isAISignalActive) "ACTIVE ✅" else "INACTIVE ❌"}")
        Log.d(TAG, "   User Logged In: ${loginRepository.isLoggedIn()}")

        // ✅ Restore AI Signal notifications if mode is active
        if (isAISignalActive && loginRepository.isLoggedIn()) {
            Log.d(TAG, "   🔔 Restoring AI Signal notifications...")
            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(true)
        }
        Log.d(TAG, "=" .repeat(60))
    }

    override fun onPause() {
        super.onPause()

        // ✅ Check AI Signal status, NOT login status
        val isAISignalActive = getAISignalStatus()

        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "📱 APP PAUSED")
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "   AI Signal Status: ${if (isAISignalActive) "ACTIVE ✅" else "INACTIVE ❌"}")
        Log.d(TAG, "   User Logged In: ${loginRepository.isLoggedIn()}")

        // ✅ Only disable notifications if AI Signal Mode is NOT active
        if (!isAISignalActive) {
            Log.d(TAG, "   🔇 AI Signal inactive - Disabling notifications")
            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
        } else {
            Log.d(TAG, "   🔔 AI Signal ACTIVE - Keeping notifications enabled")
        }
        Log.d(TAG, "=" .repeat(60))
    }

    // MainActivity.kt - UPDATE onDestroy dan tambah method baru

    override fun onDestroy() {
        super.onDestroy()

        // ✅ CHECK: Hanya clear jika user BENAR-BENAR TIDAK LOGIN
        if (!loginRepository.isLoggedIn()) {
            Log.d(TAG, "App destroyed and not logged in - Full cleanup")
            clearAISignalStatus()
            clearFCMTokenFromFirestore() // ✅ TAMBAH INI
            unsubscribeFromFCMTopic()     // ✅ TAMBAH INI
            com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
        } else {
            // ✅ NEW: Bahkan saat user login, tetap clear jika AI Signal tidak aktif
            val isAISignalActive = getAISignalStatus()
            if (!isAISignalActive) {
                Log.d(TAG, "App destroyed, user logged in but AI Signal NOT active - Clearing FCM")
                clearFCMTokenFromFirestore()
                unsubscribeFromFCMTopic()
                com.autotrade.finalstc.service.TradingSignalMessagingService.setAISignalModeActive(false)
            } else {
                Log.d(TAG, "App destroyed but AI Signal ACTIVE - Preserving FCM setup")
            }
        }
    }

    // ✅ NEW METHOD: Clear FCM token from Firestore
    private fun clearFCMTokenFromFirestore() {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val userSession = loginRepository.getUserSession()
                if (userSession != null) {
                    val email = userSession.email
                    val userId = userSession.userId

                    Log.d(TAG, "🔕 Clearing FCM token from Firestore...")

                    // Check if admin
                    val isAdmin = firebaseRepository.checkIsAdmin(email)

                    if (isAdmin) {
                        firebaseRepository.clearAdminFCMToken(email)
                        Log.d(TAG, "✅ Admin FCM token cleared from Firestore")
                    } else {
                        firebaseRepository.clearUserFCMToken(userId)
                        Log.d(TAG, "✅ User FCM token cleared from Firestore")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing FCM token from Firestore: ${e.message}")
            }
        }
    }

    // ✅ NEW METHOD: Unsubscribe from FCM topic
    private fun unsubscribeFromFCMTopic() {
        try {
            Log.d(TAG, "🔕 Unsubscribing from FCM topic...")

            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("trading_signals")
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Successfully unsubscribed from trading_signals topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to unsubscribe: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from topic: ${e.message}")
        }
    }

    // ✅ Helper methods for AI Signal status
    private fun getAISignalStatus(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AI_SIGNAL_ACTIVE, false)
    }

    private fun clearAISignalStatus() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AI_SIGNAL_ACTIVE, false).apply()
    }

    // ✅ Public method to save AI Signal status (called from DashboardViewModel)
    fun saveAISignalStatus(isActive: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AI_SIGNAL_ACTIVE, isActive).apply()
        Log.d(TAG, "💾 AI Signal status saved: $isActive")
    }
}

@Composable
fun TradingApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val startDestination = if (appViewModel.isLoggedIn()) "main" else "login"
    val context = LocalContext.current

    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000L)
            backPressedOnce = false
        }
    }

    BackHandler(enabled = navController.currentDestination?.route in listOf("main", "login")) {
        if (backPressedOnce) {
            (context as? ComponentActivity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(
                context,
                "Tekan sekali lagi untuk keluar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}