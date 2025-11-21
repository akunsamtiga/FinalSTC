package com.autotrade.finalstc.presentation.main.history

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autotrade.finalstc.utils.StringsManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.request.CachePolicy
import com.autotrade.finalstc.presentation.main.dashboard.DashboardColors
import com.autotrade.finalstc.presentation.theme.ThemeViewModel

private val DarkBackground = Color(0xFF1B1B1B)
private val DarkSurface = Color(0xFF1F1F1F)
private val CardBackground = Color(0xFF2B2B2B)
private val AccentSecondary = Color(0xFFDC4D4D)
private val AccentWarning = Color(0xFFFDA359)
private val TextPrimary = Color(0xFFEBEBEB)
private val TextSecondary = Color(0xFFBAC1CB)
private val TextMuted = Color(0xBA7E7E7E)
private val BorderColor = Color(0xFF323232)
private val WifiGreen = Color(0xFF67D88B)
private val AccentProfit = Color(0xFF7AF1C1)
private val StatusBlue = Color(0xFF64B5F6)


// ✅ TAMBAH: Sealed class untuk state management icon
sealed class IconLoadState {
    object Loading : IconLoadState()
    object Success : IconLoadState()
    data class Error(val throwable: Throwable?) : IconLoadState()
    object NoIcon : IconLoadState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel() // ✅ TAMBAH INI
    ) {
    val dashboardTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val colors = dashboardTheme.colors // ✅ AMBIL COLORS

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.currentCurrency.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("today") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        viewModel.loadTradingHistory()
    }

    val filteredHistory = remember(historyList, selectedFilter) {
        when (selectedFilter) {
            "today" -> historyList.filter { isToday(it.createdAt) }
            "won" -> historyList.filter { it.status == "won" }
            "lost" -> historyList.filter { it.status == "lost" }
            "opened" -> historyList.filter { it.status == "opened" }
            "week" -> historyList.filter { isWithinLastWeek(it.createdAt) }
            else -> historyList
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(600))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ImprovedHeader(
                        lang = lang,
                        uiState = uiState,
                        selectedFilter = selectedFilter,
                        onFilterClick = { showFilterSheet = true },
                        onRefresh = { viewModel.refreshHistory() },
                        onToggleAccount = { viewModel.toggleAccountType() },
                        colors = colors
                    )
                }

                if (historyList.isNotEmpty()) {
                    item {
                        CompactStatisticsSection(
                            lang = lang,
                            historyList = historyList,
                            currency = currency,
                            colors = colors
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        item {
                            LoadingSection(lang = lang)
                        }
                    }
                    uiState.error != null -> {
                        item {
                            ErrorSection(
                                lang = lang,
                                error = uiState.error ?: "Unknown error occurred",
                                onRetry = { viewModel.refreshHistory() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }
                    filteredHistory.isEmpty() -> {
                        item {
                            EmptyStateSection(
                                lang = lang,
                                selectedFilter = selectedFilter
                            )
                        }
                    }
                    else -> {
                        items(
                            items = filteredHistory,
                            key = { it.id }
                        ) { trade ->
                            ImprovedHistoryCard(
                                lang = lang,
                                trade = trade,
                                currency = currency
                            )
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                lang = lang,
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}


@Composable
private fun ImprovedHeader(
    lang: String,
    uiState: HistoryUiState,
    selectedFilter: String,
    onFilterClick: () -> Unit,
    onRefresh: () -> Unit,
    onToggleAccount: () -> Unit,
    colors: DashboardColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CONTAINER UNTUK LAYOUT DENGAN TOMBOL MELAYANG
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // LEFT SIDE: JUDUL + TOMBOL ACCOUNT TYPE
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // JUDUL
                    Text(
                        text = StringsManager.getTradingHistory(lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )

                    // TOMBOL ACCOUNT TYPE
                    OutlinedCard(
                        onClick = onToggleAccount,
                        modifier = Modifier
                            .height(44.dp)
                            .wrapContentWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (uiState.showDemoAccount)
                                AccentWarning.copy(alpha = 0.15f)
                            else
                                WifiGreen.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            0.8.dp,
                            if (uiState.showDemoAccount)
                                AccentWarning.copy(alpha = 0.5f)
                            else
                                WifiGreen.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.showDemoAccount)
                                    Icons.Default.School
                                else
                                    Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (uiState.showDemoAccount)
                                    AccentWarning
                                else
                                    WifiGreen
                            )
                            Text(
                                text = if (uiState.showDemoAccount)
                                    StringsManager.getDemoAccount(lang)
                                else
                                    StringsManager.getRealAccount(lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // RIGHT SIDE: TOMBOL REFRESH + FILTER (POJOK KANAN, CENTERED VERTICALLY)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterEnd) // Pojok kanan, tengah vertikal
                ) {
                    // TOMBOL REFRESH
                    Surface(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = StatusBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusBlue.copy(alpha = 0.3f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = StringsManager.getRefresh(lang),
                                tint = StatusBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // TOMBOL FILTER
                    Surface(
                        onClick = onFilterClick,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = StatusBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusBlue.copy(alpha = 0.3f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = StringsManager.getFilter(lang),
                                tint = StatusBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ROW 3: BADGE FILTER AKTIF (JIKA ADA)
            if (selectedFilter != "all") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BADGE UNTUK FILTER YANG AKTIF
                    when (selectedFilter) {
                        "today" -> { // Tambahkan case ini
                            FilterBadge(
                                icon = Icons.Default.Today,
                                text = StringsManager.getToday(lang),
                                iconColor = Color(0xFF9C27B0), // Warna ungu
                                backgroundColor = Color(0xFF2D1F3D),
                                borderColor = Color(0x809C27B0)
                            )
                        }

                        "week" -> {
                            FilterBadge(
                                icon = Icons.Default.DateRange,
                                text = StringsManager.getThisWeek(lang),
                                iconColor = Color(0xFF2196F3),
                                backgroundColor = Color(0xFF1A2B3D),
                                borderColor = Color(0x802196F3)
                            )
                        }
                        "won" -> {
                            FilterBadge(
                                icon = Icons.Outlined.TrendingUp,
                                text = StringsManager.getWonTrades(lang),
                                iconColor = WifiGreen,
                                backgroundColor = WifiGreen.copy(alpha = 0.1f),
                                borderColor = WifiGreen.copy(alpha = 0.3f)
                            )
                        }
                        "lost" -> {
                            FilterBadge(
                                icon = Icons.Outlined.TrendingDown,
                                text = StringsManager.getLostTrades(lang),
                                iconColor = AccentSecondary,
                                backgroundColor = AccentSecondary.copy(alpha = 0.1f),
                                borderColor = AccentSecondary.copy(alpha = 0.3f)
                            )
                        }
                        "opened" -> {
                            FilterBadge(
                                icon = Icons.Outlined.Schedule,
                                text = StringsManager.getOpenTrades(lang),
                                iconColor = AccentWarning,
                                backgroundColor = AccentWarning.copy(alpha = 0.1f),
                                borderColor = AccentWarning.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// COMPOSABLE BADGE: Filter Badge
@Composable
private fun FilterBadge(
    icon: ImageVector,
    text: String,
    iconColor: Color,
    backgroundColor: Color,
    borderColor: Color
) {
    OutlinedCard(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactStatisticsSection(
    lang: String,
    historyList: List<TradingHistoryNew>,
    currency: String,
    colors: DashboardColors
) {
    val weeklyTrades = historyList.filter { isWithinLastWeek(it.createdAt) }
    val allTrades = historyList

    val weeklyTotalTrades = weeklyTrades.size
    val weeklyWonTrades = weeklyTrades.count { it.status == "won" }
    val weeklyWinRate = if (weeklyTotalTrades > 0) (weeklyWonTrades.toFloat() / weeklyTotalTrades * 100) else 0f
    val weeklyProfit = weeklyTrades.sumOf { (it.win - it.amount) }

    val totalTrades = allTrades.size
    val wonTrades = allTrades.count { it.status == "won" }
    val winRate = if (totalTrades > 0) (wonTrades.toFloat() / totalTrades * 100) else 0f
    val totalProfit = allTrades.sumOf { (it.win - it.amount) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = StringsManager.getThisWeekPerformance(lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = getWeekDateRange(),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    if (weeklyTotalTrades > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (weeklyWinRate >= 50) WifiGreen.copy(alpha = 0.2f) else AccentSecondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${weeklyWinRate.toInt()}% ${StringsManager.getWinRate(lang).uppercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (weeklyWinRate >= 50) WifiGreen else AccentSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (weeklyTotalTrades > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (weeklyProfit >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                contentDescription = null,
                                tint = if (weeklyProfit >= 0) WifiGreen else AccentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = StringsManager.getTotalPnL(lang),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formatCurrencyByISO(weeklyProfit, currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (weeklyProfit >= 0) WifiGreen else AccentSecondary
                                )
                            }
                        }

                        Text(
                            text = "$weeklyTotalTrades ${StringsManager.getTrades(lang)}",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = StringsManager.getNoTradesThisWeek(lang),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = StringsManager.getTotal(lang),
                value = totalTrades.toString(),
                icon = Icons.Outlined.BarChart,
                color = StatusBlue,
                colors = colors
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = StringsManager.getWinRate(lang),
                value = "${winRate.toInt()}%",
                icon = Icons.Outlined.TrendingUp,
                color = if (winRate >= 50) WifiGreen else AccentSecondary,
                colors = colors
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = StringsManager.getAllPnL(lang),
                value = formatCurrencyCompact(totalProfit, currency),
                icon = Icons.Outlined.AccountBalance,
                color = if (totalProfit >= 0) WifiGreen else AccentSecondary,
                colors = colors
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    colors: DashboardColors
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImprovedHistoryCard(
    lang: String,
    trade: TradingHistoryNew,
    currency: String
) {
    val statusColor = when (trade.status) {
        "won" -> WifiGreen
        "lost" -> AccentSecondary
        "opened" -> AccentWarning
        else -> TextMuted
    }

    val displayDirection = when (trade.trend.lowercase()) {
        "call", "buy" -> "BUY"
        "put", "sell" -> "SELL"
        else -> trade.trend.uppercase()
    }

    val directionColor = when (trade.trend.lowercase()) {
        "call", "buy" -> WifiGreen
        "put", "sell" -> AccentSecondary
        else -> TextSecondary
    }

    val entryTime = formatEntryTime(trade.createdAt)
    val exitTime = if (trade.finishedAt != null) formatEntryTime(trade.finishedAt) else "-"

    // ✅ IMPROVEMENT: State management untuk icon loading
    var iconLoadState by remember { mutableStateOf<IconLoadState>(IconLoadState.Loading) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ROW 1: ICON + ASSET INFO + AMOUNTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // LEFT SIDE: ICON + ASSET INFO
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // ICON CONTAINER
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = DarkSurface,
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                // CASE 1: ADA ICON URL - LOAD DENGAN COIL
                                !trade.iconUrl.isNullOrEmpty() -> {
                                    LaunchedEffect(trade.iconUrl) {
                                        iconLoadState = IconLoadState.Loading
                                    }

                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(trade.iconUrl)
                                            .crossfade(true)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        contentDescription = "${trade.assetName} icon",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            iconLoadState = IconLoadState.Loading
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = StatusBlue,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        },
                                        error = { error ->
                                            iconLoadState = IconLoadState.Error(error.result.throwable)
                                            Log.e("HistoryScreen", "❌ Failed to load icon: ${trade.iconUrl} - ${error.result.throwable?.message}")

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = getAssetInitials(trade.assetName),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        },
                                        onSuccess = {
                                            iconLoadState = IconLoadState.Success
                                            Log.d("HistoryScreen", "✅ Successfully loaded icon: ${trade.iconUrl}")
                                        }
                                    )
                                }

                                // CASE 2: TIDAK ADA ICON URL - LANGSUNG SHOW INITIALS
                                else -> {
                                    iconLoadState = IconLoadState.NoIcon
                                    Text(
                                        text = getAssetInitials(trade.assetName),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ASSET INFO COLUMN
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = trade.assetName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = displayDirection,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = directionColor,
                            maxLines = 1
                        )
                    }
                }

                // RIGHT SIDE: AMOUNTS
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val profitLoss = trade.win - trade.amount
                    Text(
                        text = formatCurrencyByISO(trade.win, currency),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            profitLoss > 0 -> WifiGreen
                            profitLoss < 0 -> AccentSecondary
                            else -> StatusBlue
                        },
                        maxLines = 1
                    )

                    Text(
                        text = formatCurrencyByISO(trade.amount, currency),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        maxLines = 1
                    )

                }
            }

            // ROW 2: TIME INFO + STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: TIME INFO
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$entryTime  ${StringsManager.getEntry(lang)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (trade.status != "opened") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ExitToApp,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$exitTime  ${StringsManager.getExit(lang)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // RIGHT SIDE: STATUS BADGE
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = when (trade.status) {
                            "won" -> "WON"
                            "lost" -> "LOST"
                            "opened" -> "OPENED"
                            else -> trade.status.uppercase()
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ✅ TAMBAH: Helper function untuk asset initials
private fun getAssetInitials(assetName: String): String {
    return assetName.take(2).uppercase()
}

// Helper function to format entry/exit time
private fun formatEntryTime(dateTime: String): String {
    return try {
        val deviceTimeZone = TimeZone.getDefault()
        val deviceLocale = Locale.getDefault()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", deviceLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val outputFormat = SimpleDateFormat("dd MMM HH.mm.ss", deviceLocale).apply {
            timeZone = deviceTimeZone
        }

        val date = inputFormat.parse(dateTime)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        try {
            val deviceTimeZone = TimeZone.getDefault()
            val deviceLocale = Locale.getDefault()

            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", deviceLocale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val outputFormat = SimpleDateFormat("dd MMM HH.mm.ss", deviceLocale).apply {
                timeZone = deviceTimeZone
            }

            val cleanDateTime = dateTime.replace("T", " ").take(19)
            val date = inputFormat2.parse(cleanDateTime)
            outputFormat.format(date ?: Date())
        } catch (e2: Exception) {
            dateTime.take(16).replace("T", " ")
        }
    }
}

@Composable
private fun TradeMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingSection(lang: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .rotate(rotation),
                color = StatusBlue,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = StringsManager.getLoadingTradingHistory(lang),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorSection(
    lang: String,
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, AccentSecondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
                tint = AccentSecondary,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = StringsManager.getSomethingWentWrong(lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Text(
                text = error,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(StringsManager.getDismiss(lang), fontSize = 13.sp)
                }

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(StringsManager.getRetry(lang), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateSection(
    lang: String,
    selectedFilter: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryToggleOff,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (selectedFilter) {
                    "today" -> StringsManager.getNoTradesToday(lang)
                    "all" -> StringsManager.getNoTradingHistory(lang)
                    "week" -> StringsManager.getNoTradesThisWeek(lang)
                    "won" -> StringsManager.getNoWonTrades(lang)
                    "lost" -> StringsManager.getNoLostTrades(lang)
                    "opened" -> StringsManager.getNoOpenedTrades(lang)
                    else -> StringsManager.getNoTradingHistory(lang)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (selectedFilter) {
                    "all" -> StringsManager.getStartTradingToSeeHistory(lang)
                    "week" -> StringsManager.getNoTradesFound(lang)
                    else -> StringsManager.getTryChangingFilter(lang)
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    lang: String,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val filters = listOf(
        "today" to StringsManager.getToday(lang),
        "all" to StringsManager.getAllTrades(lang),
        "week" to StringsManager.getThisWeek(lang),
        "won" to StringsManager.getWonTrades(lang),
        "lost" to StringsManager.getLostTrades(lang),
        "opened" to StringsManager.getOpenTrades(lang)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() },
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(TextMuted, RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = StringsManager.getFilterTrades(lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    filters.forEach { (filter, label) ->
                        FilterOption(
                            label = label,
                            isSelected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) StatusBlue.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = StatusBlue,
                    unselectedColor = TextMuted
                ),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) StatusBlue else TextPrimary
            )
        }
    }
}

private fun isWithinLastWeek(dateTime: String): Boolean {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val tradeDate = inputFormat.parse(dateTime) ?: return false
        val currentDate = Date()
        val weekAgo = Date(currentDate.time - TimeUnit.DAYS.toMillis(7))

        tradeDate.after(weekAgo) || tradeDate == weekAgo
    } catch (e: Exception) {
        try {
            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val cleanDateTime = dateTime.replace("T", " ").take(19)
            val tradeDate = inputFormat2.parse(cleanDateTime) ?: return false
            val currentDate = Date()
            val weekAgo = Date(currentDate.time - TimeUnit.DAYS.toMillis(7))

            tradeDate.after(weekAgo) || tradeDate == weekAgo
        } catch (e2: Exception) {
            false
        }
    }
}

private fun getWeekDateRange(): String {
    val currentDate = Date()
    val weekAgo = Date(currentDate.time - TimeUnit.DAYS.toMillis(7))

    val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    return "${outputFormat.format(weekAgo)} - ${outputFormat.format(currentDate)}"
}

private fun formatPrice(price: Double): String {
    return String.format("%.5f", price)
}

private fun formatDateTime(dateTime: String): String {
    return try {
        val deviceTimeZone = TimeZone.getDefault()
        val deviceLocale = Locale.getDefault()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", deviceLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val outputFormat = SimpleDateFormat("dd MMM, HH:mm", deviceLocale).apply {
            timeZone = deviceTimeZone
        }

        val date = inputFormat.parse(dateTime)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        try {
            val deviceTimeZone = TimeZone.getDefault()
            val deviceLocale = Locale.getDefault()

            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", deviceLocale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", deviceLocale).apply {
                timeZone = deviceTimeZone
            }

            val cleanDateTime = dateTime.replace("T", " ").take(19)
            val date = inputFormat2.parse(cleanDateTime)
            outputFormat.format(date ?: Date())
        } catch (e2: Exception) {
            dateTime.take(16).replace("T", " ")
        }
    }
}

// ✅ FUNGSI UTAMA: Format currency dengan ISO code (untuk tampilan detail)
private fun formatCurrencyByISO(amount: Long, currencyISO: String): String {
    val amountValue = amount / 100.0

    return when (currencyISO.uppercase()) {
        "IDR" -> {
            formatRupiahCompact(amountValue, includeSymbol = true)
        }
        "USD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "$${formatter.format(amountValue)}"
        }
        "EUR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.GERMANY).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "€${formatter.format(amountValue)}"
        }
        "GBP" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.UK).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "£${formatter.format(amountValue)}"
        }
        "JPY" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.JAPAN).apply {
                groupingSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0", symbols)
            "¥${formatter.format(amountValue.toLong())}"
        }
        "CNY", "RMB" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.CHINA).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "¥${formatter.format(amountValue)}"
        }
        "KRW" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.KOREA).apply {
                groupingSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0", symbols)
            "₩${formatter.format(amountValue.toLong())}"
        }
        "SGD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "SG")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "S${formatter.format(amountValue)}"
        }
        "MYR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ms", "MY")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "RM${formatter.format(amountValue)}"
        }
        "THB" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("th", "TH")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "฿${formatter.format(amountValue)}"
        }
        "PHP" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "PH")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "₱${formatter.format(amountValue)}"
        }
        "VND" -> {
            formatVietnameseDongCompact(amountValue, includeSymbol = true)
        }
        "INR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "IN")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "₹${formatter.format(amountValue)}"
        }
        "AUD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "AU")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "A${formatter.format(amountValue)}"
        }
        "CAD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.CANADA).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "C${formatter.format(amountValue)}"
        }
        else -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            "$currencyISO ${formatter.format(amountValue)}"
        }
    }
}

// ✅ FUNGSI UTAMA: Format currency compact (untuk statistik card)
private fun formatCurrencyCompact(amount: Long, currencyISO: String): String {
    val amountValue = amount / 100.0

    return when (currencyISO.uppercase()) {
        "IDR" -> {
            formatRupiahVeryCompact(amountValue)
        }
        "VND" -> {
            formatVietnameseDongCompact(amountValue, includeSymbol = false)
        }
        "USD", "SGD", "AUD", "CAD", "NZD", "HKD", "TWD" -> {
            formatDollarCompact(amountValue)
        }
        "EUR", "GBP", "CHF" -> {
            formatEuroCompact(amountValue)
        }
        "JPY", "KRW" -> {
            formatAsianCurrencyCompact(amountValue)
        }
        "CNY", "RMB" -> {
            formatChineseYuanCompact(amountValue)
        }
        "INR", "PKR", "LKR", "BDT" -> {
            formatRupeeCompact(amountValue)
        }
        "THB" -> {
            formatThaiBahtCompact(amountValue)
        }
        "MYR" -> {
            formatMalaysianRinggitCompact(amountValue)
        }
        "PHP" -> {
            formatPhilippinePesoCompact(amountValue)
        }
        else -> {
            formatGenericCompact(amountValue)
        }
    }
}

// ✅ FUNGSI KHUSUS: Format Rupiah dengan singkatan Indonesia (untuk tampilan detail)
private fun formatRupiahCompact(amount: Double, includeSymbol: Boolean = true): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""
    val symbol = if (includeSymbol) "Rp " else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix$symbol${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI KHUSUS: Format Rupiah sangat compact (untuk statistik card)
private fun formatRupiahVeryCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI KHUSUS: Format Vietnamese Dong (nilai sangat besar)
private fun formatVietnameseDongCompact(amount: Double, includeSymbol: Boolean = true): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""
    val symbol = if (includeSymbol) "₫" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("vi", "VN")).apply {
        groupingSeparator = ','
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix$symbol${formatter.format(absAmount.toLong())}"
}

// Perbaikan function isToday()
private fun isToday(dateTime: String): Boolean {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val tradeDate = inputFormat.parse(dateTime) ?: return false

        // Convert UTC trade date ke local timezone
        val localCalendar = Calendar.getInstance().apply {
            time = tradeDate
        }

        // Get today's date in local timezone
        val todayCalendar = Calendar.getInstance()

        // Compare year, month, and day
        return localCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                localCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                localCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)

    } catch (e: Exception) {
        try {
            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val cleanDateTime = dateTime.replace("T", " ").take(19)
            val tradeDate = inputFormat2.parse(cleanDateTime) ?: return false

            val localCalendar = Calendar.getInstance().apply {
                time = tradeDate
            }

            val todayCalendar = Calendar.getInstance()

            return localCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    localCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                    localCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)

        } catch (e2: Exception) {
            false
        }
    }
}

// ✅ FUNGSI BANTU: Format untuk mata uang dollar
private fun formatDollarCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk mata uang euro
private fun formatEuroCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.GERMANY).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk mata uang Asia (Yen, Won)
private fun formatAsianCurrencyCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.JAPAN).apply {
        groupingSeparator = ','
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk Chinese Yuan
private fun formatChineseYuanCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.CHINA).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk Rupee (India, Pakistan, dll)
private fun formatRupeeCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "IN")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk Thai Baht
private fun formatThaiBahtCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("th", "TH")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk Malaysian Ringgit
private fun formatMalaysianRinggitCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ms", "MY")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format untuk Philippine Peso
private fun formatPhilippinePesoCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "PH")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}

// ✅ FUNGSI BANTU: Format generic untuk mata uang lainnya
private fun formatGenericCompact(amount: Double): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = java.text.DecimalFormat("#,##0", symbols)
    return "$prefix${formatter.format(absAmount.toLong())}"
}