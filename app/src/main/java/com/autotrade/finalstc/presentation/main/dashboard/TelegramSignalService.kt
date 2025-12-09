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
    private var scope: CoroutineScope? = kotlinx.coroutines.GlobalScope
    private var onSignalReceived: ((TelegramSignal) -> Unit)? = null
    private var onStatusUpdate: ((String) -> Unit)? = null
    private var serverTimeService: ServerTimeService? = null
    private var isActive = false
    private var fcmToken: String? = null

    companion object {
        private const val TAG = "TelegramSignalService"
        private const val TOPIC_NAME = "trading_signals"
    }

    fun isMonitoringActive(): Boolean = isActive

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

        TradingSignalMessagingService.setSignalCallback { fcmSignal ->
            handleFCMSignal(fcmSignal)
        }

        Log.d(TAG, "✅ TelegramSignalService initialized")
    }

    fun startMonitoring() {
        if (isActive) {
            Log.w(TAG, "⚠️ Monitoring already active, re-validating...")
            // Re-validate subscription
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    FirebaseMessaging.getInstance()
                        .subscribeToTopic(TOPIC_NAME)
                        .await()
                    Log.d(TAG, "✅ Re-validated topic subscription")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to re-validate: ${e.message}")
                }
            }
            return
        }

        // ✅ CHANGE: Use GlobalScope
        kotlinx.coroutines.GlobalScope.launch {
            try {
                isActive = true
                onStatusUpdate?.invoke("🔄 Initializing FCM...")

                fcmToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "📱 FCM TOKEN RETRIEVED (GLOBAL SCOPE)")
                Log.d(TAG, "=" .repeat(60))
                Log.d(TAG, "Token: $fcmToken")
                Log.d(TAG, "Scope: BACKGROUND (survives lifecycle)")
                Log.d(TAG, "=" .repeat(60))

                onStatusUpdate?.invoke("🔄 Subscribing to topic...")

                FirebaseMessaging.getInstance()
                    .subscribeToTopic(TOPIC_NAME)
                    .await()

                Log.d(TAG, "✅ Subscribed to topic: $TOPIC_NAME (BACKGROUND)")
                onStatusUpdate?.invoke("✅ Connected - Listening via FCM (Background)")

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
                onStatusUpdate?.invoke("🔄 Unsubscribing from topic...")

                // ✅ CRITICAL: Unsubscribe from FCM topic
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(TOPIC_NAME)
                    .await()

                Log.d(TAG, "✅ Unsubscribed from $TOPIC_NAME")

                // ✅ TAMBAH: Clear FCM token if callback is set
                try {
                    Log.d(TAG, "🔕 Clearing local FCM token cache...")
                    fcmToken = null
                    Log.d(TAG, "✅ Local FCM token cleared")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error clearing local token: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error unsubscribing: ${e.message}")
            } finally {
                isActive = false
                onStatusUpdate?.invoke("ℹ️ Monitoring stopped")
            }
        }
    }

    private fun handleFCMSignal(fcmSignal: TelegramSignalData) {
        try {
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "📩 FCM SIGNAL RECEIVED (DETAILED)")
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "   Trend: ${fcmSignal.trend}")
            Log.d(TAG, "   Has time: ${fcmSignal.hasTime}")
            Log.d(TAG, "   Hour: ${fcmSignal.hour}")
            Log.d(TAG, "   Minute: ${fcmSignal.minute}")
            Log.d(TAG, "   Second: ${fcmSignal.second}")
            Log.d(TAG, "   Original: ${fcmSignal.originalMessage}")
            Log.d(TAG, "   Received at: ${fcmSignal.receivedAt}")
            Log.d(TAG, "=" .repeat(60))

            if (fcmSignal.hasTime && (fcmSignal.hour == null || fcmSignal.minute == null)) {
                Log.e(TAG, "❌ CRITICAL ERROR: hasTime=true but hour/minute is null!")
                Log.e(TAG, "   hour: ${fcmSignal.hour}")
                Log.e(TAG, "   minute: ${fcmSignal.minute}")
                return
            }

            // ✅ CALCULATE EXECUTION TIME WITH SECOND
            val executionTime = if (fcmSignal.hasTime &&
                fcmSignal.hour != null &&
                fcmSignal.minute != null) {
                Log.d(TAG, "📅 Using specified time: ${fcmSignal.hour}:${fcmSignal.minute}:${fcmSignal.second}")
                calculateExecutionTimeForSpecifiedTime(
                    fcmSignal.hour,
                    fcmSignal.minute,
                    fcmSignal.second,
                    fcmSignal.receivedAt
                )
            } else {
                Log.d(TAG, "📅 Using next minute boundary logic")
                calculateExecutionTimeFromNow(fcmSignal.receivedAt)
            }

            val signal = TelegramSignal(
                trend = fcmSignal.trend,
                receivedAt = fcmSignal.receivedAt,
                executionTime = executionTime,
                originalMessage = fcmSignal.originalMessage,
                hasSpecificTime = fcmSignal.hasTime,
                specifiedHour = fcmSignal.hour,
                specifiedMinute = fcmSignal.minute,
                specifiedSecond = fcmSignal.second
            )

            Log.d(TAG, "✅ Signal processed successfully:")
            Log.d(TAG, "   Received: ${formatTime(signal.receivedAt)}")
            Log.d(TAG, "   Execute: ${formatTime(signal.executionTime)}")
            Log.d(TAG, "   Delay: ${signal.getDelaySeconds()}s")
            Log.d(TAG, "   Formatted: ${signal.getFormattedExecutionTime()}")
            Log.d(TAG, "=" .repeat(60))

            onSignalReceived?.invoke(signal)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling FCM signal: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun calculateExecutionTimeForSpecifiedTime(
        hour: Int,
        minute: Int,
        second: Int,
        messageTimestamp: Long
    ): Long {
        val serverTime = serverTimeService?.getCurrentServerTimeMillis() ?: System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = serverTime

        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, second)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        var executionTime = calendar.timeInMillis

        if (executionTime < messageTimestamp) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            executionTime = calendar.timeInMillis
            Log.d(TAG, "   ⚠️ Time already passed, scheduling for tomorrow")
        }

        Log.d(TAG, "   📅 Execution scheduled at: ${formatTime(executionTime)}")
        Log.d(TAG, "      Hour: $hour, Minute: $minute, Second: $second")

        return executionTime
    }

    // ✅ FIXED: Eksekusi di :00 detik, bukan :59
    private fun calculateExecutionTimeFromNow(messageTimestamp: Long): Long {
        val serverTime = serverTimeService?.getCurrentServerTimeMillis() ?: System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = serverTime

        val currentSecond = calendar.get(java.util.Calendar.SECOND)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        Log.d(TAG, "📊 AI SIGNAL EXECUTION TIME CALCULATION (FIXED):")
        Log.d(TAG, "   Current time: ${formatTime(serverTime)}")
        Log.d(TAG, "   Current: $currentHour:$currentMinute:$currentSecond")

        // ✅ LOGIC BARU:
        // - Jika sisa waktu ke akhir menit >= 30 detik → eksekusi di :00 menit berikutnya
        // - Jika sisa waktu ke akhir menit < 30 detik → eksekusi di :00 menit setelahnya

        val secondsToNextMinuteEnd = 60 - currentSecond

        Log.d(TAG, "   Seconds to next minute end: $secondsToNextMinuteEnd")

        val minutesToAdd = if (secondsToNextMinuteEnd >= 30) {
            // ✅ Contoh: 12:32:21 → sisa 39 detik → eksekusi di 12:33:00
            Log.d(TAG, "   Sisa waktu >= 30s ($secondsToNextMinuteEnd detik)")
            Log.d(TAG, "   → Eksekusi di :00 menit berikutnya")
            1
        } else {
            // ✅ Contoh: 12:31:52 → sisa 8 detik → eksekusi di 12:33:00
            Log.d(TAG, "   Sisa waktu < 30s ($secondsToNextMinuteEnd detik)")
            Log.d(TAG, "   → Eksekusi di :00 menit setelahnya (skip 1 menit)")
            2
        }

        // ✅ SET EXECUTION TIME AT :00 (bukan :59)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.add(java.util.Calendar.MINUTE, minutesToAdd)

        val executionTime = calendar.timeInMillis
        val executionHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val executionMinute = calendar.get(java.util.Calendar.MINUTE)
        val executionSecond = calendar.get(java.util.Calendar.SECOND)

        Log.d(TAG, "📅 Execution schedule (FIXED):")
        Log.d(TAG, "   Target minute: +$minutesToAdd")
        Log.d(TAG, "   Execute at: ${formatTime(executionTime)} ($executionHour:$executionMinute:$executionSecond)")
        Log.d(TAG, "   Delay: ${(executionTime - serverTime) / 1000}s")

        // ✅ VERIFICATION LOG
        Log.d(TAG, "=" .repeat(60))
        Log.d(TAG, "🔍 VERIFICATION:")
        Log.d(TAG, "   Input: $currentHour:$currentMinute:$currentSecond")
        Log.d(TAG, "   Output: $executionHour:$executionMinute:$executionSecond")

        when {
            currentSecond in 0..29 -> {
                // Sisa >= 30 detik → next minute :00
                val expectedMinute = (currentMinute + 1) % 60
                if (executionMinute == expectedMinute && executionSecond == 0) {
                    Log.d(TAG, "   ✅ CORRECT: Next minute :00")
                } else {
                    Log.e(TAG, "   ❌ ERROR: Expected $expectedMinute:00, got $executionMinute:$executionSecond")
                }
            }
            currentSecond in 30..59 -> {
                // Sisa < 30 detik → skip 1, next+1 minute :00
                val expectedMinute = (currentMinute + 2) % 60
                if (executionMinute == expectedMinute && executionSecond == 0) {
                    Log.d(TAG, "   ✅ CORRECT: Skip 1 minute, execute at :00")
                } else {
                    Log.e(TAG, "   ❌ ERROR: Expected $expectedMinute:00, got $executionMinute:$executionSecond")
                }
            }
        }
        Log.d(TAG, "=" .repeat(60))

        return executionTime
    }

    fun injectTestSignal(signalText: String) {
        val testData = TelegramSignalData(
            trend = if (signalText.contains("B", ignoreCase = true)) "call" else "put",
            hasTime = false,
            hour = null,
            minute = null,
            second = 0,
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
            "fcm_token_full" to (fcmToken ?: "N/A"),
            "topic" to TOPIC_NAME,
            "callback_registered" to (onSignalReceived != null)
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
    val specifiedMinute: Int? = null,
    val specifiedSecond: Int? = null
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