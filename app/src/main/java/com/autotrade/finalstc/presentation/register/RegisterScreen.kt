package com.autotrade.finalstc.presentation.register

import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.web.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import android.util.Log
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autotrade.finalstc.presentation.theme.ThemeViewModel
import androidx.compose.material.icons.filled.Help

object GoogleColors {
    val Blue = Color(0xFF4285F4)
    val Green = Color(0xFF34A853)
    val Red = Color(0xFFEA4335)
    val Yellow = Color(0xFFFBBC04)
    val DarkGray = Color(0xFF202124)
    val MediumGray = Color(0xFF5F6368)
    val LightGray = Color(0xFFF8F9FA)
    val White = Color(0xFFFFFFFF)
}

object PastelBlueColors {
    val LightBlue = Color(0xFF87CEEB)
    val SoftBlue = Color(0xFF9BB4D6)
    val PowderBlue = Color(0xFFB0C4DE)
    val BabyBlue = Color(0xFFADD8E6)
}

data class SuccessNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun LoadingOverlay(
    progress: Float,
    isVisible: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutCubic
        ),
        label = "progressAnimation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "fadeAnimation"
    )

    if (isVisible || alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GoogleColors.White)
                .zIndex(1000f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            ) {
                SimpleLoadingDots()
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Loading ${(animatedProgress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoogleColors.MediumGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SimpleLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colors = listOf(
            PastelBlueColors.LightBlue,
            PastelBlueColors.SoftBlue,
            PastelBlueColors.PowderBlue,
            PastelBlueColors.BabyBlue
        )

        colors.forEachIndexed { index, color ->
            val animatedY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = EaseInOutCubic
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(y = animatedY.dp)
                    .background(
                        color = color,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
fun ConfigErrorDialog(
    error: String,
    onRetry: () -> Unit,
    onContinueWithDefault: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = GoogleColors.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = GoogleColors.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Configuration Error",
                    fontWeight = FontWeight.Bold,
                    color = GoogleColors.DarkGray,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Text(
                text = error,
                color = GoogleColors.MediumGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleColors.Blue,
                    contentColor = GoogleColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onContinueWithDefault,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GoogleColors.MediumGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Medium)
            }
        }
    )
}

class UrlDetectorWebViewClient(
    private val onUrlChanged: (String) -> Unit
) : AccompanistWebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.toString()?.let { url ->
            onUrlChanged(url)
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        url?.let {
            onUrlChanged(it)
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        url?.let {
            onUrlChanged(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val currentTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val colors = currentTheme.colors

    val whatsappNumber by viewModel.whatsappNumber.collectAsState()

    var hasClickedDaftar by remember { mutableStateOf(false) }
    var isWebFullyLoaded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var hasShownSuccessDialog by remember { mutableStateOf(false) }
    var showConfigError by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }
    var isExtractingData by remember { mutableStateOf(false) }

    var showLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }

    var currentDetectedUrl by remember { mutableStateOf<String?>(null) }

    var registrationAuthToken by remember { mutableStateOf<String?>(null) }
    var registrationDeviceId by remember { mutableStateOf<String?>(null) }
    var registrationEmail by remember { mutableStateOf<String?>(null) }
    var isDataExtracted by remember { mutableStateOf(false) }

    val successNavItems = listOf(
        SuccessNavItem("dashboard", Icons.Outlined.Dashboard, "Dashboard"),
        SuccessNavItem("history", Icons.Outlined.History, "History"),
        SuccessNavItem("webview", Icons.Outlined.Language, "Trade"),
        SuccessNavItem("profile", Icons.Outlined.Person, "Profile")
    )

    var showBottomNavigation by remember { mutableStateOf(false) }

    val webViewState = rememberWebViewState(url = uiState.registrationUrl)
    val navigator = rememberWebViewNavigator()

    val successUrlPatterns = listOf(
        "/trading", "/onboarding", "/welcome", "/dashboard",
        "/home", "/account", "/main", "/member", "/profile",
        "/user", "/app", "/portal", "/logged"
    )

    fun isSuccessUrl(url: String): Boolean {
        return successUrlPatterns.any { pattern ->
            url.contains(pattern, ignoreCase = true)
        }
    }

    LaunchedEffect(uiState.saveToWhitelistSuccess) {
        if (uiState.saveToWhitelistSuccess) {
            showSavingDialog = false
            delay(500)
            onNavigateToDashboard()
            viewModel.clearSaveError()
        }
    }

    LaunchedEffect(uiState.saveToWhitelistError) {
        if (uiState.saveToWhitelistError != null) {
            showSavingDialog = false
        }
    }

    LaunchedEffect(uiState.error, uiState.isLoading) {
        showConfigError = uiState.error != null && uiState.isLoading
    }

    LaunchedEffect(Unit) {
        showLoading = true
        loadingProgress = 0f
        for (i in 0..10) {
            loadingProgress = i / 100f
            delay(50)
        }

        registrationDeviceId = UUID.randomUUID().toString().replace("-", "")
        Log.d("RegisterScreen", "Generated Device ID: $registrationDeviceId")
    }

    LaunchedEffect(webViewState.loadingState) {
        when (val loadingState = webViewState.loadingState) {
            is LoadingState.Loading -> {
                if (!hasClickedDaftar && loadingProgress < 0.5f) {
                    coroutineScope.launch {
                        val currentProgress = loadingProgress
                        val targetProgress = 0.5f
                        val steps = 40
                        val increment = (targetProgress - currentProgress) / steps
                        for (i in 1..steps) {
                            loadingProgress = currentProgress + (increment * i)
                            delay(25)
                        }
                    }
                }
            }
            is LoadingState.Finished -> {
                if (!isWebFullyLoaded && !hasClickedDaftar) {
                    isWebFullyLoaded = true

                    coroutineScope.launch {
                        delay(12000)

                        if (!hasClickedDaftar) {
                            Log.d("RegisterScreen", "Attempting to click Daftar button")

                            navigator.loadUrl("""javascript:(function() {
                                var buttons = document.querySelectorAll('button, a, input[type=button], input[type=submit]');
                                
                                for (var i = 0; i < buttons.length; i++) {
                                    var btn = buttons[i];
                                    var text = (btn.innerText || btn.textContent || btn.value || '').trim().toLowerCase();
                                    
                                    if (text.indexOf('daftar') >= 0) {
                                        try {
                                            btn.scrollIntoView();
                                            btn.focus();
                                            btn.click();
                                            console.log('DAFTAR_BUTTON_CLICKED');
                                            break;
                                        } catch (error) {
                                            console.log('Error: ' + error.message);
                                        }
                                    }
                                }
                            })(); void(0);""")

                            delay(2000)
                            hasClickedDaftar = true
                            Log.d("RegisterScreen", "Daftar button clicked, hasClickedDaftar = true")

                            val currentProgress = loadingProgress
                            val targetProgress = 1f
                            val steps = 50
                            val increment = (targetProgress - currentProgress) / steps
                            for (i in 1..steps) {
                                loadingProgress = currentProgress + (increment * i)
                                delay(20)
                            }

                            launch {
                                repeat(30) {
                                    delay(1000)

                                    if (!hasShownSuccessDialog) {
                                        navigator.loadUrl("""javascript:(function() {
                                            try {
                                                console.log('Current URL: ' + window.location.href);
                                                console.log('Current Path: ' + window.location.pathname);
                                            } catch (e) {
                                                console.log('Error: ' + e.message);
                                            }
                                        })(); void(0);""")
                                    } else {
                                        return@repeat
                                    }
                                }
                            }

                            delay(800)
                            if (!hasShownSuccessDialog) {
                                showLoading = false
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(webViewState.lastLoadedUrl) {
        val url = webViewState.lastLoadedUrl
        if (url != null && url != currentDetectedUrl) {
            currentDetectedUrl = url
            Log.d("RegisterScreen", "URL changed: $url")
        }
    }

    LaunchedEffect(currentDetectedUrl, hasClickedDaftar, isDataExtracted) {
        val url = currentDetectedUrl

        if (url != null && hasClickedDaftar && !hasShownSuccessDialog && !isDataExtracted) {
            if (isSuccessUrl(url)) {
                Log.d("RegisterScreen", "Success URL detected: $url")
                Log.d("RegisterScreen", "Starting data extraction...")

                isExtractingData = true

                coroutineScope.launch {
                    Log.d("RegisterScreen", "Attempt 1: Extract from cookies")
                    navigator.loadUrl("""
                        javascript:(function() {
                            try {
                                var data = { authtoken: '', deviceId: '', email: '' };
                                var cookies = document.cookie.split(';');
                                for (var i = 0; i < cookies.length; i++) {
                                    var cookie = cookies[i].trim();
                                    if (cookie.indexOf('authtoken=') === 0) {
                                        data.authtoken = cookie.substring('authtoken='.length);
                                    }
                                    if (cookie.indexOf('device_id=') === 0) {
                                        data.deviceId = cookie.substring('device_id='.length);
                                    }
                                    if (cookie.indexOf('email=') === 0) {
                                        data.email = cookie.substring('email='.length);
                                    }
                                }
                                console.log('REGDATA_COOKIES:' + JSON.stringify(data));
                            } catch (e) {
                                console.log('REGDATA_COOKIES_ERROR:' + e.message);
                            }
                        })();
                    """.trimIndent())

                    delay(800)

                    Log.d("RegisterScreen", "Attempt 2: Extract from localStorage")
                    navigator.loadUrl("""
                        javascript:(function() {
                            try {
                                var data = { authtoken: '', deviceId: '', email: '' };
                                if (typeof(Storage) !== 'undefined') {
                                    data.authtoken = localStorage.getItem('authtoken') || '';
                                    data.deviceId = localStorage.getItem('device_id') || '';
                                    data.email = localStorage.getItem('email') || localStorage.getItem('user_email') || '';
                                }
                                console.log('REGDATA_STORAGE:' + JSON.stringify(data));
                            } catch (e) {
                                console.log('REGDATA_STORAGE_ERROR:' + e.message);
                            }
                        })();
                    """.trimIndent())

                    delay(800)

                    Log.d("RegisterScreen", "Attempt 3: Extract from sessionStorage")
                    navigator.loadUrl("""
                        javascript:(function() {
                            try {
                                var data = { authtoken: '', deviceId: '', email: '' };
                                if (typeof(Storage) !== 'undefined') {
                                    data.authtoken = sessionStorage.getItem('authtoken') || '';
                                    data.deviceId = sessionStorage.getItem('device_id') || '';
                                    data.email = sessionStorage.getItem('email') || sessionStorage.getItem('user_email') || '';
                                }
                                console.log('REGDATA_SESSION:' + JSON.stringify(data));
                            } catch (e) {
                                console.log('REGDATA_SESSION_ERROR:' + e.message);
                            }
                        })();
                    """.trimIndent())

                    delay(800)

                    Log.d("RegisterScreen", "Attempt 4: Extract from window variables")
                    navigator.loadUrl("""
                        javascript:(function() {
                            try {
                                var data = { authtoken: '', deviceId: '', email: '' };
                                if (window.authtoken) data.authtoken = window.authtoken;
                                if (window.device_id) data.deviceId = window.device_id;
                                if (window.userEmail) data.email = window.userEmail;
                                if (window.user && window.user.email) data.email = window.user.email;
                                console.log('REGDATA_WINDOW:' + JSON.stringify(data));
                            } catch (e) {
                                console.log('REGDATA_WINDOW_ERROR:' + e.message);
                            }
                        })();
                    """.trimIndent())

                    delay(1000)

                    isExtractingData = false
                    isDataExtracted = true

                    Log.d("RegisterScreen", "Data extraction completed")
                    Log.d("RegisterScreen", "AuthToken: ${registrationAuthToken?.take(20)}...")
                    Log.d("RegisterScreen", "DeviceId: $registrationDeviceId")
                    Log.d("RegisterScreen", "Email: $registrationEmail")

                    if (registrationAuthToken.isNullOrEmpty()) {
                        Log.w("RegisterScreen", "AuthToken still empty after all attempts")
                    }

                    val currentProgress = loadingProgress
                    val targetProgress = 1f
                    val steps = 50
                    val increment = (targetProgress - currentProgress) / steps
                    for (i in 1..steps) {
                        loadingProgress = currentProgress + (increment * i)
                        delay(20)
                    }

                    delay(800)
                    showLoading = false
                    hasShownSuccessDialog = true
                    showSuccessDialog = true
                    showBottomNavigation = true

                    Log.d("RegisterScreen", "Success dialog shown")
                }
            }
        }
    }

    if (showConfigError) {
        ConfigErrorDialog(
            error = uiState.error ?: "Unknown configuration error",
            onRetry = {
                viewModel.retryLoadConfig()
                showConfigError = false
            },
            onContinueWithDefault = {
                viewModel.clearError()
                showConfigError = false
            }
        )
    }

    if (showSavingDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GoogleColors.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = GoogleColors.Blue,
                        strokeWidth = 4.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Menghubungkan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoogleColors.DarkGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Mohon tunggu sebentar...",
                        fontSize = 14.sp,
                        color = GoogleColors.MediumGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (uiState.saveToWhitelistError != null) {
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { viewModel.clearSaveError() },
            containerColor = GoogleColors.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = GoogleColors.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Login Tidak Berhasil",
                        fontWeight = FontWeight.Bold,
                        color = GoogleColors.DarkGray,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = uiState.saveToWhitelistError!!,
                    color = GoogleColors.MediumGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val waUrl = "https://wa.me/$whatsappNumber"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(waUrl)
                        }
                        context.startActivity(intent)
                        viewModel.clearSaveError()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleColors.Green,
                        contentColor = GoogleColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hubungi Admin", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.clearSaveError()
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoogleColors.MediumGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kembali", fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (showSuccessDialog) {
        LaunchedEffect(Unit) {
            Log.d("RegisterScreen", "=== SUCCESS DIALOG SHOWN - CHECKING COOKIES ===")
            try {
                val cookieManager = CookieManager.getInstance()
                val allCookies = cookieManager.getCookie("https://stockity.id")
                Log.d("RegisterScreen", "All cookies: $allCookies")

                if (allCookies != null) {
                    val cookieArray = allCookies.split(";")
                    cookieArray.forEach { cookie ->
                        val parts = cookie.trim().split("=")
                        if (parts.size >= 2) {
                            Log.d("RegisterScreen", "Cookie: ${parts[0]} = ${parts[1].take(30)}...")

                            when (parts[0]) {
                                "authtoken" -> {
                                    if (registrationAuthToken.isNullOrEmpty()) {
                                        registrationAuthToken = parts[1]
                                        Log.d("RegisterScreen", "AuthToken captured: ${parts[1].take(20)}...")
                                    }
                                }
                                "device_id" -> {
                                    if (parts[1].isNotEmpty()) {
                                        registrationDeviceId = parts[1]
                                        Log.d("RegisterScreen", "DeviceId captured: ${parts[1]}")
                                    }
                                }
                                "email" -> {
                                    if (registrationEmail.isNullOrEmpty()) {
                                        registrationEmail = parts[1]
                                        Log.d("RegisterScreen", "Email captured: ${parts[1]}")
                                    }
                                }
                            }
                        }
                    }
                }

                Log.d("RegisterScreen", "=== FINAL DATA STATUS ===")
                Log.d("RegisterScreen", "AuthToken: ${if (registrationAuthToken.isNullOrEmpty()) "EMPTY" else "OK (${registrationAuthToken!!.take(20)}...)"}")
                Log.d("RegisterScreen", "DeviceId: ${registrationDeviceId ?: "EMPTY"}")
                Log.d("RegisterScreen", "Email: ${registrationEmail ?: "EMPTY"}")
                Log.d("RegisterScreen", "========================")
            } catch (e: Exception) {
                Log.e("RegisterScreen", "Error checking cookies in dialog: ${e.message}", e)
            }
        }

        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GoogleColors.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp,
                    pressedElevation = 20.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = GoogleColors.Green.copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    modifier = Modifier.size(24.dp),
                                    tint = GoogleColors.Green
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Registrasi Berhasil!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoogleColors.DarkGray,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Selamat! Akun Anda telah berhasil dibuat.",
                        fontSize = 17.sp,
                        color = GoogleColors.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Apa yang ingin Anda lakukan selanjutnya?",
                        fontSize = 15.sp,
                        color = GoogleColors.MediumGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                Log.d("RegisterScreen", "Login STC button clicked")
                                Log.d("RegisterScreen", "Using extracted data:")
                                Log.d("RegisterScreen", "  AuthToken: ${registrationAuthToken?.take(20)}...")
                                Log.d("RegisterScreen", "  DeviceId: $registrationDeviceId")
                                Log.d("RegisterScreen", "  Email: $registrationEmail")

                                if (registrationAuthToken.isNullOrEmpty()) {
                                    Log.w("RegisterScreen", "AuthToken is empty, trying CookieManager fallback")

                                    try {
                                        val cookieManager = android.webkit.CookieManager.getInstance()
                                        val cookies = cookieManager.getCookie("https://stockity.id")

                                        Log.d("RegisterScreen", "Cookies from CookieManager: $cookies")

                                        if (cookies != null) {
                                            val cookieArray = cookies.split(";")
                                            for (cookie in cookieArray) {
                                                val cookiePair = cookie.trim().split("=")
                                                if (cookiePair.size >= 2) {
                                                    when (cookiePair[0]) {
                                                        "authtoken" -> {
                                                            registrationAuthToken = cookiePair[1]
                                                            Log.d("RegisterScreen", "AuthToken from CookieManager: ${cookiePair[1].take(20)}...")
                                                        }
                                                        "device_id" -> {
                                                            registrationDeviceId = cookiePair[1]
                                                            Log.d("RegisterScreen", "DeviceId from CookieManager: ${cookiePair[1]}")
                                                        }
                                                        "email" -> {
                                                            registrationEmail = cookiePair[1]
                                                            Log.d("RegisterScreen", "Email from CookieManager: ${cookiePair[1]}")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterScreen", "Error extracting from CookieManager: ${e.message}", e)
                                    }
                                }

                                if (registrationAuthToken.isNullOrEmpty()) {
                                    Log.e("RegisterScreen", "AuthToken is still empty after fallback!")
                                    showSuccessDialog = false
                                    onBackClick()
                                } else {
                                    showSuccessDialog = false
                                    showSavingDialog = true

                                    val authToken = registrationAuthToken!!
                                    val deviceId = registrationDeviceId ?: UUID.randomUUID().toString().replace("-", "")
                                    val email = registrationEmail ?: ""

                                    Log.d("RegisterScreen", "Saving to whitelist with:")
                                    Log.d("RegisterScreen", "  AuthToken: ${authToken.take(20)}...")
                                    Log.d("RegisterScreen", "  DeviceId: $deviceId")
                                    Log.d("RegisterScreen", "  Email: $email")

                                    viewModel.saveUserToWhitelistAndLogin(
                                        authToken,
                                        deviceId,
                                        email
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoogleColors.Blue,
                                contentColor = GoogleColors.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 8.dp
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 0.8.dp,
                                brush = SolidColor(GoogleColors.White)
                            ),
                        ) {
                            Text(
                                text = "Login STC Autotrade",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.1.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                Log.d("RegisterScreen", "Continue trading button clicked")
                                showSuccessDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 0.8.dp,
                                brush = SolidColor(GoogleColors.MediumGray.copy(alpha = 0.3f))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = GoogleColors.White,
                                contentColor = GoogleColors.DarkGray
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Text(
                                text = "Lanjut Trading",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isLoading && !showConfigError) {
            WebView(
                state = webViewState,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(bottom = if (showBottomNavigation) 80.dp else 0.dp),
                onCreated = { webView ->
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = false
                        loadWithOverviewMode = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        minimumFontSize = 8
                        defaultFontSize = 16
                        textZoom = 100
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                        setSupportMultipleWindows(true)
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991B) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webView, true)
                    }

                    Log.d("RegisterScreen", "WebView created and configured")

                    webView.webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val message = it.message()
                                Log.d("RegisterScreen", "Console: $message")

                                fun parseAndSaveData(jsonStr: String, source: String) {
                                    try {
                                        val authTokenRegex = """"authtoken":"([^"]*)"""".toRegex()
                                        val deviceIdRegex = """"deviceId":"([^"]*)"""".toRegex()
                                        val emailRegex = """"email":"([^"]*)"""".toRegex()

                                        val authToken = authTokenRegex.find(jsonStr)?.groupValues?.get(1)
                                        val deviceIdFromWeb = deviceIdRegex.find(jsonStr)?.groupValues?.get(1)
                                        val email = emailRegex.find(jsonStr)?.groupValues?.get(1)

                                        if (!authToken.isNullOrEmpty() && registrationAuthToken.isNullOrEmpty()) {
                                            registrationAuthToken = authToken
                                            Log.d("RegisterScreen", "AuthToken captured: ${authToken.take(20)}...")
                                        }

                                        if (!deviceIdFromWeb.isNullOrEmpty() && deviceIdFromWeb != registrationDeviceId) {
                                            registrationDeviceId = deviceIdFromWeb
                                            Log.d("RegisterScreen", "DeviceId captured: $deviceIdFromWeb")
                                        }

                                        if (!email.isNullOrEmpty() && registrationEmail.isNullOrEmpty()) {
                                            registrationEmail = email
                                            Log.d("RegisterScreen", "Email captured: $email")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterScreen", "Error parsing $source data: ${e.message}", e)
                                    }
                                }

                                when {
                                    message.startsWith("REGDATA_COOKIES:") -> {
                                        val jsonStr = message.substring("REGDATA_COOKIES:".length)
                                        Log.d("RegisterScreen", "Received cookies data: $jsonStr")
                                        parseAndSaveData(jsonStr, "COOKIES")
                                    }
                                    message.startsWith("REGDATA_STORAGE:") -> {
                                        val jsonStr = message.substring("REGDATA_STORAGE:".length)
                                        Log.d("RegisterScreen", "Received localStorage data: $jsonStr")
                                        parseAndSaveData(jsonStr, "LOCALSTORAGE")
                                    }
                                    message.startsWith("REGDATA_SESSION:") -> {
                                        val jsonStr = message.substring("REGDATA_SESSION:".length)
                                        Log.d("RegisterScreen", "Received sessionStorage data: $jsonStr")
                                        parseAndSaveData(jsonStr, "SESSIONSTORAGE")
                                    }
                                    message.startsWith("REGDATA_WINDOW:") -> {
                                        val jsonStr = message.substring("REGDATA_WINDOW:".length)
                                        Log.d("RegisterScreen", "Received window data: $jsonStr")
                                        parseAndSaveData(jsonStr, "WINDOW")
                                    }
                                    message.startsWith("REGDATA_COOKIES_ERROR:") -> {
                                        Log.w("RegisterScreen", "Cookies extraction error: ${message.substring(23)}")
                                    }
                                    message.startsWith("REGDATA_STORAGE_ERROR:") -> {
                                        Log.w("RegisterScreen", "LocalStorage extraction error: ${message.substring(23)}")
                                    }
                                    message.startsWith("REGDATA_SESSION_ERROR:") -> {
                                        Log.w("RegisterScreen", "SessionStorage extraction error: ${message.substring(23)}")
                                    }
                                    message.startsWith("REGDATA_WINDOW_ERROR:") -> {
                                        Log.w("RegisterScreen", "Window extraction error: ${message.substring(22)}")
                                    }
                                    message == "DAFTAR_BUTTON_CLICKED" -> {
                                        Log.d("RegisterScreen", "Daftar button was clicked successfully")
                                    }
                                }
                            }
                            return true
                        }
                    }
                },
                client = UrlDetectorWebViewClient(
                    onUrlChanged = { url ->
                        currentDetectedUrl = url
                    }
                ),
                chromeClient = AccompanistWebChromeClient()
            )
        }

        LoadingOverlay(
            progress = loadingProgress,
            isVisible = showLoading || uiState.isLoading
        )

        if (showBottomNavigation) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = colors.bgmain2,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    Divider(
                        color = colors.borderColor,
                        thickness = 0.5.dp
                    )

                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        successNavItems.forEach { item ->
                            val selected = false

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
                                        contentAlignment = Alignment.Center
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
                                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    if (!showSuccessDialog) {
                                        showSuccessDialog = true
                                    }
                                    Log.d("RegisterScreen", "Navigation item clicked: ${item.route}")
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = colors.accentPrimary2main,
                                    selectedTextColor = colors.accentPrimary2main,
                                    unselectedIconColor = colors.TextPrimary1,
                                    unselectedTextColor = colors.TextPrimary1
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}