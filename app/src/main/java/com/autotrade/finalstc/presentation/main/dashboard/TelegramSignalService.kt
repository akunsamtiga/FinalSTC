package com.autotrade.finalstc.presentation.main.dashboard

import android.content.Context
import android.util.Log
import com.autotrade.finalstc.service.TelegramSignalData
import com.autotrade.finalstc.service.TradingSignalMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramSignalService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var scope: CoroutineScope? = null
    private var onSignalReceived: ((TelegramSignal) -> Unit)? = null
    private var onStatusUpdate: ((String) -> Unit)? = null
    private var serverTimeService: ServerTimeService? = null
    private var isActive = false
    private var fcmToken: String? = null

    companion object {
        private const val TAG = "TelegramSignalService"
    }

    fun initialize(
        scope: CoroutineScope,
        onSignalReceived: (TelegramSignal) -> Unit,
        onStatusUpdate: (String) -> Unit,
        serverTimeService: ServerTimeService
    ) {
        this.scope = scope
        this.onSignalReceived = onSignalReceived
        this.onStatusUpdate = onStatusUpdate
        this.serverTimeService = serverTimeService

        // Setup FCM callback
        TradingSignalMessagingService.setSignalCallback { fcmSignal ->
            handleFCMSignal(fcmSignal)
        }
    }

    fun startMonitoring() {
        if (isActive) {
            Log.w(TAG, "⚠️ Monitoring already active")
            return
        }

        scope?.launch {
            try {
                isActive = true
                onStatusUpdate?.invoke("🔄 Subscribing to FCM...")

                // Get FCM token
                fcmToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "✅ FCM Token obtained: $fcmToken")

                // Subscribe to topic
                FirebaseMessaging.getInstance()
                    .subscribeToTopic("trading_signals")
                    .await()

                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "🎧 TELEGRAM SIGNAL SERVICE (FCM MODE)")
                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "✅ Subscribed to: trading_signals")
                Log.d(TAG, "📡 FCM Token: ${fcmToken?.take(20)}...")
                Log.d(TAG, "🎯 Ready to receive signals from Python bridge")
                Log.d(TAG, "=" .repeat(60))

                onStatusUpdate?.invoke("✅ Connected - Listening via FCM")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting monitoring: ${e.message}", e)
                isActive = false
                onStatusUpdate?.invoke("❌ Connection failed: ${e.message}")
            }
        }
    }

    fun stopMonitoring() {
        if (!isActive) return

        scope?.launch {
            try {
                // Unsubscribe from topic
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("trading_signals")
                    .await()

                Log.d(TAG, "✅ Unsubscribed from trading_signals")

            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error unsubscribing: ${e.message}")
            } finally {
                isActive = false
                onStatusUpdate?.invoke("⏹️ Monitoring stopped")
            }
        }
    }

    private fun handleFCMSignal(fcmSignal: TelegramSignalData) {
        try {
            Log.d(TAG, "🎯 Processing FCM signal...")
            Log.d(TAG, "   Trend: ${fcmSignal.trend}")
            Log.d(TAG, "   Has time: ${fcmSignal.hasTime}")

            val executionTime = if (fcmSignal.hasTime && fcmSignal.hour != null && fcmSignal.minute != null) {
                calculateExecutionTimeForSpecifiedTime(fcmSignal.hour, fcmSignal.minute, fcmSignal.receivedAt)
            } else {
                calculateExecutionTimeFromNow(fcmSignal.receivedAt)
            }

            val signal = TelegramSignal(
                trend = fcmSignal.trend,
                receivedAt = fcmSignal.receivedAt,
                executionTime = executionTime,
                originalMessage = fcmSignal.originalMessage,
                hasSpecificTime = fcmSignal.hasTime,
                specifiedHour = fcmSignal.hour,
                specifiedMinute = fcmSignal.minute
            )

            Log.d(TAG, "✅ Signal processed:")
            Log.d(TAG, "   Execution time: ${formatTime(executionTime)}")
            Log.d(TAG, "   Delay: ${signal.getDelaySeconds()}s")

            onSignalReceived?.invoke(signal)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling FCM signal: ${e.message}", e)
        }
    }

    private fun calculateExecutionTimeForSpecifiedTime(
        hour: Int,
        minute: Int,
        messageTimestamp: Long
    ): Long {
        val serverTime = serverTimeService?.getCurrentServerTimeMillis() ?: System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = serverTime

        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        var executionTime = calendar.timeInMillis

        // If time already passed today, execute tomorrow
        if (executionTime < messageTimestamp) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            executionTime = calendar.timeInMillis
        }

        return executionTime
    }

    private fun calculateExecutionTimeFromNow(messageTimestamp: Long): Long {
        val serverTime = serverTimeService?.getCurrentServerTimeMillis() ?: System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = serverTime

        val currentSecond = calendar.get(java.util.Calendar.SECOND)

        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val minutesToAdd = if (currentSecond < 30) 1 else 2

        calendar.add(java.util.Calendar.MINUTE, minutesToAdd)

        return calendar.timeInMillis
    }

    fun injectTestSignal(signalText: String) {
        // For testing purposes
        val testData = TelegramSignalData(
            trend = if (signalText.contains("B", ignoreCase = true)) "call" else "put",
            hasTime = false,
            hour = null,
            minute = null,
            originalMessage = signalText,
            receivedAt = System.currentTimeMillis()
        )
        handleFCMSignal(testData)
    }

    fun isMonitoring(): Boolean = isActive

    fun getStatus(): Map<String, Any> {
        return mapOf(
            "is_active" to isActive,
            "connection_type" to "FCM",
            "fcm_token" to (fcmToken?.take(20) ?: "N/A"),
            "topic" to "trading_signals"
        )
    }

    private fun formatTime(timeMillis: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )
    }

    fun cleanup() {
        stopMonitoring()
        TradingSignalMessagingService.clearSignalCallback()
    }
}

data class TelegramSignal(
    val trend: String,
    val receivedAt: Long,
    val executionTime: Long,
    val originalMessage: String,
    val hasSpecificTime: Boolean,
    val specifiedHour: Int? = null,
    val specifiedMinute: Int? = null
) {
    fun getDelaySeconds(): Long = (executionTime - receivedAt) / 1000

    fun getFormattedExecutionTime(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = executionTime
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )
    }
}