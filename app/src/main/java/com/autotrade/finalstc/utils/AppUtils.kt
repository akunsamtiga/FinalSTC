package com.autotrade.finalstc.utils

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ==================== FILE EXPORT HELPER ====================
class FileExportHelper(private val context: Context) {

    companion object {
        private const val TAG = "FileExportHelper"
        private const val AUTHORITY = "com.autotrade.finalstc.fileprovider"
    }

    fun exportJsonToDownload(jsonData: String, fileName: String = "whitelist_export"): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fullFileName = "${fileName}_$timestamp.json"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(jsonData, fullFileName, "application/json")
            } else {
                saveToDownloads(jsonData, fullFileName)
            }

            showToast("File berhasil disimpan di Downloads/$fullFileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting JSON: ${e.message}", e)
            showToast("Gagal menyimpan file: ${e.message}")
            false
        }
    }

    fun exportCsvToDownload(csvData: String, fileName: String = "whitelist_export"): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fullFileName = "${fileName}_$timestamp.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(csvData, fullFileName, "text/csv")
            } else {
                saveToDownloads(csvData, fullFileName)
            }

            showToast("File berhasil disimpan di Downloads/$fullFileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV: ${e.message}", e)
            showToast("Gagal menyimpan file: ${e.message}")
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(data: String, fileName: String, mimeType: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(data.toByteArray())
                outputStream.flush()
            }
            Log.d(TAG, "File saved to: $uri")
        } ?: throw Exception("Gagal membuat file di MediaStore")
    }

    private fun saveToDownloads(data: String, fileName: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(data.toByteArray())
            outputStream.flush()
        }

        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(file)
        context.sendBroadcast(intent)

        Log.d(TAG, "File saved to: ${file.absolutePath}")
    }

    fun shareFile(data: String, fileName: String, mimeType: String = "application/json"): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(data.toByteArray())
                outputStream.flush()
            }

            val uri = FileProvider.getUriForFile(context, AUTHORITY, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Export Whitelist Data")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            showToast("Gagal berbagi file: ${e.message}")
            false
        }
    }

    fun readJsonFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: ${e.message}", e)
            showToast("Gagal membaca file: ${e.message}")
            null
        }
    }

    fun copyToClipboard(text: String, label: String = "Export Data"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("Data berhasil disalin ke clipboard")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard: ${e.message}", e)
            showToast("Gagal menyalin data")
            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

// ==================== WEBVIEW EXTENSIONS ====================
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
            Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// ==================== WEBVIEW CONFIG MANAGER ====================
object WebViewConfigManager {
    private const val TAG = "WebViewConfig"

    fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true
            allowContentAccess = true

            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            minimumFontSize = 8
            defaultFontSize = 16
            textZoom = 100

            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false

            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991B) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        configureCookieManager(webView)
        Log.d(TAG, "WebView configured with modern caching")
    }

    private fun configureCookieManager(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.flush()
        Log.d(TAG, "Cookie Manager configured with persistence")
    }

    @Deprecated("Use WebViewCacheManager.clearCookiesOnly() instead",
        ReplaceWith("WebViewCacheManager.clearCookiesOnly(onComplete)"))
    fun clearAllCookies(onComplete: () -> Unit = {}) {
        WebViewCacheManager.clearCookiesOnly { onComplete() }
    }

    @Deprecated("Use WebViewCacheManager.clearAllWebViewData() instead")
    fun clearCache(webView: WebView) {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        Log.d(TAG, "Cache cleared")
    }

    fun getAllCookies(url: String = "https://stockity.id"): String? {
        return WebViewCacheManager.getAllCookies(url)
    }

    fun isUserLoggedIn(url: String = "https://stockity.id"): Boolean {
        return WebViewCacheManager.isStockityLoggedIn(url)
    }

    fun flushCookies() {
        WebViewCacheManager.flushCookies()
    }
}

// ==================== PERMISSIONS HANDLER ====================
class PermissionsHandler(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "PermissionsHandler"
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value

            Log.d(TAG, "Permission $permission: ${if (isGranted) "GRANTED" else "DENIED"}")

            when (permission) {
                Manifest.permission.POST_NOTIFICATIONS -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted - Trading alerts enabled")
                        onNotificationPermissionGranted()
                    } else {
                        Log.w(TAG, "Notification permission denied - Trading alerts may not work")
                        onNotificationPermissionDenied()
                    }
                }
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO -> {
                    if (isGranted) {
                        Log.d(TAG, "Media permission granted")
                        onMediaPermissionGranted()
                    }
                }
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (isGranted) {
                        Log.d(TAG, "Storage permission granted (legacy)")
                        onStoragePermissionGranted()
                    }
                }
            }
        }
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d(TAG, "Requesting notification permission for trading alerts")
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (!hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting ${permissionsToRequest.size} permissions: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "All required permissions already granted")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onNotificationPermissionGranted() {}

    private fun onNotificationPermissionDenied() {
        Log.w(TAG, "Trading alerts disabled - notification permission required")
    }

    private fun onMediaPermissionGranted() {
        Log.d(TAG, "Media access enabled - chart saving/sharing available")
    }

    private fun onStoragePermissionGranted() {
        Log.d(TAG, "Storage access enabled (legacy)")
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }
}

// ==================== IMAGE LOADER CONFIG ====================
object ImageLoaderConfig {

    fun createImageLoader(context: Context): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .crossfade(300)
            .build()
    }
}

// ==================== CURRENCY HELPER ====================
object CurrencyHelper {

    fun formatCurrency(amount: Long, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)
        val formatter = when (currencyIso) {
            "IDR" -> NumberFormat.getNumberInstance(Locale("id", "ID"))
            "USD" -> NumberFormat.getNumberInstance(Locale.US)
            "EUR" -> NumberFormat.getNumberInstance(Locale.GERMANY)
            else -> NumberFormat.getNumberInstance(Locale.getDefault())
        }
        return "$unit ${formatter.format(amount)}"
    }

    fun formatCurrency(amount: Double, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)
        val formatter = when (currencyIso) {
            "IDR" -> NumberFormat.getNumberInstance(Locale("id", "ID"))
            "USD" -> NumberFormat.getNumberInstance(Locale.US)
            "EUR" -> NumberFormat.getNumberInstance(Locale.GERMANY)
            else -> NumberFormat.getNumberInstance(Locale.getDefault())
        }
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return "$unit ${formatter.format(amount)}"
    }

    fun getCurrencySymbol(currencyIso: String): String {
        return when (currencyIso.uppercase()) {
            "IDR" -> "Rp"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "CHF" -> "Fr"
            "CNY" -> "¥"
            "SGD" -> "S$"
            "MYR" -> "RM"
            "THB" -> "฿"
            "PHP" -> "₱"
            "VND" -> "₫"
            else -> currencyIso
        }
    }

    fun getCurrencyName(currencyIso: String): String {
        return when (currencyIso.uppercase()) {
            "IDR" -> "Indonesian Rupiah"
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "GBP" -> "British Pound"
            "JPY" -> "Japanese Yen"
            "AUD" -> "Australian Dollar"
            "CAD" -> "Canadian Dollar"
            "CHF" -> "Swiss Franc"
            "CNY" -> "Chinese Yuan"
            "SGD" -> "Singapore Dollar"
            "MYR" -> "Malaysian Ringgit"
            "THB" -> "Thai Baht"
            "PHP" -> "Philippine Peso"
            "VND" -> "Vietnamese Dong"
            else -> currencyIso
        }
    }

    fun formatCompactCurrency(amount: Long, currencyIso: String): String {
        val unit = getCurrencySymbol(currencyIso)

        return when {
            amount >= 1_000_000_000 -> {
                val billions = amount / 1_000_000_000.0
                "$unit %.1fB".format(billions)
            }
            amount >= 1_000_000 -> {
                val millions = amount / 1_000_000.0
                "$unit %.1fM".format(millions)
            }
            amount >= 1_000 -> {
                val thousands = amount / 1_000.0
                "$unit %.1fK".format(thousands)
            }
            else -> formatCurrency(amount, currencyIso)
        }
    }

    fun parseCurrency(formattedAmount: String): Long? {
        return try {
            val cleanAmount = formattedAmount.replace(Regex("[^0-9]"), "")
            cleanAmount.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

// Extension Functions for Currency
fun Long.toCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCurrency(this, currencyIso)
}

fun Double.toCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCurrency(this, currencyIso)
}

fun Long.toCompactCurrency(currencyIso: String = "IDR"): String {
    return CurrencyHelper.formatCompactCurrency(this, currencyIso)
}