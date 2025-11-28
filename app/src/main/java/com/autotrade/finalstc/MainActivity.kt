package com.autotrade.finalstc

import android.os.Bundle
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
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.presentation.login.LoginScreen
import com.autotrade.finalstc.presentation.main.MainScreen
import com.autotrade.finalstc.presentation.register.RegisterScreen
import com.autotrade.finalstc.ui.theme.FinalSTCTheme
import com.autotrade.finalstc.utils.PermissionsHandler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = loginRepository.isLoggedIn()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionsHandler: PermissionsHandler

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
            delay(2000)
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