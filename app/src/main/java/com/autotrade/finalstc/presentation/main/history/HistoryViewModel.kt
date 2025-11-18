package com.autotrade.finalstc.presentation.main.history

import android.util.Log
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
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _historyList = MutableStateFlow<List<TradingHistoryNew>>(emptyList())
    val historyList: StateFlow<List<TradingHistoryNew>> = _historyList.asStateFlow()

    private val _currentAccountType = MutableStateFlow(true)
    val currentAccountType: StateFlow<Boolean> = _currentAccountType.asStateFlow()

    private val _currentLanguage = MutableStateFlow("id")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _currentCurrency = MutableStateFlow("IDR")
    val currentCurrency: StateFlow<String> = _currentCurrency.asStateFlow()

    init {
        loadLanguage()
        loadCurrency()
        observeLanguageChanges()

        // ✅ IMPROVED: Preload assets when ViewModel is created
        preloadAssets()
    }

    private fun loadLanguage() {
        _currentLanguage.value = languageManager.getLanguage()
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            languageManager.currentLanguage.collect { newLanguage ->
                _currentLanguage.value = newLanguage
                Log.d(TAG, "🌐 Language changed to $newLanguage")
            }
        }
    }

    private fun loadCurrency() {
        val currency = sessionManager.getCurrency()
        _currentCurrency.value = currency
        Log.d(TAG, "💰 Loaded currency: $currency")
    }

    // ✅ NEW: Preload assets to populate cache
    private fun preloadAssets() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Preloading asset icons...")
                tradingHistoryRepository.preloadAssets()

                // Log cache status after preload
                val cacheStatus = tradingHistoryRepository.getCacheStatus()
                Log.d(TAG, "✅ Asset cache status: $cacheStatus")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error preloading assets: ${e.message}", e)
            }
        }
    }

    fun loadTradingHistory(isDemoAccount: Boolean? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d(TAG, "📊 Loading trading history...")

                isDemoAccount?.let {
                    _currentAccountType.value = it
                    _uiState.value = _uiState.value.copy(showDemoAccount = it)
                }

                // ✅ Log cache status before fetching
                val cacheStatus = tradingHistoryRepository.getCacheStatus()
                Log.d(TAG, "   Cache status before fetch: $cacheStatus")

                val history = tradingHistoryRepository.getTradingHistory(
                    isDemoAccount ?: _currentAccountType.value
                )

                _historyList.value = history

                Log.d(TAG, "✅ Loaded ${history.size} history items")

                // ✅ Log icon statistics
                val withIcons = history.count { !it.iconUrl.isNullOrBlank() }
                val withoutIcons = history.size - withIcons
                Log.d(TAG, "   📊 Icon stats: $withIcons with icons, $withoutIcons without")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading history: ${e.message}", e)
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

        Log.d(TAG, "🔄 Toggled account type to: ${if (newAccountType) "demo" else "real"}")
        loadTradingHistory(newAccountType)
    }

    fun refreshHistory() {
        Log.d(TAG, "🔄 Manual refresh triggered")
        loadCurrency()

        // ✅ Optionally refresh asset cache on manual refresh
        viewModelScope.launch {
            try {
                tradingHistoryRepository.refreshAssetCache()
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error refreshing asset cache: ${e.message}")
            }
        }

        loadTradingHistory(_currentAccountType.value)
    }

    fun refreshFromWebSocketTrigger() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 WebSocket refresh triggered")
            loadTradingHistory(_currentAccountType.value)
        }
    }

    fun refreshFromWebSocketTrigger(isDemoAccount: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "🔄 WebSocket refresh (${if(isDemoAccount) "demo" else "real"}) triggered")
            loadTradingHistory(isDemoAccount)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ✅ NEW: Debug function to check cache status
    fun logCacheStatus() {
        viewModelScope.launch {
            val status = tradingHistoryRepository.getCacheStatus()
            Log.d(TAG, "🔍 Current cache status: $status")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 ViewModel cleared")
    }
}