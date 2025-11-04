package com.autotrade.finalstc.presentation.main.history

data class TradingHistory(
    val id: String,
    val symbol: String,
    val type: String,
    val amount: String,
    val price: String,
    val profit: String,
    val date: String,
    val status: String
)

data class TradingHistoryNew(
    val id: Long,
    val status: String,
    val amount: Long,
    val dealType: String,
    val createdAt: String,
    val uuid: String,
    val win: Long,
    val assetId: Int,
    val closeRate: Double?,
    val finishedAt: String?,
    val trend: String,
    val payment: Long,
    val paymentRate: Int,
    val assetName: String,
    val assetRic: String,
    val closeQuoteCreatedAt: String?,
    val openQuoteCreatedAt: String?,
    val openRate: Double,
    val tradeType: String,
    val accountType: String? = null, // "demo" or "real"
    val isDemoAccount: Boolean? = null, // Alternative field if you prefer boolean

    val isDemo: Boolean = false // This would be the preferred approach

)

data class TradingHistoryRaw(
    val id: Long,
    val status: String,
    val amount: Long,
    val deal_type: String,
    val created_at: String,
    val uuid: String,
    val win: Long?,
    val asset_id: Int,
    val close_rate: Double?,
    val requested_by: String?,
    val finished_at: String?,
    val trend: String,
    val payment: Long,
    val payment_rate: Int,
    val asset_name: String,
    val asset_ric: String,
    val close_quote_created_at: String?,
    val open_quote_created_at: String?,
    val open_rate: Double,
    val trade_type: String
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDemoAccount: Boolean = true // ADD: Track current account type being displayed
)