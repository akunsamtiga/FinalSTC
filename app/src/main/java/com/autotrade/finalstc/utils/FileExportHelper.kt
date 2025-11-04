package com.autotrade.finalstc.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
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