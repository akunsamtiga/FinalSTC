package com.autotrade.finalstc.presentation.main.history

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.currentCurrency.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("all") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        viewModel.loadTradingHistory()
    }

    val filteredHistory = remember(historyList, selectedFilter) {
        when (selectedFilter) {
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
                        onToggleAccount = { viewModel.toggleAccountType() }
                    )
                }

                if (historyList.isNotEmpty()) {
                    item {
                        CompactStatisticsSection(
                            lang = lang,
                            historyList = historyList,
                            currency = currency
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
    onToggleAccount: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = StringsManager.getTradingHistory(lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedCard(
                            modifier = Modifier.wrapContentSize(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (uiState.showDemoAccount) Color(0xFF3A2F00) else Color(0xFF1A3A1A),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.showDemoAccount) Color(0x80FFCC80) else Color(0x804CAF50)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.showDemoAccount) Icons.Default.School else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (uiState.showDemoAccount) Color(0xFFFFCC80) else Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.showDemoAccount)
                                        StringsManager.getDemoAccount(lang)
                                    else
                                        StringsManager.getRealAccount(lang),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (selectedFilter == "week") {
                            OutlinedCard(
                                modifier = Modifier.wrapContentSize(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = Color(0xFF1A2B3D),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0x802196F3)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = StringsManager.getThisWeek(lang),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onFilterClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = StringsManager.getFilter(lang),
                            tint = StatusBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleAccount,
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = StringsManager.getToggleAccount(lang),
                            tint = StatusBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatisticsSection(
    lang: String,
    historyList: List<TradingHistoryNew>,
    currency: String
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
            border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
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
                color = StatusBlue
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = StringsManager.getWinRate(lang),
                value = "${winRate.toInt()}%",
                icon = Icons.Outlined.TrendingUp,
                color = if (winRate >= 50) WifiGreen else AccentSecondary
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = StringsManager.getAllPnL(lang),
                value = formatCurrencyCompact(totalProfit, currency),
                icon = Icons.Outlined.AccountBalance,
                color = if (totalProfit >= 0) WifiGreen else AccentSecondary
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
    color: Color
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
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
                fontSize = 16.sp,
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

    val profit = trade.win - trade.amount
    val profitColor = if (profit >= 0) AccentProfit else AccentSecondary
    val isWeeklyTrade = isWithinLastWeek(trade.createdAt)

    val displayDirection = when (trade.trend.lowercase()) {
        "call" -> "BUY"
        "put" -> "SELL"
        "buy" -> "BUY"
        "sell" -> "SELL"
        else -> trade.trend.uppercase()
    }

    val directionColor = when (trade.trend.lowercase()) {
        "call", "buy" -> WifiGreen
        "put", "sell" -> AccentSecondary
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isWeeklyTrade) StatusBlue.copy(alpha = 0.3f) else BorderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                statusColor.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trade.assetName.take(2).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = trade.assetName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trade.assetRic,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            if (isWeeklyTrade) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StatusBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = StringsManager.getThisWeek(lang),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatusBlue,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = trade.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TradeMetric(
                    label = StringsManager.getAmount(lang),
                    value = formatCurrencyByISO(trade.amount, currency),
                    color = TextPrimary
                )

                TradeMetric(
                    label = StringsManager.getDirection(lang),
                    value = displayDirection,
                    color = directionColor
                )

                TradeMetric(
                    label = "P&L",
                    value = formatCurrencyByISO(profit, currency),
                    color = profitColor
                )
            }

            if (trade.openRate > 0 && trade.closeRate != null) {
                HorizontalDivider(
                    color = BorderColor.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TradeMetric(
                        label = StringsManager.getOpen(lang),
                        value = formatPrice(trade.openRate),
                        color = TextSecondary
                    )

                    TradeMetric(
                        label = StringsManager.getClose(lang),
                        value = formatPrice(trade.closeRate),
                        color = TextSecondary
                    )

                    TradeMetric(
                        label = StringsManager.getPayout(lang),
                        value = "${trade.paymentRate}%",
                        color = StatusBlue
                    )
                }
            }

            HorizontalDivider(
                color = BorderColor.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDateTime(trade.createdAt),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Text(
                    text = "#${trade.id}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
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
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
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

private fun formatCurrencyByISO(amount: Long, currencyISO: String): String {
    return when (currencyISO.uppercase()) {
        "IDR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "USD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "EUR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.GERMANY).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "GBP" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.UK).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "JPY" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.JAPAN).apply {
                groupingSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0", symbols)
            formatter.format(amount / 100.0)
        }
        "CNY", "RMB" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.CHINA).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "KRW" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.KOREA).apply {
                groupingSeparator = ','
            }
            val formatter = java.text.DecimalFormat("#,##0", symbols)
            formatter.format(amount / 100.0)
        }
        "SGD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "SG")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "MYR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ms", "MY")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "THB" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("th", "TH")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "PHP" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "PH")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "VND" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("vi", "VN")).apply {
                groupingSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0", symbols)
            formatter.format(amount / 100.0)
        }
        "INR" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "IN")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "AUD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "AU")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "CAD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.CANADA).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "CHF" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("de", "CH")).apply {
                groupingSeparator = '\''
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        "NZD" -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale("en", "NZ")).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
        else -> {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = java.text.DecimalFormat("#,##0.00", symbols)
            formatter.format(amount / 100.0)
        }
    }
}

private fun formatCurrencyCompact(amount: Long, currencyISO: String): String {
    val absAmount = Math.abs(amount)
    val isNegative = amount < 0
    val prefix = if (isNegative) "-" else ""

    return when (currencyISO.uppercase()) {
        "IDR", "USD", "SGD", "AUD", "CAD", "NZD", "EUR", "GBP", "JPY", "KRW",
        "CNY", "RMB", "MYR", "THB", "PHP", "VND", "INR", "CHF" -> {
            when {
                absAmount >= 1_000_000_000 -> String.format("${prefix}%.1fB", absAmount / 1_000_000_000.0)
                absAmount >= 1_000_000 -> String.format("${prefix}%.1fM", absAmount / 1_000_000.0)
                absAmount >= 1_000 -> String.format("${prefix}%.1fK", absAmount / 1_000.0)
                else -> "${prefix}$absAmount"
            }
        }
        else -> {
            when {
                absAmount >= 1_000_000_000 -> String.format("${prefix}%.1fB", absAmount / 1_000_000_000.0)
                absAmount >= 1_000_000 -> String.format("${prefix}%.1fM", absAmount / 1_000_000.0)
                absAmount >= 1_000 -> String.format("${prefix}%.1fK", absAmount / 1_000.0)
                else -> "${prefix}$absAmount"
            }
        }
    }
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