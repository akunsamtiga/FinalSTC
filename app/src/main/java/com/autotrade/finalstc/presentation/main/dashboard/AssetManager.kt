package com.autotrade.finalstc.presentation.main.dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AssetManager(
    private val scope: CoroutineScope,
    private val onAssetsUpdate: (List<Asset>) -> Unit,
    private val onLoadingStateChange: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stockity.id/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val stockityApi = retrofit.create(StockityApi::class.java)

    private val typeNameMapping = mapOf(
        1 to "Forex",
        2 to "Crypto",
        3 to "Saham",
        4 to "Komoditas",
        5 to "Indeks",
        6 to "ETF",
        7 to "OTC",
        8 to "Event",
        9 to "AI Index",
        10 to "Synthetic Index",
        11 to "Metal"
    )

    fun fetchAssetsFromApi(authToken: String, deviceType: String, deviceId: String) {
        scope.launch {
            try {
                onLoadingStateChange(true)

                val response = stockityApi.getAssets(
                    authToken = authToken,
                    deviceType = deviceType,
                    deviceId = deviceId,
                    timezone = "Asia/Jakarta",
                    origin = "https://stockity.id",
                    referer = "https://stockity.id",
                    accept = "application/json, text/plain, */*"
                )

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val rawAssets = apiResponse.data.assets
                    val processedAssets = processAssets(rawAssets)

                    onAssetsUpdate(processedAssets)
                    onLoadingStateChange(false)
                } else {
                    onError("Failed to fetch assets: ${response.code()}")
                    onLoadingStateChange(false)
                }
            } catch (e: Exception) {
                onError("Error fetching assets: ${e.message}")
                onLoadingStateChange(false)
            }
        }
    }

    private fun processAssets(rawAssets: List<AssetRaw>): List<Asset> {
        val processedAssets = mutableListOf<Asset>()

        rawAssets.forEach { asset ->
            val ric = asset.ric
            val name = asset.name
            val assetType = asset.type
            val typeName = typeNameMapping[assetType] ?: "Tipe-$assetType"

            var profit: Double? = null

            asset.personal_user_payment_rates?.forEach { rateEntry ->
                if (rateEntry.trading_type == "turbo") {
                    profit = rateEntry.payment_rate
                    return@forEach
                }
            }

            if (profit == null) {
                val settings = asset.trading_tools_settings
                profit = settings?.ftt?.user_statuses?.vip?.payment_rate_turbo
                    ?: settings?.bo?.payment_rate_turbo
                            ?: settings?.payment_rate_turbo
            }

            profit?.let { profitRate ->
                processedAssets.add(
                    Asset(
                        ric = ric,
                        name = name,
                        typeName = typeName,
                        profitRate = profitRate,
                        isActive = true
                    )
                )
            }
        }

        return processedAssets.sortedByDescending { it.profitRate }
    }
}
