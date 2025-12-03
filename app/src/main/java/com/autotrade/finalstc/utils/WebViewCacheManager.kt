package com.autotrade.finalstc.utils

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Centralized WebView cache and cookie management
 */
object WebViewCacheManager {
    private const val TAG = "WebViewCacheManager"

    /**
     * Clear all WebView data (cache, cookies, storage)
     * Call this on logout
     */
    fun clearAllWebViewData(context: Context) {
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "🧹 CLEARING ALL WEBVIEW DATA")
        Log.d(TAG, "=" .repeat(60))

        try {
            // 1. Clear cookies
            clearAllCookies {
                Log.d(TAG, "✅ Cookies cleared")
            }

            // 2. Clear WebStorage (localStorage, sessionStorage, IndexedDB)
            clearWebStorage {
                Log.d(TAG, "✅ Web Storage cleared")
            }

            // 3. Clear cache from WebView instance
            clearWebViewCache(context)

            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "✅ ALL WEBVIEW DATA CLEARED")
            Log.d(TAG, "=" .repeat(60))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing WebView data: ${e.message}", e)
        }
    }

    /**
     * Clear all cookies
     */
    private fun clearAllCookies(onComplete: () -> Unit) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies { success ->
                Log.d(TAG, "Cookies removal: $success")
                cookieManager.flush()
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cookies: ${e.message}", e)
            onComplete()
        }
    }

    /**
     * Public method to clear cookies only (for external use)
     */
    fun clearCookiesOnly(onComplete: () -> Unit) {
        clearAllCookies(onComplete)
    }

    /**
     * Clear WebStorage (localStorage, sessionStorage, etc)
     */
    private fun clearWebStorage(onComplete: () -> Unit) {
        try {
            WebStorage.getInstance().deleteAllData()
            Log.d(TAG, "WebStorage deleted")
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebStorage: ${e.message}", e)
            onComplete()
        }
    }

    /**
     * Clear WebView cache
     */
    private fun clearWebViewCache(context: Context) {
        try {
            // Create temporary WebView to clear cache
            val webView = WebView(context)

            // Clear cache
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            webView.clearSslPreferences()

            // Clear application cache
            context.cacheDir.deleteRecursively()

            Log.d(TAG, "WebView cache cleared")

            // Destroy WebView
            webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView cache: ${e.message}", e)
        }
    }

    /**
     * Flush cookies to persistent storage
     */
    fun flushCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.flush()
            Log.d(TAG, "Cookies flushed to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing cookies: ${e.message}", e)
        }
    }

    /**
     * Check if user is logged in to Stockity
     */
    fun isStockityLoggedIn(url: String = "https://stockity.id"): Boolean {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url)
            val hasAuthToken = cookies?.contains("authtoken=") == true
            Log.d(TAG, "Stockity login status: $hasAuthToken")
            hasAuthToken
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status: ${e.message}", e)
            false
        }
    }

    /**
     * Get all cookies for a URL (for debugging)
     */
    fun getAllCookies(url: String = "https://stockity.id"): String? {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url)
            Log.d(TAG, "Cookies for $url: $cookies")
            cookies
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cookies: ${e.message}", e)
            null
        }
    }
}