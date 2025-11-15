package com.autotrade.finalstc.presentation.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.presentation.main.dashboard.DashboardScreen
import com.autotrade.finalstc.presentation.main.dashboard.DashboardColors
import com.autotrade.finalstc.presentation.main.history.HistoryScreen
import com.autotrade.finalstc.presentation.main.webview.WebViewScreen
import com.autotrade.finalstc.presentation.main.profile.ProfileScreen
import com.autotrade.finalstc.presentation.admin.AdminScreen
import com.autotrade.finalstc.presentation.theme.ThemeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : androidx.lifecycle.ViewModel() {

    fun logout() {
        loginRepository.logout()
    }

    fun getUserSession() = loginRepository.getUserSession()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userSession = viewModel.getUserSession()
    val currentUserEmail = userSession?.email ?: ""

    val currentTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val colors = currentTheme.colors

    val bottomNavItems = listOf(
        BottomNavItem("dashboard", Icons.Outlined.Dashboard, "Dashboard"),
        BottomNavItem("history", Icons.Outlined.History, "History"),
        BottomNavItem("webview", Icons.Outlined.Language, "Trade"),
        BottomNavItem("profile", Icons.Outlined.Person, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val scaffoldBackgroundColor = if (currentRoute == "webview") {
        colors.bgmain2
    } else {
        colors.background
    }

    Scaffold(
        containerColor = scaffoldBackgroundColor,
        contentColor = colors.textPrimary,
        bottomBar = {
            if (currentRoute != "admin") {
                ThemedBottomNavigationBar(
                    navController = navController,
                    items = bottomNavItems,
                    colors = colors
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scaffoldBackgroundColor)
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                enterTransition = {
                    when (targetState.destination.route) {
                        "admin" -> slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        )
                        else -> fadeIn(
                            animationSpec = tween(durationMillis = 250)
                        )
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        "admin" -> slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 250)
                        )
                        else -> fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        )
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        "admin" -> slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 350)
                        )
                        else -> fadeIn(
                            animationSpec = tween(durationMillis = 250)
                        )
                    }
                },
                popExitTransition = {
                    when (initialState.destination.route) {
                        "admin" -> slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 250)
                        )
                        else -> fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        )
                    }
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        onLogout = onLogout,
                        viewModel = viewModel,
                        themeViewModel = themeViewModel
                    )
                }
                composable("history") {
                    HistoryScreen()
                }
                composable("webview") {
                    WebViewScreen()
                }
                composable("profile") {
                    ProfileScreen(
                        onLogout = onLogout,
                        onNavigateToAdmin = {
                            navController.navigate("admin")
                        },
                        viewModel = viewModel,
                    )
                }
                composable("admin") {
                    AdminScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        currentUserEmail = currentUserEmail
                    )
                }
            }
        }
    }
}

@Composable
fun ThemedBottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem>,
    colors: DashboardColors
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationBarColor = if (currentRoute == "webview") {
        colors.bgmain2
    } else {
        colors.bgmain
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = navigationBarColor,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route

                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selected) colors.accentPrimary2main.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) colors.accentPrimary2main.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) colors.accentPrimary2main else colors.TextPrimary1,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                color = if (selected) colors.accentPrimary2main else colors.TextPrimary1,
                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Medium
                                else androidx.compose.ui.text.font.FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = colors.accentPrimary1,
                            selectedTextColor = colors.accentPrimary1,
                            unselectedIconColor = colors.textSecondary,
                            unselectedTextColor = colors.textSecondary
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}