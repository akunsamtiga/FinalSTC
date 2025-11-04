package com.autotrade.finalstc.presentation.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.data.repository.TradingHistoryRepository
import com.autotrade.finalstc.data.local.LanguageManager
import com.autotrade.finalstc.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import javax.inject.Inject

interface TradingHistoryApi {
    @GET("bo-deals-history/v3/deals/trade")
    suspend fun getTradingHistoryRaw(
        @Query("type") type: String,
        @Query("locale") locale: String = "id",
        @Header("authorization-token") authToken: String,
        @Header("device-type") deviceType: String,
        @Header("device-id") deviceId: String,
        @Header("user-timezone") timezone: String,
        @Header("origin") origin: String,
        @Header("referer") referer: String,
        @Header("accept") accept: String
    ): Response<Any>
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val tradingHistoryRepository: TradingHistoryRepository,
    private val languageManager: LanguageManager,
    private val sessionManager: SessionManager // ADD: SessionManager injection
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _historyList = MutableStateFlow<List<TradingHistoryNew>>(emptyList())
    val historyList: StateFlow<List<TradingHistoryNew>> = _historyList.asStateFlow()

    private val _currentAccountType = MutableStateFlow(true) // true = demo, false = real
    val currentAccountType: StateFlow<Boolean> = _currentAccountType.asStateFlow()

    private val _currentLanguage = MutableStateFlow("id")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // ADD: Currency state
    private val _currentCurrency = MutableStateFlow("IDR")
    val currentCurrency: StateFlow<String> = _currentCurrency.asStateFlow()

    init {
        loadLanguage()
        loadCurrency() // ADD: Load currency on init
        observeLanguageChanges() // ADD: Observe language changes

    }

    private fun loadLanguage() {
        _currentLanguage.value = languageManager.getLanguage()
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            languageManager.currentLanguage.collect { newLanguage ->
                _currentLanguage.value = newLanguage
                println("🌐 HistoryViewModel: Language changed to $newLanguage")
            }
        }
    }

    // ADD: Load currency from session
    private fun loadCurrency() {
        val currency = sessionManager.getCurrency()
        _currentCurrency.value = currency
        println("📊 HistoryViewModel: Loaded currency from session: $currency")
    }

    fun loadTradingHistory(isDemoAccount: Boolean? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // If isDemoAccount is provided, update current account type
                isDemoAccount?.let {
                    _currentAccountType.value = it
                    _uiState.value = _uiState.value.copy(showDemoAccount = it)
                }

                // Load history data from repository
                val history = tradingHistoryRepository.getTradingHistory(
                    isDemoAccount ?: _currentAccountType.value
                )
                _historyList.value = history

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load trading history"
                )
            }
        }
    }

    fun toggleAccountType() {
        val newAccountType = !_currentAccountType.value
        _currentAccountType.value = newAccountType
        _uiState.value = _uiState.value.copy(showDemoAccount = newAccountType)
        loadTradingHistory(newAccountType)
    }

    fun refreshHistory() {
        loadCurrency() // ADD: Refresh currency when refreshing history
        loadTradingHistory(_currentAccountType.value)
    }

    fun refreshFromWebSocketTrigger() {
        viewModelScope.launch {
            println("🔄 HistoryViewModel: Refreshing from WebSocket trigger")
            loadTradingHistory(_currentAccountType.value)
        }
    }

    fun refreshFromWebSocketTrigger(isDemoAccount: Boolean) {
        viewModelScope.launch {
            println("🔄 HistoryViewModel: Refreshing ${if(isDemoAccount) "demo" else "real"} account from WebSocket trigger")
            loadTradingHistory(isDemoAccount)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}