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
        private const val TOPIC_NAME = "trading_signals"

        private var signalCallback: ((TelegramSignalData) -> Unit)? = null
        private var isAISignalModeActive: Boolean = false

        fun setSignalCallback(callback: (TelegramSignalData) -> Unit) {
            signalCallback = callback
            Log.d(TAG, "✅ Signal callback registered")
        }

        fun clearSignalCallback() {
            signalCallback = null
            Log.d(TAG, "🗑️ Signal callback cleared")
        }

        fun setAISignalModeActive(isActive: Boolean) {
            isAISignalModeActive = isActive
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "🔔 AI SIGNAL MODE STATUS UPDATED")
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "   Status: ${if (isActive) "ACTIVE ✅" else "INACTIVE ❌"}")
            Log.d(TAG, "   Notifications: ${if (isActive) "ENABLED ✅" else "DISABLED ❌"}")
            Log.d(TAG, "   FCM Messages: ${if (isActive) "WILL SHOW NOTIFICATION + PROCESS" else "ONLY PROCESS (NO NOTIFICATION)"}")
            Log.d(TAG, "   Background: ${if (isActive) "BOT RUNS IN BACKGROUND" else "NO BACKGROUND OPERATION"}")
            Log.d(TAG, "=" .repeat(60))
        }
        
        fun ensureTopicSubscription() {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic(TOPIC_NAME)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Successfully subscribed to topic: $TOPIC_NAME")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to subscribe to topic: ${e.message}")
                }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "🔄 FCM TOKEN UPDATED")
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "Token: $token")
        Log.d(TAG, "Token length: ${token.length}")
        Log.d(TAG, "Token preview: ${token.take(20)}...${token.takeLast(20)}")
        Log.d(TAG, "=" .repeat(60))

        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic(TOPIC_NAME)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Re-subscribed to '$TOPIC_NAME' topic with new token")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to re-subscribe: ${e.message}")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "📩 FCM MESSAGE RECEIVED")
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "Message ID: ${message.messageId}")
        Log.d(TAG, "Sent Time: ${message.sentTime}")
        Log.d(TAG, "Data size: ${message.data.size}")
        Log.d(TAG, "Has notification: ${message.notification != null}")

        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "🔔 NOTIFICATION CONTROL CHECK")
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "   AI Signal Mode Active: $isAISignalModeActive")
        Log.d(TAG, "   Will show notification: ${if (isAISignalModeActive) "YES ✅" else "NO ❌"}")
        Log.d(TAG, "   Will process signal: YES ✅ (always)")
        Log.d(TAG, "=" .repeat(60))

        message.notification?.let { notif ->
            Log.d(TAG, "Notification title: ${notif.title}")
            Log.d(TAG, "Notification body: ${notif.body}")
        }

        message.data.forEach { (key, value) ->
            Log.d(TAG, "Data[$key]: '$value' (type: ${value.javaClass.simpleName})")
        }

        val messageType = message.data["type"]

        if (messageType != "TRADING_SIGNAL") {
            Log.w(TAG, "⚠️ Unknown message type: $messageType")
            Log.w(TAG, "Expected: TRADING_SIGNAL")

            if (messageType == "TOKEN_CHECK") {
                Log.i(TAG, "ℹ️ This is a token check message - ignoring")
            }
            return
        }

        Log.d(TAG, "✅ Trading signal confirmed - Processing...")
        handleTradingSignal(message.data)

        Log.d(TAG, "=" .repeat(60))
    }

    private fun handleTradingSignal(data: Map<String, String>) {
        try {
            Log.d(TAG, "🔄 Processing trading signal...")

            // ✅ EXTRACT ALL TIME FIELDS INCLUDING SECOND
            val trend = data["trend"]
            val hasTimeStr = data["has_time"]
            val hourStr = data["hour"]
            val minuteStr = data["minute"]
            val secondStr = data["second"]  // ✅ TAMBAH: Extract second
            val originalMessage = data["original_message"] ?: ""
            val timestampStr = data["timestamp"]
            val formattedMessage = data["formatted_message"]
            val autoTimeAdded = data["auto_time_added"]

            Log.d(TAG, "📦 Extracted raw values:")
            Log.d(TAG, "   trend: '$trend'")
            Log.d(TAG, "   has_time: '$hasTimeStr'")
            Log.d(TAG, "   hour: '$hourStr'")
            Log.d(TAG, "   minute: '$minuteStr'")
            Log.d(TAG, "   second: '$secondStr'")  // ✅ TAMBAH: Log second
            Log.d(TAG, "   original: '$originalMessage'")
            Log.d(TAG, "   formatted: '$formattedMessage'")
            Log.d(TAG, "   auto_time_added: '$autoTimeAdded'")
            Log.d(TAG, "   timestamp: '$timestampStr'")

            if (trend == null) {
                Log.e(TAG, "❌ CRITICAL: Missing 'trend' field!")
                Log.e(TAG, "   Available keys: ${data.keys}")
                return
            }

            val hasTime = when (hasTimeStr?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> {
                    Log.w(TAG, "⚠️ Invalid has_time value: '$hasTimeStr', defaulting to false")
                    false
                }
            }

            Log.d(TAG, "   Parsed has_time: $hasTime")

            // ✅ PARSE HOUR
            val hour = try {
                hourStr?.toIntOrNull()?.also {
                    Log.d(TAG, "   Parsed hour: $it")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing hour '$hourStr': ${e.message}")
                null
            }

            // ✅ PARSE MINUTE
            val minute = try {
                minuteStr?.toIntOrNull()?.also {
                    Log.d(TAG, "   Parsed minute: $it")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing minute '$minuteStr': ${e.message}")
                null
            }

            // ✅ PARSE SECOND (NEW)
            val second = try {
                secondStr?.toIntOrNull()?.also {
                    Log.d(TAG, "   Parsed second: $it")
                } ?: 0  // Default to 0 if not provided
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error parsing second '$secondStr': ${e.message}, defaulting to 0")
                0
            }

            // ✅ VALIDATION
            if (hasTime && (hour == null || minute == null)) {
                Log.e(TAG, "❌ CRITICAL ERROR: has_time=true but hour/minute is null!")
                Log.e(TAG, "   hour: $hour")
                Log.e(TAG, "   minute: $minute")
                Log.e(TAG, "   second: $second")
                return
            }

            if (hour != null && minute != null) {
                if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) {
                    Log.e(TAG, "❌ Invalid time range: $hour:$minute:$second")
                    return
                }
            }

            val timestamp = try {
                timestampStr?.toLongOrNull() ?: System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error parsing timestamp: ${e.message}")
                System.currentTimeMillis()
            }

            Log.d(TAG, "✅ Data validation passed")

            // ✅ CREATE SIGNAL DATA WITH SECOND
            val signalData = TelegramSignalData(
                trend = trend,
                hasTime = hasTime,
                hour = hour,
                minute = minute,
                second = second,  // ✅ TAMBAH: Include second
                originalMessage = originalMessage,
                receivedAt = timestamp
            )

            Log.d(TAG, "📊 Signal created:")
            Log.d(TAG, "   trend: ${signalData.trend}")
            Log.d(TAG, "   hasTime: ${signalData.hasTime}")
            Log.d(TAG, "   hour: ${signalData.hour}")
            Log.d(TAG, "   minute: ${signalData.minute}")
            Log.d(TAG, "   second: ${signalData.second}")  // ✅ TAMBAH: Log second
            Log.d(TAG, "   formatted time: ${hour?.let { h ->
                minute?.let { m ->
                    String.format("%02d:%02d:%02d", h, m, second)  // ✅ INCLUDE SECOND
                }
            } ?: "N/A"}")

            // ✅ NOTIFICATION DECISION
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "🔔 NOTIFICATION DECISION")
            Log.d(TAG, "=" .repeat(60))
            if (isAISignalModeActive) {
                Log.d(TAG, "   ✅ AI Signal Mode ACTIVE")
                Log.d(TAG, "   ✅ Showing notification to user")
                showSignalNotification(signalData)
            } else {
                Log.d(TAG, "   ❌ AI Signal Mode INACTIVE")
                Log.d(TAG, "   ❌ Skipping notification (silent processing)")
                Log.d(TAG, "   💡 Signal will still be forwarded to callback")
            }
            Log.d(TAG, "=" .repeat(60))

            // ✅ FORWARD TO CALLBACK
            if (signalCallback != null) {
                Log.d(TAG, "🎯 Invoking signal callback...")
                signalCallback?.invoke(signalData)
                Log.d(TAG, "✅ Callback invoked successfully")
            } else {
                Log.w(TAG, "⚠️ Signal callback is NULL")
                Log.w(TAG, "   Signal received but not processed by app")
            }

            Log.d(TAG, "✅ SIGNAL PROCESSED SUCCESSFULLY")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing signal: ${e.message}", e)
            e.printStackTrace()
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
                .setSmallIcon(R.drawable.ic_mode_6)
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

            Log.d(TAG, "✅ Notification shown: $content")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification: ${e.message}", e)
        }
    }

    private fun buildNotificationContent(signal: TelegramSignalData): String {
        return buildString {
            append("${signal.trend.uppercase()}: ${signal.originalMessage}")

            if (signal.hasTime && signal.hour != null && signal.minute != null) {
                // ✅ INCLUDE SECOND IN NOTIFICATION
                append("\n⏰ Execute at: ${String.format("%02d:%02d:%02d", signal.hour, signal.minute, signal.second)}")
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

// ✅ UPDATED DATA CLASS WITH SECOND FIELD
data class TelegramSignalData(
    val trend: String,
    val hasTime: Boolean,
    val hour: Int?,
    val minute: Int?,
    val second: Int = 0,  // ✅ TAMBAH: Default to 0 for backward compatibility
    val originalMessage: String,
    val receivedAt: Long
)