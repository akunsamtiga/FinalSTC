package com.autotrade.finalstc.utils

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.util.Log

object WebViewConfigManager {
    private const val TAG = "WebViewConfig"

    /**
     * Configure WebView dengan settings yang optimal untuk caching dan state preservation
     */
    fun configureWebView(webView: WebView) {
        webView.settings.apply {
            // JavaScript & DOM Storage
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // ✅ MODERN CACHE SETTINGS untuk state preservation
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK // Prioritaskan cache
            allowFileAccess = true
            allowContentAccess = true

            // ✅ OPTIMAL CACHE CONFIGURATION
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true

            // Layout & Display
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Font Settings
            minimumFontSize = 8
            defaultFontSize = 16
            textZoom = 100

            // Security & Content
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false

            // ✅ OPTIMIZED USER AGENT
            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991B) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        // Configure Cookie Manager - KUNCI untuk sharing session
        configureCookieManager(webView)

        Log.d(TAG, "WebView configured with modern caching")
    }

    /**
     * Configure Cookie Manager untuk persistent cookies
     */
    private fun configureCookieManager(webView: WebView) {
        val cookieManager = CookieManager.getInstance()

        // Enable cookies dan persistent storage
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // ✅ FORCE FLUSH COOKIES ke persistent storage
        cookieManager.flush()

        Log.d(TAG, "Cookie Manager configured with persistence")
    }

    /**
     * Clear semua cookies (untuk logout/reset)
     */
    fun clearAllCookies(onComplete: () -> Unit = {}) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { success ->
            Log.d(TAG, "Cookies cleared: $success")
            onComplete()
        }
    }

    /**
     * Clear cache dari WebView
     */
    fun clearCache(webView: WebView) {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Get all cookies untuk debugging
     */
    fun getAllCookies(url: String = "https://stockity.id"): String? {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        Log.d(TAG, "Cookies for $url: $cookies")
        return cookies
    }

    /**
     * Check apakah user sudah login (ada authtoken cookie)
     */
    fun isUserLoggedIn(url: String = "https://stockity.id"): Boolean {
        val cookies = getAllCookies(url)
        val hasAuthToken = cookies?.contains("authtoken=") == true
        Log.d(TAG, "User logged in: $hasAuthToken")
        return hasAuthToken
    }

    /**
     * Force flush cookies to persistent storage
     */
    fun flushCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        Log.d(TAG, "Cookies flushed to storage")
    }
}
