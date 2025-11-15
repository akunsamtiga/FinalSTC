package com.autotrade.finalstc.presentation.main.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.combine
import com.autotrade.finalstc.data.repository.CurrencyRepository
import com.autotrade.finalstc.data.local.LanguageManager
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.repository.FirebaseRepository
import kotlin.math.abs

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val currencyRepository: CurrencyRepository,
    private val languageManager: LanguageManager,
    private val sessionManager: SessionManager,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _multiMomentumOrders = MutableStateFlow<List<MultiMomentumOrder>>(emptyList())
    val multiMomentumOrders: StateFlow<List<MultiMomentumOrder>> = _multiMomentumOrders.asStateFlow()

    private lateinit var multiMomentumOrderManager: MultiMomentumOrderManager

    private val _whatsappNumber = MutableStateFlow("6285959860015")
    val whatsappNumber: StateFlow<String> = _whatsappNumber.asStateFlow()

    private val _whitelistCheckState = MutableStateFlow<WhitelistCheckState>(WhitelistCheckState.Checking)
    val whitelistCheckState: StateFlow<WhitelistCheckState> = _whitelistCheckState.asStateFlow()

    val currentLanguage: StateFlow<String> = languageManager.currentLanguage

    private val _historyList = MutableStateFlow<List<TradingHistoryNew>>(emptyList())
    val historyList: StateFlow<List<TradingHistoryNew>> = _historyList.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _tradeResults = MutableStateFlow<List<TradeResult>>(emptyList())
    val tradeResults: StateFlow<List<TradeResult>> = _tradeResults.asStateFlow()

    private val _scheduledOrders = MutableStateFlow<List<ScheduledOrder>>(emptyList())
    val scheduledOrders: StateFlow<List<ScheduledOrder>> = _scheduledOrders.asStateFlow()

    private val _followOrders = MutableStateFlow<List<FollowOrder>>(emptyList())
    val followOrders: StateFlow<List<FollowOrder>> = _followOrders.asStateFlow()

    private val _indicatorOrders = MutableStateFlow<List<IndicatorOrder>>(emptyList())
    val indicatorOrders: StateFlow<List<IndicatorOrder>> = _indicatorOrders.asStateFlow()

    private val _ctcOrders = MutableStateFlow<List<CTCOrder>>(emptyList())
    val ctcOrders: StateFlow<List<CTCOrder>> = _ctcOrders.asStateFlow()


    private val _refreshTrigger = MutableSharedFlow<Long>()
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _todayStats = MutableStateFlow(TodayStats())
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()

    private val _todayProfit = MutableStateFlow(0L)
    val todayProfit: StateFlow<Long> = _todayProfit.asStateFlow()

    private val _isCalculatingTodayProfit = MutableStateFlow(false)
    val isCalculatingTodayProfit: StateFlow<Boolean> = _isCalculatingTodayProfit.asStateFlow()

    private val _indicatorPredictionInfo = MutableStateFlow<Map<String, Any>>(emptyMap())
    val indicatorPredictionInfo: StateFlow<Map<String, Any>> = _indicatorPredictionInfo.asStateFlow()

    private val _localTradingStats = MutableStateFlow(LocalStatsTracker.LocalTradingStats())
    val localTradingStats: StateFlow<LocalStatsTracker.LocalTradingStats> = _localTradingStats.asStateFlow()

    private lateinit var localStatsTracker: LocalStatsTracker
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var tradeManager: TradeManager
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var assetManager: AssetManager
    private lateinit var martingaleManager: MartingaleManager
    private lateinit var serverTimeService: ServerTimeService
    private lateinit var continuousTradeMonitor: ContinuousTradeMonitor
    private lateinit var stopLossProfitManager: StopLossProfitManager
    private lateinit var followOrderManager: FollowOrderManager
    private lateinit var indicatorOrderManager: IndicatorOrderManager
    private lateinit var ctcOrderManager: CTCOrderManager
    private lateinit var todayProfitCalculator: TodayProfitCalculator

    private var serverTimeOffset = 0L

    private var todayProfitCache: TodayProfitCache? = null
    private var lastTodayProfitUpdate = 0L

    private data class TodayProfitCache(
        val date: String,
        val profit: Long,
        val lastUpdate: Long,
        val processedTradeIds: Set<String>
    )

    init {
        checkWhitelistStatus()

        viewModelScope.launch {
            try {
                val userCurrency = currencyRepository.getCurrencyWithFetch()
                Log.d("DashboardViewModel", "Currency fetched in init: $userCurrency")
                loadUserCurrency()

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Gagal refresh currency: ${e.message}")
            }
        }
        initializeManagers()
        localStatsTracker = LocalStatsTracker(serverTimeService)
        viewModelScope.launch {
            localStatsTracker.localStats.collect { stats ->
                _localTradingStats.value = stats
            }
        }

        loadUserProfile()
        syncServerTime()
        fetchAssetsFromApi()
        connectToWebSocket()
        startConnectionMonitoring()
        startTodayProfitResetScheduler()
        startIndicatorPredictionInfoUpdates()
        startLocalStatsResetScheduler()
        loadUserCurrency()
    }

    private fun checkWhitelistStatus() {
        viewModelScope.launch {
            try {
                _whitelistCheckState.value = WhitelistCheckState.Checking
                Log.d("DashboardViewModel", "🔍 Checking whitelist status...")

                val userSession = loginRepository.getUserSession()

                if (userSession == null) {
                    Log.e("DashboardViewModel", "❌ No user session found")
                    _whitelistCheckState.value = WhitelistCheckState.Failed(
                        reason = "NO_SESSION",
                        message = "Sesi pengguna tidak ditemukan. Silakan login kembali."
                    )
                    return@launch
                }

                val userId = userSession.userId
                val email = userSession.email

                Log.d("DashboardViewModel", "Checking whitelist for:")
                Log.d("DashboardViewModel", "  UserID: $userId")
                Log.d("DashboardViewModel", "  Email: $email")

                // Cek koneksi Firestore
                val isFirestoreConnected = firebaseRepository.testFirestoreConnection()
                if (!isFirestoreConnected) {
                    Log.e("DashboardViewModel", "❌ Firestore connection failed")
                    _whitelistCheckState.value = WhitelistCheckState.Failed(
                        reason = "FIRESTORE_CONNECTION",
                        message = "Tidak dapat terhubung ke database. Periksa koneksi internet Anda."
                    )
                    return@launch
                }

                // Cek whitelist by userId
                val isWhitelisted = firebaseRepository.checkUserInWhitelistByUserId(userId)

                if (!isWhitelisted) {
                    Log.e("DashboardViewModel", "❌ User not in whitelist")

                    // Cek apakah user ada tapi tidak aktif
                    val userExists = firebaseRepository.debugCheckUserIdExists(userId)

                    val failureReason = if (userExists) {
                        WhitelistCheckState.Failed(
                            reason = "INACTIVE",
                            message = "Akun Anda terdaftar tetapi tidak aktif.\n\n" +
                                    "UserID: $userId\n" +
                                    "Email: $email\n\n" +
                                    "Silakan hubungi administrator untuk mengaktifkan akses Anda."
                        )
                    } else {
                        WhitelistCheckState.Failed(
                            reason = "NOT_REGISTERED",
                            message = "Akun Anda belum terdaftar di aplikasi ini.\n\n" +
                                    "UserID: $userId\n" +
                                    "Email: $email\n\n" +
                                    "Silakan hubungi administrator untuk mendaftarkan akses Anda."
                        )
                    }

                    _whitelistCheckState.value = failureReason
                    return@launch
                }

                // Update last login
                try {
                    firebaseRepository.updateLastLogin(userId)
                    Log.d("DashboardViewModel", "✅ Last login updated")
                } catch (e: Exception) {
                    Log.w("DashboardViewModel", "⚠️ Failed to update last login: ${e.message}")
                }

                // ✅ WHITELIST CHECK PASSED
                Log.d("DashboardViewModel", "✅ Whitelist check passed")
                _whitelistCheckState.value = WhitelistCheckState.Verified(
                    userId = userId,
                    email = email
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ Whitelist check error: ${e.message}", e)
                _whitelistCheckState.value = WhitelistCheckState.Failed(
                    reason = "EXCEPTION",
                    message = "Terjadi kesalahan saat memverifikasi akses:\n${e.message}"
                )
            }
        }
    }

    // ✅ PUBLIC FUNCTION UNTUK RETRY
    fun retryWhitelistCheck() {
        checkWhitelistStatus()
    }

    fun updateLanguage(languageCode: String, countryCode: String) {
        languageManager.saveLanguage(languageCode, countryCode)
    }

    private fun loadUserCurrency() {
        viewModelScope.launch {
            try {
                Log.d("DashboardViewModel", "Loading user currency from API...")

                val result = currencyRepository.fetchUserCurrency()

                result.onSuccess { currencyData ->
                    val currentCurrency = currencyData.current
                    Log.d("DashboardViewModel", "Currency fetched successfully: $currentCurrency")

                    val currencyType = when (currentCurrency) {
                        "IDR" -> CurrencyType.IDR
                        "USD" -> CurrencyType.USD
                        "EUR" -> CurrencyType.EUR
                        "MYR" -> CurrencyType.MYR
                        else -> {
                            Log.w("DashboardViewModel", "Unknown currency: $currentCurrency, using IDR")
                            CurrencyType.IDR
                        }
                    }

                    val newCurrencySettings = _uiState.value.currencySettings.adjustForCurrency(currencyType)
                    _uiState.value = _uiState.value.copy(currencySettings = newCurrencySettings)

                    tradeManager.updateCurrency(currencyType)
                    martingaleManager.updateCurrency(currencyType)

                    Log.d("DashboardViewModel", "Currency updated to: ${currencyType.code}")
                    Log.d("DashboardViewModel", "Minimum amount: ${currencyType.formatAmount(currencyType.minAmountInCents)}")
                }

                result.onFailure { exception ->
                    Log.e("DashboardViewModel", "Failed to fetch currency: ${exception.message}")
                }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading currency: ${e.message}", e)
            }
        }
    }

    fun refreshCurrency() {
        viewModelScope.launch {
            try {
                Log.d("DashboardViewModel", "Refreshing currency from API...")

                val result = currencyRepository.fetchUserCurrency()

                result.onSuccess { currencyData ->
                    val currentCurrency = currencyData.current
                    Log.d("DashboardViewModel", "Currency refreshed: $currentCurrency")

                    val currencyType = when (currentCurrency) {
                        "IDR" -> CurrencyType.IDR
                        "USD" -> CurrencyType.USD
                        "EUR" -> CurrencyType.EUR
                        "MYR" -> CurrencyType.MYR
                        else -> CurrencyType.IDR
                    }

                    if (_uiState.value.currencySettings.selectedCurrency != currencyType) {
                        val newCurrencySettings = _uiState.value.currencySettings.adjustForCurrency(currencyType)
                        _uiState.value = _uiState.value.copy(currencySettings = newCurrencySettings)

                        tradeManager.updateCurrency(currencyType)
                        martingaleManager.updateCurrency(currencyType)

                        Log.d("DashboardViewModel", "Currency changed to: ${currencyType.code}")
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error refreshing currency: ${e.message}", e)
            }
        }
    }

    fun getCurrentCurrencyFromApi(): Map<String, Any> {
        val currency = _uiState.value.currencySettings.selectedCurrency
        return mapOf(
            "code" to currency.code,
            "symbol" to currency.symbol,
            "source" to "API",
            "min_amount" to currency.formatAmount(currency.minAmountInCents),
            "base_amount" to _uiState.value.currencySettings.getFormattedBaseAmount()
        )
    }

    fun setSelectedCurrency(currency: CurrencyType) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Cannot change currency while trading mode is active"
            )
            return
        }

        val currentSettings = _uiState.value.currencySettings
        val newSettings = currentSettings.adjustForCurrency(currency)

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Currency validation failed: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        val updatedMartingale = _uiState.value.martingaleSettings.copy(
            baseAmount = newSettings.baseAmountInCents
        )

        _uiState.value = _uiState.value.copy(
            currencySettings = newSettings,
            martingaleSettings = updatedMartingale
        )

        tradeManager.updateCurrency(currency)
        martingaleManager.updateCurrency(currency)

        println("Currency changed to ${currency.code}")
        println("  Minimum amount: ${currency.formatAmount(currency.minAmountInCents)}")
        println("  Base amount adjusted to: ${newSettings.getFormattedBaseAmount()}")
        println("All managers updated with new currency: ${currency.code}")
    }

    fun setBaseAmountInCurrency(input: String) {
        if (!_uiState.value.canModifySettings()) return

        val currentCurrency = _uiState.value.currencySettings.selectedCurrency
        val amountInCents = currentCurrency.parseUserInput(input)

        if (amountInCents == null) {
            _uiState.value = _uiState.value.copy(
                error = "Invalid amount format for ${currentCurrency.code}"
            )
            return
        }

        val validationMessage = currentCurrency.getValidationMessage(amountInCents)
        if (validationMessage != null) {
            _uiState.value = _uiState.value.copy(
                error = validationMessage
            )
            return
        }

        val newCurrencySettings = _uiState.value.currencySettings.copy(
            baseAmountInCents = amountInCents
        )

        val updatedMartingale = _uiState.value.martingaleSettings.copy(
            baseAmount = amountInCents
        )

        _uiState.value = _uiState.value.copy(
            currencySettings = newCurrencySettings,
            martingaleSettings = updatedMartingale
        )
    }

    fun getAvailableCurrencies(): List<CurrencyType> {
        return CurrencyType.values().toList()
    }

    fun getCurrentCurrencyInfo(): Map<String, String> {
        val settings = _uiState.value.currencySettings
        val currency = settings.selectedCurrency

        return mapOf(
            "code" to currency.code,
            "symbol" to currency.symbol,
            "min_amount" to currency.formatAmount(currency.minAmountInCents),
            "base_amount" to settings.getFormattedBaseAmount(),
            "decimal_places" to currency.decimalPlaces.toString()
        )
    }

    private fun startLocalStatsResetScheduler() {
        viewModelScope.launch {
            while (true) {
                try {
                    delay(60000L)

                    localStatsTracker.initializeOrReset(_uiState.value.isDemoAccount)

                } catch (e: Exception) {
                    println("Error in local stats reset scheduler: ${e.message}")
                    delay(300000L)
                }
            }
        }
    }

    fun getMultiMomentumCandleHistory(): List<Map<String, Any>> {
        return if (_uiState.value.isMultiMomentumModeActive) {
            val stats = multiMomentumOrderManager.getPerformanceStats()
            stats["recent_candles"] as? List<Map<String, Any>> ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun ensureStableConnection(): Boolean {
        val connectionHealthy = webSocketManager.isConnectionHealthy()
        val channelsReady = webSocketManager.isRequiredChannelsReady()
        val isConnected = _uiState.value.isWebSocketConnected

        val connectionStats = webSocketManager.getConnectionStats()
        val reconnectionAttempts = connectionStats["reconnection_attempts"] as? Int ?: 0
        val timeSinceLastMessage = connectionStats["time_since_last_message_ms"] as? Long ?: 0L
        val isConnecting = connectionStats["is_connecting"] as? Boolean ?: false

        Log.d("DashboardViewModel",
            "Connection Check - Healthy: $connectionHealthy, " +
                    "Channels: $channelsReady, " +
                    "Connected: $isConnected, " +
                    "Reconnect Attempts: $reconnectionAttempts, " +
                    "Time Since Msg: ${timeSinceLastMessage}ms, " +
                    "Connecting: $isConnecting"
        )

        if (isConnecting || reconnectionAttempts > 0) {
            Log.w("DashboardViewModel", "Connection unstable - still connecting or recent reconnections")
            return false
        }

        if (timeSinceLastMessage > 30000) {
            Log.w("DashboardViewModel", "Connection unstable - no recent messages")
            return false
        }

        return connectionHealthy && channelsReady && isConnected
    }

    private fun isTradeCompleted(trade: TradingHistoryNew): Boolean {
        val status = trade.status.lowercase().trim()

        val completedStatuses = setOf(
            "won", "win",
            "lost", "lose", "loss", "failed",
            "stand", "draw", "tie", "draw_trade"
        )

        val runningStatuses = setOf(
            "opened", "open", "pending", "running",
            "active", "executing", "processing",
            "waiting", "in_progress"
        )

        return when {
            completedStatuses.contains(status) -> {
                val hasFinishTime = !trade.finishedAt.isNullOrEmpty() &&
                        trade.finishedAt != "null" &&
                        trade.finishedAt.trim().isNotEmpty()

                if (!hasFinishTime) {
                    println("   Trade ${trade.uuid}: Status '$status' but no finish time")
                    return false
                }
                true
            }

            runningStatuses.contains(status) -> {
                println("   Trade ${trade.uuid}: Still running (status: $status)")
                false
            }

            else -> {
                println("   Trade ${trade.uuid}: Unknown status '$status' - excluding from calculation")
                false
            }
        }
    }

    private fun loadWhatsappNumber() {
        viewModelScope.launch {
            try {
                val config = firebaseRepository.getRegistrationConfig()
                _whatsappNumber.value = config.whatsappHelpNumber
                Log.d("DashboardViewModel", "WhatsApp number loaded: ${config.whatsappHelpNumber}")
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading WhatsApp number: ${e.message}")
                // Keep default value
            }
        }
    }

    private fun calculateTradeProfit(history: TradingHistoryNew): Long {
        return when (history.status.lowercase()) {
            "won", "win" -> {
                val actualPayout = if (history.payment > 0) history.payment else history.win
                actualPayout - history.amount
            }
            "lost", "lose", "loss" -> -history.amount
            "stand", "draw", "tie" -> 0L
            else -> {
                println("Unknown status '${history.status}' for trade ${history.uuid}")
                -history.amount
            }
        }
    }

    private fun getCurrentServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    private fun validateTradeData(trade: TradingHistoryNew): String? {
        return when {
            trade.status.lowercase() in listOf("won", "win") -> {
                when {
                    trade.win <= 0 && trade.payment <= 0 ->
                        "Win trade but both win(${trade.win}) and payment(${trade.payment}) are zero or negative"

                    trade.win > 0 && trade.payment > 0 && trade.win != trade.payment ->
                        "Win trade has different win(${trade.win}) and payment(${trade.payment}) values"

                    trade.amount <= 0 ->
                        "Win trade has invalid amount(${trade.amount})"

                    else -> null
                }
            }

            trade.status.lowercase() in listOf("lost", "lose", "loss", "failed") -> {
                when {
                    trade.amount <= 0 ->
                        "Lost trade has invalid amount(${trade.amount})"

                    else -> null
                }
            }

            else -> null
        }
    }

    private fun startTodayProfitResetScheduler() {
        viewModelScope.launch {
            while (true) {
                try {
                    val jakartaCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))

                    if (jakartaCalendar.get(Calendar.HOUR_OF_DAY) == 0 &&
                        jakartaCalendar.get(Calendar.MINUTE) == 0) {

                        println("MIDNIGHT DETECTED - Resetting today profit")

                        _todayProfit.value = 0L
                        _todayStats.value = TodayStats()
                        _uiState.value = _uiState.value.copy(todayProfit = 0L)

                        delay(70000L)
                    } else {
                        delay(30000L)
                    }

                } catch (e: Exception) {
                    println("Error in today profit reset scheduler: ${e.message}")
                    delay(60000L)
                }
            }
        }
    }

    fun validateTodayProfitCalculation(): Map<String, Any> {
        return try {
            val historyList = _historyList.value
            val isDemoAccount = _uiState.value.isDemoAccount

            val result = todayProfitCalculator.calculateTodayProfit(historyList, isDemoAccount)
            val warnings = todayProfitCalculator.validateResults(result)
            val calculatorStatus = todayProfitCalculator.getCalculatorStatus()

            val uiProfit = _todayProfit.value
            val calculatedProfit = result.totalProfit
            val isConsistent = (uiProfit == calculatedProfit)

            mapOf(
                "validation_status" to if (isConsistent) "CONSISTENT" else "INCONSISTENT",
                "ui_profit" to (uiProfit / 100.0),
                "calculated_profit" to (calculatedProfit / 100.0),
                "difference" to ((calculatedProfit - uiProfit) / 100.0),
                "warnings_count" to warnings.size,
                "warnings" to warnings,
                "debug_info" to result.debugInfo,
                "calculator_status" to calculatorStatus,
                "calculation_timestamp" to System.currentTimeMillis(),
                "stability_features" to mapOf(
                    "incremental_calculation" to "ENABLED",
                    "state_tracking" to "ENABLED",
                    "anti_fluctuation" to "ENABLED",
                    "new_day_auto_reset" to "ENABLED"
                ),
                "data_integrity" to mapOf(
                    "total_history" to historyList.size,
                    "account_type" to if (isDemoAccount) "demo" else "real",
                    "timezone" to "Asia/Jakarta",
                    "method" to "STABLE_INCREMENTAL"
                )
            )
        } catch (e: Exception) {
            mapOf(
                "validation_status" to "ERROR",
                "error" to (e.message ?: "Unknown error"),
                "ui_profit" to (_todayProfit.value / 100.0)
            )
        }
    }

    fun getProcessedTradesToday(): Map<String, Any> {
        return try {
            val processedTrades = todayProfitCalculator.getProcessedTrades()
            val calculationHistory = todayProfitCalculator.getCalculationHistory()

            mapOf(
                "processed_trades" to processedTrades.values.map { trade ->
                    mapOf(
                        "trade_id" to trade.tradeId,
                        "status" to trade.status,
                        "amount" to (trade.amount / 100.0),
                        "profit" to (trade.profit / 100.0),
                        "processed_time" to Date(trade.processedTime).toString(),
                        "account_type" to trade.accountType,
                        "trade_date" to trade.tradeDate
                    )
                },
                "calculation_history" to calculationHistory.map { snapshot ->
                    mapOf(
                        "timestamp" to Date(snapshot.timestamp).toString(),
                        "total_profit" to (snapshot.totalProfit / 100.0),
                        "new_trades" to snapshot.newTrades,
                        "changed_trades" to snapshot.changedTrades,
                        "total_processed" to snapshot.totalProcessed,
                        "trigger" to snapshot.trigger
                    )
                },
                "summary" to mapOf(
                    "total_processed" to processedTrades.size,
                    "calculation_count" to calculationHistory.size,
                    "last_calculation" to if (calculationHistory.isNotEmpty())
                        Date(calculationHistory.last().timestamp).toString() else "Never"
                )
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "processed_trades" to emptyList<Any>(),
                "calculation_history" to emptyList<Any>()
            )
        }
    }

    fun refreshTodayProfit() {
        println("Manual refresh today profit requested")
        val historyList = _historyList.value
        updateTodayProfitSingleSource(historyList)
    }

    fun getTodayProfitDebugInfo(): Map<String, Any> {
        return try {
            val historyList = _historyList.value
            val calculatorStatus = todayProfitCalculator.getCalculatorStatus()
            val processedTrades = todayProfitCalculator.getProcessedTrades()
            val calculationHistory = todayProfitCalculator.getCalculationHistory()
            val currentCurrency = _uiState.value.currencySettings.selectedCurrency

            val result = todayProfitCalculator.calculateTodayProfit(
                historyList = historyList,
                isDemoAccount = _uiState.value.isDemoAccount,
                forceFull = false,
                currencyCode = currentCurrency.code
            )

            val warnings = todayProfitCalculator.validateResults(result)

            mapOf(
                "current_profit_ui" to (_todayProfit.value / 100.0),
                "calculated_profit" to (result.totalProfit / 100.0),
                "profit_match" to (_todayProfit.value == result.totalProfit),
                "currency_code" to currentCurrency.code,
                "currency_symbol" to currentCurrency.symbol,
                "formatted_profit" to currentCurrency.formatAmount(_todayProfit.value),

                "calculation_method" to "STABLE_INCREMENTAL",
                "is_incremental" to result.isIncremental,
                "debug_info" to result.debugInfo,
                "validation_warnings" to warnings,

                "calculator_status" to calculatorStatus,
                "processed_trades_count" to processedTrades.size,
                "calculation_history" to calculationHistory,

                "statistics" to mapOf(
                    "win_count" to result.stats.winCount,
                    "lose_count" to result.stats.loseCount,
                    "draw_count" to result.stats.drawCount,
                    "total_trades" to result.stats.totalTrades,
                    "win_rate" to "${String.format("%.1f", result.stats.getWinRate())}%"
                ),

                "currency_info" to mapOf(
                    "current_currency" to currentCurrency.code,
                    "symbol" to currentCurrency.symbol,
                    "min_amount" to currentCurrency.formatAmount(currentCurrency.minAmountInCents),
                    "decimal_places" to currentCurrency.decimalPlaces,
                    "source" to "API"
                ),

                "timezone_info" to mapOf(
                    "calculation_timezone" to "Asia/Jakarta",
                    "server_offset_ms" to serverTimeOffset,
                    "current_server_time" to Date(serverTimeService?.getCurrentServerTimeMillis() ?: System.currentTimeMillis()),
                    "uses_server_time" to (serverTimeService != null)
                ),

                "data_source_info" to mapOf(
                    "total_history_records" to historyList.size,
                    "history_empty" to historyList.isEmpty(),
                    "account_type" to if (_uiState.value.isDemoAccount) "demo" else "real",
                    "last_update" to System.currentTimeMillis()
                ),

                "performance_info" to mapOf(
                    "anti_fluctuation" to "ENABLED",
                    "incremental_calculation" to "ENABLED",
                    "state_tracking" to "ENABLED",
                    "auto_new_day_reset" to "ENABLED",
                    "currency_aware" to "ENABLED"
                )
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "current_profit_ui" to (_todayProfit.value / 100.0),
                "calculation_method" to "ERROR_FALLBACK",
                "currency_code" to _uiState.value.currencySettings.selectedCurrency.code
            )
        }
    }

    private fun startIndicatorPredictionInfoUpdates() {
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isIndicatorModeActive && indicatorOrderManager.isActive()) {
                    try {
                        val predictionInfo = indicatorOrderManager.getPredictionInfo()
                        _indicatorPredictionInfo.value = predictionInfo

                        val (supportLevel, resistanceLevel) = indicatorOrderManager.getSupportResistanceLevels()
                        val lastValues = indicatorOrderManager.getLastIndicatorValues()

                        _uiState.value = _uiState.value.copy(
                            currentSupportLevel = supportLevel,
                            currentResistanceLevel = resistanceLevel,
                            lastIndicatorValues = lastValues
                        )

                    } catch (e: Exception) {
                        Log.e("DashboardViewModel", "Error updating prediction info: ${e.message}")
                    }
                } else {
                    _indicatorPredictionInfo.value = emptyMap()
                }

                delay(5000L)
            }
        }
    }

    fun getCurrentIndicatorPredictionInfo(): Map<String, Any> {
        return if (_uiState.value.isIndicatorModeActive) {
            try {
                indicatorOrderManager.getPredictionInfo()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error getting prediction info: ${e.message}")
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    fun forceRecalculateTodayProfit() {
        viewModelScope.launch {
            try {
                println("=== FORCE RECALCULATE TODAY PROFIT WITH CURRENCY ===")

                _isCalculatingTodayProfit.value = true
                val historyList = _historyList.value
                val currentCurrency = _uiState.value.currencySettings.selectedCurrency

                println("Currency: ${currentCurrency.code}")

                val result = todayProfitCalculator.forceFullRecalculation(
                    historyList = historyList,
                    isDemoAccount = _uiState.value.isDemoAccount,
                    currencyCode = currentCurrency.code
                )

                _todayProfit.value = result.totalProfit
                _todayStats.value = result.stats
                _uiState.value = _uiState.value.copy(todayProfit = result.totalProfit)

                println("FORCE RECALCULATION COMPLETE:")
                println("  Currency: ${currentCurrency.code}")
                println("  Final Profit: ${result.totalProfit / 100.0} ${currentCurrency.code}")
                println("  Formatted: ${currentCurrency.formatAmount(result.totalProfit)}")
                println("  Total Trades: ${result.stats.totalTrades}")

            } catch (e: Exception) {
                println("ERROR in force recalculation: ${e.message}")
            } finally {
                _isCalculatingTodayProfit.value = false
            }
        }
    }

    fun formatTodayProfitWithCurrency(profit: Long): String {
        val currency = _uiState.value.currencySettings.selectedCurrency
        return currency.formatAmount(profit)
    }

    fun manualProfitCorrection(adjustmentCents: Long, reason: String) {
        viewModelScope.launch {
            try {
                println("=== MANUAL PROFIT CORRECTION ===")
                println("Adjustment: ${adjustmentCents / 100.0} IDR")
                println("Reason: $reason")

                todayProfitCalculator.manualCorrection(adjustmentCents, reason)

                val calculatorStatus = todayProfitCalculator.getCalculatorStatus()
                val newProfit = calculatorStatus["cached_total_profit"] as Long

                _todayProfit.value = newProfit
                _uiState.value = _uiState.value.copy(todayProfit = newProfit)

                println("Manual correction applied. New total: ${newProfit / 100.0} IDR")

            } catch (e: Exception) {
                println("ERROR in manual correction: ${e.message}")
            }
        }
    }

    fun updateHistoryList(historyList: List<TradingHistoryNew>) {
        println("DashboardViewModel: Updating history list with ${historyList.size} items")

        _historyList.value = historyList

        updateTodayProfitSingleSource(historyList)

        updateTradingSession(historyList)

        localStatsTracker.initializeOrReset(_uiState.value.isDemoAccount)

        checkStopConditionsAfterUpdate()
    }

    private fun handleTradeResultForLocalStats(
        tradeId: String?,
        orderId: String,
        result: String,
        isMartingaleAttempt: Boolean = false,
        martingaleStep: Int = 0,
        maxMartingaleSteps: Int = 5
    ) {
        try {
            val safeTradeId = tradeId ?: "generated_${orderId}_${System.currentTimeMillis()}"

            localStatsTracker.handleTradeResult(
                tradeId = safeTradeId,
                orderId = orderId,
                result = result,
                isMartingaleAttempt = isMartingaleAttempt,
                martingaleStep = martingaleStep,
                maxMartingaleSteps = maxMartingaleSteps
            )

            println("Local stats updated for trade: $safeTradeId | Result: $result | Martingale: $isMartingaleAttempt (step $martingaleStep)")
        } catch (e: Exception) {
            println("Error updating local stats: ${e.message}")
        }
    }

    private fun handleMartingaleCompletionForLocalStats(
        orderId: String,
        isWin: Boolean,
        finalStep: Int,
        tradeId: String? = null
    ) {
        try {
            val safeTradeId = tradeId ?: "martingale_completion_${orderId}_${finalStep}_${System.currentTimeMillis()}"

            println("🔥 MARTINGALE COMPLETION FOR LOCAL STATS:")
            println("   Order ID: $orderId")
            println("   Is Win: $isWin")
            println("   Final Step: $finalStep")
            println("   Trade ID: $safeTradeId")

            localStatsTracker.handleMartingaleCompletion(
                orderId = orderId,
                isWin = isWin,
                finalStep = finalStep,
                tradeId = safeTradeId
            )

            println("✅ Local stats martingale completion: $orderId | Win: $isWin | Final step: $finalStep | TradeId: $safeTradeId")
        } catch (e: Exception) {
            println("❌ Error handling martingale completion for local stats: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updateTradingSession(historyList: List<TradingHistoryNew>) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val updatedSession = stopLossProfitManager.calculateSessionFromHistory(
                    historyList = historyList,
                    isDemoAccount = currentState.isDemoAccount,
                    sessionStartTime = currentState.tradingSession.startTime
                )

                _uiState.value = currentState.copy(tradingSession = updatedSession)

                println("TRADING SESSION UPDATED (separate from today profit):")
                println("  Net Profit: ${updatedSession.getNetProfit() / 100.0} IDR")
                println("  Total Trades: ${updatedSession.totalTrades}")

            } catch (e: Exception) {
                println("ERROR updating trading session: ${e.message}")
            }
        }
    }

    private fun updateTodayProfitSingleSource(historyList: List<TradingHistoryNew>) {
        viewModelScope.launch {
            try {
                _isCalculatingTodayProfit.value = true

                println("=== UPDATING TODAY PROFIT (STABLE VERSION WITH CURRENCY) ===")

                val currentCurrency = _uiState.value.currencySettings.selectedCurrency
                println("Currency: ${currentCurrency.code}")

                val result = todayProfitCalculator.calculateTodayProfit(
                    historyList = historyList,
                    isDemoAccount = _uiState.value.isDemoAccount,
                    forceFull = false,
                    currencyCode = currentCurrency.code
                )

                val warnings = todayProfitCalculator.validateResults(result)
                warnings.forEach { warning ->
                    println("TODAY PROFIT WARNING: $warning")
                }

                _todayProfit.value = result.totalProfit
                _todayStats.value = result.stats
                _uiState.value = _uiState.value.copy(todayProfit = result.totalProfit)

                val calculatorStatus = todayProfitCalculator.getCalculatorStatus()
                println("TODAY PROFIT UPDATED (${if (result.isIncremental) "INCREMENTAL" else "FULL"}):")
                println("  Currency: ${currentCurrency.code} (${currentCurrency.symbol})")
                println("  Profit: ${result.totalProfit / 100.0} ${currentCurrency.code}")
                println("  Formatted: ${currentCurrency.formatAmount(result.totalProfit)}")
                println("  Stats: Win=${result.stats.winCount}, Lose=${result.stats.loseCount}, Draw=${result.stats.drawCount}")
                println("  Method: STABLE_INCREMENTAL")
                println("  Processed Trades: ${calculatorStatus["processed_trades_count"]}")
                println("==================================================")

            } catch (e: Exception) {
                println("ERROR updating today profit: ${e.message}")
                e.printStackTrace()
            } finally {
                _isCalculatingTodayProfit.value = false
            }
        }
    }

    private fun checkStopConditionsAfterUpdate() {
        viewModelScope.launch {
            delay(100)
            stopLossProfitManager.checkStopConditions(
                _uiState.value.stopLossSettings,
                _uiState.value.stopProfitSettings
            )
        }
    }

    private fun initializeManagers() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.stockity.id/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        serverTimeService = ServerTimeService(
            scope = viewModelScope,
            onServerTimeSync = { offset ->
                serverTimeOffset = offset
                tradeManager.updateServerTimeOffset(offset)
                martingaleManager.updateServerTimeOffset(offset)
                continuousTradeMonitor.updateServerTimeOffset(offset)
                followOrderManager.updateServerTimeOffset(offset)
                indicatorOrderManager.updateServerTimeOffset(offset)
                ServerTimeService.cachedServerTimeOffset = offset

                val offsetInSeconds = offset / 1000.0
                _uiState.value = _uiState.value.copy(
                    connectionStatus = "Server tersinkronisasi (offset ${
                        String.format("%.1f", offsetInSeconds)
                    } detik)"
                )
            }
        )

        todayProfitCalculator = TodayProfitCalculator(serverTimeService)

        stopLossProfitManager = StopLossProfitManager(
            scope = viewModelScope,
            onStopTriggered = { type, reason ->
                handleStopLossProfitTriggered(type, reason)
            }
        )

        webSocketManager = WebSocketManager(
            scope = viewModelScope,
            onConnectionStatusChange = { isConnected, status ->
                _uiState.value = _uiState.value.copy(
                    isWebSocketConnected = isConnected,
                    connectionStatus = status
                )

                if (!isConnected && (_uiState.value.botState == BotState.RUNNING ||
                            _uiState.value.isFollowModeActive ||
                            _uiState.value.isIndicatorModeActive)
                ) {
                    viewModelScope.launch {
                        delay(5000)
                        if (!webSocketManager.isConnectionHealthy() && !isConnected) {
                            _uiState.value = _uiState.value.copy(
                                connectionStatus = "Mencoba koneksi ulang otomatis..."
                            )
                            webSocketManager.forceReconnect()
                        }
                    }
                }
            },
            onTradeResponse = { ref, payload ->
                tradeManager.handleTradeResponse(ref, payload)
            },
            onWebSocketMessage = { _ -> },
            onTradeUpdate = { message ->
                martingaleManager.handleWebSocketTradeUpdate(message)
                continuousTradeMonitor.handleWebSocketTradeUpdate(message)
                followOrderManager.handleWebSocketTradeUpdate(message)
                indicatorOrderManager.handleWebSocketTradeUpdate(message)

                handleTradingEventForTodayProfit(message)
            },
            onTradeClosed = {
                refreshDashboardData()

                viewModelScope.launch {
                    println("onTradeClosed: Refreshing history and today profit")
                    _refreshTrigger.emit(System.currentTimeMillis())

                    delay(2000L)
                    refreshTodayProfit()
                }
            }
        )

        tradeManager = TradeManager(
            scope = viewModelScope,
            webSocketManager = webSocketManager,
            onTradeResult = { result -> handleTradeResult(result) },
            serverTimeService = serverTimeService
        )

        tradeManager.updateCurrency(_uiState.value.currencySettings.selectedCurrency)

        martingaleManager = MartingaleManager(
            scope = viewModelScope,
            onMartingaleResult = { result ->
                handleMartingaleResult(result)
            },
            onExecuteNextTrade = { trend, amount, scheduledOrderId ->
                executeInstantMartingaleTrade(trend, amount, scheduledOrderId)
            },
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            webSocketManager = webSocketManager,
            onStepUpdate = { orderId, step ->
                println("🔗 VM: Received step update - Order: $orderId, Step: $step")
                scheduleManager.updateMartingaleStepRealtime(orderId, step)
            }
        )

        continuousTradeMonitor = ContinuousTradeMonitor(
            scope = viewModelScope,
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            onTradeResultDetected = { scheduledOrderId, isWin, details ->
                handleInstantTradeResult(scheduledOrderId, isWin, details)
            },
            serverTimeService = serverTimeService
        )

        scheduleManager = ScheduleManager(
            scope = viewModelScope,
            onScheduledOrdersUpdate = { orders ->
                _scheduledOrders.value = orders
                checkBotAutoStop()
            },
            onExecuteScheduledTrade = { trend, orderId ->
                executeScheduledTrade(trend, orderId)
            },
            onAllSchedulesCompleted = {
                handleAllSchedulesCompleted()
            },
            sessionManager = sessionManager,
            onMartingaleStepUpdate = { orderId, step ->
                println("🔄 VM: Martingale step update - Order: $orderId, Step: $step")
            }
        )

        scheduleManager.loadScheduledOrdersFromStorage()

        assetManager = AssetManager(
            scope = viewModelScope,
            onAssetsUpdate = { assets ->
                _assets.value = assets
                _uiState.value = _uiState.value.copy(
                    connectionStatus = "Assets berhasil dimuat: ${assets.size} tersedia"
                )
            },
            onLoadingStateChange = { isLoading ->
                _uiState.value = _uiState.value.copy(assetsLoading = isLoading)
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        )

        followOrderManager = FollowOrderManager(
            scope = viewModelScope,
            onFollowOrdersUpdate = { orders ->
                _followOrders.value = orders
            },
            onExecuteFollowTrade = { trend, followOrderId, amount, isBoundaryMode ->
                executeFollowTrade(trend, followOrderId, amount, isBoundaryMode)
            },
            onModeStatusUpdate = { status ->
                _uiState.value = _uiState.value.copy(followOrderStatus = status)
            },
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            serverTimeService = serverTimeService,
            onFollowMartingaleResult = { result ->
                handleFollowMartingaleResult(result)
            },
            tradeManager = tradeManager,
            onFollowTradeStatsUpdate = { tradeId, orderId, result ->
                localStatsTracker.handleTradeResult(
                    tradeId = tradeId,
                    orderId = orderId,
                    result = result,
                    isMartingaleAttempt = false,
                    martingaleStep = 0,
                    maxMartingaleSteps = _uiState.value.martingaleSettings.maxSteps
                )
            }
        )

        indicatorOrderManager = IndicatorOrderManager(
            scope = viewModelScope,
            onIndicatorOrdersUpdate = { orders ->
                _indicatorOrders.value = orders
            },
            onExecuteIndicatorTrade = { trend, indicatorOrderId, amount ->
                executeIndicatorTrade(trend, indicatorOrderId, amount)
            },
            onModeStatusUpdate = { status ->
                _uiState.value = _uiState.value.copy(indicatorOrderStatus = status)
            },
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            serverTimeService = serverTimeService,
            onIndicatorMartingaleResult = { result ->
                handleIndicatorMartingaleResult(result)
            },
            priceApi = retrofit.create(PriceDataApi::class.java),
            onIndicatorModeComplete = { reason, message ->
                handleIndicatorModeCompletion(reason, message)
            }
        )

        ctcOrderManager = CTCOrderManager(
            scope = viewModelScope,
            onCTCOrdersUpdate = { orders ->
                _ctcOrders.value = orders
            },
            onExecuteCTCTrade = { trend, ctcOrderId, amount, isBoundaryMode ->
                executeCTCTrade(trend, ctcOrderId, amount, isBoundaryMode)
            },
            onModeStatusUpdate = { status ->
                _uiState.value = _uiState.value.copy(ctcOrderStatus = status)
            },
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            serverTimeService = serverTimeService,
            onCTCMartingaleResult = { result ->
                handleCTCMartingaleResult(result)
            },
            tradeManager = tradeManager,
            onCTCTradeStatsUpdate = { tradeId, orderId, result ->
                localStatsTracker.handleTradeResult(
                    tradeId = tradeId,
                    orderId = orderId,
                    result = result,
                    isMartingaleAttempt = false,
                    martingaleStep = 0,
                    maxMartingaleSteps = _uiState.value.martingaleSettings.maxSteps
                )
            }
        )

        multiMomentumOrderManager = MultiMomentumOrderManager(
            scope = viewModelScope,
            onMultiMomentumOrdersUpdate = { orders ->
                _multiMomentumOrders.value = orders
            },
            onExecuteMultiMomentumTrade = { trend, orderId, amount, momentumType ->
                executeMultiMomentumTrade(trend, orderId, amount, momentumType)
            },
            onModeStatusUpdate = { status ->
                _uiState.value = _uiState.value.copy(multiMomentumOrderStatus = status)
            },
            getUserSession = {
                val session = loginRepository.getUserSession()
                if (session != null) {
                    UserSession(
                        authtoken = session.authtoken ?: "",
                        deviceType = session.deviceType ?: "",
                        deviceId = session.deviceId ?: "",
                        email = session.email ?: "",
                        userAgent = session.userAgent ?: ""
                    )
                } else null
            },
            serverTimeService = serverTimeService,
            onMultiMomentumMartingaleResult = { result ->
                handleMultiMomentumMartingaleResult(result)
            },
            tradeManager = tradeManager,
            onMultiMomentumTradeStatsUpdate = { tradeId, orderId, result ->
                localStatsTracker.handleTradeResult(
                    tradeId = tradeId,
                    orderId = orderId,
                    result = result,
                    isMartingaleAttempt = false,
                    martingaleStep = 0,
                    maxMartingaleSteps = _uiState.value.martingaleSettings.maxSteps
                )
            }
        )
    }

    private fun startConnectionMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(60000)

                val currentState = _uiState.value

                if (currentState.botState == BotState.RUNNING ||
                    currentState.isFollowModeActive ||
                    currentState.isIndicatorModeActive ||
                    currentState.isCTCModeActive
                ) {

                    val isHealthy = webSocketManager.isConnectionHealthy()
                    val isConnected = currentState.isWebSocketConnected

                    if (!isHealthy && isConnected) {
                        val modeInfo = when {
                            currentState.isCTCModeActive -> "CTC Order ULTRA-FAST mode"
                            currentState.isFollowModeActive -> "Follow Order ULTRA-FAST mode"
                            currentState.isIndicatorModeActive -> "Indicator Order mode"
                            else -> "Schedule mode"
                        }

                        _uiState.value = currentState.copy(
                            connectionStatus = "Mendeteksi masalah koneksi dalam $modeInfo, mencoba perbaikan..."
                        )

                        webSocketManager.forceReconnect()
                    }
                }
            }
        }
    }

    private fun handleIndicatorModeCompletion(reason: String, message: String) {
        val currentState = _uiState.value

        if (!currentState.isIndicatorModeActive) {
            Log.w("DashboardViewModel", "Received completion signal but indicator mode not active")
            return
        }

        Log.d("DashboardViewModel", "ENHANCED: Indicator Mode Completion: $reason - $message")

        val shouldAutoRestart = when (reason) {
            "INDICATOR_WIN" -> true
            "MARTINGALE_WIN" -> true
            "SINGLE_LOSS" -> true
            "MARTINGALE_FAILED" -> true
            "CONSECUTIVE_LOSS" -> false
            "MAX_RESTARTS" -> false
            "RESTART_FAILED" -> false
            "RESTART_ERROR" -> false
            else -> false
        }

        if (shouldAutoRestart) {
            Log.d("DashboardViewModel", "ENHANCED: Indicator completion will trigger auto-restart")

            _uiState.value = currentState.copy(
                indicatorOrderStatus = "Completing cycle - Auto-restart in progress...",
                error = null
            )

            return
        }

        Log.d("DashboardViewModel", "ENHANCED: Stopping indicator mode (reason: $reason)")

        viewModelScope.launch {
            try {
                _indicatorPredictionInfo.value = emptyMap()

                val stopResult = indicatorOrderManager.stopIndicatorMode()

                if (stopResult.isSuccess) {
                    _uiState.value = currentState.copy(
                        isIndicatorModeActive = false,
                        tradingMode = TradingMode.SCHEDULE,
                        indicatorOrderStatus = message,
                        activeIndicatorOrderId = null,
                        indicatorMartingaleStep = 0,
                        currentSupportLevel = 0.0,
                        currentResistanceLevel = 0.0,
                        lastIndicatorValues = null,
                        connectionStatus = "Connected - Indicator mode stopped: $reason",
                        error = null
                    )

                    Log.d("DashboardViewModel", "ENHANCED: Indicator mode stopped successfully")
                    Log.d("DashboardViewModel", "  Reason: $reason")
                    Log.d("DashboardViewModel", "  Message: $message")

                    when (reason) {
                        "CONSECUTIVE_LOSS" -> {
                            Log.i("DashboardViewModel", "⛔ INDICATOR CONSECUTIVE LOSS LIMIT - Mode stopped")
                        }
                        "MAX_RESTARTS" -> {
                            Log.i("DashboardViewModel", "⛔ INDICATOR MAX RESTARTS - Mode stopped")
                        }
                        "RESTART_FAILED", "RESTART_ERROR" -> {
                            Log.i("DashboardViewModel", "⛔ INDICATOR RESTART ERROR - Mode stopped")
                        }
                    }

                } else {
                    _uiState.value = currentState.copy(
                        error = "Failed to stop indicator mode: ${stopResult.exceptionOrNull()?.message}",
                        indicatorOrderStatus = "Stop failed: $message"
                    )
                }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "ENHANCED: Error handling indicator mode completion: ${e.message}", e)
                _uiState.value = currentState.copy(
                    error = "Error during indicator completion: ${e.message}",
                    indicatorOrderStatus = "Completion error: $message"
                )
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val userSession = loginRepository.getUserSession()
                _uiState.value = _uiState.value.copy(
                    userEmail = userSession?.email ?: "Tidak diketahui",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memuat profil: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun handleTradingEventForTodayProfit(message: org.json.JSONObject) {
        try {
            val event = message.optString("event", "")

            if (event in listOf("closed", "deal_result")) {
                val payload = message.optJSONObject("payload")
                val status = payload?.optString("status", "") ?: ""

                if (status.lowercase() in listOf("won", "lost", "win", "lose", "loss", "stand", "draw")) {
                    // ADD THIS:
                    if (_uiState.value.isMultiMomentumModeActive) {
                        multiMomentumOrderManager.handleWebSocketTradeUpdate(message)
                    }
                    if (_uiState.value.isCTCModeActive) {
                        ctcOrderManager.handleWebSocketTradeUpdate(message)
                    }
                    if (_uiState.value.isFollowModeActive) {
                        followOrderManager.handleWebSocketTradeUpdate(message)
                    }
                    if (_uiState.value.isIndicatorModeActive) {
                        indicatorOrderManager.handleWebSocketTradeUpdate(message)
                    }

                    viewModelScope.launch {
                        delay(500L)
                        _refreshTrigger.emit(System.currentTimeMillis())
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling trading event: ${e.message}")
        }
    }

    private fun syncServerTime() {
        viewModelScope.launch {
            try {
                val userSession = loginRepository.getUserSession()
                if (userSession?.authtoken == null) {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "Tidak ada token autentikasi untuk sinkronisasi server"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    connectionStatus = "Menyinkronkan waktu server..."
                )

                serverTimeService.synchronizeServerTime(
                    authToken = userSession.authtoken,
                    deviceType = userSession.deviceType,
                    deviceId = userSession.deviceId
                )

                delay(3000)
                if (serverTimeOffset == 0L) {
                    serverTimeService.syncUsingAssetEndpoint(
                        authToken = userSession.authtoken,
                        deviceType = userSession.deviceType,
                        deviceId = userSession.deviceId
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = "Sinkronisasi server gagal, menggunakan waktu lokal"
                )
            }
        }
    }

    private fun fetchAssetsFromApi() {
        viewModelScope.launch {
            try {
                val userSession = loginRepository.getUserSession()
                if (userSession?.authtoken == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Token autentikasi tidak tersedia untuk mengambil data aset"
                    )
                    return@launch
                }

                assetManager.fetchAssetsFromApi(
                    authToken = userSession.authtoken,
                    deviceType = userSession.deviceType,
                    deviceId = userSession.deviceId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal menginisialisasi pengambilan data aset: ${e.message}"
                )
            }
        }
    }

    private fun connectToWebSocket() {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 3

            while (attempts < maxAttempts) {
                try {
                    attempts++

                    val userSession = loginRepository.getUserSession()
                    if (userSession?.authtoken == null) {
                        _uiState.value = _uiState.value.copy(
                            error = "Token autentikasi tidak tersedia",
                            isWebSocketConnected = false,
                            connectionStatus = "Token tidak tersedia untuk koneksi"
                        )
                        return@launch
                    }

                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "Mempersiapkan koneksi WebSocket... (percobaan $attempts/$maxAttempts)"
                    )

                    webSocketManager.connectToWebSocket(
                        userAgent = userSession.userAgent ?: "StockityBot/1.0",
                        authToken = userSession.authtoken,
                        deviceType = userSession.deviceType ?: "android",
                        deviceId = userSession.deviceId ?: "unknown"
                    )

                    delay(15000)

                    if (webSocketManager.isConnectionHealthy()) {
                        break
                    } else if (attempts < maxAttempts) {
                        _uiState.value = _uiState.value.copy(
                            connectionStatus = "Koneksi gagal, mencoba lagi... ($attempts/$maxAttempts)"
                        )
                        delay(3000)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            connectionStatus = "Koneksi gagal setelah $maxAttempts percobaan",
                            error = "Tidak dapat terhubung ke WebSocket setelah $maxAttempts percobaan"
                        )
                    }

                } catch (e: Exception) {
                    if (attempts >= maxAttempts) {
                        _uiState.value = _uiState.value.copy(
                            isWebSocketConnected = false,
                            connectionStatus = "Pengaturan koneksi gagal: ${e.message}",
                            error = "Koneksi WebSocket gagal: ${e.message}"
                        )
                    } else {
                        delay(2000)
                    }
                }
            }
        }
    }

    fun forceReconnectWebSocket() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (webSocketManager.isConnectionHealthy()) {
                _uiState.value = currentState.copy(
                    connectionStatus = "Connection is already healthy"
                )
                return@launch
            }

            _uiState.value = currentState.copy(
                connectionStatus = "Force reconnecting WebSocket...",
                isWebSocketConnected = false
            )

            try {
                webSocketManager.forceReconnect()

                var attempts = 0
                val maxAttempts = 30

                while (attempts < maxAttempts) {
                    delay(500L)
                    attempts++

                    if (webSocketManager.isConnectionHealthy()) {
                        _uiState.value = _uiState.value.copy(
                            connectionStatus = "Force reconnect successful",
                            isWebSocketConnected = true
                        )
                        Log.d("DashboardViewModel", "Force reconnect successful after ${attempts * 500}ms")
                        return@launch
                    }

                    val remainingSeconds = ((maxAttempts - attempts) * 500) / 1000
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "Reconnecting... (${remainingSeconds}s remaining)"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    error = "Force reconnect timeout. Please check your internet connection.",
                    connectionStatus = "Reconnect failed - connection timeout"
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Force reconnect error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Force reconnect error: ${e.message}",
                    connectionStatus = "Reconnect failed - ${e.message}"
                )
            }
        }
    }

    fun startCTCMode() {
        val currentState = _uiState.value

        if (!ensureStableConnection()) {
            _uiState.value = currentState.copy(
                error = "WebSocket connection not stable. Please wait or force reconnect."
            )
            return
        }

        if (!currentState.canStartCTCMode()) {
            _uiState.value = currentState.copy(
                error = "Cannot start CTC Order: ${getCTCModeStartError(currentState)}"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(
                    ctcOrderStatus = "Starting CTC Order ULTRA-FAST mode...",
                    error = null
                )

                if (currentState.botState != BotState.STOPPED) {
                    stopBot()
                }
                if (currentState.isFollowModeActive) {
                    stopFollowMode()
                }
                if (currentState.isIndicatorModeActive) {
                    stopIndicatorMode()
                }

                delay(1000)

                if (!ensureStableConnection()) {
                    _uiState.value = currentState.copy(
                        error = "WebSocket connection became unstable during mode switching. Try force reconnect.",
                        ctcOrderStatus = "CTC Order inactive"
                    )
                    return@launch
                }

                stopLossProfitManager.startNewSession()

                Log.d("DashboardViewModel", "CTC MODE: Starting CTC Order Mode")
                Log.d("DashboardViewModel", "  Asset: ${selectedAsset.name}")
                Log.d("DashboardViewModel", "  Account: ${if (currentState.isDemoAccount) "Demo" else "Real"}")
                Log.d("DashboardViewModel", "  Server time: ${serverTimeService.getCurrentServerTimeMillis()}")
                Log.d("DashboardViewModel", "  Flow: Identical to Follow Order - Cycle-based → Price Analysis → Cached Trend → ULTRA-FAST Detection")

                val result = ctcOrderManager.startCTCMode(
                    selectedAsset = selectedAsset,
                    isDemoAccount = currentState.isDemoAccount,
                    martingaleSettings = currentState.martingaleSettings
                )

                result.fold(
                    onSuccess = { message ->
                        _uiState.value = currentState.copy(
                            isCTCModeActive = true,
                            tradingMode = TradingMode.CTC_ORDER,
                            ctcOrderStatus = "CTC Order ULTRA-FAST aktif - ${selectedAsset.name}",
                            tradingSession = stopLossProfitManager.getCurrentSession(),
                            connectionStatus = "Connected - CTC Order ULTRA-FAST active",
                            error = null
                        )

                        Log.d("DashboardViewModel", "CTC MODE: CTC Order mode started successfully")
                        Log.d("DashboardViewModel", "  Detection speed: Same as Follow Order")
                        Log.d("DashboardViewModel", "  Monitoring interval: 50ms ultra-fast")
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            error = exception.message,
                            ctcOrderStatus = "CTC Order inactive"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "CTC MODE: Error starting mode: ${e.message}", e)
                _uiState.value = currentState.copy(
                    error = "Error starting CTC Order ULTRA-FAST: ${e.message}",
                    ctcOrderStatus = "CTC Order inactive"
                )
            }
        }
    }

    fun stopCTCMode() {
        val currentState = _uiState.value

        if (!currentState.canStopCTCMode()) {
            _uiState.value = currentState.copy(
                error = "CTC Order mode is not active"
            )
            return
        }

        Log.d("DashboardViewModel", "Manual stop of CTC Order mode requested")

        val result = ctcOrderManager.stopCTCMode()

        result.fold(
            onSuccess = { message ->
                _uiState.value = currentState.copy(
                    isCTCModeActive = false,
                    tradingMode = TradingMode.SCHEDULE,
                    ctcOrderStatus = "CTC Order manually stopped",
                    activeCTCOrderId = null,
                    ctcMartingaleStep = 0,
                    connectionStatus = "Connected - Mode can be changed",
                    error = null
                )

                Log.d("DashboardViewModel", "CTC Order mode manually stopped")
            },
            onFailure = { exception ->
                _uiState.value = currentState.copy(
                    error = exception.message
                )
            }
        )
    }

    private fun executeCTCTrade(trend: String, ctcOrderId: String, amount: Long, isBoundaryMode: Boolean) {
        val currentState = _uiState.value

        if (!currentState.isCTCModeActive) {
            Log.w("DashboardViewModel", "CTC mode not active, ignoring execution request")
            return
        }

        val (shouldPreventTrade, preventReason) = stopLossProfitManager.shouldPreventNewTrade(
            currentState.stopLossSettings,
            currentState.stopProfitSettings
        )

        if (shouldPreventTrade) {
            Log.w("DashboardViewModel", "CTC Trade prevented: $preventReason")
            _uiState.value = currentState.copy(
                ctcOrderStatus = "CTC Trade dicegah: $preventReason"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) {
            Log.w("DashboardViewModel", "WebSocket not ready for CTC Trade")
            return
        }

        val isMartingaleAttempt = ctcOrderManager.isMartingaleActive()
        val martingaleStep = if (isMartingaleAttempt) ctcOrderManager.getCurrentMartingaleStep() else 0

        Log.d("DashboardViewModel", "CTC: Executing ${if (isBoundaryMode) "boundary" else "instant"} order")
        _uiState.value = currentState.copy(
            activeCTCOrderId = ctcOrderId,
            ctcMartingaleStep = martingaleStep,
            ctcOrderStatus = if (isMartingaleAttempt) {
                "CTC: Martingale Step $martingaleStep - ${formatAmount(amount)}"
            } else {
                "CTC: Eksekusi ${if (isBoundaryMode) "boundary" else "instant"} $trend - ${formatAmount(amount)}"
            }
        )

        if (isBoundaryMode) {
            tradeManager.executeFollowBoundaryTrade(
                assetRic = selectedAsset.ric,
                trend = trend,
                amount = amount,
                isDemoAccount = currentState.isDemoAccount,
                followOrderId = ctcOrderId
            )
        } else {
            tradeManager.executeFollowInstantTrade(
                assetRic = selectedAsset.ric,
                trend = trend,
                amount = amount,
                isDemoAccount = currentState.isDemoAccount,
                followOrderId = ctcOrderId,
                isMartingaleAttempt = isMartingaleAttempt,
                martingaleStep = martingaleStep
            )
        }
    }

    private fun handleCTCMartingaleResult(result: CTCMartingaleResult) {
        val currentState = _uiState.value

        Log.d("DashboardViewModel", "CTC Martingale result:")
        Log.d("DashboardViewModel", "  Step: ${result.step}")
        Log.d("DashboardViewModel", "  Is Win: ${result.isWin}")
        Log.d("DashboardViewModel", "  Should Continue: ${result.shouldContinue}")
        Log.d("DashboardViewModel", "  Is Max Reached: ${result.isMaxReached}")

        when {
            result.isWin -> {
                _uiState.value = currentState.copy(
                    activeCTCOrderId = null,
                    ctcMartingaleStep = 0,
                    ctcOrderStatus = "CTC Martingale WIN - Continuing cycle"
                )

                Log.d("DashboardViewModel", "CTC Martingale WIN at step ${result.step}")

                handleMartingaleCompletionForLocalStats(
                    orderId = currentState.activeCTCOrderId ?: "ctc_${System.currentTimeMillis()}",
                    isWin = true,
                    finalStep = result.step
                )
            }

            result.shouldContinue -> {
                _uiState.value = currentState.copy(
                    ctcMartingaleStep = result.step,
                    ctcOrderStatus = "CTC Martingale Step ${result.step} - ${formatAmount(result.amount)}"
                )

                Log.d("DashboardViewModel", "CTC Martingale continues to step ${result.step}")
            }

            result.isMaxReached -> {
                _uiState.value = currentState.copy(
                    activeCTCOrderId = null,
                    ctcMartingaleStep = 0,
                    ctcOrderStatus = "CTC Martingale failed - Starting new cycle"
                )

                Log.d("DashboardViewModel", "CTC Martingale failed at step ${result.step}")

                handleMartingaleCompletionForLocalStats(
                    orderId = currentState.activeCTCOrderId ?: "ctc_${System.currentTimeMillis()}",
                    isWin = false,
                    finalStep = result.step
                )
            }
        }
    }

    private fun getCTCModeStartError(state: DashboardUiState): String {
        return when {
            state.selectedAsset == null -> "Belum ada aset yang dipilih untuk CTC Order"
            !state.isWebSocketConnected -> "WebSocket tidak terhubung"
            !webSocketManager.isRequiredChannelsReady() -> "WebSocket channels belum siap"
            state.isCTCModeActive -> "CTC Order mode sudah aktif"
            state.isFollowModeActive -> "Follow Order mode masih aktif, hentikan terlebih dahulu"
            state.isIndicatorModeActive -> "Indicator Order mode masih aktif, hentikan terlebih dahulu"
            state.botState != BotState.STOPPED -> "Schedule mode masih aktif, hentikan terlebih dahulu"
            state.martingaleSettings.validate(state.currencySettings.selectedCurrency).isFailure ->
                "Pengaturan martingale tidak valid: ${state.getMartingaleValidationError()}"
            state.stopLossSettings.validate().isFailure -> "Stop loss settings tidak valid"
            state.stopProfitSettings.validate().isFailure -> "Stop profit settings tidak valid"
            else -> "Kondisi tidak memenuhi syarat untuk memulai CTC Order"
        }
    }

    fun getCTCPerformanceInfo(): Map<String, Any> {
        return if (_uiState.value.isCTCModeActive) {
            val baseStats = ctcOrderManager.getPerformanceStats()
            val ultraFastStats = mapOf(
                "precision_mode" to "ULTRA_FAST_CTC_ORDER",
                "identical_to" to "FOLLOW_ORDER_MODE",
                "server_time_sync" to (serverTimeService.getCurrentServerTimeMillis() != 0L),
                "timing_method" to "SERVER_TIME_ONLY",
                "execution_modes" to mapOf(
                    "boundary" to "1_MINUTE_FORCED_EXPIRY",
                    "instant" to "REALTIME_EXECUTION",
                    "martingale" to "INSTANT_REALTIME"
                ),
                "connection_stability" to ensureStableConnection(),
                "monitoring_speed" to "SAME_AS_FOLLOW_ORDER",
                "detection_interval" to "50ms_ultra_fast",
                "api_prewarming" to "ENABLED",
                "websocket_priority" to "ENABLED",
                "balance_detection" to "ENABLED",
                "fallback_layers" to "TRIPLE_LAYER_DETECTION",
                "performance_mode" to "ULTRA_OPTIMIZED"
            )
            baseStats + ultraFastStats
        } else {
            mapOf(
                "is_active" to false,
                "message" to "CTC Order mode tidak aktif"
            )
        }
    }

    fun startIndicatorMode() {
        val currentState = _uiState.value

        if (!ensureStableConnection()) {
            _uiState.value = currentState.copy(
                error = "WebSocket connection not stable. Please wait or force reconnect."
            )
            return
        }

        if (!currentState.canStartIndicatorMode()) {
            _uiState.value = currentState.copy(
                error = "Cannot start Indicator Order: ${getIndicatorModeStartError(currentState)}"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(
                    indicatorOrderStatus = "Starting Indicator Order with auto-restart and ultra-fast monitoring...",
                    error = null
                )

                if (currentState.botState != BotState.STOPPED) {
                    stopBot()
                }
                if (currentState.isFollowModeActive) {
                    stopFollowMode()
                }

                delay(1000)

                if (!ensureStableConnection()) {
                    _uiState.value = currentState.copy(
                        error = "WebSocket connection became unstable during mode switching. Try force reconnect.",
                        indicatorOrderStatus = "Indicator Order inactive"
                    )
                    return@launch
                }

                stopLossProfitManager.startNewSession()

                Log.d("DashboardViewModel", "ENHANCED INDICATOR MODE: Starting with Auto-Restart + Ultra-Fast Monitoring")
                Log.d("DashboardViewModel", "  Asset: ${selectedAsset.name}")
                Log.d("DashboardViewModel", "  Account: ${if (currentState.isDemoAccount) "Demo" else "Real"}")
                Log.d("DashboardViewModel", "  Auto-restart: ENABLED")
                Log.d("DashboardViewModel", "  Detection speed: 50ms ultra-fast (same as Follow Order)")
                Log.d("DashboardViewModel", "  Multi-layer detection: WebSocket + Balance + API")
                Log.d("DashboardViewModel", "  Restart scenarios: WIN, MARTINGALE_WIN, SINGLE_LOSS, MARTINGALE_FAILED")

                val result = indicatorOrderManager.startIndicatorMode(
                    selectedAsset = selectedAsset,
                    isDemoAccount = currentState.isDemoAccount,
                    indicatorSettings = currentState.indicatorSettings,
                    martingaleSettings = currentState.martingaleSettings,
                    maxConsecutiveLosses = currentState.consecutiveLossSettings.maxConsecutiveLosses,
                    enableConsecutiveLossLimit = currentState.consecutiveLossSettings.isEnabled
                )

                result.fold(
                    onSuccess = { message ->
                        _uiState.value = currentState.copy(
                            isIndicatorModeActive = true,
                            tradingMode = TradingMode.INDICATOR_ORDER,
                            indicatorOrderStatus = "Indicator Order ACTIVE with AUTO-RESTART - ${selectedAsset.name} - Ultra-fast monitoring",
                            tradingSession = stopLossProfitManager.getCurrentSession(),
                            connectionStatus = "Connected - Indicator Order ACTIVE with auto-restart + ultra-fast monitoring",
                            error = null
                        )

                        viewModelScope.launch {
                            delay(2000)
                            try {
                                val predictionInfo = indicatorOrderManager.getPredictionInfo()
                                _indicatorPredictionInfo.value = predictionInfo

                                Log.d("DashboardViewModel", "ENHANCED INDICATOR: Initial prediction info loaded")
                                Log.d("DashboardViewModel", "  Predictions: ${predictionInfo["total_predictions"]}")
                                Log.d("DashboardViewModel", "  Auto-restart: ENABLED")
                                Log.d("DashboardViewModel", "  Detection: ULTRA-FAST")
                            } catch (e: Exception) {
                                Log.e("DashboardViewModel", "Error loading initial prediction info: ${e.message}")
                            }
                        }

                        Log.d("DashboardViewModel", "ENHANCED INDICATOR: Mode started successfully")
                        Log.d("DashboardViewModel", "  Auto-restart capability: ENABLED")
                        Log.d("DashboardViewModel", "  Ultra-fast monitoring: ACTIVE")
                        Log.d("DashboardViewModel", "  Detection speed: Same as Follow Order (50ms)")
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            error = exception.message,
                            indicatorOrderStatus = "Indicator Order inactive"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "ENHANCED INDICATOR: Error starting mode: ${e.message}", e)
                _uiState.value = currentState.copy(
                    error = "Error starting Indicator Order: ${e.message}",
                    indicatorOrderStatus = "Indicator Order inactive"
                )
            }
        }
    }

    fun stopIndicatorMode() {
        val currentState = _uiState.value

        if (!currentState.canStopIndicatorMode()) {
            _uiState.value = currentState.copy(
                error = "Indicator Order mode is not active"
            )
            return
        }

        Log.d("DashboardViewModel", "Manual stop of Indicator Order mode requested")

        val result = indicatorOrderManager.stopIndicatorMode()

        result.fold(
            onSuccess = { message ->
                _indicatorPredictionInfo.value = emptyMap()

                _uiState.value = currentState.copy(
                    isIndicatorModeActive = false,
                    tradingMode = TradingMode.SCHEDULE,
                    indicatorOrderStatus = "Indicator Order manually stopped",
                    activeIndicatorOrderId = null,
                    indicatorMartingaleStep = 0,
                    currentSupportLevel = 0.0,
                    currentResistanceLevel = 0.0,
                    lastIndicatorValues = null,
                    connectionStatus = "Connected - Mode can be changed",
                    error = null
                )

                Log.d("DashboardViewModel", "Indicator Order mode manually stopped")
                Log.d("DashboardViewModel", "  Continuous monitoring: STOPPED")
                Log.d("DashboardViewModel", "  Auto-stop: DISABLED")
            },
            onFailure = { exception ->
                _uiState.value = currentState.copy(
                    error = exception.message
                )
            }
        )
    }

    fun refreshIndicatorPredictions() {
        if (_uiState.value.isIndicatorModeActive) {
            viewModelScope.launch {
                try {
                    val predictionInfo = indicatorOrderManager.getPredictionInfo()
                    _indicatorPredictionInfo.value = predictionInfo

                    Log.d("DashboardViewModel", "Prediction info manually refreshed")
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error refreshing prediction info: ${e.message}")
                }
            }
        }
    }

    private fun executeIndicatorTrade(trend: String, indicatorOrderId: String, amount: Long) {
        val currentState = _uiState.value

        if (!currentState.isIndicatorModeActive) return

        val (shouldPreventTrade, preventReason) = stopLossProfitManager.shouldPreventNewTrade(
            currentState.stopLossSettings,
            currentState.stopProfitSettings
        )

        if (shouldPreventTrade) {
            Log.w("DashboardViewModel", "Indicator Trade prevented: $preventReason")
            _uiState.value = currentState.copy(
                indicatorOrderStatus = "Indicator Trade prevented: $preventReason"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) {
            Log.w("DashboardViewModel", "WebSocket not ready for Indicator Trade")
            return
        }

        val isMartingaleAttempt = indicatorOrderManager.isMartingaleActive()
        val martingaleStep = indicatorOrderManager.getCurrentMartingaleStep()

        _uiState.value = currentState.copy(
            activeIndicatorOrderId = indicatorOrderId,
            indicatorOrderStatus = if (isMartingaleAttempt) {
                "Executing Martingale Step $martingaleStep IMMEDIATE - ${formatAmount(amount)} - Ultra-fast monitoring"
            } else {
                "Executing Trade $trend BOUNDARY - ${formatAmount(amount)} - Focus monitoring"
            }
        )

        tradeManager.executeIndicatorTrade(
            assetRic = selectedAsset.ric,
            trend = trend,
            amount = amount,
            isDemoAccount = currentState.isDemoAccount,
            indicatorOrderId = indicatorOrderId,
            isMartingaleAttempt = isMartingaleAttempt,
            martingaleStep = martingaleStep
        )

        Log.d("DashboardViewModel", "Indicator Trade executed with differentiated timing:")
        Log.d("DashboardViewModel", "   - Order ID: $indicatorOrderId")
        Log.d("DashboardViewModel", "   - Trend: $trend")
        Log.d("DashboardViewModel", "   - Amount: ${formatAmount(amount)}")
        Log.d("DashboardViewModel", "   - Is Martingale: $isMartingaleAttempt")
        Log.d("DashboardViewModel", "   - Execution Mode: ${if (isMartingaleAttempt) "IMMEDIATE (realtime)" else "BOUNDARY (1-min forced)"}")
        Log.d("DashboardViewModel", "   - Monitoring: ACTIVE_TRADE_ONLY")
    }

    private fun handleIndicatorMartingaleResult(result: IndicatorMartingaleResult) {
        val currentState = _uiState.value

        Log.d("DashboardViewModel", "Indicator Martingale result:")
        Log.d("DashboardViewModel", "  Step: ${result.step}")
        Log.d("DashboardViewModel", "  Is Win: ${result.isWin}")
        Log.d("DashboardViewModel", "  Should Continue: ${result.shouldContinue}")
        Log.d("DashboardViewModel", "  Is Max Reached: ${result.isMaxReached}")

        when {
            result.isWin -> {
                _uiState.value = currentState.copy(
                    activeIndicatorOrderId = null,
                    indicatorMartingaleStep = 0,
                    indicatorOrderStatus = "Martingale WIN - Mode will auto-stop"
                )

                Log.d("DashboardViewModel", "Martingale WIN at step ${result.step} - Auto-stop will trigger")

                handleMartingaleCompletionForLocalStats(
                    orderId = currentState.activeIndicatorOrderId ?: "indicator_${System.currentTimeMillis()}",
                    isWin = true,
                    finalStep = result.step
                )
            }

            result.shouldContinue -> {
                _uiState.value = currentState.copy(
                    indicatorMartingaleStep = result.step,
                    indicatorOrderStatus = "Martingale Step ${result.step} - ${formatAmount(result.amount)} - Continue monitoring"
                )

                Log.d("DashboardViewModel", "Martingale continues to step ${result.step}")
            }

            result.isMaxReached -> {
                _uiState.value = currentState.copy(
                    activeIndicatorOrderId = null,
                    indicatorMartingaleStep = 0,
                    indicatorOrderStatus = "Martingale failed - Mode will auto-stop"
                )

                Log.d("DashboardViewModel", "Martingale failed at step ${result.step} - Auto-stop will trigger")

                handleMartingaleCompletionForLocalStats(
                    orderId = currentState.activeIndicatorOrderId ?: "indicator_${System.currentTimeMillis()}",
                    isWin = false,
                    finalStep = result.step
                )
            }
        }

        val (supportLevel, resistanceLevel) = indicatorOrderManager.getSupportResistanceLevels()
        val lastValues = indicatorOrderManager.getLastIndicatorValues()

        _uiState.value = _uiState.value.copy(
            currentSupportLevel = supportLevel,
            currentResistanceLevel = resistanceLevel,
            lastIndicatorValues = lastValues
        )
    }

    fun getLocalStatsDebugInfo(): Map<String, Any> {
        return localStatsTracker.getDebugInfo()
    }

    fun manualAddWin() {
        localStatsTracker.manualAddWin()
    }

    fun manualAddDraw() {
        localStatsTracker.manualAddDraw()
    }

    fun manualAddLose() {
        localStatsTracker.manualAddLose()
    }

    fun forceResetLocalStats() {
        localStatsTracker.forceReset(_uiState.value.isDemoAccount)
    }

    private fun getIndicatorModeStartError(state: DashboardUiState): String {
        return when {
            state.selectedAsset == null -> "Belum ada aset yang dipilih untuk Indicator Order"
            !state.isWebSocketConnected -> "WebSocket tidak terhubung"
            !webSocketManager.isRequiredChannelsReady() -> "WebSocket channels belum siap"
            state.isIndicatorModeActive -> "Indicator Order mode sudah aktif"
            state.isFollowModeActive -> "Follow Order mode masih aktif, hentikan terlebih dahulu"
            state.botState != BotState.STOPPED -> "Schedule mode masih aktif, hentikan terlebih dahulu"
            state.getIndicatorValidationError() != null -> "Pengaturan indicator tidak valid: ${state.getIndicatorValidationError()}"
            state.martingaleSettings.validate(state.currencySettings.selectedCurrency).isFailure ->
                "Pengaturan martingale tidak valid: ${state.getMartingaleValidationError()}"
            state.stopLossSettings.validate().isFailure -> "Stop loss settings tidak valid"
            state.stopProfitSettings.validate().isFailure -> "Stop profit settings tidak valid"
            else -> "Kondisi tidak memenuhi syarat untuk memulai Indicator Order"
        }
    }

    fun setIndicatorType(type: IndicatorType) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan indikator saat ada mode trading aktif"
            )
            return
        }

        val newSettings = IndicatorSettings.getDefaultSettings(type)
        _uiState.value = _uiState.value.copy(indicatorSettings = newSettings)
    }

    fun setIndicatorPeriod(period: Int) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.indicatorSettings
        val newSettings = currentSettings.copy(period = period)

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi indikator gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(indicatorSettings = newSettings)
    }

    fun setIndicatorRSILevels(overbought: BigDecimal, oversold: BigDecimal) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.indicatorSettings
        val newSettings = currentSettings.copy(
            rsiOverbought = overbought,
            rsiOversold = oversold
        )

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi RSI levels gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(indicatorSettings = newSettings)
    }

    fun setIndicatorSensitivity(sensitivity: BigDecimal) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.indicatorSettings
        val newSettings = currentSettings.copy(sensitivity = sensitivity)

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi sensitivity gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(indicatorSettings = newSettings)
    }

    fun setIndicatorSensitivityPreset(preset: String) {
        if (!_uiState.value.canModifySettings()) return

        val sensitivityValue = when (preset.lowercase()) {
            "low" -> IndicatorSettings.SENSITIVITY_LOW
            "medium" -> IndicatorSettings.SENSITIVITY_MEDIUM
            "high" -> IndicatorSettings.SENSITIVITY_HIGH
            "very_high", "v.high" -> IndicatorSettings.SENSITIVITY_VERY_HIGH
            "max", "maximum" -> IndicatorSettings.SENSITIVITY_MAX_LEVEL
            else -> return
        }

        setIndicatorSensitivity(sensitivityValue)
    }

    fun setIndicatorSensitivityFromString(sensitivityInput: String) {
        if (!_uiState.value.canModifySettings()) return

        try {
            val sensitivity = BigDecimal(sensitivityInput)
            setIndicatorSensitivity(sensitivity)
        } catch (e: NumberFormatException) {
            _uiState.value = _uiState.value.copy(
                error = "Format sensitivity tidak valid: harus berupa angka desimal"
            )
        }
    }

    fun setConsecutiveLossLimit(enabled: Boolean, maxLosses: Int = 5) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan consecutive loss saat ada mode trading aktif"
            )
            return
        }

        val newSettings = ConsecutiveLossSettings(
            isEnabled = enabled,
            maxConsecutiveLosses = maxLosses
        )

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi consecutive loss limit gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(consecutiveLossSettings = newSettings)
    }

    fun startFollowMode() {
        val currentState = _uiState.value

        if (!ensureStableConnection()) {
            _uiState.value = currentState.copy(
                error = "WebSocket connection not stable for ULTRA-FAST Follow Order execution. Please wait or force reconnect."
            )
            return
        }

        if (!currentState.canStartFollowMode()) {
            _uiState.value = currentState.copy(
                error = "Cannot start Follow Order: ${getFollowModeStartError(currentState)}"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(
                    followOrderStatus = "Starting Follow Order ULTRA-FAST mode...",
                    error = null
                )

                if (currentState.botState != BotState.STOPPED) {
                    stopBot()
                }
                if (currentState.isIndicatorModeActive) {
                    stopIndicatorMode()
                }

                delay(1000)

                if (!ensureStableConnection()) {
                    _uiState.value = currentState.copy(
                        error = "WebSocket connection became unstable during mode switching. Try force reconnect.",
                        followOrderStatus = "Follow Order inactive"
                    )
                    return@launch
                }

                stopLossProfitManager.startNewSession()

                Log.d("DashboardViewModel", "ULTRA-FAST MODE: Starting Follow Order Mode")
                Log.d("DashboardViewModel", "  Asset: ${selectedAsset.name}")
                Log.d("DashboardViewModel", "  Account: ${if (currentState.isDemoAccount) "Demo" else "Real"}")
                Log.d("DashboardViewModel", "  Server time: ${serverTimeService.getCurrentServerTimeMillis()}")
                Log.d("DashboardViewModel", "  Flow: Cycle-based → Price Analysis → Cached Trend → ULTRA-FAST Detection")
                Log.d("DashboardViewModel", "  Detection speed: Same as Schedule mode (50ms intervals)")

                val result = followOrderManager.startFollowMode(
                    selectedAsset = selectedAsset,
                    isDemoAccount = currentState.isDemoAccount,
                    martingaleSettings = currentState.martingaleSettings
                )

                result.fold(
                    onSuccess = { message ->
                        _uiState.value = currentState.copy(
                            isFollowModeActive = true,
                            tradingMode = TradingMode.FOLLOW_ORDER,
                            followOrderStatus = "Follow Order ULTRA-FAST aktif - ${selectedAsset.name}",
                            tradingSession = stopLossProfitManager.getCurrentSession(),
                            connectionStatus = "Connected - Follow Order ULTRA-FAST active",
                            error = null
                        )

                        Log.d("DashboardViewModel", "ULTRA-FAST MODE: Follow Order mode started successfully")
                        Log.d("DashboardViewModel", "  Detection speed: Same as Schedule mode")
                        Log.d("DashboardViewModel", "  Monitoring interval: 50ms ultra-fast")
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            error = exception.message,
                            followOrderStatus = "Follow Order inactive"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "ULTRA-FAST MODE: Error starting follow mode: ${e.message}", e)
                _uiState.value = currentState.copy(
                    error = "Error starting Follow Order ULTRA-FAST: ${e.message}",
                    followOrderStatus = "Follow Order inactive"
                )
            }
        }
    }

    fun stopFollowMode() {
        val currentState = _uiState.value

        if (!currentState.canStopFollowMode()) {
            _uiState.value = currentState.copy(
                error = "Follow Order mode is not active"
            )
            return
        }

        val result = followOrderManager.stopFollowMode()

        result.fold(
            onSuccess = { message ->
                _uiState.value = currentState.copy(
                    isFollowModeActive = false,
                    tradingMode = TradingMode.SCHEDULE,
                    followOrderStatus = "Follow Order inactive",
                    activeFollowOrderId = null,
                    followMartingaleStep = 0,
                    connectionStatus = "Connected - Mode can be changed",
                    error = null
                )

                Log.d("DashboardViewModel", "Follow Order mode stopped successfully")
            },
            onFailure = { exception ->
                _uiState.value = currentState.copy(
                    error = exception.message
                )
            }
        )
    }

    private fun ensureStableConnectionForFollowOrder(): Boolean {
        val connectionHealthy = webSocketManager.isConnectionHealthy()
        val channelsReady = webSocketManager.isRequiredChannelsReady()
        val isConnected = _uiState.value.isWebSocketConnected

        val connectionStats = webSocketManager.getConnectionStats()
        val reconnectionAttempts = connectionStats["reconnection_attempts"] as? Int ?: 0
        val timeSinceLastMessage = connectionStats["time_since_last_message_ms"] as? Long ?: 0L
        val isConnecting = connectionStats["is_connecting"] as? Boolean ?: false

        Log.d("DashboardViewModel", "PRECISION FIXED: Follow Order Connection Check")
        Log.d("DashboardViewModel", "  Healthy: $connectionHealthy")
        Log.d("DashboardViewModel", "  Channels: $channelsReady")
        Log.d("DashboardViewModel", "  Connected: $isConnected")
        Log.d("DashboardViewModel", "  Reconnect Attempts: $reconnectionAttempts")
        Log.d("DashboardViewModel", "  Time Since Msg: ${timeSinceLastMessage}ms")
        Log.d("DashboardViewModel", "  Is Connecting: $isConnecting")

        if (isConnecting || reconnectionAttempts > 0) {
            Log.w("DashboardViewModel", "PRECISION FIXED: Connection unstable for Follow Order - still connecting or recent reconnections")
            return false
        }

        if (timeSinceLastMessage > 20000) {
            Log.w("DashboardViewModel", "PRECISION FIXED: Connection unstable for Follow Order - no recent messages")
            return false
        }

        return connectionHealthy && channelsReady && isConnected
    }

    fun getFollowOrderPerformanceInfo(): Map<String, Any> {
        return if (_uiState.value.isFollowModeActive) {
            val baseStats = followOrderManager.getPerformanceStats()
            val ultraFastStats = mapOf(
                "precision_mode" to "ULTRA_FAST_FOLLOW_ORDER",
                "server_time_sync" to (serverTimeService.getCurrentServerTimeMillis() != 0L),
                "timing_method" to "SERVER_TIME_ONLY",
                "execution_modes" to mapOf(
                    "boundary" to "1_MINUTE_FORCED_EXPIRY",
                    "instant" to "REALTIME_EXECUTION",
                    "martingale" to "INSTANT_REALTIME"
                ),
                "connection_stability" to ensureStableConnection(),
                "monitoring_speed" to "SAME_AS_SCHEDULE_MODE",
                "detection_interval" to "50ms_ultra_fast",
                "api_prewarming" to "ENABLED",
                "websocket_priority" to "ENABLED",
                "balance_detection" to "ENABLED",
                "fallback_layers" to "TRIPLE_LAYER_DETECTION",
                "performance_mode" to "ULTRA_OPTIMIZED"
            )
            baseStats + ultraFastStats
        } else {
            mapOf(
                "is_active" to false,
                "message" to "Follow Order mode tidak aktif"
            )
        }
    }

    private fun executeFollowTrade(trend: String, followOrderId: String, amount: Long, isBoundaryMode: Boolean) {
        val currentState = _uiState.value

        if (!currentState.isFollowModeActive) {
            Log.w("DashboardViewModel", "Follow mode not active, ignoring execution request")
            return
        }

        val (shouldPreventTrade, preventReason) = stopLossProfitManager.shouldPreventNewTrade(
            currentState.stopLossSettings,
            currentState.stopProfitSettings
        )

        if (shouldPreventTrade) {
            Log.w("DashboardViewModel", "Follow Trade prevented: $preventReason")
            _uiState.value = currentState.copy(
                followOrderStatus = "Follow Trade dicegah: $preventReason"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) {
            Log.w("DashboardViewModel", "WebSocket not ready for Follow Trade")
            return
        }

        if (isBoundaryMode) {
            Log.d("DashboardViewModel", "CYCLE: Executing boundary order (first of cycle)")
            _uiState.value = currentState.copy(
                activeFollowOrderId = followOrderId,
                followOrderStatus = "Cycle: Eksekusi boundary $trend - ${formatAmount(amount)}"
            )

            tradeManager.executeFollowBoundaryTrade(
                assetRic = selectedAsset.ric,
                trend = trend,
                amount = amount,
                isDemoAccount = currentState.isDemoAccount,
                followOrderId = followOrderId
            )
        } else {
            val isMartingaleAttempt = followOrderManager.isMartingaleActive()
            val martingaleStep = if (isMartingaleAttempt) followOrderManager.getCurrentMartingaleStep() else 0

            Log.d("DashboardViewModel", "CYCLE: Executing immediate order (cached trend)")
            _uiState.value = currentState.copy(
                activeFollowOrderId = followOrderId,
                followMartingaleStep = martingaleStep,
                followOrderStatus = if (isMartingaleAttempt) {
                    "Cycle: Martingale Step $martingaleStep - ${formatAmount(amount)}"
                } else {
                    "Cycle: Eksekusi immediate $trend - ${formatAmount(amount)}"
                }
            )

            tradeManager.executeFollowInstantTrade(
                assetRic = selectedAsset.ric,
                trend = trend,
                amount = amount,
                isDemoAccount = currentState.isDemoAccount,
                followOrderId = followOrderId,
                isMartingaleAttempt = isMartingaleAttempt,
                martingaleStep = martingaleStep
            )
        }
    }

    private fun handleFollowMartingaleResult(result: FollowMartingaleResult) {
        val currentState = _uiState.value

        Log.d("DashboardViewModel", "PRECISION FIXED: Follow Martingale result received")
        Log.d("DashboardViewModel", "  Step: ${result.step}")
        Log.d("DashboardViewModel", "  Is Win: ${result.isWin}")
        Log.d("DashboardViewModel", "  Should Continue: ${result.shouldContinue}")
        Log.d("DashboardViewModel", "  Is Max Reached: ${result.isMaxReached}")

        when {
            result.isWin -> {
                _uiState.value = currentState.copy(
                    activeFollowOrderId = null,
                    followMartingaleStep = 0,
                    followOrderStatus = "Follow Martingale MENANG - Kembali ke mode normal"
                )

                Log.d("DashboardViewModel", "PRECISION FIXED: Follow Martingale WIN at step ${result.step}")
                Log.d("DashboardViewModel", "   Recovery: ${formatAmount(result.totalRecovered)}")

                // NEW: Handle follow martingale completion - WIN
                handleMartingaleCompletionForLocalStats(
                    orderId = "follow_martingale_${System.currentTimeMillis()}",
                    isWin = true,
                    finalStep = result.step
                )
            }

            result.shouldContinue -> {
                _uiState.value = currentState.copy(
                    followMartingaleStep = result.step,
                    followOrderStatus = "Follow Martingale Step ${result.step} - ${formatAmount(result.amount)}"
                )

                Log.d("DashboardViewModel", "PRECISION FIXED: Follow Martingale continues to step ${result.step}")
                Log.d("DashboardViewModel", "   Next amount: ${formatAmount(result.amount)}")
            }

            result.isMaxReached -> {
                _uiState.value = currentState.copy(
                    activeFollowOrderId = null,
                    followMartingaleStep = 0,
                    followOrderStatus = "Follow Martingale gagal - Max step tercapai"
                )

                Log.d("DashboardViewModel", "PRECISION FIXED: Follow Martingale failed at step ${result.step}")
                Log.d("DashboardViewModel", "   Total loss: ${formatAmount(result.totalLoss)}")

                // NEW: Handle follow martingale completion - FAILED
                handleMartingaleCompletionForLocalStats(
                    orderId = "follow_martingale_${System.currentTimeMillis()}",
                    isWin = false,
                    finalStep = result.step
                )
            }
        }
    }


    private fun getFollowModeStartError(state: DashboardUiState): String {
        return when {
            state.selectedAsset == null -> "Belum ada aset yang dipilih untuk Follow Order"
            !state.isWebSocketConnected -> "WebSocket tidak terhubung"
            !webSocketManager.isRequiredChannelsReady() -> "WebSocket channels belum siap"
            state.isFollowModeActive -> "Follow Order mode sudah aktif"
            state.isIndicatorModeActive -> "Indicator Order mode masih aktif, hentikan terlebih dahulu"
            state.botState != BotState.STOPPED -> "Schedule mode masih aktif, hentikan terlebih dahulu"
            state.martingaleSettings.validate(state.currencySettings.selectedCurrency).isFailure ->
                "Pengaturan martingale tidak valid: ${state.getMartingaleValidationError()}"
            state.stopLossSettings.validate().isFailure -> "Stop loss settings tidak valid"
            state.stopProfitSettings.validate().isFailure -> "Stop profit settings tidak valid"
            else -> "Kondisi tidak memenuhi syarat untuk memulai Follow Order"
        }
    }

    fun startBot() {
        val currentState = _uiState.value

        // Check connection stability first
        if (!ensureStableConnection()) {
            _uiState.value = currentState.copy(
                error = "WebSocket connection not stable. Please wait or force reconnect."
            )
            return
        }

        // Check for active modes
        if (currentState.isFollowModeActive) {
            _uiState.value = currentState.copy(
                error = "Follow Order mode is active. Stop Follow Order first."
            )
            return
        }
        if (currentState.isIndicatorModeActive) {
            _uiState.value = currentState.copy(
                error = "Indicator Order mode is active. Stop Indicator Order first."
            )
            return
        }

        if (!currentState.canStartBot()) {
            _uiState.value = currentState.copy(
                error = "Cannot start bot: ${getBotStartError(currentState)}"
            )
            return
        }

        val scheduledOrders = _scheduledOrders.value
        if (scheduledOrders.isEmpty()) {
            _uiState.value = currentState.copy(
                error = "Cannot start bot: No scheduled orders. Please add orders first."
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(
                    botStatus = "Starting Schedule mode...",
                    error = null
                )

                // Double-check connection stability after delay
                delay(500) // Small delay to ensure UI updates
                if (!ensureStableConnection()) {
                    _uiState.value = currentState.copy(
                        error = "WebSocket connection became unstable during startup. Try force reconnect.",
                        botStatus = "Bot Stopped"
                    )
                    return@launch
                }

                stopLossProfitManager.startNewSession()

                val currentTime = System.currentTimeMillis()
                val nextOrder = scheduledOrders.filter { !it.isExecuted && !it.isSkipped }
                    .minByOrNull { it.timeInMillis }

                val timingInfo = if (nextOrder != null) {
                    val timeUntil = (nextOrder.timeInMillis - currentTime) / 1000
                    "Next: ${nextOrder.time} in ${timeUntil}s"
                } else {
                    "No pending orders"
                }

                _uiState.value = currentState.copy(
                    botState = BotState.RUNNING,
                    tradingMode = TradingMode.SCHEDULE,
                    botStatus = "Bot Started ($timingInfo)",
                    tradingSession = stopLossProfitManager.getCurrentSession(),
                    connectionStatus = "Connected - Schedule mode active",
                    error = null
                )

                scheduleManager.startBot()
                continuousTradeMonitor.startMonitoring()

                Log.d("DashboardViewModel", "Bot started successfully with ${scheduledOrders.size} orders")

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error starting bot: ${e.message}", e)
                _uiState.value = currentState.copy(
                    botState = BotState.STOPPED,
                    botStatus = "Bot Stopped",
                    error = "Error starting bot: ${e.message}"
                )
            }
        }
    }

    fun pauseBot() {
        val currentState = _uiState.value

        if (!currentState.canPauseBot()) {
            _uiState.value = currentState.copy(
                error = "Tidak dapat menjeda bot dari state ${currentState.botState.name}"
            )
            return
        }

        _uiState.value = currentState.copy(
            botState = BotState.PAUSED,
            botStatus = "Bot Dijeda - Order ditahan, martingale tetap berjalan jika aktif"
        )

        try {
            scheduleManager.pauseBot()
            continuousTradeMonitor.pauseMonitoring()

            println("Bot berhasil dijeda:")
            println("   - ScheduleManager: Paused")
            println("   - ContinuousTradeMonitor: Paused")
            println("   - MartingaleManager: Tetap aktif (${martingaleManager.isActive()})")

        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                error = "Gagal menjeda bot: ${e.message}"
            )

            try {
                scheduleManager.resumeBot()
                continuousTradeMonitor.resumeMonitoring()
            } catch (rollbackError: Exception) {
                println("Error during pause rollback: ${rollbackError.message}")
            }
        }
    }

    fun resumeBot() {
        val currentState = _uiState.value

        if (!currentState.canResumeBot()) {
            _uiState.value = currentState.copy(
                error = "Tidak dapat melanjutkan bot dari state ${currentState.botState.name}"
            )
            return
        }

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) {
            _uiState.value = currentState.copy(
                error = "WebSocket tidak terhubung. Tidak dapat melanjutkan bot."
            )
            return
        }

        val scheduledOrders = _scheduledOrders.value
        val hasActiveOrders = scheduledOrders.any { !it.isExecuted && !it.isSkipped }
        val hasActiveMartingale = martingaleManager.isActive()

        if (!hasActiveOrders && !hasActiveMartingale) {
            _uiState.value = currentState.copy(
                error = "Tidak ada aktivitas untuk dilanjutkan. Semua order sudah selesai."
            )
            return
        }

        _uiState.value = currentState.copy(
            botState = BotState.RUNNING,
            botStatus = "Bot Dilanjutkan - Pemantauan berkelanjutan aktif"
        )

        try {
            scheduleManager.resumeBot()
            continuousTradeMonitor.resumeMonitoring()

            println("Bot berhasil dilanjutkan:")
            println("   - ScheduleManager: Resumed")
            println("   - ContinuousTradeMonitor: Resumed")
            println("   - Active orders: ${hasActiveOrders}")
            println("   - Active martingale: ${hasActiveMartingale}")

        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                botState = BotState.PAUSED,
                error = "Gagal melanjutkan bot: ${e.message}"
            )
        }
    }

    fun stopBot() {
        val currentState = _uiState.value

        if (!currentState.canStopBot()) {
            _uiState.value = currentState.copy(
                error = "Tidak dapat menghentikan bot dari state ${currentState.botState.name}"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: _assets.value.firstOrNull()
        val originalOrders = _scheduledOrders.value.toList()

        println("Menghentikan bot...")
        println("   - Scheduled orders: ${originalOrders.size}")
        println("   - Active martingale: ${martingaleManager.isActive()}")
        println("   - Continuous monitoring: ${continuousTradeMonitor.isActive()}")

        try {
            scheduleManager.stopBot()
            martingaleManager.stopMartingale(resetLoss = false)
            continuousTradeMonitor.stopMonitoring()

            println("Semua services berhasil dihentikan")

        } catch (e: Exception) {
            println("Error saat menghentikan services: ${e.message}")
        }

        _uiState.value = currentState.copy(
            botState = BotState.STOPPED,
            tradingMode = TradingMode.SCHEDULE,
            botStatus = "Bot Dihentikan - Semua aktivitas dihentikan, data order dipertahankan",
            activeOrderId = null,
            activeMartingaleStep = 0,
            selectedAsset = selectedAsset,
            error = null
        )

        println("Bot stopped - Ready untuk restart dengan ${originalOrders.size} orders")
    }

    // ... (Keep all existing methods for scheduled orders, martingale, etc.) ...

    private fun executeScheduledTrade(trend: String, orderId: String) {
        val currentState = _uiState.value

        if (currentState.botState != BotState.RUNNING) return

        val (shouldPreventTrade, preventReason) = stopLossProfitManager.shouldPreventNewTrade(
            currentState.stopLossSettings,
            currentState.stopProfitSettings
        )

        if (shouldPreventTrade) {
            scheduleManager.skipOrder(orderId, preventReason ?: "Stop condition reached")
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) return

        if (martingaleManager.isActive() && martingaleManager.getCurrentScheduledOrderId() != orderId) {
            scheduleManager.skipOrder(orderId, "Martingale aktif dari order sebelumnya")
            return
        }

        _uiState.value = currentState.copy(
            activeOrderId = orderId,
            botStatus = "Mengeksekusi order pada ${getCurrentTimeString()} - Pemantauan berkelanjutan aktif"
        )

        val baseAmount = currentState.martingaleSettings.baseAmount
        val currentServerTime = serverTimeService.getCurrentServerTimeMillis()

        if (ServerTimeService.cachedServerTimeOffset == 0L) {
            _uiState.value = currentState.copy(
                error = "Offset waktu server belum tersinkronisasi, eksekusi dibatalkan"
            )
            return
        }

        continuousTradeMonitor.startMonitoringScheduledOrder(
            scheduledOrderId = orderId,
            trend = trend,
            amount = baseAmount,
            assetRic = selectedAsset.ric,
            isDemoAccount = currentState.isDemoAccount,
            martingaleSettings = currentState.martingaleSettings,
            startTimeMillis = currentServerTime
        )

        tradeManager.executeScheduledTrade(
            assetRic = selectedAsset.ric,
            trend = trend,
            amount = baseAmount,
            isDemoAccount = currentState.isDemoAccount,
            scheduledOrderId = orderId,
            startTimeMillis = currentServerTime
        )
    }

    private fun executeInstantMartingaleTrade(
        trend: String,
        amount: Long,
        scheduledOrderId: String
    ) {
        val currentState = _uiState.value

        if (currentState.botState != BotState.RUNNING) {
            martingaleManager.stopMartingale()
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        _uiState.value = currentState.copy(
            activeMartingaleStep = martingaleManager.getCurrentStep(),
            botStatus = "Martingale Instan Langkah ${
                maxOf(
                    0,
                    martingaleManager.getCurrentStep() - 1
                )
            } - Eksekusi tanpa delay"
        )

        tradeManager.executeMartingaleTrade(
            assetRic = selectedAsset.ric,
            trend = trend,
            amount = amount,
            isDemoAccount = currentState.isDemoAccount,
            scheduledOrderId = scheduledOrderId,
            martingaleStep = martingaleManager.getCurrentStep()
        )
    }

    private fun handleInstantTradeResult(
        scheduledOrderId: String,
        isWin: Boolean,
        details: Map<String, Any>
    ) {
        val currentState = _uiState.value

        // Handle different modes: Schedule, Follow Order, and Indicator Order
        when {
            currentState.isIndicatorModeActive -> {
                indicatorOrderManager.handleIndicatorTradeResult(scheduledOrderId, isWin, details)

                // NEW: Update local stats for indicator trades
                val result = if (isWin) "WIN" else "LOSE"
                handleTradeResultForLocalStats(
                    tradeId = "indicator_${scheduledOrderId}_${System.currentTimeMillis()}",
                    orderId = scheduledOrderId,
                    result = result,
                    isMartingaleAttempt = indicatorOrderManager.isMartingaleActive(),
                    martingaleStep = indicatorOrderManager.getCurrentMartingaleStep(),
                    maxMartingaleSteps = currentState.martingaleSettings.maxSteps
                )
                return
            }

            currentState.isFollowModeActive -> {
                followOrderManager.handleFollowTradeResult(scheduledOrderId, isWin, details)

                // NEW: Update local stats for follow trades
                val result = if (isWin) "WIN" else "LOSE"
                handleTradeResultForLocalStats(
                    tradeId = "follow_${scheduledOrderId}_${System.currentTimeMillis()}",
                    orderId = scheduledOrderId,
                    result = result,
                    isMartingaleAttempt = followOrderManager.isMartingaleActive(),
                    martingaleStep = followOrderManager.getCurrentMartingaleStep(),
                    maxMartingaleSteps = currentState.martingaleSettings.maxSteps
                )
                return
            }

            else -> {
                // ===== SCHEDULE MODE HANDLING =====
                if (isWin) {
                    scheduleManager.completeOrder(scheduledOrderId, true)

                    val updatedOrders = _scheduledOrders.value.map { order ->
                        if (order.id == scheduledOrderId) {
                            order.copy(isExecuted = true, result = "WIN")
                        } else order
                    }
                    _scheduledOrders.value = updatedOrders

                    _uiState.value = currentState.copy(
                        activeOrderId = null,
                        activeMartingaleStep = 0,
                        botStatus = "Order MENANG - Deteksi instan berhasil!"
                    )

                    // ✅ FIXED: Count win immediately (initial trade or martingale win)
                    handleTradeResultForLocalStats(
                        tradeId = "schedule_${scheduledOrderId}_${System.currentTimeMillis()}",
                        orderId = scheduledOrderId,
                        result = "WIN",
                        isMartingaleAttempt = martingaleManager.isActive(),
                        martingaleStep = if (martingaleManager.isActive()) martingaleManager.getCurrentStep() else 0,
                        maxMartingaleSteps = currentState.martingaleSettings.maxSteps
                    )

                } else {
                    // Initial trade LOST
                    if (currentState.martingaleSettings.isEnabled) {
                        // ✅ MARTINGALE ENABLED: Don't count as LOSS yet, start martingale
                        startInstantMartingaleForLostOrder(scheduledOrderId, "Trade kalah - martingale instan")

                        // ❌ DON'T COUNT AS LOSS HERE - only count when martingale fails at max step
                        println("📊 Schedule: Initial trade LOST - Starting martingale (NOT counted as LOSS yet)")

                    } else {
                        // ✅ MARTINGALE DISABLED: Count as immediate LOSS
                        scheduleManager.completeOrder(scheduledOrderId, false)

                        val updatedOrders = _scheduledOrders.value.map { order ->
                            if (order.id == scheduledOrderId) {
                                order.copy(isExecuted = true, result = "LOSE")
                            } else order
                        }
                        _scheduledOrders.value = updatedOrders

                        _uiState.value = currentState.copy(
                            activeOrderId = null,
                            botStatus = "Order KALAH - Martingale tidak diaktifkan"
                        )

                        // ✅ COUNT LOSS: No martingale, count as direct loss
                        handleTradeResultForLocalStats(
                            tradeId = "schedule_${scheduledOrderId}_${System.currentTimeMillis()}",
                            orderId = scheduledOrderId,
                            result = "LOSE",
                            isMartingaleAttempt = false,
                            martingaleStep = 1,
                            maxMartingaleSteps = 1
                        )

                        println("📊 Schedule: Direct LOSS - Martingale disabled (counted as LOSS)")
                    }
                }
            }
        }
    }

    private fun startInstantMartingaleForLostOrder(scheduledOrderId: String, lossReason: String) {
        val currentState = _uiState.value
        val selectedAsset = currentState.selectedAsset ?: return

        val scheduledOrder = _scheduledOrders.value.find { it.id == scheduledOrderId }
        val trend = scheduledOrder?.trend ?: "buy"

        val martingaleSettings = currentState.martingaleSettings
        val initialTradeAmount = martingaleSettings.getAmountForStep(1)

        val serverNow = serverTimeService.getCurrentServerTimeMillis()

        val martingaleOrder = TradeOrder(
            amount = initialTradeAmount,
            createdAt = serverNow,
            dealType = if (currentState.isDemoAccount) "demo" else "real",
            expireAt = serverNow + 60000L,
            iso = "IDR",
            optionType = "turbo",
            ric = selectedAsset.ric,
            trend = trend,
            duration = 1,
            scheduledOrderId = scheduledOrderId,
            isMartingaleAttempt = true,
            martingaleStep = 0
        )

        martingaleManager.startMartingaleOrderInstant(
            initialTrade = martingaleOrder,
            martingaleSettings = martingaleSettings,
            trend = trend,
            scheduledOrderId = scheduledOrderId
        )

        scheduleManager.startMartingaleForOrder(scheduledOrderId)

        _uiState.value = currentState.copy(
            activeMartingaleStep = 1,
            botStatus = "Martingale Instan Langkah 1 - Eksekusi tanpa delay"
        )
    }

    private fun handleTradeResult(result: TradeResult) {
        val updatedResults = _tradeResults.value + result
        _tradeResults.value = updatedResults
        _uiState.value = _uiState.value.copy(lastTradeResult = result)

        if (result.isScheduledOrder && result.scheduledOrderId != null) {
            if (result.isMartingaleAttempt) {
                handleMartingaleExecutionResult(result)
            } else {
                handleScheduledOrderExecutionResult(result)
            }
        }
        if (result.success && (result.executionType == "TRADE_CLOSED" || result.executionType == "MARTINGALE_CLOSED")) {
            viewModelScope.launch {
                println("handleTradeResult: Triggering history refresh")
                _refreshTrigger.emit(System.currentTimeMillis())
            }
        }
    }

    private fun handleScheduledOrderExecutionResult(result: TradeResult) {
        val scheduledOrderId = result.scheduledOrderId ?: return
        val currentState = _uiState.value

        if (result.success) {
            _uiState.value = currentState.copy(
                botStatus = "Order terkirim - Pemantauan berkelanjutan aktif untuk deteksi hasil instan"
            )
        } else {
            scheduleManager.completeOrder(scheduledOrderId, false)
            continuousTradeMonitor.stopMonitoringOrder(scheduledOrderId)
            _uiState.value = currentState.copy(
                activeOrderId = null,
                botStatus = "Eksekusi order gagal - tidak dapat mengirim ke server"
            )
        }
    }

    private fun refreshDashboardData() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true

                _refreshTrigger.emit(System.currentTimeMillis())

                val currentStatus = _uiState.value.connectionStatus
                _uiState.value = _uiState.value.copy(
                    connectionStatus = "$currentStatus - Diperbarui"
                )

                delay(1500)
                _uiState.value = _uiState.value.copy(
                    connectionStatus = currentStatus
                )

            } catch (e: Exception) {
                println("Kesalahan dalam memperbarui dashboard: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun handleMartingaleExecutionResult(result: TradeResult) {
        if (result.success) {
            _uiState.value = _uiState.value.copy(
                botStatus = "Martingale berjalan - Pemantauan instan aktif"
            )
        } else {
            martingaleManager.stopMartingale()
            val scheduledOrderId = result.scheduledOrderId
            if (scheduledOrderId != null) {
                scheduleManager.completeOrder(scheduledOrderId, false)
                continuousTradeMonitor.stopMonitoringOrder(scheduledOrderId)
            }
            _uiState.value = _uiState.value.copy(
                activeOrderId = null,
                activeMartingaleStep = 0,
                botStatus = "Eksekusi Martingale Langkah ${result.martingaleStep + 1} gagal"
            )
        }
    }

    private fun handleMartingaleResult(result: MartingaleResult) {
        val currentState = _uiState.value
        val scheduledOrderId = result.scheduledOrderId

        // Create trade result for existing logic
        val tradeResult = TradeResult(
            success = result.isWin,
            message = result.message,
            orderId = result.tradeId,
            isMartingaleAttempt = true,
            martingaleStep = result.step,
            martingaleTotalLoss = result.totalLoss,
            martingaleStatus = when {
                result.isWin -> "WIN"
                result.shouldContinue -> "CONTINUE"
                result.isMaxReached -> "MAX_REACHED"
                else -> "FAILED"
            },
            scheduledOrderId = scheduledOrderId,
            isScheduledOrder = scheduledOrderId != null,
            details = mapOf(
                "martingale_step" to result.step,
                "amount" to result.amount,
                "total_loss" to result.totalLoss,
                "total_recovered" to result.totalRecovered,
                "is_win" to result.isWin,
                "should_continue" to result.shouldContinue,
                "is_max_reached" to result.isMaxReached,
                "execution_type" to when {
                    result.isWin -> "MARTINGALE_WIN"
                    result.shouldContinue -> "MARTINGALE_CONTINUE"
                    result.isMaxReached -> "MARTINGALE_MAX_REACHED"
                    else -> "MARTINGALE_FAILED"
                }
            )
        )

        val updatedResults = _tradeResults.value + tradeResult
        _tradeResults.value = updatedResults

        when {
            result.isWin -> {
                if (scheduledOrderId != null) {
                    scheduleManager.completeOrder(scheduledOrderId, true)
                    continuousTradeMonitor.stopMonitoringOrder(scheduledOrderId)

                    // ✅ FIX CRITICAL: Generate valid tradeId
                    val validTradeId = result.tradeId ?: "martingale_win_${scheduledOrderId}_${System.currentTimeMillis()}"

                    println("📊 MARTINGALE WIN STATS UPDATE:")
                    println("   Order ID: $scheduledOrderId")
                    println("   Trade ID: $validTradeId")
                    println("   Final Step: ${result.step}")

                    // ✅ CALL WITH VALID TRADE ID
                    handleMartingaleCompletionForLocalStats(
                        orderId = scheduledOrderId,
                        isWin = true,
                        finalStep = result.step,
                        tradeId = validTradeId
                    )

                    println("✅ LocalStats WIN counted for martingale")
                }

                _uiState.value = currentState.copy(
                    activeOrderId = null,
                    activeMartingaleStep = 0,
                    lastTradeResult = tradeResult,
                    botStatus = "Martingale MENANG! Order selesai dengan sukses"
                )
            }

            result.shouldContinue -> {
                // ⚠️ INTERMEDIATE LOSS - Don't count yet
                _uiState.value = currentState.copy(
                    activeMartingaleStep = result.step,
                    lastTradeResult = tradeResult,
                    botStatus = "Martingale Langkah ${result.step} - Eksekusi instan sedang berlangsung"
                )

                println("📊 Schedule: Martingale step ${result.step} LOST - Continuing (NOT counted yet)")
            }

            result.isMaxReached -> {
                if (scheduledOrderId != null) {
                    scheduleManager.completeOrder(scheduledOrderId, false)
                    continuousTradeMonitor.stopMonitoringOrder(scheduledOrderId)

                    // ✅ FIX CRITICAL: Generate valid tradeId
                    val validTradeId = result.tradeId ?: "martingale_fail_${scheduledOrderId}_${System.currentTimeMillis()}"

                    println("📊 MARTINGALE FAILED STATS UPDATE:")
                    println("   Order ID: $scheduledOrderId")
                    println("   Trade ID: $validTradeId")
                    println("   Final Step: ${result.step}")

                    // ✅ CALL WITH VALID TRADE ID
                    handleMartingaleCompletionForLocalStats(
                        orderId = scheduledOrderId,
                        isWin = false,
                        finalStep = result.step,
                        tradeId = validTradeId
                    )

                    println("📊 Schedule: Martingale FAILED at max step ${result.step} (NOW counted as LOSS)")
                }

                _uiState.value = currentState.copy(
                    activeOrderId = null,
                    activeMartingaleStep = 0,
                    lastTradeResult = tradeResult,
                    botStatus = "Martingale gagal - Maksimum ${result.step} langkah tercapai"
                )
            }
        }
    }

    fun setStopLossEnabled(enabled: Boolean) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan stop loss saat ada mode trading aktif" // UPDATED
            )
            return
        }

        val currentSettings = _uiState.value.stopLossSettings
        val newSettings = currentSettings.copy(isEnabled = enabled)

        val finalSettings = if (enabled && newSettings.maxLossAmount <= 0) {
            newSettings.copy(maxLossAmount = 500_000_000L)
        } else {
            newSettings
        }

        val validationResult = finalSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi stop loss gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(stopLossSettings = finalSettings)
    }

    fun resetTradingSession() {
        if (_uiState.value.botState == BotState.RUNNING ||
            _uiState.value.isFollowModeActive ||
            _uiState.value.isIndicatorModeActive
        ) { // UPDATED
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat reset session saat ada mode trading aktif"
            )
            return
        }

        stopLossProfitManager.resetSession()
        _uiState.value = _uiState.value.copy(
            tradingSession = stopLossProfitManager.getCurrentSession(),
            botStatus = "Session trading direset"
        )
    }

    fun refreshTradingSession() {
        viewModelScope.launch {
            val currentHistoryList = _historyList.value
            val currentState = _uiState.value

            val updatedSession = stopLossProfitManager.calculateSessionFromHistory(
                historyList = currentHistoryList,
                isDemoAccount = currentState.isDemoAccount,
                sessionStartTime = currentState.tradingSession.startTime
            )

            _uiState.value = currentState.copy(tradingSession = updatedSession)

            println("Manual session refresh - Net Profit: ${updatedSession.getNetProfit() / 100.0} IDR")
        }
    }

    fun setStopLossMaxAmount(amount: Long) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.stopLossSettings
        val newSettings = currentSettings.copy(maxLossAmount = amount)

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi stop loss gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(stopLossSettings = newSettings)
    }

    fun setStopProfitEnabled(enabled: Boolean) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan stop profit saat ada mode trading aktif" // UPDATED
            )
            return
        }

        val currentSettings = _uiState.value.stopProfitSettings
        val newSettings = currentSettings.copy(isEnabled = enabled)

        val finalSettings = if (enabled && newSettings.targetProfitAmount <= 0) {
            newSettings.copy(targetProfitAmount = 1_000_000_000L)
        } else {
            newSettings
        }

        val validationResult = finalSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi stop profit gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(stopProfitSettings = finalSettings)
    }

    fun setStopProfitTargetAmount(amount: Long) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.stopProfitSettings
        val newSettings = currentSettings.copy(targetProfitAmount = amount)

        val validationResult = newSettings.validate()
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi stop profit gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(stopProfitSettings = newSettings)
    }

    private fun checkBotAutoStop() {
        val currentState = _uiState.value
        if (currentState.botState != BotState.RUNNING) return

        val scheduledOrders = _scheduledOrders.value
        val hasActiveOrders = scheduledOrders.any { !it.isExecuted && !it.isSkipped }
        val hasActiveMartingale = martingaleManager.isActive()
        val hasActiveContinuousMonitoring = continuousTradeMonitor.isActive()

        if (!hasActiveOrders && !hasActiveMartingale && !hasActiveContinuousMonitoring) {
            viewModelScope.launch {
                delay(2000)
                continuousTradeMonitor.stopMonitoring()
                _uiState.value = _uiState.value.copy(
                    botState = BotState.STOPPED,
                    botStatus = "Bot otomatis berhenti - Semua order selesai",
                    activeOrderId = null,
                    activeMartingaleStep = 0
                )
            }
        }
    }

    private fun handleAllSchedulesCompleted() {
        viewModelScope.launch {
            delay(2000)

            val hasActiveOrders = _scheduledOrders.value.any { !it.isExecuted && !it.isSkipped }
            val hasActiveMartingale = martingaleManager.isActive()
            val hasActiveContinuousMonitoring = continuousTradeMonitor.isActive()

            if (!hasActiveOrders && !hasActiveMartingale && !hasActiveContinuousMonitoring) {
                continuousTradeMonitor.stopMonitoring()
                _uiState.value = _uiState.value.copy(
                    botState = BotState.STOPPED,
                    botStatus = "Semua order selesai - Bot otomatis berhenti",
                    activeOrderId = null,
                    activeMartingaleStep = 0
                )
            }
        }
    }

    fun addScheduledOrders() {
        val input = _uiState.value.scheduleInput.trim()
        if (input.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Harap masukkan jadwal order")
            return
        }

        val result = scheduleManager.addScheduledOrders(input)
        result.fold(
            onSuccess = { count ->
                _uiState.value = _uiState.value.copy(
                    scheduleInput = "",
                    error = null,
                    botStatus = "$count order berhasil ditambahkan. Siap untuk memulai bot."
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Kesalahan parsing: ${exception.message}"
                )
            }
        )
    }

    fun updateScheduleInput(input: String) {
        _uiState.value = _uiState.value.copy(scheduleInput = input)
    }

    fun removeScheduledOrder(orderId: String) {
        continuousTradeMonitor.stopMonitoringOrder(orderId)
        scheduleManager.removeScheduledOrder(orderId)
    }

    fun clearAllScheduledOrders() {
        if (_uiState.value.botState == BotState.RUNNING) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat menghapus order saat bot sedang berjalan. Hentikan bot terlebih dahulu."
            )
            return
        }

        continuousTradeMonitor.stopMonitoring()
        scheduleManager.clearAllScheduledOrders()
        _uiState.value = _uiState.value.copy(
            botStatus = "Semua order telah dihapus"
        )
    }

    fun setMartingaleEnabled(enabled: Boolean) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan martingale saat ada mode trading aktif"
            )
            return
        }

        val currentSettings = _uiState.value.martingaleSettings
        val newSettings = currentSettings.copy(isEnabled = enabled)

        // ✅ FIX: Pass currency to validation
        val validationResult = newSettings.validate(_uiState.value.currencySettings.selectedCurrency)
        if (validationResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                error = "Validasi martingale gagal: ${validationResult.exceptionOrNull()?.message}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(martingaleSettings = newSettings)
    }

    fun setStopLossSettings(newSettings: StopLossSettings) {
        _uiState.value = _uiState.value.copy(stopLossSettings = newSettings)
    }

    fun setStopProfitSettings(newSettings: StopProfitSettings) {
        _uiState.value = _uiState.value.copy(stopProfitSettings = newSettings)
    }

    fun setMartingaleMaxSteps(maxSteps: Int) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan martingale saat ada mode trading aktif"
            )
            return
        }

        // Basic range validation only
        if (maxSteps < 1 || maxSteps > 10) {
            _uiState.value = _uiState.value.copy(
                error = "Maksimum langkah harus antara 1 dan 10"
            )
            return
        }

        val currentSettings = _uiState.value.martingaleSettings
        val newSettings = currentSettings.copy(maxSteps = maxSteps)

        // ✅ OPTIONAL: You can add validation here if you want
        // val validationResult = newSettings.validate(_uiState.value.currencySettings.selectedCurrency)
        // if (validationResult.isFailure) { ... }

        // Just apply the settings
        _uiState.value = _uiState.value.copy(martingaleSettings = newSettings)
    }

    fun setTradingMode(mode: TradingMode) {
        val currentState = _uiState.value

        // Don't allow mode change if any trading mode is active
        if (currentState.botState != BotState.STOPPED ||
            currentState.isFollowModeActive ||
            currentState.isIndicatorModeActive ||
            currentState.isCTCModeActive
        ) {
            _uiState.value = currentState.copy(
                error = "Tidak dapat mengubah trading mode saat ada mode trading aktif"
            )
            return
        }

        _uiState.value = currentState.copy(
            tradingMode = mode,
            isTradingModeSelected = true, // ✅ TAMBAH INI
            error = null
        )
    }

    fun setMartingaleBaseAmount(amount: Long) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah pengaturan martingale saat ada mode trading aktif"
            )
            return
        }

        // ✅ FIX: Use currency-specific minimum
        val currentCurrency = _uiState.value.currencySettings.selectedCurrency
        if (amount < currentCurrency.minAmountInCents) {
            _uiState.value = _uiState.value.copy(
                error = "Jumlah minimum adalah ${currentCurrency.formatAmount(currentCurrency.minAmountInCents)}"
            )
            return
        }

        val currentSettings = _uiState.value.martingaleSettings
        val newSettings = currentSettings.copy(baseAmount = amount)

        // ✅ OPTIONAL: Add full validation
        // val validationResult = newSettings.validate(currentCurrency)
        // if (validationResult.isFailure) { ... }

        // Just apply the settings
        _uiState.value = _uiState.value.copy(martingaleSettings = newSettings)
    }

    fun setMartingaleMultiplierType(type: MultiplierType) {
        if (!_uiState.value.canModifySettings()) return

        val defaultValue = when (type) {
            MultiplierType.FIXED -> 2.0
            MultiplierType.PERCENTAGE -> 50.0
        }

        val currentSettings = _uiState.value.martingaleSettings
        val newSettings = currentSettings.copy(
            multiplierType = type,
            multiplierValue = defaultValue
        )

        // ✅ OPTIONAL: Add validation
        // val validationResult = newSettings.validate(_uiState.value.currencySettings.selectedCurrency)
        // if (validationResult.isFailure) { ... }

        // Just apply the settings
        _uiState.value = _uiState.value.copy(martingaleSettings = newSettings)
    }

    fun setMartingaleMultiplierValue(value: Double) {
        if (!_uiState.value.canModifySettings()) return

        val currentSettings = _uiState.value.martingaleSettings
        val newSettings = currentSettings.copy(multiplierValue = value)

        // Basic range validation only
        val isValidRange = when (currentSettings.multiplierType) {
            MultiplierType.FIXED -> value >= 1.1 && value <= 15.0
            MultiplierType.PERCENTAGE -> value >= 10.0 && value <= 1000.0
        }

        if (!isValidRange) {
            _uiState.value = _uiState.value.copy(
                error = when (currentSettings.multiplierType) {
                    MultiplierType.FIXED -> "Multiplier harus antara 1.1 - 15.0"
                    MultiplierType.PERCENTAGE -> "Persentase harus antara 10% - 1000%"
                }
            )
            return
        }

        // ✅ OPTIONAL: Add validation
        // val validationResult = newSettings.validate(_uiState.value.currencySettings.selectedCurrency)
        // if (validationResult.isFailure) { ... }

        // Just apply the settings
        _uiState.value = _uiState.value.copy(martingaleSettings = newSettings)
    }

    fun selectAsset(asset: Asset) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah aset saat ada mode trading aktif" // UPDATED
            )
            return
        }

        _uiState.value = _uiState.value.copy(selectedAsset = asset)

        if (_uiState.value.isWebSocketConnected && webSocketManager.isRequiredChannelsReady()) {
            subscribeToAsset(asset.ric)
        }
    }

    fun setAccountType(isDemo: Boolean) {
        if (!_uiState.value.canModifySettings()) {
            _uiState.value = _uiState.value.copy(
                error = "Tidak dapat mengubah jenis akun saat ada mode trading aktif"
            )
            return
        }
        _uiState.value = _uiState.value.copy(isDemoAccount = isDemo)

        localStatsTracker.initializeOrReset(isDemo)
    }

    private fun getBotStartError(state: DashboardUiState): String {
        return when {
            state.selectedAsset == null -> "Belum ada aset yang dipilih untuk trading"
            !state.isWebSocketConnected -> "WebSocket tidak terhubung"
            !webSocketManager.isRequiredChannelsReady() -> "WebSocket channels belum siap"
            state.botState != BotState.STOPPED -> "Bot sudah ${state.botState.name.lowercase()}"
            state.isFollowModeActive -> "Follow Order mode masih aktif"
            state.isIndicatorModeActive -> "Indicator Order mode masih aktif"
            _scheduledOrders.value.isEmpty() -> "Belum ada order terjadwal"
            // ✅ FIX: Pass currency to validation
            state.martingaleSettings.validate(state.currencySettings.selectedCurrency).isFailure ->
                "Pengaturan martingale tidak valid: ${state.getMartingaleValidationError()}"
            state.stopLossSettings.validate().isFailure -> "Stop loss settings invalid"
            state.stopProfitSettings.validate().isFailure -> "Stop profit settings invalid"
            else -> "Kondisi tidak memenuhi syarat untuk memulai bot"
        }
    }
    private fun handleStopLossProfitTriggered(type: String, reason: String) {
        val currentState = _uiState.value

        val updatedSession = stopLossProfitManager.getCurrentSession()

        // ADD THIS:
        if (currentState.isMultiMomentumModeActive) {
            stopMultiMomentumMode()
        } else if (currentState.isCTCModeActive) {
            stopCTCMode()
        } else if (currentState.isIndicatorModeActive) {
            stopIndicatorMode()
        } else if (currentState.isFollowModeActive) {
            stopFollowMode()
        } else {
            stopBot()
        }

        _uiState.value = _uiState.value.copy(
            botState = BotState.STOPPED,
            isFollowModeActive = false,
            isIndicatorModeActive = false,
            isCTCModeActive = false,
            isMultiMomentumModeActive = false, // ADD THIS
            botStatus = "$type - $reason",
            tradingSession = updatedSession,
            error = null
        )
    }

    // Method utility tetap sama
    fun getConnectionStats(): Map<String, Any> {
        val baseStats = webSocketManager.getConnectionStats()
        val healthStats = mapOf(
            "connection_healthy" to webSocketManager.isConnectionHealthy(),
            "required_channels_ready" to webSocketManager.isRequiredChannelsReady(),
            "bot_state" to _uiState.value.botState.name,
            "follow_mode_active" to _uiState.value.isFollowModeActive,
            "follow_mode_ultra_fast" to if (_uiState.value.isFollowModeActive) "ENABLED" else "DISABLED",
            "indicator_mode_active" to _uiState.value.isIndicatorModeActive,
            "current_status" to _uiState.value.connectionStatus,
            "ultra_fast_monitoring" to if (_uiState.value.isFollowModeActive) {
                followOrderManager.getPerformanceStats()["ultra_fast_monitoring"] ?: "UNAVAILABLE"
            } else "NOT_APPLICABLE"
        )
        return baseStats + healthStats
    }

    fun getUltraFastFollowOrderDebugInfo(): Map<String, Any> {
        return if (_uiState.value.isFollowModeActive) {
            val performanceStats = followOrderManager.getPerformanceStats()
            val ultraFastMonitoring = performanceStats["ultra_fast_monitoring"] as? Map<String, Any> ?: emptyMap()

            mapOf(
                "mode" to "ULTRA_FAST_FOLLOW_ORDER",
                "status" to _uiState.value.followOrderStatus,
                "detection_speed_comparison" to "SAME_AS_SCHEDULE_MODE",
                "monitoring_intervals" to mapOf(
                    "ultra_fast" to "50ms",
                    "balance_check" to "200ms",
                    "api_prewarming" to "3000ms"
                ),
                "detection_layers" to listOf(
                    "WEBSOCKET_PRIORITY",
                    "BALANCE_CHANGE_DETECTION",
                    "API_POLLING_FALLBACK"
                ),
                "cycle_info" to mapOf(
                    "current_cycle" to (performanceStats["current_cycle"] ?: 0),
                    "cached_trend" to (performanceStats["cached_trend"] ?: "None"),
                    "cycle_in_progress" to (performanceStats["cycle_in_progress"] ?: false)
                ),
                "performance_metrics" to performanceStats,
                "ultra_fast_stats" to ultraFastMonitoring,
                "comparison_with_schedule" to mapOf(
                    "detection_speed" to "IDENTICAL",
                    "monitoring_interval" to "IDENTICAL",
                    "api_optimization" to "IDENTICAL",
                    "websocket_priority" to "IDENTICAL"
                )
            )
        } else {
            mapOf(
                "mode" to "INACTIVE",
                "message" to "Follow Order ultra-fast mode not active"
            )
        }
    }

    fun isConnectionHealthy(): Boolean {
        return webSocketManager.isConnectionHealthy()
    }

    fun subscribeToAsset(assetRic: String) {
        if (!_uiState.value.isWebSocketConnected) {
            _uiState.value = _uiState.value.copy(
                error = "WebSocket tidak terhubung, tidak dapat subscribe ke asset"
            )
            return
        }

        webSocketManager.subscribeToAsset(assetRic)
        _uiState.value = _uiState.value.copy(
            connectionStatus = "Berlangganan update untuk asset: $assetRic"
        )
    }

    fun getWebSocketPerformanceInfo(): Map<String, Any> {
        val stats = webSocketManager.getConnectionStats()
        val isHealthy = webSocketManager.isConnectionHealthy()

        return mapOf(
            "connection_health" to isHealthy,
            "connection_stats" to stats,
            "required_channels_ready" to webSocketManager.isRequiredChannelsReady(),
            "pending_trades_count" to webSocketManager.pendingTrades.size
        )
    }

    private fun getCurrentTimeString(): String {
        val now = Date()
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(now)
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "${amount / 1_000_000}M"
            amount >= 1_000 -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshAssets() {
        fetchAssetsFromApi()
    }

    fun refreshServerTime() {
        syncServerTime()
    }

    fun getServerTimeInfo(): String {
        val offsetInSeconds = serverTimeOffset / 1000.0
        return when {
            serverTimeOffset == 0L -> "Menggunakan waktu lokal (tidak tersinkron dengan server)"
            serverTimeOffset > 0 -> "Server lebih cepat ${
                String.format(
                    "%.1f",
                    offsetInSeconds
                )
            } detik"

            else -> "Server lebih lambat ${
                String.format(
                    "%.1f",
                    kotlin.math.abs(offsetInSeconds)
                )
            } detik"
        }
    }

    fun getBotPerformanceInfo(): Map<String, Any> {
        val scheduledOrders = _scheduledOrders.value
        val completedOrders = scheduledOrders.filter { it.isExecuted }
        val skippedOrders = scheduledOrders.filter { it.isSkipped }
        val pendingOrders = scheduledOrders.filter { !it.isExecuted && !it.isSkipped }

        val martingaleOrders = completedOrders.filter { it.martingaleState.isCompleted }
        val martingaleWins = martingaleOrders.filter { it.martingaleState.finalResult == "WIN" }
        val martingaleLosses = martingaleOrders.filter { it.martingaleState.finalResult == "LOSS" }

        val sessionStats = stopLossProfitManager.getSessionStats()

        val websocketStats = getWebSocketPerformanceInfo()
        val connectionStats = websocketStats["connection_stats"] as? Map<*, *>

        val reconnectionAttempts = try {
            (connectionStats?.get("reconnection_attempts") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
        val messagesReceived = try {
            (connectionStats?.get("total_messages_received") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
        val messagesSent = try {
            (connectionStats?.get("total_messages_sent") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
        val pendingTrades = try {
            (connectionStats?.get("pending_trades") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
        val joinedChannelsCount = try {
            (connectionStats?.get("joined_channels") as? List<*>)?.size ?: 0
        } catch (e: Exception) {
            0
        }
        val timeSinceLastMessage = try {
            (connectionStats?.get("time_since_last_message_ms") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }

        val ctcOrderStats = if (_uiState.value.isCTCModeActive) {
            val baseStats = ctcOrderManager.getPerformanceStats()
            baseStats + mapOf(
                "ultra_fast_enabled" to true,
                "identical_to_follow_order" to true,
                "detection_speed" to "SAME_AS_FOLLOW_ORDER",
                "monitoring_interval" to "50ms",
                "api_prewarming" to "ACTIVE",
                "multi_layer_detection" to "WEBSOCKET_BALANCE_API"
            )
        } else {
            emptyMap()
        }

        val followOrderStats = if (_uiState.value.isFollowModeActive) {
            val baseStats = followOrderManager.getPerformanceStats()
            baseStats + mapOf(
                "ultra_fast_enabled" to true,
                "detection_speed" to "SAME_AS_SCHEDULE_MODE",
                "monitoring_interval" to "50ms",
                "api_prewarming" to "ACTIVE",
                "multi_layer_detection" to "WEBSOCKET_BALANCE_API"
            )
        } else {
            emptyMap()
        }


        // NEW: Indicator Order stats
        val indicatorOrderStats = if (_uiState.value.isIndicatorModeActive) {
            val baseStats = indicatorOrderManager.getPerformanceStats()
            val continuousMonitorStats = baseStats["continuous_monitor"] as? Map<String, Any> ?: emptyMap()

            baseStats + mapOf(
                "ultra_fast_enabled" to true,
                "auto_stop_enabled" to true,
                "detection_speed" to "50ms",
                "continuous_monitor_active" to continuousMonitorStats["is_active"],
                "monitoring_performance" to continuousMonitorStats
            )
        } else {
            mapOf(
                "ultra_fast_enabled" to false,
                "auto_stop_enabled" to false,
                "continuous_monitor_active" to false
            )
        }

        val baseMap = mutableMapOf<String, Any>(
            "total_orders" to scheduledOrders.size,
            "completed_orders" to completedOrders.size,
            "skipped_orders" to skippedOrders.size,
            "pending_orders" to pendingOrders.size,
            "martingale_orders" to martingaleOrders.size,
            "martingale_wins" to martingaleWins.size,
            "martingale_losses" to martingaleLosses.size,
            "current_bot_state" to _uiState.value.botState.name,
            "current_trading_mode" to _uiState.value.tradingMode.name,
            "is_follow_mode_active" to _uiState.value.isFollowModeActive,
            "is_indicator_mode_active" to _uiState.value.isIndicatorModeActive, // NEW
            "active_martingale" to martingaleManager.isActive(),
            "continuous_monitoring" to continuousTradeMonitor.isActive(),
            "websocket_status" to _uiState.value.connectionStatus,
            "server_time_sync" to getServerTimeInfo(),
            "timing_precision" to "CONTINUOUS",
            "execution_method" to "INSTANT",
            "is_ctc_mode_active" to _uiState.value.isCTCModeActive,  // 🔥 NEW
            "ctc_order_stats" to ctcOrderStats,  // 🔥 NEW
            "session_stats" to sessionStats,
            "stop_loss_enabled" to _uiState.value.stopLossSettings.isEnabled,
            "stop_profit_enabled" to _uiState.value.stopProfitSettings.isEnabled,
            "websocket_performance" to websocketStats,
            "connection_health" to isConnectionHealthy(),
            "indicator_order_stats" to indicatorOrderStats,
            "follow_order_stats" to followOrderStats,
            "follow_order_ultra_fast" to _uiState.value.isFollowModeActive,

            )

        baseMap["websocket_reconnection_attempts"] = reconnectionAttempts
        baseMap["websocket_messages_received"] = messagesReceived
        baseMap["websocket_messages_sent"] = messagesSent
        baseMap["websocket_pending_trades"] = pendingTrades
        baseMap["websocket_joined_channels"] = joinedChannelsCount
        baseMap["websocket_time_since_last_message"] = timeSinceLastMessage

        return baseMap.toMap()
    }

    fun startMultiMomentumMode() {
        val currentState = _uiState.value

        if (!ensureStableConnection()) {
            _uiState.value = currentState.copy(
                error = "WebSocket connection not stable. Please wait or force reconnect."
            )
            return
        }

        if (!currentState.canStartMultiMomentumMode()) {
            _uiState.value = currentState.copy(
                error = "Cannot start Multi-Momentum: ${getMultiMomentumModeStartError(currentState)}"
            )
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(
                    multiMomentumOrderStatus = "Starting Multi-Momentum mode...",
                    error = null
                )

                // Stop other modes
                if (currentState.botState != BotState.STOPPED) {
                    stopBot()
                }
                if (currentState.isFollowModeActive) {
                    stopFollowMode()
                }
                if (currentState.isIndicatorModeActive) {
                    stopIndicatorMode()
                }
                if (currentState.isCTCModeActive) {
                    stopCTCMode()
                }

                delay(1000)

                if (!ensureStableConnection()) {
                    _uiState.value = currentState.copy(
                        error = "WebSocket connection became unstable. Try force reconnect.",
                        multiMomentumOrderStatus = "Multi-Momentum inactive"
                    )
                    return@launch
                }

                stopLossProfitManager.startNewSession()

                Log.d("DashboardViewModel", "MULTI-MOMENTUM MODE: Starting with 4 parallel momentums")
                Log.d("DashboardViewModel", "  Asset: ${selectedAsset.name}")
                Log.d("DashboardViewModel", "  Account: ${if (currentState.isDemoAccount) "Demo" else "Real"}")

                val result = multiMomentumOrderManager.startMultiMomentumMode(
                    selectedAsset = selectedAsset,
                    isDemoAccount = currentState.isDemoAccount,
                    martingaleSettings = currentState.martingaleSettings
                )

                result.fold(
                    onSuccess = { message ->
                        _uiState.value = currentState.copy(
                            isMultiMomentumModeActive = true,
                            tradingMode = TradingMode.MULTI_MOMENTUM,
                            multiMomentumOrderStatus = "Multi-Momentum ACTIVE - ${selectedAsset.name}",
                            tradingSession = stopLossProfitManager.getCurrentSession(),
                            connectionStatus = "Connected - Multi-Momentum active",
                            error = null
                        )

                        Log.d("DashboardViewModel", "Multi-Momentum mode started successfully")
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            error = exception.message,
                            multiMomentumOrderStatus = "Multi-Momentum inactive"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error starting Multi-Momentum: ${e.message}", e)
                _uiState.value = currentState.copy(
                    error = "Error starting Multi-Momentum: ${e.message}",
                    multiMomentumOrderStatus = "Multi-Momentum inactive"
                )
            }
        }
    }

    // 5. ADD STOP FUNCTION
    fun stopMultiMomentumMode() {
        val currentState = _uiState.value

        if (!currentState.canStopMultiMomentumMode()) {
            _uiState.value = currentState.copy(
                error = "Multi-Momentum mode is not active"
            )
            return
        }

        Log.d("DashboardViewModel", "Manual stop of Multi-Momentum mode requested")

        val result = multiMomentumOrderManager.stopMultiMomentumMode()

        result.fold(
            onSuccess = { message ->
                _uiState.value = currentState.copy(
                    isMultiMomentumModeActive = false,
                    tradingMode = TradingMode.SCHEDULE,
                    multiMomentumOrderStatus = "Multi-Momentum manually stopped",
                    activeMultiMomentumOrderId = null,
                    multiMomentumMartingaleSteps = emptyMap(),
                    multiMomentumCandleCount = 0,
                    connectionStatus = "Connected - Mode can be changed",
                    error = null
                )

                Log.d("DashboardViewModel", "Multi-Momentum mode manually stopped")
            },
            onFailure = { exception ->
                _uiState.value = currentState.copy(
                    error = exception.message
                )
            }
        )
    }

    // 6. ADD TRADE EXECUTION HANDLER
    private fun executeMultiMomentumTrade(
        trend: String,
        orderId: String,
        amount: Long,
        momentumType: String
    ) {
        val currentState = _uiState.value

        if (!currentState.isMultiMomentumModeActive) return

        val (shouldPreventTrade, preventReason) = stopLossProfitManager.shouldPreventNewTrade(
            currentState.stopLossSettings,
            currentState.stopProfitSettings
        )

        if (shouldPreventTrade) {
            Log.w("DashboardViewModel", "Multi-Momentum Trade prevented: $preventReason")
            return
        }

        val selectedAsset = currentState.selectedAsset ?: return

        if (!currentState.isWebSocketConnected || !webSocketManager.isRequiredChannelsReady()) {
            Log.w("DashboardViewModel", "WebSocket not ready for Multi-Momentum Trade")
            return
        }

        Log.d("DashboardViewModel", "Executing Multi-Momentum order:")
        Log.d("DashboardViewModel", "  Momentum: $momentumType")
        Log.d("DashboardViewModel", "  Trend: $trend")
        Log.d("DashboardViewModel", "  Amount: ${formatAmount(amount)}")

        _uiState.value = currentState.copy(
            activeMultiMomentumOrderId = orderId,
            multiMomentumOrderStatus = "$momentumType: Executing $trend - ${formatAmount(amount)}"
        )

        tradeManager.executeFollowInstantTrade(
            assetRic = selectedAsset.ric,
            trend = trend,
            amount = amount,
            isDemoAccount = currentState.isDemoAccount,
            followOrderId = orderId,
            isMartingaleAttempt = false,
            martingaleStep = 0
        )
    }

    // 7. ADD MARTINGALE RESULT HANDLER
    private fun handleMultiMomentumMartingaleResult(result: MultiMomentumMartingaleResult) {
        val currentState = _uiState.value

        Log.d("DashboardViewModel", "Multi-Momentum Martingale result:")
        Log.d("DashboardViewModel", "  Momentum: ${result.momentumType}")
        Log.d("DashboardViewModel", "  Step: ${result.step}")
        Log.d("DashboardViewModel", "  Is Win: ${result.isWin}")

        when {
            result.isWin -> {
                val updatedSteps = currentState.multiMomentumMartingaleSteps.toMutableMap()
                updatedSteps.remove(result.momentumType)

                _uiState.value = currentState.copy(
                    multiMomentumMartingaleSteps = updatedSteps,
                    multiMomentumOrderStatus = "${result.momentumType} Martingale WIN - Continuing"
                )

                Log.d("DashboardViewModel", "${result.momentumType} Martingale WIN at step ${result.step}")
            }

            result.shouldContinue -> {
                val updatedSteps = currentState.multiMomentumMartingaleSteps.toMutableMap()
                updatedSteps[result.momentumType] = result.step

                _uiState.value = currentState.copy(
                    multiMomentumMartingaleSteps = updatedSteps,
                    multiMomentumOrderStatus = "${result.momentumType} Martingale Step ${result.step}"
                )

                Log.d("DashboardViewModel", "${result.momentumType} continues to step ${result.step}")
            }

            result.isMaxReached -> {
                val updatedSteps = currentState.multiMomentumMartingaleSteps.toMutableMap()
                updatedSteps.remove(result.momentumType)

                _uiState.value = currentState.copy(
                    multiMomentumMartingaleSteps = updatedSteps,
                    multiMomentumOrderStatus = "${result.momentumType} Martingale failed"
                )

                Log.d("DashboardViewModel", "${result.momentumType} Martingale failed at step ${result.step}")
            }
        }
    }

    // 8. ADD HELPER FUNCTIONS
    private fun getMultiMomentumModeStartError(state: DashboardUiState): String {
        return when {
            state.selectedAsset == null -> "Belum ada aset yang dipilih"
            !state.isWebSocketConnected -> "WebSocket tidak terhubung"
            !webSocketManager.isRequiredChannelsReady() -> "WebSocket channels belum siap"
            state.isMultiMomentumModeActive -> "Multi-Momentum mode sudah aktif"
            state.isCTCModeActive -> "CTC Order masih aktif"
            state.isFollowModeActive -> "Follow Order masih aktif"
            state.isIndicatorModeActive -> "Indicator Order masih aktif"
            state.botState != BotState.STOPPED -> "Schedule mode masih aktif"
            state.martingaleSettings.validate(state.currencySettings.selectedCurrency).isFailure ->
                "Pengaturan martingale tidak valid"
            else -> "Kondisi tidak memenuhi syarat"
        }
    }

    fun getMultiMomentumPerformanceInfo(): Map<String, Any> {
        return if (_uiState.value.isMultiMomentumModeActive) {
            val baseStats = multiMomentumOrderManager.getPerformanceStats()
            val lastOrders = _multiMomentumOrders.value.takeLast(5)

            // ✅ EXTRACT OHLC DATA FROM LAST 5 ORDERS
            val ohlcData = lastOrders.mapNotNull { order ->
                order.sourceCandle?.let { candle ->
                    mapOf(
                        "momentum_type" to order.momentumType,
                        "open" to candle.open.toPlainString(),
                        "high" to candle.high.toPlainString(),
                        "low" to candle.low.toPlainString(),
                        "close" to candle.close.toPlainString(),
                        "trend" to candle.getTrend(),
                        "body_size" to String.format("%.5f", abs((candle.close - candle.open).toDouble())),
                        "range" to String.format("%.5f", (candle.high - candle.low).toDouble()),
                        "timestamp" to Date(order.executionTime).toString(),
                        "is_executed" to order.isExecuted
                    )
                }
            }

            // ✅ FIX: Filter out null values from baseStats
            val filteredBaseStats = baseStats.filterValues { it != null }

            filteredBaseStats + mapOf(
                "last_5_orders_ohlc" to ohlcData,
                "total_candles_analyzed" to (baseStats["candle_count"] ?: 0)
            )
        } else {
            mapOf(
                "is_active" to false,
                "message" to "Multi-Momentum mode tidak aktif",
                "last_5_orders_ohlc" to emptyList<Map<String, Any>>()
            )
        }
    }

    fun getIndicatorPerformanceInfo(): Map<String, Any> {
        return if (_uiState.value.isIndicatorModeActive) {
            val baseStats = indicatorOrderManager.getPerformanceStats()
            val predictionInfo = indicatorOrderManager.getPredictionInfo()
            val currentSettings = _uiState.value.indicatorSettings

            // Extract continuous monitor stats
            val continuousMonitorStats = baseStats["continuous_monitor"] as? Map<String, Any> ?: emptyMap()

            // Merge base stats with enhanced monitoring info
            baseStats + mapOf(
                "prediction_details" to predictionInfo,
                "live_prediction_monitoring" to true,
                "prediction_update_interval" to "5_SECONDS",
                "ui_integration" to "REAL_TIME_DISPLAY",
                "auto_restart_capability" to mapOf(
                    "enabled" to (baseStats["auto_restart_enabled"] ?: false),
                    "consecutive_restarts" to (baseStats["consecutive_restarts"] ?: 0),
                    "max_restarts" to (baseStats["max_restarts"] ?: 50),
                    "restart_scenarios" to listOf("WIN", "MARTINGALE_WIN", "SINGLE_LOSS", "MARTINGALE_FAILED"),
                    "stop_scenarios" to listOf("CONSECUTIVE_LOSS", "MAX_RESTARTS", "MANUAL_STOP")
                ),
                "ultra_fast_monitoring" to mapOf(
                    "enabled" to true,
                    "detection_speed" to "50ms_same_as_follow_order",
                    "multi_layer_detection" to listOf("WEBSOCKET", "BALANCE", "API"),
                    "monitoring_comparison" to "IDENTICAL_TO_FOLLOW_ORDER",
                    "monitor_stats" to continuousMonitorStats
                ),
                "execution_flow" to mapOf(
                    "prediction_generation" to "180_CANDLES_ANALYSIS",
                    "execution_timing" to "BOUNDARY_DELAYED",
                    "result_detection" to "ULTRA_FAST_CONTINUOUS",
                    "mode_completion" to "AUTO_RESTART_ON_WIN",
                    "cycle_management" to "AUTOMATIC_RESTART"
                ),
                "performance_comparison" to mapOf(
                    "vs_follow_order" to mapOf(
                        "detection_speed" to "IDENTICAL",
                        "monitoring_interval" to "IDENTICAL",
                        "websocket_priority" to "IDENTICAL",
                        "balance_detection" to "IDENTICAL",
                        "api_fallback" to "IDENTICAL"
                    ),
                    "unique_features" to listOf(
                        "AUTO_RESTART_ON_COMPLETION",
                        "CONTINUOUS_CYCLE_OPERATION",
                        "PREDICTION_BASED_EXECUTION",
                        "SINGLE_PREDICTION_FOCUS"
                    )
                ),
                "sensitivity_config" to mapOf(
                    "current_sensitivity" to currentSettings.sensitivity.toPlainString(),
                    "sensitivity_level" to currentSettings.getSensitivityLevel(),
                    "sensitivity_display" to currentSettings.getSensitivityDisplayText(),
                    "precision_mode" to "BIGDECIMAL_FULL_PRECISION"
                )
            )
        } else {
            mapOf(
                "is_active" to false,
                "message" to "Indicator Order mode tidak aktif",
                "auto_restart_capability" to mapOf(
                    "enabled" to false,
                    "status" to "INACTIVE"
                ),
                "ultra_fast_monitoring" to mapOf(
                    "enabled" to false,
                    "status" to "INACTIVE"
                ),
                "sensitivity_config" to mapOf(
                    "precision_mode" to "BIGDECIMAL_READY_NOT_ACTIVE"
                )
            )
        }
    }


    // NEW: Get current indicator status
    fun getIndicatorStatusInfo(): Map<String, String> {
        val currentState = _uiState.value
        val predictionInfo = _indicatorPredictionInfo.value
        val performanceStats = if (currentState.isIndicatorModeActive) {
            indicatorOrderManager.getPerformanceStats()
        } else {
            emptyMap()
        }

        val predictions = predictionInfo["predictions"] as? List<Map<String, Any>> ?: emptyList()
        val activePredictions = predictions.filter {
            val isTriggered = it["is_triggered"] as? Boolean ?: true
            val isDisabled = it["is_disabled"] as? Boolean ?: false
            !isTriggered && !isDisabled
        }
        val triggeredPredictions = predictions.filter { it["is_triggered"] as? Boolean ?: false }
        val disabledPredictions = predictions.filter { it["is_disabled"] as? Boolean ?: false }

        return mapOf(
            "mode_status" to indicatorOrderManager.getModeStatus(),
            "indicator_type" to (currentState.indicatorSettings.type.name),
            "sensitivity" to currentState.indicatorSettings.getSensitivityDisplayText(),
            "sensitivity_level" to currentState.indicatorSettings.getSensitivityLevel(),
            "support_level" to if (currentState.currentSupportLevel > 0) String.format(
                "%.5f",
                currentState.currentSupportLevel
            ) else "N/A",
            "resistance_level" to if (currentState.currentResistanceLevel > 0) String.format(
                "%.5f",
                currentState.currentResistanceLevel
            ) else "N/A",
            "current_value" to (currentState.lastIndicatorValues?.getFormattedPrimaryValue() ?: "N/A"),
            "trend" to (currentState.lastIndicatorValues?.trend ?: "UNKNOWN"),
            "strength" to (currentState.lastIndicatorValues?.strength ?: "UNKNOWN"),
            "martingale_step" to if (indicatorOrderManager.isMartingaleActive()) "${indicatorOrderManager.getCurrentMartingaleStep()}" else "0",
            "total_predictions" to "${predictions.size}",
            "active_predictions" to "${activePredictions.size}",
            "triggered_predictions" to "${triggeredPredictions.size}",
            "disabled_predictions" to "${disabledPredictions.size}",
            "monitoring_mode" to (predictionInfo["monitoring_mode"] as? String ?: "PREDICTIONS"),
            "active_trade_id" to (predictionInfo["active_trade_id"] as? String ?: "NONE"),
            "execution_flow" to "SINGLE_PREDICTION_FOCUS",
            "auto_restart_enabled" to "${performanceStats["auto_restart_enabled"] ?: false}",
            "consecutive_restarts" to "${performanceStats["consecutive_restarts"] ?: 0}",
            "ultra_fast_monitoring" to "ENABLED_SAME_AS_FOLLOW_ORDER",
            "detection_speed" to "50ms_ultra_fast",
            "restart_capability" to "ENABLED_ON_WIN"
        )
    }

    fun logout() {
        stopBot()
        stopFollowMode()
        stopIndicatorMode()
        stopCTCMode()  // 🔥 NEW
        webSocketManager.disconnect()
        scheduleManager.cleanup()
        tradeManager.cleanup()
        martingaleManager.cleanup()
        continuousTradeMonitor.cleanup()
        stopLossProfitManager.cleanup()
        followOrderManager.cleanup()
        indicatorOrderManager.cleanup()
        ctcOrderManager.cleanup()  // 🔥 NEW
        loginRepository.logout()
        stopMultiMomentumMode() // ADD THIS
    }

    override fun onCleared() {
        super.onCleared()
        stopBot()
        stopFollowMode()
        stopIndicatorMode()
        stopCTCMode()  // 🔥 NEW
        webSocketManager.disconnect()
        scheduleManager.cleanup()
        tradeManager.cleanup()
        martingaleManager.cleanup()
        continuousTradeMonitor.cleanup()
        stopLossProfitManager.cleanup()
        followOrderManager.cleanup()
        indicatorOrderManager.cleanup()
        ctcOrderManager.cleanup()  // 🔥 NEW
        stopMultiMomentumMode() // ADD THIS
    }
}

sealed class WhitelistCheckState {
    object Checking : WhitelistCheckState()

    data class Verified(
        val userId: String,
        val email: String
    ) : WhitelistCheckState()

    data class Failed(
        val reason: String,
        val message: String
    ) : WhitelistCheckState()
}
