package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.isActive
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class WebSocketManager(
    private val scope: CoroutineScope,
    private val onConnectionStatusChange: (Boolean, String) -> Unit,
    private val onTradeResponse: (Int, JSONObject?) -> Unit,
    private val onWebSocketMessage: (String) -> Unit,
    private val onTradeUpdate: ((JSONObject) -> Unit)? = null,
    private val onTradeClosed: (() -> Unit)? = null,
    private val isNetworkAvailable: () -> Boolean = { true }
) {
    private var webSocket: WebSocket? = null
    private var refCounter = AtomicInteger(1)
    private val joinedChannels = mutableSetOf<String>()

    private val isConnecting = AtomicBoolean(false)
    private val isManualDisconnect = AtomicBoolean(false)
    private val connectionMutex = Mutex()
    private val isNetworkTransition = AtomicBoolean(false)
    private val isStable = AtomicBoolean(false)
    private val isChannelJoining = AtomicBoolean(false)

    private var reconnectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var healthCheckJob: Job? = null
    private var networkRecoveryJob: Job? = null
    private var stabilityCheckJob: Job? = null
    private var channelRecoveryJob: Job? = null

    private var reconnectionAttempts = AtomicInteger(0)
    private var consecutiveFailures = AtomicInteger(0)
    private var networkFailures = AtomicInteger(0)
    private val maxReconnectionAttempts = 15
    private val baseReconnectDelay = 1500L
    private val maxReconnectDelay = 45000L
    private val networkTransitionDelay = 3000L
    private val stabilityRequirement = 10000L

    private var lastMessageReceived = AtomicLong(System.currentTimeMillis())
    private var lastMessageSent = AtomicLong(System.currentTimeMillis())
    private var lastSuccessfulConnection = AtomicLong(0L)
    private var connectionStartTime = AtomicLong(0L)
    private var lastNetworkCheck = AtomicLong(0L)
    private var lastHeartbeatResponse = AtomicLong(0L)
    private var messageCount = AtomicLong(0L)
    private var sentMessageCount = AtomicLong(0L)

    val pendingTrades = mutableMapOf<Int, TradeOrder>()
    private val tradeTimeouts = mutableMapOf<Int, Job>()

    private data class ConnectionCredentials(
        val userAgent: String,
        val authToken: String,
        val deviceType: String,
        val deviceId: String
    )
    private var lastCredentials: ConnectionCredentials? = null

    companion object {
        private const val TAG = "WebSocketManager"

        private const val HEARTBEAT_INTERVAL = 25000L
        private const val CONNECTION_TIMEOUT = 15000L
        private const val MESSAGE_TIMEOUT = 12000L
        private const val HEALTH_CHECK_INTERVAL = 20000L
        private const val MAX_TIME_WITHOUT_MESSAGE = 45000L
        private const val HEARTBEAT_RESPONSE_TIMEOUT = 8000L
        private const val RECONNECT_COOLDOWN = 2000L
        private const val NETWORK_CHECK_INTERVAL = 3000L
        private const val STABILITY_CHECK_INTERVAL = 5000L
        private const val CHANNEL_RECOVERY_DELAY = 2000L
        private const val MAX_NETWORK_FAILURES = 3
    }

    fun connectToWebSocket(userAgent: String, authToken: String, deviceType: String, deviceId: String) {
        lastCredentials = ConnectionCredentials(userAgent, authToken, deviceType, deviceId)

        scope.launch {
            try {
                performConnection(userAgent, authToken, deviceType, deviceId, isReconnection = false)
            } catch (e: Exception) {
                Log.e(TAG, "Koneksi gagal: ${e.message}", e)
                handleConnectionError(e)
            }
        }
    }

    private suspend fun performConnection(
        userAgent: String,
        authToken: String,
        deviceType: String,
        deviceId: String,
        isReconnection: Boolean = false,
        isNetworkRecovery: Boolean = false
    ) {
        connectionMutex.withLock {
            if (isConnecting.get() && !isNetworkRecovery) {
                Log.d(TAG, "Koneksi sedang berlangsung, melewati")
                return@withLock
            }

            if (!checkNetworkAvailabilityWithRetry()) {
                Log.w(TAG, "Jaringan tidak tersedia setelah retry, menjadwalkan pemulihan jaringan")
                if (!isNetworkRecovery) {
                    scheduleNetworkRecovery()
                }
                return@withLock
            }

            isConnecting.set(true)
            isNetworkTransition.set(isNetworkRecovery)
            isStable.set(false)
            connectionStartTime.set(System.currentTimeMillis())

            try {
                Log.d(TAG, "Memulai koneksi (reconnection: $isReconnection, network recovery: $isNetworkRecovery)")

                cleanupConnection(preserveCredentials = true, preserveStats = isReconnection)

                if (isNetworkRecovery) {
                    delay(networkTransitionDelay)
                } else if (isReconnection) {
                    val adaptiveDelay = calculateAdaptiveDelay()
                    delay(adaptiveDelay)
                }

                val client = createEnhancedOkHttpClient()
                val request = createWebSocketRequest(userAgent, authToken, deviceType, deviceId)

                val statusMessage = when {
                    isNetworkRecovery -> "Memulihkan dari perubahan jaringan..."
                    isReconnection -> "Menyambung kembali... (percobaan ${reconnectionAttempts.get() + 1}/$maxReconnectionAttempts)"
                    else -> "Menyambung ke WebSocket..."
                }
                onConnectionStatusChange(false, statusMessage)

                webSocket = client.newWebSocket(request, createWebSocketListener())

            } catch (e: Exception) {
                Log.e(TAG, "Gagal membuat WebSocket: ${e.message}", e)
                isConnecting.set(false)
                handleConnectionError(e)
                throw e
            }
        }
    }

    private fun calculateAdaptiveDelay(): Long {
        val attempts = reconnectionAttempts.get()
        val networkFailureCount = networkFailures.get()

        val baseDelay = if (networkFailureCount > MAX_NETWORK_FAILURES) {
            networkTransitionDelay * 2
        } else {
            baseReconnectDelay
        }

        val exponentialDelay = baseDelay * (2.0.pow((attempts - 1).coerceAtMost(5))).toLong()
        return min(exponentialDelay, maxReconnectDelay)
    }

    private suspend fun checkNetworkAvailabilityWithRetry(maxRetries: Int = 3): Boolean {
        repeat(maxRetries) { attempt ->
            if (isNetworkAvailable()) {
                return true
            }
            if (attempt < maxRetries - 1) {
                delay(1000L)
            }
        }
        return false
    }

    private fun createEnhancedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(MESSAGE_TIMEOUT, TimeUnit.MILLISECONDS)
            .pingInterval(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .callTimeout(CONNECTION_TIMEOUT + 5000, TimeUnit.MILLISECONDS)
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            .build()
    }

    private fun createWebSocketRequest(
        userAgent: String,
        authToken: String,
        deviceType: String,
        deviceId: String
    ): Request {
        return Request.Builder()
            .url("wss://ws.stockity.id/?v=2&vsn=2.0.0")
            .addHeader("User-Agent", userAgent)
            .addHeader("Origin", "https://stockity.id")
            .addHeader("Cookie", "authtoken=$authToken; device_type=$deviceType; device_id=$deviceId")
            .addHeader("Sec-WebSocket-Protocol", "phoenix")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .build()
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket berhasil dibuka")

                scope.launch {
                    try {
                        cleanupJobs()

                        isConnecting.set(false)
                        isNetworkTransition.set(false)
                        reconnectionAttempts.set(0)
                        consecutiveFailures.set(0)
                        networkFailures.set(0)
                        lastSuccessfulConnection.set(System.currentTimeMillis())
                        lastMessageReceived.set(System.currentTimeMillis())
                        lastMessageSent.set(System.currentTimeMillis())
                        lastHeartbeatResponse.set(System.currentTimeMillis())

                        onConnectionStatusChange(true, "Terhubung ke Stockity WebSocket")

                        delay(1000)

                        joinChannelsWithRetry()

                        if (this@WebSocketManager.webSocket != null && !isManualDisconnect.get()) {
                            startEnhancedMonitoring()
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error dalam onOpen handler: ${e.message}", e)
                        handleConnectionFailure(e, null)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val now = System.currentTimeMillis()
                lastMessageReceived.set(now)
                messageCount.incrementAndGet()

                try {
                    val json = JSONObject(text)
                    val event = json.optString("event", "")
                    if (event == "phx_reply" && json.optString("topic") == "phoenix") {
                        lastHeartbeatResponse.set(now)
                        Log.v(TAG, "Respons heartbeat diterima")
                    }
                } catch (e: Exception) {
                }

                scope.launch {
                    try {
                        onWebSocketMessage(text)
                        handleWebSocketMessage(text)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error memproses pesan WebSocket: ${e.message}", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket menutup: $reason (Kode: $code)")
                handleConnectionClosed(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket ditutup: $reason (Kode: $code)")
                handleConnectionClosed(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket gagal: ${t.message}", t)
                consecutiveFailures.incrementAndGet()

                if (isNetworkRelatedError(t)) {
                    networkFailures.incrementAndGet()
                }

                handleConnectionFailure(t, response)
            }
        }
    }

    private fun isNetworkRelatedError(throwable: Throwable): Boolean {
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is SSLException -> true
            else -> throwable.message?.contains("network", ignoreCase = true) == true ||
                    throwable.message?.contains("timeout", ignoreCase = true) == true ||
                    throwable.message?.contains("connection", ignoreCase = true) == true
        }
    }

    private suspend fun joinChannelsWithRetry() {
        if (isChannelJoining.getAndSet(true)) {
            Log.d(TAG, "Bergabung dengan channel sedang berlangsung")
            return
        }

        try {
            val channels = listOf("connection", "tournament", "user", "cfd_zero_spread", "bo", "asset", "account")
            val requiredChannels = setOf("bo", "account", "asset")

            synchronized(joinedChannels) {
                joinedChannels.clear()
            }

            var retryCount = 0
            val maxRetries = 3

            while (retryCount < maxRetries && scope.isActive && !isManualDisconnect.get()) {
                var allJoined = true

                for (channel in channels) {
                    if (!scope.isActive || isManualDisconnect.get()) break

                    if (webSocket == null) {
                        Log.w(TAG, "WebSocket null saat bergabung dengan channel")
                        allJoined = false
                        break
                    }

                    if (synchronized(joinedChannels) { channel in joinedChannels }) {
                        continue
                    }

                    val message = WebSocketMessage(
                        topic = channel,
                        event = "phx_join",
                        payload = emptyMap(),
                        ref = getNextRef()
                    )

                    if (sendWebSocketMessage(message, skipIfNotConnected = true)) {
                        synchronized(joinedChannels) {
                            joinedChannels.add(channel)
                        }
                        delay(800)
                        Log.v(TAG, "Berhasil bergabung dengan channel: $channel")
                    } else {
                        Log.w(TAG, "Gagal bergabung dengan channel: $channel")
                        allJoined = false
                    }
                }

                val hasAllRequired = synchronized(joinedChannels) {
                    requiredChannels.all { it in joinedChannels }
                }

                if (hasAllRequired) {
                    Log.d(TAG, "Semua channel yang dibutuhkan berhasil bergabung")
                    onConnectionStatusChange(true, "Siap untuk trading otomatis")
                    isStable.set(true)
                    break
                } else {
                    retryCount++
                    Log.w(TAG, "Tidak semua channel yang dibutuhkan bergabung (percobaan $retryCount/$maxRetries). Bergabung: ${joinedChannels.joinToString()}")

                    if (retryCount < maxRetries) {
                        delay(2000)
                    }
                }
            }

            val hasEssentialChannels = synchronized(joinedChannels) {
                setOf("bo", "account").all { it in joinedChannels }
            }

            if (hasEssentialChannels) {
                Log.d(TAG, "Channel esensial tersedia, melanjutkan")
                onConnectionStatusChange(true, "Terhubung dengan channel esensial")
                if (!isStable.get()) {
                    isStable.set(true)
                }
            } else {
                Log.w(TAG, "Gagal bergabung dengan channel esensial setelah $maxRetries percobaan")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saat bergabung dengan channel: ${e.message}", e)
        } finally {
            isChannelJoining.set(false)
        }
    }

    private fun startEnhancedMonitoring() {
        startHeartbeat()
        startHealthCheck()
        startNetworkMonitoring()
        startStabilityCheck()

        Log.d(TAG, "Monitoring canggih dimulai")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (scope.isActive && !isManualDisconnect.get()) {
                try {
                    val heartbeatMessage = WebSocketMessage(
                        topic = "phoenix",
                        event = "heartbeat",
                        payload = emptyMap(),
                        ref = getNextRef()
                    )

                    if (sendWebSocketMessage(heartbeatMessage, skipIfNotConnected = true)) {
                        Log.v(TAG, "Heartbeat dikirim")
                        lastMessageSent.set(System.currentTimeMillis())

                        scope.launch {
                            delay(HEARTBEAT_RESPONSE_TIMEOUT)
                            val timeSinceResponse = System.currentTimeMillis() - lastHeartbeatResponse.get()
                            if (timeSinceResponse > HEARTBEAT_RESPONSE_TIMEOUT) {
                                Log.w(TAG, "Timeout respons heartbeat: ${timeSinceResponse}ms")
                            }
                        }
                    } else {
                        Log.w(TAG, "Gagal mengirim heartbeat")
                    }

                    delay(HEARTBEAT_INTERVAL)

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error heartbeat: ${e.message}", e)
                    delay(5000)
                }
            }
        }
    }

    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (scope.isActive && !isManualDisconnect.get()) {
                try {
                    delay(HEALTH_CHECK_INTERVAL)

                    val now = System.currentTimeMillis()
                    val timeSinceLastMessage = now - lastMessageReceived.get()
                    val timeSinceLastHeartbeat = now - lastHeartbeatResponse.get()
                    val isConnected = webSocket != null

                    val messageThreshold = when {
                        isNetworkTransition.get() -> MAX_TIME_WITHOUT_MESSAGE * 2
                        isChannelJoining.get() -> MAX_TIME_WITHOUT_MESSAGE * 1.5
                        else -> MAX_TIME_WITHOUT_MESSAGE
                    }.toLong()

                    val messageHealthy = timeSinceLastMessage <= messageThreshold
                    val heartbeatHealthy = timeSinceLastHeartbeat <= (HEARTBEAT_INTERVAL * 2)
                    val channelsHealthy = isRequiredChannelsReady() || isChannelJoining.get()

                    Log.v(TAG, "Pemeriksaan kesehatan - Pesan: ${timeSinceLastMessage}ms, Heartbeat: ${timeSinceLastHeartbeat}ms, Channel: $channelsHealthy")

                    if (!messageHealthy) {
                        Log.w(TAG, "Pemeriksaan kesehatan gagal: tidak ada pesan selama ${timeSinceLastMessage}ms (batas: ${messageThreshold}ms)")
                        forceReconnect()
                        return@launch
                    }

                    if (isConnected && !heartbeatHealthy && isStable.get()) {
                        Log.w(TAG, "Pemeriksaan kesehatan gagal: tidak ada respons heartbeat selama ${timeSinceLastHeartbeat}ms")
                        forceReconnect()
                        return@launch
                    }


                    if (isConnected && !channelsHealthy && !isChannelJoining.get()) {
                        Log.w(TAG, "Channel yang dibutuhkan hilang, mencoba pemulihan")
                        scheduleChannelRecovery()
                    }

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error pemeriksaan kesehatan: ${e.message}", e)
                    delay(10000)
                }
            }
        }
    }

    private fun startStabilityCheck() {
        stabilityCheckJob?.cancel()
        stabilityCheckJob = scope.launch {
            while (scope.isActive && !isManualDisconnect.get()) {
                try {
                    delay(STABILITY_CHECK_INTERVAL)

                    val now = System.currentTimeMillis()
                    val connectionAge = now - lastSuccessfulConnection.get()
                    val wasStable = isStable.get()

                    val isCurrentlyStable = connectionAge >= stabilityRequirement &&
                            webSocket != null &&
                            isRequiredChannelsReady() &&
                            (now - lastMessageReceived.get()) < MAX_TIME_WITHOUT_MESSAGE

                    if (isCurrentlyStable != wasStable) {
                        isStable.set(isCurrentlyStable)
                        if (isCurrentlyStable) {
                            Log.d(TAG, "Koneksi stabil setelah ${connectionAge}ms")
                            onConnectionStatusChange(true, "Koneksi stabil dan siap")
                        }
                    }

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error pemeriksaan stabilitas: ${e.message}", e)
                    delay(5000)
                }
            }
        }
    }

    private fun scheduleChannelRecovery() {
        channelRecoveryJob?.cancel()
        channelRecoveryJob = scope.launch {
            try {
                delay(CHANNEL_RECOVERY_DELAY)

                if (webSocket != null && !isManualDisconnect.get() && !isChannelJoining.get()) {
                    Log.d(TAG, "Mencoba pemulihan channel")
                    joinChannelsWithRetry()
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e(TAG, "Error pemulihan channel: ${e.message}", e)
            }
        }
    }

    private fun startNetworkMonitoring() {
        networkRecoveryJob?.cancel()
        networkRecoveryJob = scope.launch {
            var previousNetworkState = isNetworkAvailable()
            var networkDownTime = 0L

            while (scope.isActive && !isManualDisconnect.get()) {
                try {
                    delay(NETWORK_CHECK_INTERVAL)

                    val currentNetworkState = isNetworkAvailable()
                    val now = System.currentTimeMillis()
                    lastNetworkCheck.set(now)

                    if (!previousNetworkState && currentNetworkState) {
                        val downDuration = if (networkDownTime > 0) now - networkDownTime else 0
                        Log.d(TAG, "Jaringan pulih setelah ${downDuration}ms, memulai pemulihan koneksi")
                        networkDownTime = 0L
                        scheduleNetworkRecovery()
                    } else if (previousNetworkState && !currentNetworkState) {
                        Log.d(TAG, "Jaringan terputus terdeteksi")
                        networkDownTime = now
                        onConnectionStatusChange(false, "Jaringan tidak tersedia")
                    } else if (!currentNetworkState && networkDownTime > 0) {
                        val downDuration = now - networkDownTime
                        if (downDuration % 10000 == 0L) {
                            onConnectionStatusChange(false, "Jaringan terputus selama ${downDuration/1000}s")
                        }
                    }

                    previousNetworkState = currentNetworkState

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring jaringan: ${e.message}", e)
                    delay(5000)
                }
            }
        }
    }

    private fun scheduleNetworkRecovery() {
        if (isManualDisconnect.get()) return

        val credentials = lastCredentials ?: return

        networkRecoveryJob?.cancel()
        networkRecoveryJob = scope.launch {
            try {
                var stabilityCount = 0
                val requiredStabilityChecks = 3

                while (stabilityCount < requiredStabilityChecks) {
                    delay(1000L)
                    if (isNetworkAvailable()) {
                        stabilityCount++
                    } else {
                        stabilityCount = 0
                    }
                }

                Log.d(TAG, "Jaringan tampak stabil, mencoba pemulihan")

                if (isNetworkAvailable() && !isManualDisconnect.get()) {
                    performConnection(
                        credentials.userAgent,
                        credentials.authToken,
                        credentials.deviceType,
                        credentials.deviceId,
                        isReconnection = true,
                        isNetworkRecovery = true
                    )
                } else {
                    Log.d(TAG, "Jaringan masih tidak tersedia, mencoba ulang pemulihan")
                    delay(5000L)
                    scheduleNetworkRecovery()
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e(TAG, "Pemulihan jaringan gagal: ${e.message}", e)
                handleConnectionError(e)
            }
        }
    }

    private fun handleConnectionClosed(code: Int, reason: String) {
        cleanupConnection(preserveCredentials = true, preserveStats = true)

        if (!isManualDisconnect.get()) {
            onConnectionStatusChange(false, "Koneksi ditutup: $reason")

            val isNetworkIssue = when (code) {
                1000 -> false
                1001, 1006, 1011, 1015 -> true
                else -> reason.contains("network", ignoreCase = true) ||
                        reason.contains("timeout", ignoreCase = true) ||
                        reason.contains("connection", ignoreCase = true)
            }

            scheduleReconnection(isNetworkTransition = isNetworkIssue)
        } else {
            onConnectionStatusChange(false, "Terputus")
        }
    }

    private fun handleConnectionFailure(throwable: Throwable, response: Response?) {
        cleanupConnection(preserveCredentials = true, preserveStats = true)

        if (!isManualDisconnect.get()) {
            val isNetworkIssue = isNetworkRelatedError(throwable)

            Log.w(TAG, "Kegagalan koneksi (masalah jaringan: $isNetworkIssue): ${throwable.message}")
            onConnectionStatusChange(false, "Koneksi gagal: ${throwable.message}")

            handleConnectionError(throwable)
        }
    }

    private fun handleConnectionError(throwable: Throwable) {
        val isNetworkIssue = isNetworkRelatedError(throwable)

        if (isNetworkIssue) {
            if (networkFailures.get() <= MAX_NETWORK_FAILURES) {
                scheduleNetworkRecovery()
            } else {
                scheduleReconnection(isNetworkTransition = true)
            }
        } else {
            scheduleReconnection(isNetworkTransition = false)
        }
    }

    private fun scheduleReconnection(isNetworkTransition: Boolean = false) {
        if (isManualDisconnect.get()) {
            Log.d(TAG, "Pemutusan manual aktif, melewati reconnection")
            return
        }

        val credentials = lastCredentials ?: run {
            Log.w(TAG, "Tidak ada kredensial yang tersedia untuk reconnection")
            return
        }

        val currentAttempts = reconnectionAttempts.incrementAndGet()
        if (currentAttempts > 2) {
            Log.w(TAG, "Reconnection gagal berulang ($currentAttempts kali), memaksa reset WebSocket...")
            forceReconnect()
            return
        }

        if (currentAttempts > maxReconnectionAttempts) {
            Log.e(TAG, "Percobaan reconnection maksimum tercapai ($maxReconnectionAttempts)")
            onConnectionStatusChange(false, "Koneksi gagal setelah $maxReconnectionAttempts percobaan")
            return
        }


        val delay = calculateAdaptiveDelay()

        Log.d(TAG, "Menjadwalkan reconnection #$currentAttempts dalam ${delay}ms (transisi jaringan: $isNetworkTransition, kegagalan jaringan: ${networkFailures.get()})")

        cancelReconnection()
        reconnectionJob = scope.launch {
            try {
                delay(delay)

                if (!isManualDisconnect.get() && scope.isActive) {
                    if (checkNetworkAvailabilityWithRetry()) {
                        performConnection(
                            credentials.userAgent,
                            credentials.authToken,
                            credentials.deviceType,
                            credentials.deviceId,
                            isReconnection = true,
                            isNetworkRecovery = isNetworkTransition
                        )
                    } else {
                        Log.d(TAG, "Jaringan tidak tersedia untuk reconnection, menjadwalkan pemulihan jaringan")
                        scheduleNetworkRecovery()
                    }
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e(TAG, "Reconnection gagal: ${e.message}", e)
                delay(5000L)
                handleConnectionError(e)
            }
        }
    }

    private fun cancelReconnection() {
        reconnectionJob?.cancel()
        reconnectionJob = null
    }

    private fun cleanupJobs() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        healthCheckJob?.cancel()
        healthCheckJob = null
        stabilityCheckJob?.cancel()
        stabilityCheckJob = null
        channelRecoveryJob?.cancel()
        channelRecoveryJob = null
    }

    private fun cancelAllReconnectionJobs() {
        reconnectionJob?.cancel()
        reconnectionJob = null
        networkRecoveryJob?.cancel()
        networkRecoveryJob = null
        channelRecoveryJob?.cancel()
        channelRecoveryJob = null
    }

    private fun cleanupConnection(preserveCredentials: Boolean = false, preserveStats: Boolean = false) {
        isConnecting.set(false)
        isStable.set(false)
        isChannelJoining.set(false)

        if (!preserveCredentials) {
            isNetworkTransition.set(false)
        }

        cleanupJobs()

        if (!preserveCredentials) {
            networkRecoveryJob?.cancel()
            networkRecoveryJob = null
        }

        synchronized(joinedChannels) {
            joinedChannels.clear()
        }

        try {
            webSocket?.close(1000, "Pembersihan")
        } catch (e: Exception) {
            Log.w(TAG, "Error menutup WebSocket: ${e.message}")
        } finally {
            webSocket = null
        }

        tradeTimeouts.values.forEach { it.cancel() }
        tradeTimeouts.clear()

        if (!preserveStats) {
            messageCount.set(0L)
            sentMessageCount.set(0L)
        }
    }

    fun forceReconnect() {
        scope.launch {
            Log.d(TAG, "Force reconnect diminta")

            val credentials = lastCredentials
            if (credentials == null) {
                Log.w(TAG, "Tidak dapat force reconnect: kredensial tidak tersedia")
                return@launch
            }

            isManualDisconnect.set(false)
            reconnectionAttempts.set(0)
            consecutiveFailures.set(0)
            networkFailures.set(0)

            cleanupConnection(preserveCredentials = true, preserveStats = false)
            cancelAllReconnectionJobs()

            delay(1000)

            try {
                performConnection(
                    credentials.userAgent,
                    credentials.authToken,
                    credentials.deviceType,
                    credentials.deviceId,
                    isReconnection = false,
                    isNetworkRecovery = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Force reconnect gagal: ${e.message}", e)
                handleConnectionError(e)
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Pemutusan manual diminta")

        isManualDisconnect.set(true)
        cancelAllReconnectionJobs()
        cleanupJobs()
        reconnectionAttempts.set(0)
        consecutiveFailures.set(0)
        networkFailures.set(0)

        cleanupConnection(preserveCredentials = false, preserveStats = false)

        pendingTrades.clear()
        lastCredentials = null
    }

    fun isConnectionHealthy(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastMessage = now - lastMessageReceived.get()
        val timeSinceLastNetworkCheck = now - lastNetworkCheck.get()
        val connectionAge = now - lastSuccessfulConnection.get()

        val basicHealth = webSocket != null &&
                timeSinceLastMessage < MAX_TIME_WITHOUT_MESSAGE &&
                !isConnecting.get() &&
                !isManualDisconnect.get() &&
                isNetworkAvailable()

        val channelHealth = isRequiredChannelsReady() ||
                (isChannelJoining.get() && connectionAge < 30000L)

        val networkHealth = timeSinceLastNetworkCheck < (NETWORK_CHECK_INTERVAL * 2)

        val stabilityHealth = isStable.get() || connectionAge < stabilityRequirement

        return basicHealth && channelHealth && networkHealth && stabilityHealth
    }

    fun isRequiredChannelsReady(): Boolean {
        val requiredChannels = setOf("bo", "account", "asset")
        return synchronized(joinedChannels) {
            requiredChannels.all { it in joinedChannels }
        }
    }

    fun sendWebSocketMessage(message: WebSocketMessage, skipIfNotConnected: Boolean = false): Boolean {
        return try {
            val currentWebSocket = webSocket
            if (currentWebSocket == null) {
                if (!skipIfNotConnected) {
                    Log.w(TAG, "Tidak dapat mengirim pesan: WebSocket null")
                }
                return false
            }

            val json = JSONObject().apply {
                put("topic", message.topic)
                put("event", message.event)
                put("payload", JSONObject(message.payload))
                put("ref", message.ref)
            }

            val success = currentWebSocket.send(json.toString())

            if (success) {
                sentMessageCount.incrementAndGet()
                lastMessageSent.set(System.currentTimeMillis())

                if (message.ref in pendingTrades.keys) {
                    setTradeTimeout(message.ref)
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error mengirim pesan: ${e.message}", e)
            false
        }
    }

    private fun setTradeTimeout(ref: Int) {
        tradeTimeouts[ref]?.cancel()
        tradeTimeouts[ref] = scope.launch {
            delay(30000)
            if (ref in pendingTrades.keys) {
                onTradeResponse(ref, null)
                pendingTrades.remove(ref)
                tradeTimeouts.remove(ref)
            }
        }
    }

    fun subscribeToAsset(assetRic: String) {
        val subscribeMessage = WebSocketMessage(
            topic = "asset",
            event = "subscribe",
            payload = mapOf("rics" to listOf(assetRic)),
            ref = getNextRef()
        )
        sendWebSocketMessage(subscribeMessage)
    }

    fun getNextRef(): Int = refCounter.getAndIncrement()

    private suspend fun handleWebSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event", "")
            val topic = json.optString("topic", "")
            val payload = json.optJSONObject("payload")
            val ref = json.optInt("ref", -1)

            when (event) {
                "phx_reply" -> handlePhxReply(topic, payload, ref)
                "opened" -> handleTradeOpened(topic, json, payload)
                "closed" -> handleTradeClosed(topic, json, payload)
                "deal_result" -> handleDealResult(topic, json, payload)
                "trade_update" -> handleTradeUpdate(payload)
                "close_deal_batch" -> handleCloseDealBatch(topic, json, payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pesan WebSocket: ${e.message}", e)
        }
    }

    private fun handlePhxReply(topic: String, payload: JSONObject?, ref: Int) {
        val status = payload?.optString("status", "")

        if (status == "ok") {
            synchronized(joinedChannels) {
                joinedChannels.add(topic)
            }
        }

        if (ref in pendingTrades.keys) {
            onTradeResponse(ref, payload)
            tradeTimeouts[ref]?.cancel()
            tradeTimeouts.remove(ref)
        }
    }

    private fun handleTradeOpened(topic: String, json: JSONObject, payload: JSONObject?) {
        if (topic == "bo" && payload != null) {
            onTradeUpdate?.invoke(json)
        }
    }

    private fun handleTradeClosed(topic: String, json: JSONObject, payload: JSONObject?) {
        if (topic == "bo" && payload != null) {
            onTradeUpdate?.invoke(json)
            onTradeClosed?.invoke()
        }
    }

    private fun handleDealResult(topic: String, json: JSONObject, payload: JSONObject?) {
        if (topic == "bo" && payload != null) {
            onTradeUpdate?.invoke(json)
            onTradeClosed?.invoke()
        }
    }

    private fun handleTradeUpdate(payload: JSONObject?) {
        if (payload != null) {
            onTradeUpdate?.invoke(JSONObject().apply {
                put("event", "trade_update")
                put("payload", payload)
            })
        }
    }

    private fun handleCloseDealBatch(topic: String, json: JSONObject, payload: JSONObject?) {
        if (topic == "bo" && payload != null) {
            onTradeUpdate?.invoke(json)
            onTradeClosed?.invoke()
        }
    }

    fun getConnectionStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            "is_connected" to (webSocket != null),
            "is_connecting" to isConnecting.get(),
            "is_stable" to isStable.get(),
            "is_network_transition" to isNetworkTransition.get(),
            "is_channel_joining" to isChannelJoining.get(),
            "joined_channels" to synchronized(joinedChannels) { joinedChannels.toList() },
            "reconnection_attempts" to reconnectionAttempts.get(),
            "consecutive_failures" to consecutiveFailures.get(),
            "network_failures" to networkFailures.get(),
            "time_since_last_message_ms" to (now - lastMessageReceived.get()),
            "time_since_last_heartbeat_ms" to (now - lastHeartbeatResponse.get()),
            "time_since_last_network_check_ms" to (now - lastNetworkCheck.get()),
            "pending_trades" to pendingTrades.size,
            "manual_disconnect" to isManualDisconnect.get(),
            "has_credentials" to (lastCredentials != null),
            "connection_age_ms" to if (lastSuccessfulConnection.get() > 0)
                (now - lastSuccessfulConnection.get()) else 0L,
            "stability_duration_ms" to if (isStable.get() && lastSuccessfulConnection.get() > 0)
                (now - lastSuccessfulConnection.get()) else 0L,
            "network_available" to isNetworkAvailable(),
            "total_messages_received" to messageCount.get(),
            "total_messages_sent" to sentMessageCount.get(),
            "message_rate_per_minute" to calculateMessageRate(),
            "connection_quality" to getConnectionQuality()
        )
    }

    private fun calculateMessageRate(): Double {
        val connectionAge = System.currentTimeMillis() - lastSuccessfulConnection.get()
        if (connectionAge <= 0) return 0.0

        val ageInMinutes = connectionAge / 60000.0
        return if (ageInMinutes > 0) messageCount.get() / ageInMinutes else 0.0
    }

    private fun getConnectionQuality(): String {
        val now = System.currentTimeMillis()
        val timeSinceLastMessage = now - lastMessageReceived.get()
        val timeSinceLastHeartbeat = now - lastHeartbeatResponse.get()
        val isConnected = webSocket != null
        val hasRequiredChannels = isRequiredChannelsReady()
        val isStableConnection = isStable.get()

        return when {
            !isConnected -> "TERPUTUS"
            !hasRequiredChannels -> "MENYAMBUNG"
            !isStableConnection -> "TIDAK_STABIL"
            timeSinceLastMessage > 30000 -> "BURUK"
            timeSinceLastHeartbeat > 60000 -> "MENURUN"
            timeSinceLastMessage > 15000 -> "CUKUP"
            else -> "SANGAT_BAIK"
        }
    }
}