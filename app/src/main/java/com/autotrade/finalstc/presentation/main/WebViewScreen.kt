package com.autotrade.finalstc.presentation.main.webview

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.autotrade.finalstc.utils.WebViewConfigManager
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

private val DarkBackground = Color(0xFF1B1B1B)

@Composable
fun WebViewScreen(
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // ✅ IMPROVED: Gunakan rememberWebViewState dengan URL tetap
    val webViewState = rememberWebViewState(url = "https://stockity.id/trading")

    // ✅ TRACK VISIBILITY STATE dengan LaunchedEffect yang lebih baik
    var isScreenVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Log.d("WebViewScreen", "🔄 WebViewScreen Composed - Page Title: ${webViewState.pageTitle}, Loading: ${webViewState.isLoading}")

        // Log initial state
        if (webViewState.pageTitle != null && !webViewState.isLoading) {
            Log.d("WebViewScreen", "✅ Resuming existing WebView state")
        }
    }

    // ✅ OPTIMIZED: Gunakan DisposableEffect untuk lifecycle management
    DisposableEffect(Unit) {
        Log.d("WebViewScreen", "👁️ WebViewScreen BECAME VISIBLE")
        isScreenVisible = true

        onDispose {
            Log.d("WebViewScreen", "👻 WebViewScreen BECAME HIDDEN - State preserved")
            isScreenVisible = false
            // ✅ JANGAN clear cache/cookies di sini agar state tetap terjaga
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ✅ RENDER WEBVIEW - Accompanist WebView sudah handle visibility secara internal
        WebView(
            state = webViewState,
            onCreated = { webView ->
                Log.d("WebViewScreen", "🌐 WebView Created - Current URL: ${webViewState.lastLoadedUrl}")

                // Konfigurasi WebView dengan cache yang optimal
                WebViewConfigManager.configureWebView(webView)

                // ✅ CEK STATE SAAT INI
                if (webViewState.pageTitle != null && !webViewState.isLoading) {
                    Log.d("WebViewScreen", "✅ WebView state preserved - no reload needed")
                } else {
                    Log.d("WebViewScreen", "🔄 WebView will load URL")
                }

                // Log cookies untuk debugging
                WebViewConfigManager.getAllCookies("https://stockity.id")
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
