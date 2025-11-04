package com.autotrade.finalstc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.presentation.login.LoginScreen
import com.autotrade.finalstc.presentation.main.MainScreen
import com.autotrade.finalstc.presentation.register.RegisterScreen // ✅ NEW: Import RegisterScreen
import com.autotrade.finalstc.ui.theme.FinalSTCTheme
import com.autotrade.finalstc.utils.PermissionsHandler // ✅ NEW: Import permissions handler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = loginRepository.isLoggedIn()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ✅ NEW: Initialize permissions handler
    private lateinit var permissionsHandler: PermissionsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ✅ NEW: Initialize permissions handler for API 35
        permissionsHandler = PermissionsHandler(this)

        // ✅ NEW: Check and request permissions - CRITICAL for trading alerts!
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
}

@Composable
fun TradingApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val startDestination = if (appViewModel.isLoggedIn()) "main" else "login"

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
                // ✅ TAMBAHKAN parameter ini
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