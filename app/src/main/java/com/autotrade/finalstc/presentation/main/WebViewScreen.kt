package com.autotrade.finalstc.presentation.main.webview

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrade.finalstc.R
import com.google.accompanist.web.*

private val DarkBackground = Color(0xFF1B1B1B)
private val CardBackground = Color(0xFF2B2B2B)
private val AccentSecondary = Color(0xFFDC4D4D)
private val TextPrimary = Color(0xFFEBEBEB)
private val TextSecondary = Color(0xFFBAC1CB)
private val BorderColor = Color(0xFF323232)
private val StatusBlue = Color(0xFF64B5F6)

@Composable
fun WebViewErrorDialog(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Gagal Memuat",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Text(
                text = error,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coba Lagi", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BorderColor
                )
            ) {
                Text("Tutup", fontWeight = FontWeight.Medium)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WebViewScreen(
    isDarkMode: Boolean = true
) {
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var retryTrigger by remember { mutableStateOf(0) }

    val webViewState = rememberWebViewState(url = "https://stockity.id")
    val navigator = rememberWebViewNavigator()

    LaunchedEffect(webViewState.errorsForCurrentRequest) {
        val errors = webViewState.errorsForCurrentRequest
        if (errors.isNotEmpty()) {
            showError = true
            errorMessage = "Terjadi kesalahan saat memuat halaman. Periksa koneksi internet Anda."
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WebView(
            state = webViewState,
            navigator = navigator,
            onCreated = { webView ->
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
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

                webView.webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            println("ðŸŒ JS Console: ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                        }
                        return true
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            client = AccompanistWebViewClient(),
            chromeClient = AccompanistWebChromeClient()
        )

        if (showError) {
            WebViewErrorDialog(
                error = errorMessage,
                onRetry = {
                    showError = false
                    retryTrigger++
                    navigator.reload()
                },
                onDismiss = {
                    showError = false
                }
            )
        }
    }
}