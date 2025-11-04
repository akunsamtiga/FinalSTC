package com.autotrade.finalstc.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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

    private fun onNotificationPermissionGranted() {
    }

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