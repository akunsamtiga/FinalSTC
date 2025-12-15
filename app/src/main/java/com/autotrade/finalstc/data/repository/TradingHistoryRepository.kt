package com.autotrade.finalstc.data.repository

import android.util.Log
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.presentation.main.history.IconData
import com.autotrade.finalstc.presentation.main.history.TradingHistoryApi
import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import com.autotrade.finalstc.presentation.main.history.TradingHistoryRaw
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface AssetApi {
    @GET("bo-assets/v6/assets")
    suspend fun getAssets(
        @Header("authorization-token") authToken: String,
        @Header("device-type") deviceType: String,
        @Header("device-id") deviceId: String,
        @Header("user-timezone") timezone: String,
        @Header("origin") origin: String,
        @Header("referer") referer: String,
        @Header("accept") accept: String
    ): Response<AssetApiResponse>
}

data class AssetApiResponse(val data: AssetData)
data class AssetData(val assets: List<AssetRaw>)
data class AssetRaw(
    val ric: String,
    val name: String,
    val type: Int,
    val icon: AssetIcon?
)
data class AssetIcon(val url: String?)

@Singleton
class TradingHistoryRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "TradingHistoryRepo"
        private const val ASSET_CACHE_DURATION = 30 * 60 * 1000L
        private const val BASE_URL = "https://stockity.id"
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val historyApi = retrofit.create(TradingHistoryApi::class.java)
    private val assetApi = retrofit.create(AssetApi::class.java)

    private val cacheMutex = Mutex()
    private var assetIconCache: Map<String, String> = emptyMap()
    private var lastAssetFetchTime: Long = 0
    private var isFetchingAssets = false

    suspend fun getTradingHistory(isDemoAccount: Boolean): List<TradingHistoryNew> {
        return try {
            Log.d(TAG, "Fetching trading history (${if (isDemoAccount) "demo" else "real"})")

            val userSession = sessionManager.getUserSession()
            if (userSession?.authtoken == null) {
                Log.e(TAG, "No auth token available")
                return emptyList()
            }

            ensureAssetsCached(userSession)

            Log.d(TAG, "Making history API request")
            val response = historyApi.getTradingHistoryRaw(
                type = if (isDemoAccount) "demo" else "real",
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType ?: "web",
                deviceId = userSession.deviceId ?: "",
                timezone = "Asia/Jakarta",
                origin = BASE_URL,
                referer = "$BASE_URL/",
                accept = "application/json, text/plain, */*"
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "History API response successful")
                val responseBody = response.body()!!
                val rawHistory = parseHistoryResponse(responseBody)
                val processedHistory = processHistoryData(rawHistory, isDemoAccount)

                Log.d(TAG, "Processed ${processedHistory.size} history items")
                return processedHistory
            } else {
                Log.e(TAG, "History API failed: ${response.code()} - ${response.message()}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history: ${e.message}", e)
            return emptyList()
        }
    }

    private suspend fun ensureAssetsCached(userSession: com.autotrade.finalstc.data.model.UserSession) {
        cacheMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val isCacheValid = assetIconCache.isNotEmpty() &&
                    (currentTime - lastAssetFetchTime) < ASSET_CACHE_DURATION

            if (isCacheValid) {
                Log.d(TAG, "Using valid asset cache (${assetIconCache.size} items)")
                return
            }

            if (isFetchingAssets) {
                Log.d(TAG, "Asset fetch already in progress, waiting...")
                return
            }

            isFetchingAssets = true
        }

        try {
            fetchAssetsInternal(userSession)
        } finally {
            cacheMutex.withLock {
                isFetchingAssets = false
            }
        }
    }

    private suspend fun fetchAssetsInternal(userSession: com.autotrade.finalstc.data.model.UserSession) {
        try {
            Log.d(TAG, "Fetching fresh asset data...")

            val assetResponse = assetApi.getAssets(
                authToken = userSession.authtoken,
                deviceType = userSession.deviceType ?: "web",
                deviceId = userSession.deviceId ?: "",
                timezone = "Asia/Jakarta",
                origin = BASE_URL,
                referer = "$BASE_URL/",
                accept = "application/json, text/plain, */*"
            )

            if (assetResponse.isSuccessful && assetResponse.body() != null) {
                val assets = assetResponse.body()!!.data.assets

                val newCache = mutableMapOf<String, String>()
                assets.forEach { asset ->
                    asset.icon?.url?.let { iconPath ->
                        if (iconPath.isNotBlank()) {
                            val cleanPath = iconPath.removePrefix("/")
                            val fullUrl = "$BASE_URL/$cleanPath"
                            newCache[asset.ric] = fullUrl

                            Log.d(TAG, "  Mapped: ${asset.ric} -> $fullUrl")
                        }
                    }
                }

                cacheMutex.withLock {
                    assetIconCache = newCache
                    lastAssetFetchTime = System.currentTimeMillis()
                }

                Log.d(TAG, "Successfully cached ${newCache.size} asset icons")
                Log.d(TAG, "   Sample entries: ${newCache.entries.take(3)}")
            } else {
                Log.e(TAG, "Asset API failed: ${assetResponse.code()} - ${assetResponse.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching assets: ${e.message}", e)
        }
    }

    private fun processHistoryData(
        rawHistory: List<TradingHistoryRaw>,
        isDemoAccount: Boolean
    ): List<TradingHistoryNew> {
        return rawHistory.map { raw ->
            val iconUrl = resolveIconUrl(raw)

            if (iconUrl != null) {
                Log.d(TAG, "Icon resolved for ${raw.asset_name} (${raw.asset_ric}): $iconUrl")
            } else {
                Log.w(TAG, "No icon found for ${raw.asset_name} (${raw.asset_ric})")
            }

            TradingHistoryNew(
                id = raw.id,
                status = raw.status,
                amount = raw.amount,
                dealType = raw.deal_type,
                createdAt = raw.created_at,
                uuid = raw.uuid,
                win = raw.win ?: 0L,
                assetId = raw.asset_id,
                closeRate = raw.close_rate,
                finishedAt = raw.finished_at,
                trend = raw.trend,
                payment = raw.payment,
                paymentRate = raw.payment_rate,
                assetName = raw.asset_name,
                assetRic = raw.asset_ric,
                closeQuoteCreatedAt = raw.close_quote_created_at,
                openQuoteCreatedAt = raw.open_quote_created_at,
                openRate = raw.open_rate,
                tradeType = raw.trade_type,
                accountType = if (isDemoAccount) "demo" else "real",
                isDemoAccount = isDemoAccount,
                isDemo = isDemoAccount,
                iconUrl = iconUrl
            )
        }.sortedByDescending { it.createdAt }
    }

    private fun resolveIconUrl(raw: TradingHistoryRaw): String? {
        raw.icon?.url?.let { iconPath ->
            if (iconPath.isNotBlank()) {
                return if (iconPath.startsWith("http")) {
                    iconPath
                } else {
                    val cleanPath = iconPath.removePrefix("/")
                    "$BASE_URL/$cleanPath"
                }
            }
        }

        assetIconCache[raw.asset_ric]?.let { cachedUrl ->
            return cachedUrl
        }

        if (raw.asset_ric.isNotBlank()) {
            return "$BASE_URL/assets/images/asset-icons/${raw.asset_ric}.png"
        }

        return null
    }

    private fun parseHistoryResponse(responseBody: Any): List<TradingHistoryRaw> {
        return try {
            when (responseBody) {
                is List<*> -> parseHistoryArray(responseBody)
                is Map<*, *> -> {
                    when {
                        responseBody.containsKey("data") -> {
                            val data = responseBody["data"]
                            when (data) {
                                is Map<*, *> -> {
                                    when {
                                        data.containsKey("standard_trade_deals") -> {
                                            val deals = data["standard_trade_deals"]
                                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                                        }
                                        data.containsKey("deals") -> {
                                            val deals = data["deals"]
                                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                                        }
                                        else -> emptyList()
                                    }
                                }
                                is List<*> -> parseHistoryArray(data)
                                else -> emptyList()
                            }
                        }
                        responseBody.containsKey("standard_trade_deals") -> {
                            val deals = responseBody["standard_trade_deals"]
                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                        }
                        responseBody.containsKey("deals") -> {
                            val deals = responseBody["deals"]
                            if (deals is List<*>) parseHistoryArray(deals) else emptyList()
                        }
                        responseBody.containsKey("id") && responseBody.containsKey("status") -> {
                            listOf(parseSingleHistoryItem(responseBody))
                        }
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing history response: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseHistoryArray(array: List<*>): List<TradingHistoryRaw> {
        return array.mapNotNull { item ->
            try {
                if (item is Map<*, *>) {
                    parseSingleHistoryItem(item)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing history item: ${e.message}")
                null
            }
        }
    }

    private fun parseSingleHistoryItem(item: Map<*, *>): TradingHistoryRaw {
        val iconData = extractIconData(item)

        return TradingHistoryRaw(
            id = (item["id"] as? Number)?.toLong() ?: 0L,
            status = item["status"] as? String ?: "",
            amount = (item["amount"] as? Number)?.toLong() ?: 0L,
            deal_type = item["deal_type"] as? String ?: "",
            created_at = item["created_at"] as? String ?: "",
            uuid = item["uuid"] as? String ?: "",
            win = (item["win"] as? Number)?.toLong(),
            asset_id = (item["asset_id"] as? Number)?.toInt() ?: 0,
            close_rate = (item["close_rate"] as? Number)?.toDouble(),
            requested_by = item["requested_by"] as? String,
            finished_at = item["finished_at"] as? String,
            trend = item["trend"] as? String ?: "",
            payment = (item["payment"] as? Number)?.toLong() ?: 0L,
            payment_rate = (item["payment_rate"] as? Number)?.toInt() ?: 0,
            asset_name = item["asset_name"] as? String ?: "",
            asset_ric = item["asset_ric"] as? String ?: "",
            close_quote_created_at = item["close_quote_created_at"] as? String,
            open_quote_created_at = item["open_quote_created_at"] as? String,
            open_rate = (item["open_rate"] as? Number)?.toDouble() ?: 0.0,
            trade_type = item["trade_type"] as? String ?: "",
            icon = iconData
        )
    }

    private fun extractIconData(item: Map<*, *>): IconData? {
        return try {
            val possibleIconFields = listOf("icon", "asset_icon", "asset", "symbol")

            for (field in possibleIconFields) {
                when (val iconValue = item[field]) {
                    is Map<*, *> -> {
                        (iconValue["url"] as? String)?.let { url ->
                            if (url.isNotBlank()) {
                                Log.d(TAG, "  Found icon.url in '$field': $url")
                                return IconData(url = url)
                            }
                        }
                    }
                    is String -> {
                        if (iconValue.isNotBlank()) {
                            Log.d(TAG, "  Found icon as string in '$field': $iconValue")
                            return IconData(url = iconValue)
                        }
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting icon: ${e.message}")
            null
        }
    }

    suspend fun refreshAssetCache() {
        val userSession = sessionManager.getUserSession()
        if (userSession?.authtoken != null) {
            cacheMutex.withLock {
                assetIconCache = emptyMap()
                lastAssetFetchTime = 0
            }
            ensureAssetsCached(userSession)
        }
    }

    fun getCacheStatus(): Map<String, Any> {
        return mapOf(
            "cache_size" to assetIconCache.size,
            "last_fetch_time" to lastAssetFetchTime,
            "is_cache_valid" to (System.currentTimeMillis() - lastAssetFetchTime < ASSET_CACHE_DURATION),
            "is_fetching" to isFetchingAssets,
            "sample_entries" to assetIconCache.entries.take(5).associate { it.key to it.value }
        )
    }

    suspend fun preloadAssets() {
        val userSession = sessionManager.getUserSession()
        if (userSession?.authtoken != null) {
            ensureAssetsCached(userSession)
        }
    }
}