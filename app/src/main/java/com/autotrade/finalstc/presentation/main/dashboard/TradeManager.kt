package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class TradeManager(
    private val scope: CoroutineScope,
    private val webSocketManager: WebSocketManager,
    private val onTradeResult: (TradeResult) -> Unit,
    private val serverTimeService: ServerTimeService
) {
    private var serverTimeOffset = 0L

    private var currentCurrency: CurrencyType = CurrencyType.IDR

    fun updateServerTimeOffset(offsetMs: Long) {
        serverTimeOffset = offsetMs
    }

    fun updateCurrency(currency: CurrencyType) {
        currentCurrency = currency
        println("TradeManager: Currency updated to ${currency.code}")
    }

    private fun getCurrentServerTime(): Long {
        return serverTimeService.getCurrentServerTimeMillis()
    }

    fun executeScheduledTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        scheduledOrderId: String,
        startTimeMillis: Long
    ) {
        scope.launch {
            try {
                val tradeOrder = createTradeOrder(
                    assetRic = assetRic,
                    trend = trend,
                    amount = amount,
                    isDemoAccount = isDemoAccount,
                    scheduledOrderId = scheduledOrderId,
                    isMartingaleAttempt = false,
                    martingaleStep = 0,
                    customStartTimeMs = startTimeMillis,
                    forceOneMinute = false,
                    isScheduledOrder = true,
                    currency = currentCurrency
                )

                executeTradeOrder(tradeOrder)

            } catch (e: Exception) {
                val result = TradeResult(
                    success = false,
                    message = "Trading terjadwal gagal: ${e.message}",
                    scheduledOrderId = scheduledOrderId,
                    isScheduledOrder = true,
                    executionType = "EXECUTION_FAILED",
                    details = mapOf(
                        "error_type" to "EXECUTION_ERROR",
                        "trend" to trend,
                        "amount" to amount,
                        "currency" to currentCurrency.code
                    )
                )

                onTradeResult(result)
            }
        }
    }

    fun executeFollowBoundaryTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        followOrderId: String
    ) {
        scope.launch {
            try {
                val tradeOrder = createTradeOrder(
                    assetRic = assetRic,
                    trend = trend,
                    amount = amount,
                    isDemoAccount = isDemoAccount,
                    scheduledOrderId = followOrderId,
                    isMartingaleAttempt = false,
                    martingaleStep = 0,
                    customStartTimeMs = null,
                    forceOneMinute = true,
                    currency = currentCurrency
                )

                executeTradeOrder(tradeOrder)

            } catch (e: Exception) {
                handleTradeExecutionError(e, followOrderId, trend, amount, false, 0, "FOLLOW_BOUNDARY_ERROR")
            }
        }
    }

    fun executeFollowInstantTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        followOrderId: String,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0
    ) {
        scope.launch {
            try {
                val tradeOrder = createTradeOrder(
                    assetRic = assetRic,
                    trend = trend,
                    amount = amount,
                    isDemoAccount = isDemoAccount,
                    scheduledOrderId = followOrderId,
                    isMartingaleAttempt = isMartingaleAttempt,
                    martingaleStep = martingaleStep,
                    customStartTimeMs = null,
                    forceOneMinute = false,
                    currency = currentCurrency
                )

                executeTradeOrder(tradeOrder)

            } catch (e: Exception) {
                val errorType = if (isMartingaleAttempt) "FOLLOW_MARTINGALE_ERROR" else "FOLLOW_INSTANT_ERROR"
                handleTradeExecutionError(e, followOrderId, trend, amount, isMartingaleAttempt, martingaleStep, errorType)
            }
        }
    }

    fun executeFollowTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        followOrderId: String,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0,
        forceOneMinute: Boolean = true
    ) {
        if (forceOneMinute) {
            executeFollowBoundaryTrade(assetRic, trend, amount, isDemoAccount, followOrderId)
        } else {
            executeFollowInstantTrade(assetRic, trend, amount, isDemoAccount, followOrderId, isMartingaleAttempt, martingaleStep)
        }
    }

    fun executeIndicatorTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        indicatorOrderId: String,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0
    ) {
        scope.launch {
            try {
                val tradeOrder = createTradeOrder(
                    assetRic = assetRic,
                    trend = trend,
                    amount = amount,
                    isDemoAccount = isDemoAccount,
                    scheduledOrderId = indicatorOrderId,
                    isMartingaleAttempt = isMartingaleAttempt,
                    martingaleStep = martingaleStep,
                    customStartTimeMs = null,
                    forceOneMinute = !isMartingaleAttempt,
                    currency = currentCurrency
                )

                executeTradeOrder(tradeOrder)

            } catch (e: Exception) {
                val errorType = if (isMartingaleAttempt) "INDICATOR_MARTINGALE_ERROR" else "INDICATOR_ORDER_ERROR"
                handleTradeExecutionError(e, indicatorOrderId, trend, amount, isMartingaleAttempt, martingaleStep, errorType)
            }
        }
    }

    fun executeMartingaleTrade(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        scheduledOrderId: String,
        martingaleStep: Int
    ) {
        scope.launch {
            try {
                val tradeOrder = createTradeOrder(
                    assetRic = assetRic,
                    trend = trend,
                    amount = amount,
                    isDemoAccount = isDemoAccount,
                    scheduledOrderId = scheduledOrderId,
                    isMartingaleAttempt = true,
                    martingaleStep = martingaleStep,
                    forceOneMinute = false,
                    currency = currentCurrency
                )

                executeTradeOrder(tradeOrder)

            } catch (e: Exception) {
                handleTradeExecutionError(e, scheduledOrderId, trend, amount, true, martingaleStep, "MARTINGALE_EXECUTION_ERROR")
            }
        }
    }

    private suspend fun executeTradeOrder(tradeOrder: TradeOrder): Boolean {
        val ref = webSocketManager.getNextRef()
        webSocketManager.pendingTrades[ref] = tradeOrder

        val tradeMessage = WebSocketMessage(
            topic = "bo",
            event = "create",
            payload = mapOf(
                "amount" to tradeOrder.amount,
                "created_at" to tradeOrder.createdAt,
                "deal_type" to tradeOrder.dealType,
                "expire_at" to tradeOrder.expireAt,
                "iso" to tradeOrder.iso,
                "option_type" to tradeOrder.optionType,
                "ric" to tradeOrder.ric,
                "trend" to tradeOrder.trend
            ),
            ref = ref
        )

        if (!webSocketManager.sendWebSocketMessage(tradeMessage)) {
            webSocketManager.pendingTrades.remove(ref)

            val result = TradeResult(
                success = false,
                message = "Gagal mengirim trading via WebSocket",
                scheduledOrderId = tradeOrder.scheduledOrderId,
                isScheduledOrder = tradeOrder.scheduledOrderId != null,
                isMartingaleAttempt = tradeOrder.isMartingaleAttempt,
                martingaleStep = tradeOrder.martingaleStep,
                executionType = "WEBSOCKET_SEND_FAILED",
                details = mapOf(
                    "error_type" to "WEBSOCKET_SEND_FAILED",
                    "trend" to tradeOrder.trend,
                    "amount" to tradeOrder.amount,
                    "currency" to tradeOrder.iso
                )
            )

            onTradeResult(result)
            return false
        }

        val timeoutMs = if (tradeOrder.isMartingaleAttempt) 2000L else 3000L
        delay(timeoutMs)

        if (ref in webSocketManager.pendingTrades) {
            webSocketManager.pendingTrades.remove(ref)

            val result = TradeResult(
                success = false,
                message = "Trading timeout setelah ${timeoutMs}ms",
                scheduledOrderId = tradeOrder.scheduledOrderId,
                isScheduledOrder = tradeOrder.scheduledOrderId != null,
                isMartingaleAttempt = tradeOrder.isMartingaleAttempt,
                martingaleStep = tradeOrder.martingaleStep,
                executionType = "TIMEOUT",
                details = mapOf(
                    "error_type" to "TIMEOUT",
                    "timeout_ms" to timeoutMs,
                    "trend" to tradeOrder.trend,
                    "amount" to tradeOrder.amount,
                    "currency" to tradeOrder.iso
                )
            )

            onTradeResult(result)
            return false
        }

        return true
    }

    private fun createTradeOrder(
        assetRic: String,
        trend: String,
        amount: Long,
        isDemoAccount: Boolean,
        scheduledOrderId: String? = null,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0,
        customStartTimeMs: Long? = null,
        forceOneMinute: Boolean = false,
        isScheduledOrder: Boolean = false,
        currency: CurrencyType = CurrencyType.IDR
    ): TradeOrder {

        val baseTimeMs = customStartTimeMs ?: getCurrentServerTime()
        val serverTimeSeconds = baseTimeMs / 1000.0

        val createdAtSeconds = if (isScheduledOrder) {
            kotlin.math.floor(serverTimeSeconds).toLong()
        } else {
            kotlin.math.floor(serverTimeSeconds).toLong() + 1
        }

        val currentSeconds = createdAtSeconds
        val secondsInMinute = currentSeconds % 60

        val expireAtSeconds = if (forceOneMinute) {
            val minuteBoundary = currentSeconds + (60 - secondsInMinute)
            val duration = minuteBoundary - createdAtSeconds
            if (duration < 50) {
                currentSeconds + (120 - secondsInMinute)
            } else {
                minuteBoundary
            }
        } else {
            when {
                secondsInMinute <= 10 -> currentSeconds + (60 - secondsInMinute)
                secondsInMinute >= 50 -> currentSeconds + (120 - secondsInMinute)
                else -> currentSeconds + (120 - secondsInMinute)
            }
        }

        val duration = expireAtSeconds - createdAtSeconds

        val finalExpireAt = if (forceOneMinute) {
            if (duration < 55) {
                createdAtSeconds + 60
            } else if (duration > 65) {
                createdAtSeconds + 60
            } else {
                expireAtSeconds
            }
        } else {
            if (duration < 55) {
                createdAtSeconds + 60
            } else if (duration > 120) {
                createdAtSeconds + 60
            } else {
                expireAtSeconds
            }
        }

        val createdAtMs = createdAtSeconds * 1000
        val finalDurationSeconds = finalExpireAt - createdAtSeconds

        if (isScheduledOrder) {
            val now = System.currentTimeMillis()
            val scheduleAdvance = (now - baseTimeMs) / 1000.0
            println("📊 SCHEDULED ORDER TIMING:")
            println("   Base time: $baseTimeMs")
            println("   Current time: $now")
            println("   Advance applied: ${String.format("%.2f", scheduleAdvance)}s")
            println("   Created at: $createdAtSeconds")
            println("   Expire at: $finalExpireAt")
            println("   Duration: ${finalDurationSeconds}s")
            println("   Currency: ${currency.code}")
        }

        val tradeOrder = TradeOrder(
            amount = amount,
            createdAt = createdAtMs,
            dealType = if (isDemoAccount) "demo" else "real",
            expireAt = finalExpireAt,
            iso = currency.code,
            optionType = "turbo",
            ric = assetRic,
            trend = trend,
            duration = 1,
            scheduledOrderId = scheduledOrderId,
            isMartingaleAttempt = isMartingaleAttempt,
            martingaleStep = martingaleStep
        )

        if (forceOneMinute) {
            if (finalDurationSeconds < 50) {
                throw IllegalStateException("Boundary Order: Durasi terlalu pendek: ${finalDurationSeconds}s")
            }
            if (finalDurationSeconds > 70) {
                throw IllegalStateException("Boundary Order: Durasi terlalu panjang: ${finalDurationSeconds}s")
            }
        } else {
            if (finalDurationSeconds < 50) {
                throw IllegalStateException("Realtime Order: Durasi terlalu pendek: ${finalDurationSeconds}s")
            }
            if (finalDurationSeconds > 125) {
                throw IllegalStateException("Realtime Order: Durasi terlalu panjang: ${finalDurationSeconds}s")
            }
        }

        if (finalExpireAt <= createdAtSeconds) {
            throw IllegalStateException("Waktu kadaluarsa tidak valid")
        }

        return tradeOrder
    }

    private fun handleTradeExecutionError(
        exception: Exception,
        orderId: String,
        trend: String,
        amount: Long,
        isMartingaleAttempt: Boolean,
        martingaleStep: Int,
        errorType: String
    ) {
        val result = TradeResult(
            success = false,
            message = "Trading execution failed: ${exception.message}",
            scheduledOrderId = orderId,
            isScheduledOrder = true,
            isMartingaleAttempt = isMartingaleAttempt,
            martingaleStep = martingaleStep,
            executionType = "EXECUTION_FAILED",
            details = mapOf(
                "error_type" to errorType,
                "trend" to trend,
                "amount" to amount,
                "step" to if (isMartingaleAttempt) martingaleStep else 0,
                "currency" to currentCurrency.code
            )
        )

        onTradeResult(result)
    }

    fun handleTradeResponse(ref: Int, payload: JSONObject?) {
        val tradeOrder = webSocketManager.pendingTrades[ref] ?: return

        val status = payload?.optString("status", "")
        val response = payload?.optJSONObject("response")

        if (status == "ok") {
            handleTradeExecutionSuccess(tradeOrder, response, payload)
        } else {
            handleTradeExecutionFailed(tradeOrder, response, payload)
        }

        webSocketManager.pendingTrades.remove(ref)
    }

    private fun handleTradeExecutionSuccess(tradeOrder: TradeOrder, response: JSONObject?, payload: JSONObject?) {
        val orderId = response?.optString("id") ?: "UNKNOWN"

        val result = TradeResult(
            success = true,
            message = when {
                tradeOrder.isMartingaleAttempt -> "Trading martingale terkirim ke server Step ${tradeOrder.martingaleStep}"
                tradeOrder.scheduledOrderId != null -> "Trading terjadwal berhasil terkirim ke server"
                else -> "Trading berhasil terkirim ke server"
            },
            orderId = orderId,
            executionType = "EXECUTION_SUCCESS",
            details = mapOf(
                "asset" to tradeOrder.ric,
                "amount" to tradeOrder.amount,
                "trend" to tradeOrder.trend,
                "type" to tradeOrder.dealType,
                "duration" to "1min",
                "timestamp" to getCurrentServerTime(),
                "execution" to when {
                    tradeOrder.isMartingaleAttempt -> "martingale-trade"
                    tradeOrder.scheduledOrderId != null -> "scheduled-order"
                    else -> "manual"
                },
                "martingale_step" to tradeOrder.martingaleStep,
                "is_martingale" to tradeOrder.isMartingaleAttempt,
                "server_order_id" to orderId,
                "created_at_sent" to tradeOrder.createdAt,
                "expire_at_sent" to tradeOrder.expireAt,
                "currency" to tradeOrder.iso
            ),
            rawResponse = payload?.toString(),
            isMartingaleAttempt = tradeOrder.isMartingaleAttempt,
            martingaleStep = tradeOrder.martingaleStep,
            scheduledOrderId = tradeOrder.scheduledOrderId,
            isScheduledOrder = tradeOrder.scheduledOrderId != null
        )

        onTradeResult(result)
    }

    private fun handleTradeExecutionFailed(tradeOrder: TradeOrder, response: JSONObject?, payload: JSONObject?) {
        val errorResponse = response?.toString() ?: "Unknown error"

        val specificMessage = when {
            errorResponse.contains("expire_at") -> "Waktu kadaluarsa trading tidak valid"
            errorResponse.contains("amount") -> "Nominal trading mungkin di bawah minimum, saldo kosong, atau mata uang tidak tersedia"
            errorResponse.contains("asset") -> "Aset yang dipilih mungkin tidak tersedia"
            errorResponse.contains("duplicate") -> "Trading serupa sudah ada"
            errorResponse.contains("iso") -> "Currency ${tradeOrder.iso} mungkin tidak didukung"
            else -> "Eksekusi trading gagal: $errorResponse"
        }

        val result = TradeResult(
            success = false,
            message = when {
                tradeOrder.isMartingaleAttempt -> "Eksekusi trading martingale gagal (Langkah ${tradeOrder.martingaleStep + 1}): $specificMessage"
                tradeOrder.scheduledOrderId != null -> "Eksekusi trading terjadwal gagal: $specificMessage"
                else -> "Eksekusi trading gagal: $specificMessage"
            },
            executionType = "EXECUTION_FAILED",
            details = mapOf(
                "error_code" to "TRADE_REJECTED",
                "execution" to when {
                    tradeOrder.isMartingaleAttempt -> "martingale-trade"
                    tradeOrder.scheduledOrderId != null -> "scheduled-order"
                    else -> "manual"
                },
                "trend" to tradeOrder.trend,
                "is_martingale" to tradeOrder.isMartingaleAttempt,
                "martingale_step" to tradeOrder.martingaleStep,
                "raw_error" to errorResponse,
                "currency" to tradeOrder.iso
            ),
            rawResponse = payload?.toString(),
            isMartingaleAttempt = tradeOrder.isMartingaleAttempt,
            martingaleStep = tradeOrder.martingaleStep,
            scheduledOrderId = tradeOrder.scheduledOrderId,
            isScheduledOrder = tradeOrder.scheduledOrderId != null
        )

        onTradeResult(result)
    }

    fun cleanup() {
        webSocketManager.pendingTrades.clear()
    }
}