package com.autotrade.finalstc.utils

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView

fun WebView.extractAuthToken(): String? {
    return try {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(this.url)

        if (cookies != null) {
            val cookieArray = cookies.split(";")
            for (cookie in cookieArray) {
                val cookiePair = cookie.trim().split("=")
                if (cookiePair.size >= 2 && cookiePair[0] == "authtoken") {
                    Log.d("WebViewExtension", "Auth token found: ${cookiePair[1]}")
                    return cookiePair[1]
                }
            }
        }

        Log.w("WebViewExtension", "Auth token not found in cookies")
        null
    } catch (e: Exception) {
        Log.e("WebViewExtension", "Error extracting auth token: ${e.message}", e)
        null
    }
}

fun WebView.extractDeviceId(): String? {
    return try {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(this.url)

        if (cookies != null) {
            val cookieArray = cookies.split(";")
            for (cookie in cookieArray) {
                val cookiePair = cookie.trim().split("=")
                if (cookiePair.size >= 2 && cookiePair[0] == "device_id") {
                    Log.d("WebViewExtension", "Device ID found: ${cookiePair[1]}")
                    return cookiePair[1]
                }
            }
        }

        Log.w("WebViewExtension", "Device ID not found in cookies")
        null
    } catch (e: Exception) {
        Log.e("WebViewExtension", "Error extracting device ID: ${e.message}", e)
        null
    }
}

fun WebView.extractEmail(callback: (String?) -> Unit) {
    this.evaluateJavascript("""
        (function() {
            try {
                var email = localStorage.getItem('user_email') || 
                           localStorage.getItem('email') ||
                           sessionStorage.getItem('user_email') ||
                           sessionStorage.getItem('email');
                
                if (email) return email;
                
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = cookies[i].trim();
                    if (cookie.startsWith('email=')) {
                        return cookie.substring('email='.length);
                    }
                }
                
                return null;
            } catch (e) {
                console.error('Error extracting email: ' + e.message);
                return null;
            }
        })();
    """.trimIndent()) { result ->
        val email = result?.trim('"')?.takeIf { it != "null" && it.isNotEmpty() }
        callback(email)
    }
}

fun WebView.extractRegistrationData(callback: (RegistrationData?) -> Unit) {
    this.evaluateJavascript("""
        (function() {
            try {
                var data = {
                    authtoken: '',
                    deviceId: '',
                    email: ''
                };
                
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = cookies[i].trim();
                    if (cookie.startsWith('authtoken=')) {
                        data.authtoken = cookie.substring('authtoken='.length);
                    }
                    if (cookie.startsWith('device_id=')) {
                        data.deviceId = cookie.substring('device_id='.length);
                    }
                    if (cookie.startsWith('email=')) {
                        data.email = cookie.substring('email='.length);
                    }
                }
                
                if (!data.authtoken && typeof(Storage) !== 'undefined') {
                    data.authtoken = localStorage.getItem('authtoken') || '';
                    data.deviceId = localStorage.getItem('device_id') || '';
                    data.email = localStorage.getItem('email') || localStorage.getItem('user_email') || '';
                }
                
                return JSON.stringify(data);
            } catch (e) {
                console.error('Error extracting registration data: ' + e.message);
                return null;
            }
        })();
    """.trimIndent()) { result ->
        try {
            val jsonStr = result?.trim('"')?.replace("\\\"", "\"")
            if (jsonStr != null && jsonStr != "null") {
                val data = parseRegistrationData(jsonStr)
                callback(data)
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("WebViewExtension", "Error parsing registration data: ${e.message}", e)
            callback(null)
        }
    }
}

data class RegistrationData(
    val authToken: String,
    val deviceId: String,
    val email: String
)

private fun parseRegistrationData(jsonStr: String): RegistrationData? {
    return try {
        val authTokenRegex = """"authtoken":"([^"]+)"""".toRegex()
        val deviceIdRegex = """"deviceId":"([^"]+)"""".toRegex()
        val emailRegex = """"email":"([^"]+)"""".toRegex()

        val authToken = authTokenRegex.find(jsonStr)?.groupValues?.get(1) ?: ""
        val deviceId = deviceIdRegex.find(jsonStr)?.groupValues?.get(1) ?: ""
        val email = emailRegex.find(jsonStr)?.groupValues?.get(1) ?: ""

        if (authToken.isNotEmpty() || deviceId.isNotEmpty() || email.isNotEmpty()) {
            RegistrationData(authToken, deviceId, email)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("WebViewExtension", "Error parsing JSON: ${e.message}", e)
        null
    }
}

fun String.isValidAuthToken(): Boolean {
    return this.isNotEmpty() &&
            this.length >= 20 &&
            !this.contains(" ") &&
            this.matches(Regex("[a-zA-Z0-9-]+"))
}

fun String.isValidDeviceId(): Boolean {
    return this.isNotEmpty() &&
            this.length >= 20 &&
            !this.contains(" ") &&
            this.matches(Regex("[a-zA-Z0-9-]+"))
}

fun String.isValidEmail(): Boolean {
    return this.isNotEmpty() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}