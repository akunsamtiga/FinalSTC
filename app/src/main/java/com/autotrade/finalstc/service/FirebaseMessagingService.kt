package com.autotrade.finalstc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autotrade.finalstc.MainActivity
import com.autotrade.finalstc.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TradingSignalMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_TradingSignal"
        private const val CHANNEL_ID = "trading_signals"
        private const val CHANNEL_NAME = "Trading Signals"

        private var signalCallback: ((TelegramSignalData) -> Unit)? = null

        fun setSignalCallback(callback: (TelegramSignalData) -> Unit) {
            signalCallback = callback
        }

        fun clearSignalCallback() {
            signalCallback = null
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "✅ New FCM token: $token")

        // Subscribe to trading signals topic
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("trading_signals")
            .addOnSuccessListener {
                Log.d(TAG, "✅ Subscribed to trading_signals topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to subscribe to topic: ${e.message}")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📩 FCM Message received")
        Log.d(TAG, "   From: ${message.from}")
        Log.d(TAG, "   Data: ${message.data}")

        // Check if message is a trading signal
        val messageType = message.data["type"]

        if (messageType == "TRADING_SIGNAL") {
            handleTradingSignal(message.data)
        } else {
            Log.w(TAG, "⚠️ Unknown message type: $messageType")
        }
    }

    private fun handleTradingSignal(data: Map<String, String>) {
        try {
            Log.d(TAG, "🎯 Processing trading signal...")

            val trend = data["trend"] ?: return
            val hasTime = data["has_time"]?.toBoolean() ?: false
            val hour = data["hour"]?.toIntOrNull()
            val minute = data["minute"]?.toIntOrNull()
            val originalMessage = data["original_message"] ?: ""
            val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

            Log.d(TAG, "   Trend: ${trend.uppercase()}")
            Log.d(TAG, "   Has time: $hasTime")
            if (hasTime && hour != null && minute != null) {
                Log.d(TAG, "   Execute at: ${hour}:${minute}")
            }
            Log.d(TAG, "   Message: $originalMessage")

            val signalData = TelegramSignalData(
                trend = trend,
                hasTime = hasTime,
                hour = hour,
                minute = minute,
                originalMessage = originalMessage,
                receivedAt = timestamp
            )

            // Notify callback
            signalCallback?.invoke(signalData)

            // Show notification
            showSignalNotification(signalData)

            Log.d(TAG, "✅ Trading signal processed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing trading signal: ${e.message}", e)
        }
    }

    private fun showSignalNotification(signal: TelegramSignalData) {
        try {
            createNotificationChannel()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_ai_signal", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = "🎯 New Trading Signal"
            val content = buildNotificationContent(signal)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            Log.d(TAG, "✅ Notification shown")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification: ${e.message}", e)
        }
    }

    private fun buildNotificationContent(signal: TelegramSignalData): String {
        return buildString {
            append("${signal.trend.uppercase()}: ${signal.originalMessage}")

            if (signal.hasTime && signal.hour != null && signal.minute != null) {
                append("\n⏰ Execute at: ${signal.hour}:${String.format("%02d", signal.minute)}")
            } else {
                append("\n⏰ Execute at next minute boundary")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for trading signals from Telegram"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

data class TelegramSignalData(
    val trend: String,
    val hasTime: Boolean,
    val hour: Int?,
    val minute: Int?,
    val originalMessage: String,
    val receivedAt: Long
)