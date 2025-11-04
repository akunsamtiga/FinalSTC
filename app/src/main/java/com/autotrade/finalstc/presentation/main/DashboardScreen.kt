package com.autotrade.finalstc.presentation.main.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autotrade.finalstc.R
import com.autotrade.finalstc.presentation.main.MainViewModel
import com.autotrade.finalstc.presentation.main.history.HistoryViewModel
import com.autotrade.finalstc.presentation.main.history.TradingHistoryNew
import kotlinx.coroutines.delay
import java.text.*
import java.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import com.autotrade.finalstc.presentation.main.dashboard.*
import com.autotrade.finalstc.presentation.theme.ThemeViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import com.autotrade.finalstc.data.local.LanguageManager
import com.autotrade.finalstc.utils.StringsManager
import kotlinx.coroutines.launch

data class DashboardTheme(
    val isDarkMode: Boolean = true
) {
    val colors: DashboardColors
        get() = if (isDarkMode) DarkColors else LightColors
}

data class DashboardColors(
    val background: Color,
    val darkBackground: Color,
    val darkBackgroundClock: Color,
    val surface: Color,
    val surface2: Color,
    val bgmain: Color,
    val bgmain2: Color,
    val cardBackground: Color,

    val textPrimary: Color,
    val TextPrimary1: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textMuted2: Color,
    val placeholderText: Color,

    val accentPrimary: Color,
    val accentPrimary1: Color,
    val accentPrimary2: Color,
    val accentPrimary2main: Color,
    val accentPrimary3: Color,
    val accentPrimary4: Color,
    val accentPrimary5: Color,
    val accentSecondary: Color,
    val accentWarning: Color,
    val accentProfit: Color,
    val wifiGreen: Color,

    val borderColor: Color,
    val controls: Color,

    val toastBg: Color,
    val toastText: Color,

    val successColor: Color,
    val errorColor: Color,
    val warningColor: Color,

    val chartLine: Color,
    val chartLine2: Color,
    val chartGradientStart: Color,
    val chartGradientEnd: Color,
    val chartText: Color,
    val chartGrid: Color,

    val cardBorderPrimary: Color,
    val cardBorderSecondary: Color,
    val cardBorderAccent: Color,

    val botStatusRunningBg: Color,
    val botStatusPausedBg: Color,
    val botStatusStoppedBg: Color,
    val botButtonEnabledBg: Color,
    val botButtonDisabledBg: Color,
    val botInfoCardBg: Color,

    val botButtonResumeBg: Color,
    val botButtonResumeBorder: Color,
    val botButtonResumeText: Color,
    val botButtonResumeShadow: Color,

    val botButtonPauseBg: Color,
    val botButtonPauseBorder: Color,
    val botButtonPauseText: Color,
    val botButtonPauseShadow: Color,

    val botButtonStopBg: Color,
    val botButtonStopBorder: Color,
    val botButtonStopText: Color,
    val botButtonStopShadow: Color,

    val chartBackground: Color,
    val bgclock: Color,

    val dialogBackground: Color,
    val dialogSurface: Color,
    val dialogHeaderGradientStart: Color,
    val dialogHeaderGradientMid: Color,
    val dialogHeaderGradientEnd: Color,
    val dialogBorder: Color,
    val dialogCloseButtonBg: Color,
    val dialogCloseButtonBorder: Color,

    val dialogButtonSelected: Color,
    val dialogButtonSelectedText: Color,
    val dialogButtonUnselected: Color,
    val dialogButtonUnselectedText: Color,
    val dialogButtonBorder: Color,
    val dialogButtonSelectedBorder: Color,

    val dialogInputBg: Color,
    val dialogInputBgFocused: Color,
    val dialogInputBorder: Color,
    val dialogInputBorderFocused: Color,
    val dialogInputText: Color,
    val dialogInputPlaceholder: Color,

    val dialogCustomButtonBg: Color,
    val dialogCustomButtonBorder: Color,
    val dialogCustomButtonText: Color,
    val dialogCancelButtonBg: Color,
    val dialogCancelButtonText: Color,

    val dialogPreviewBg: Color,
    val dialogPreviewBorder: Color,
    val dialogPreviewAccent: Color,
    val dialogPreviewAmountBg: Color,
    val dialogPreviewRiskBg: Color,

    val dialogInfoBg: Color,
    val dialogInfoBorder: Color,
    val dialogInfoIconBg: Color,
    val dialogDivider: Color,

    val dialogSuccessBg: Color,
    val dialogSuccessBorder: Color,
    val dialogSuccessIcon: Color,
    val dialogSuccessText: Color,

    val dialogMultiplierFixedBg: Color,
    val dialogMultiplierFixedIcon: Color,
    val dialogMultiplierPercentBg: Color,
    val dialogMultiplierPercentIcon: Color,

    val dialogDropdownBg: Color,
    val dialogDropdownBorder: Color,
    val dialogDropdownItemHover: Color,
)

val DarkColors = DashboardColors(
    background = Color(0xFF161616),
    darkBackground = Color(0xFF191919),
    darkBackgroundClock = Color(0xFF1B1B1B),
    surface = Color(0xFF1F1F1F),
    surface2 = Color(0xFF0A141B),
    bgmain = Color(0xFF161616),
    bgmain2 = Color(0xFF242738),
    cardBackground = Color(0xFF323232),
    textPrimary = Color(0xFFEBEBEB),
    TextPrimary1 = Color(0xFFEBEBEB),
    textSecondary = Color(0xFFBAC1CB),
    textMuted = Color(0xBA7E7E7E),
    textMuted2 = Color(0xB90D1A22),
    placeholderText = Color(0xFF747474),
    accentPrimary = Color(0xFFD9D9D9),
    accentPrimary1 = Color(0xFF3B82F6),
    accentPrimary2main = Color(0xFF92FFCA),
    accentPrimary2 = Color(0xFF3B82F6),
    accentPrimary3 = Color(0xFF3B82F6),
    accentPrimary4 = Color(0xFFD9D9D9),
    accentPrimary5 = Color(0xFF1B8413),
    accentSecondary = Color(0xFFDC4D4D),
    accentWarning = Color(0xFFFDA359),
    accentProfit = Color(0xFF7AF1C1),
    wifiGreen = Color(0xFF67D88B),
    borderColor = Color(0xFF494949),
    controls = Color(0x514C4C4C),
    toastBg = Color(0xFFFFFFFF),
    toastText = Color(0xFF373737),
    successColor = Color(0xFF10B981),
    errorColor = Color(0xFFEF4444),
    warningColor = Color(0xFFFBBF24),
    chartLine = Color.Cyan,
    chartLine2 = Color.Cyan,
    chartGradientStart = Color(0xFF1E1E1E),
    chartGradientEnd = Color(0xFF0D0D0D),
    chartText = Color.Gray,
    chartGrid = Color(0xFF2C4749),
    cardBorderPrimary = Color(0xFF3A3A3A),
    cardBorderSecondary = Color(0xFF2A2A2A),
    cardBorderAccent = Color(0xFF4A4A4A),
    botStatusRunningBg = Color(0xFF1F2F27),
    botStatusPausedBg = Color(0xFF2F2819),
    botStatusStoppedBg = Color(0xFF2F1F1F),
    botButtonEnabledBg = Color(0xFF353535),
    botButtonDisabledBg = Color(0xFF151515),
    botInfoCardBg = Color(0xFF242424),

    botButtonResumeBg = Color(0xFF1F3A2E),
    botButtonResumeBorder = Color(0xFF10B981),
    botButtonResumeText = Color(0xFF10B981),
    botButtonResumeShadow = Color(0xFF10B981),

    botButtonPauseBg = Color(0xFF3A2F1F),
    botButtonPauseBorder = Color(0xFFFBBF24),
    botButtonPauseText = Color(0xFFFBBF24),
    botButtonPauseShadow = Color(0xFFFBBF24),

    botButtonStopBg = Color(0xFF3A1F1F),
    botButtonStopBorder = Color(0xFFEF4444),
    botButtonStopText = Color(0xFFEF4444),
    botButtonStopShadow = Color(0xFFEF4444),

    chartBackground = Color(0xFF1B1B1B),
    bgclock = Color(0xFF161616),

    dialogBackground = Color(0xFF1A1A1A),
    dialogSurface = Color(0xFF242424),
    dialogHeaderGradientStart = Color(0xFF252525),
    dialogHeaderGradientMid = Color(0xFF202020),
    dialogHeaderGradientEnd = Color(0xFF1A1A1A),
    dialogBorder = Color(0xFF3A3A3A),
    dialogCloseButtonBg = Color(0xFF2A2A2A),
    dialogCloseButtonBorder = Color(0xFF404040),

    dialogButtonSelected = Color(0xFF2A3F35),
    dialogButtonSelectedText = Color(0xFF10B981),
    dialogButtonUnselected = Color(0xFF252525),
    dialogButtonUnselectedText = Color(0xFF9CA3AF),
    dialogButtonBorder = Color(0xFF3A3A3A),
    dialogButtonSelectedBorder = Color(0xFF10B981),

    dialogInputBg = Color(0xFF252525),
    dialogInputBgFocused = Color(0xFF2A2A2A),
    dialogInputBorder = Color(0xFF3A3A3A),
    dialogInputBorderFocused = Color(0xFF60A5FA),
    dialogInputText = Color(0xFFE5E7EB),
    dialogInputPlaceholder = Color(0xFF6B7280),

    dialogCustomButtonBg = Color(0xFF1E3A5F),
    dialogCustomButtonBorder = Color(0xFF3B82F6),
    dialogCustomButtonText = Color(0xFF60A5FA),
    dialogCancelButtonBg = Color(0xFF3A1F1F),
    dialogCancelButtonText = Color(0xFFEF4444),

    dialogPreviewBg = Color(0xFF252525),
    dialogPreviewBorder = Color(0xFF3B82F6),
    dialogPreviewAccent = Color(0xFF60A5FA),
    dialogPreviewAmountBg = Color(0xFF1E1E1E),
    dialogPreviewRiskBg = Color(0xFF1E3A5F),

    dialogInfoBg = Color(0xFF242424),
    dialogInfoBorder = Color(0xFF3A3A3A),
    dialogInfoIconBg = Color(0xFF2A2A2A),
    dialogDivider = Color(0xFF333333),

    dialogSuccessBg = Color(0xFF1F3A2E),
    dialogSuccessBorder = Color(0xFF10B981),
    dialogSuccessIcon = Color(0xFF10B981),
    dialogSuccessText = Color(0xFF34D399),

    dialogMultiplierFixedBg = Color(0xFF1F3A2E),
    dialogMultiplierFixedIcon = Color(0xFF10B981),
    dialogMultiplierPercentBg = Color(0xFF3A2F1F),
    dialogMultiplierPercentIcon = Color(0xFFFBBF24),

    dialogDropdownBg = Color(0xFF242424),
    dialogDropdownBorder = Color(0xFF3A3A3A),
    dialogDropdownItemHover = Color(0xFF2A2A2A),
)

val LightColors = DashboardColors(
    background = Color(0xFFF8F9FA),
    darkBackground = Color(0xFFF8F9FA),
    darkBackgroundClock = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0x2AA6BAD8),
    bgmain = Color(0xFF161616),
    bgmain2 = Color(0xFF242738),
    cardBackground = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1F2937),
    TextPrimary1 = Color(0xFFEBEBEB),
    textSecondary = Color(0xFF6B7280),
    textMuted = Color(0xFF9CA3AF),
    textMuted2 = Color(0x1E6B99E3),
    placeholderText = Color(0xFFD1D5DB),
    accentPrimary = Color(0xFF3B82F6),
    accentPrimary1 = Color(0xFF3B82F6),
    accentPrimary2 = Color(0xFFD1D5DB),
    accentPrimary2main = Color(0xFF2EAB71),
    accentPrimary3 = Color(0xFF3B82F6),
    accentPrimary4 = Color(0xFF1B8413),
    accentPrimary5 = Color(0xFF1B8413),
    accentSecondary = Color(0xFFEF4444),
    accentWarning = Color(0xFFF59E0B),
    accentProfit = Color(0xFF10B981),
    wifiGreen = Color(0xFF059669),
    borderColor = Color(0xFFD6DADF),
    controls = Color(0xFFF3F4F6),
    toastBg = Color(0xFF1F2937),
    toastText = Color(0xFFFFFFFF),
    successColor = Color(0xFF059669),
    errorColor = Color(0xFFDC2626),
    warningColor = Color(0xFFD97706),
    chartLine = Color(0xFF009A11),
    chartLine2 = Color(0xFF059A00),
    chartGradientStart = Color(0xFFF8F9FA),
    chartGradientEnd = Color(0xFFE5E7EB),
    chartText = Color.DarkGray,
    chartGrid = Color(0xFFA9C5D5),
    cardBorderPrimary = Color(0xFFD1D5DB),
    cardBorderSecondary = Color(0xFFE5E7EB),
    cardBorderAccent = Color(0xFFC7CBD1),
    botStatusRunningBg = Color(0xFFE8F5EE),
    botStatusPausedBg = Color(0xFFFEF3E2),
    botStatusStoppedBg = Color(0xFFFEE8E8),
    botButtonEnabledBg = Color(0xFFF9FAFB),
    botButtonDisabledBg = Color(0xFFF3F4F6),
    botInfoCardBg = Color(0xFFF8F9FA),

    botButtonResumeBg = Color(0xFFD1FAE5),
    botButtonResumeBorder = Color(0xFF059669),
    botButtonResumeText = Color(0xFF047857),
    botButtonResumeShadow = Color(0xFF059669),

    botButtonPauseBg = Color(0xFFFEF3C7),
    botButtonPauseBorder = Color(0xFFD97706),
    botButtonPauseText = Color(0xFFB45309),
    botButtonPauseShadow = Color(0xFFD97706),

    botButtonStopBg = Color(0xFFFEE2E2),
    botButtonStopBorder = Color(0xFFDB6767),
    botButtonStopText = Color(0xFFAE5A5A),
    botButtonStopShadow = Color(0xFFDB5D5D),

    chartBackground = Color.White,
    bgclock = Color(0xFFF5F8FF),

    dialogBackground = Color(0xFFFFFFFF),
    dialogSurface = Color(0xFFF9FAFB),
    dialogHeaderGradientStart = Color(0xFFFAFBFC),
    dialogHeaderGradientMid = Color(0xFFF7F8FA),
    dialogHeaderGradientEnd = Color(0xFFFFFFFF),
    dialogBorder = Color(0xFFE5E7EB),
    dialogCloseButtonBg = Color(0xFFF3F4F6),
    dialogCloseButtonBorder = Color(0xFFD1D5DB),

    dialogButtonSelected = Color(0xFFDCFCE7),
    dialogButtonSelectedText = Color(0xFF059669),
    dialogButtonUnselected = Color(0xFFF9FAFB),
    dialogButtonUnselectedText = Color(0xFF6B7280),
    dialogButtonBorder = Color(0xFFE5E7EB),
    dialogButtonSelectedBorder = Color(0xFF10B981),

    dialogInputBg = Color(0xFFF9FAFB),
    dialogInputBgFocused = Color(0xFFFFFFFF),
    dialogInputBorder = Color(0xFFD1D5DB),
    dialogInputBorderFocused = Color(0xFF3B82F6),
    dialogInputText = Color(0xFF111827),
    dialogInputPlaceholder = Color(0xFF9CA3AF),

    dialogCustomButtonBg = Color(0xFFDBEAFE),
    dialogCustomButtonBorder = Color(0xFF3B82F6),
    dialogCustomButtonText = Color(0xFF1D4ED8),
    dialogCancelButtonBg = Color(0xFFFEE2E2),
    dialogCancelButtonText = Color(0xFFDC2626),

    dialogPreviewBg = Color(0xFFF9FAFB),
    dialogPreviewBorder = Color(0xFF3B82F6),
    dialogPreviewAccent = Color(0xFF2563EB),
    dialogPreviewAmountBg = Color(0xFFFFFFFF),
    dialogPreviewRiskBg = Color(0xFFEFF6FF),

    dialogInfoBg = Color(0xFFFAFAFA),
    dialogInfoBorder = Color(0xFFE5E7EB),
    dialogInfoIconBg = Color(0xFFF3F4F6),
    dialogDivider = Color(0xFFE5E7EB),

    dialogSuccessBg = Color(0xFFD1FAE5),
    dialogSuccessBorder = Color(0xFF10B981),
    dialogSuccessIcon = Color(0xFF059669),
    dialogSuccessText = Color(0xFF047857),

    dialogMultiplierFixedBg = Color(0xFFD1FAE5),
    dialogMultiplierFixedIcon = Color(0xFF059669),
    dialogMultiplierPercentBg = Color(0xFFFEF3C7),
    dialogMultiplierPercentIcon = Color(0xFFD97706),

    dialogDropdownBg = Color(0xFFFFFFFF),
    dialogDropdownBorder = Color(0xFFE5E7EB),
    dialogDropdownItemHover = Color(0xFFF3F4F6),
)

private val DarkBackground = Color(0xFF1B1B1B)
private val DarkSurface = Color(0xFF1F1F1F)
private val CardBackground = Color(0xFF2B2B2B)
private val AccentPrimary = Color(0xFFD9D9D9)
private val AccentSecondary = Color(0xFFDC4D4D)
private val AccentWarning = Color(0xFFFDA359)
private val TextPrimary = Color(0xFFEBEBEB)
private val TextSecondary = Color(0xFFBAC1CB)
private val TextMuted = Color(0xBA7E7E7E)
private val PlaceholderText = Color(0xFF747474)
private val BorderColor = Color(0xFF323232)
private val Controls = Color(0x514C4C4C)
private val WifiGreen = Color(0xFF67D88B)
private val AccentProfit = Color(0xFF7AF1C1)
private val ToastBg = Color(0xFFFFFFFF)
private val ToastText = Color(0xFF373737)

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel,
) {
    val dashboardTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val colors = dashboardTheme.colors

    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val assets by dashboardViewModel.assets.collectAsStateWithLifecycle()
    val tradeResults by dashboardViewModel.tradeResults.collectAsStateWithLifecycle()
    val scheduledOrders by dashboardViewModel.scheduledOrders.collectAsStateWithLifecycle()
    val historyList by historyViewModel.historyList.collectAsStateWithLifecycle()
    val followOrders by dashboardViewModel.followOrders.collectAsStateWithLifecycle()
    val indicatorOrders by dashboardViewModel.indicatorOrders.collectAsStateWithLifecycle()
    val localTradingStats by dashboardViewModel.localTradingStats.collectAsStateWithLifecycle()

    val indicatorPredictionInfo by dashboardViewModel.indicatorPredictionInfo.collectAsStateWithLifecycle()

    val todayProfit by dashboardViewModel.todayProfit.collectAsStateWithLifecycle()
    val isCalculatingTodayProfit by dashboardViewModel.isCalculatingTodayProfit.collectAsStateWithLifecycle()

    val ctcOrders by dashboardViewModel.ctcOrders.collectAsStateWithLifecycle()

    val currentLanguage by dashboardViewModel.currentLanguage.collectAsStateWithLifecycle()

    var showAssetDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showIndicatorSettingsDialog by remember { mutableStateOf(false) }
    var showMultilineScheduleDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.botState, uiState.isFollowModeActive, uiState.isIndicatorModeActive, uiState.isCTCModeActive) {
        println("🔍 BOT STATE CHECK -> botState=${uiState.botState}, follow=${uiState.isFollowModeActive}, indicator=${uiState.isIndicatorModeActive}, ctc=${uiState.isCTCModeActive}")
    }

    LaunchedEffect(uiState.isDemoAccount) {
        println("Account type changed to ${if (uiState.isDemoAccount) "Demo" else "Real"}")
        historyViewModel.loadTradingHistory(uiState.isDemoAccount)
    }

    LaunchedEffect(historyList, uiState.isDemoAccount) {
        println("History list changed: ${historyList.size} items for ${if (uiState.isDemoAccount) "Demo" else "Real"} account")
        dashboardViewModel.updateHistoryList(historyList)
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.refreshTrigger.collect { timestamp ->
            println("DashboardScreen: Received refresh trigger at $timestamp")
            historyViewModel.refreshFromWebSocketTrigger(uiState.isDemoAccount)
        }
    }

    LaunchedEffect(uiState.botState, uiState.isDemoAccount) {
        while (true) {
            val interval = when {
                uiState.botState == BotState.RUNNING || uiState.isFollowModeActive || uiState.isIndicatorModeActive -> 15000L
                uiState.botState == BotState.PAUSED -> 30000L
                else -> 60000L
            }
            delay(interval)
            println("Periodic refresh after ${interval/1000}s for ${if (uiState.isDemoAccount) "Demo" else "Real"} account")
            historyViewModel.loadTradingHistory(uiState.isDemoAccount)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            HeaderSection(
                isConnected = uiState.isWebSocketConnected,
                connectionStatus = uiState.connectionStatus,
                isDarkMode = dashboardTheme.isDarkMode,
                colors = colors,
                onForceReconnect = dashboardViewModel::forceReconnectWebSocket
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                EnhancedStatisticsRow(
                    todayProfit = todayProfit,
                    isLoading = uiState.isLoading,
                    isCalculating = isCalculatingTodayProfit,
                    isRefreshing = dashboardViewModel.isRefreshing.collectAsStateWithLifecycle().value,
                    isDemoAccount = uiState.isDemoAccount,
                    currentLanguage = currentLanguage,
                    onRefreshTodayProfit = dashboardViewModel::refreshTodayProfit,
                    onForceRecalculate = dashboardViewModel::forceRecalculateTodayProfit,
                    colors = colors,
                    currentCurrency = uiState.currencySettings.selectedCurrency
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssetAndClockCard(
                        modifier = Modifier.weight(0.7f),
                        selectedAsset = uiState.selectedAsset,
                        canModify = uiState.canModifySettings(),
                        onAddAsset = { showAssetDialog = true },
                        isLoading = uiState.isLoading,
                        isDemoAccount = uiState.isDemoAccount,
                        localStats = localTradingStats,
                        isStatsLoading = false,
                        colors = colors,
                        currentLanguage = currentLanguage,
                        isBotRunning = (
                                uiState.botState == BotState.RUNNING ||
                                        uiState.isFollowModeActive ||
                                        uiState.isIndicatorModeActive ||
                                        uiState.isCTCModeActive
                                )
                    )

                    TradingModeCard(
                        modifier = Modifier.weight(0.3f),
                        scheduleInput = uiState.scheduleInput,
                        scheduledOrders = scheduledOrders,
                        canModify = uiState.canModifySettings(),
                        botState = uiState.botState,
                        canStartBot = uiState.canStartBot(),
                        onScheduleInputChange = dashboardViewModel::updateScheduleInput,
                        onAddSchedule = dashboardViewModel::addScheduledOrders,
                        onViewSchedules = { showScheduleDialog = true },
                        onClearAll = dashboardViewModel::clearAllScheduledOrders,
                        onRemoveOrder = dashboardViewModel::removeScheduledOrder,  // ✅ TAMBAHKAN INI
                        onStartBot = dashboardViewModel::startBot,
                        isFollowModeActive = uiState.isFollowModeActive,
                        followOrders = followOrders,
                        martingaleSettings = uiState.martingaleSettings,
                        onStartFollow = dashboardViewModel::startFollowMode,
                        onStopFollow = dashboardViewModel::stopFollowMode,
                        currentMode = uiState.tradingMode,
                        onModeChange = dashboardViewModel::setTradingMode,
                        isIndicatorModeActive = uiState.isIndicatorModeActive,
                        indicatorOrders = indicatorOrders,
                        indicatorSettings = uiState.indicatorSettings,
                        indicatorOrderStatus = uiState.indicatorOrderStatus,
                        onStartIndicator = dashboardViewModel::startIndicatorMode,
                        onStopIndicator = dashboardViewModel::stopIndicatorMode,
                        onShowIndicatorSettings = { showIndicatorSettingsDialog = true },
                        isCTCModeActive = uiState.isCTCModeActive,
                        ctcOrders = ctcOrders,
                        onStartCTC = dashboardViewModel::startCTCMode,
                        onStopCTC = dashboardViewModel::stopCTCMode,
                        colors = colors,
                        currentLanguage = currentLanguage,
                        isTradingModeSelected = uiState.isTradingModeSelected,
                        onStopBot = dashboardViewModel::stopBot,
                    )
                }


                IndicatorInfoCard(
                    indicatorSettings = uiState.indicatorSettings,
                    isIndicatorModeActive = uiState.isIndicatorModeActive,
                    indicatorOrders = indicatorOrders,
                    predictionInfo = indicatorPredictionInfo
                )

                TradingSettingsCard(
                    isDemoAccount = uiState.isDemoAccount,
                    martingaleSettings = uiState.martingaleSettings,
                    currencySettings = uiState.currencySettings,
                    canModify = uiState.canModifySettings(),
                    onAccountTypeChange = dashboardViewModel::setAccountType,
                    onCurrencyChange = dashboardViewModel::setSelectedCurrency,
                    onMartingaleStepsChange = dashboardViewModel::setMartingaleMaxSteps,
                    onBaseAmountChange = dashboardViewModel::setMartingaleBaseAmount,
                    onMartingaleToggle = dashboardViewModel::setMartingaleEnabled,
                    onMultiplierTypeChange = dashboardViewModel::setMartingaleMultiplierType,
                    onMultiplierValueChange = dashboardViewModel::setMartingaleMultiplierValue,
                    colors = colors,
                    currentLanguage = currentLanguage
                )

                StopLossProfitCard(
                    stopLossSettings = uiState.stopLossSettings,
                    stopProfitSettings = uiState.stopProfitSettings,
                    tradingSession = uiState.tradingSession,
                    canModify = uiState.canModifySettings(),
                    onStopLossEnabledChange = dashboardViewModel::setStopLossEnabled,
                    onStopLossMaxAmountChange = dashboardViewModel::setStopLossMaxAmount,
                    onStopProfitEnabledChange = dashboardViewModel::setStopProfitEnabled,
                    onStopProfitTargetAmountChange = dashboardViewModel::setStopProfitTargetAmount,
                    onResetSession = dashboardViewModel::resetTradingSession,
                    colors = colors,
                    currentCurrency = uiState.currencySettings.selectedCurrency,
                    currentLanguage = currentLanguage
                )

                if (uiState.tradingMode == TradingMode.SCHEDULE) {
                    BotControlCard(
                        botState = uiState.botState,
                        botStatus = uiState.botStatus,
                        canPauseBot = uiState.canPauseBot(),
                        canResumeBot = uiState.canResumeBot(),
                        canStopBot = uiState.canStopBot(),
                        onPauseBot = dashboardViewModel::pauseBot,
                        onResumeBot = dashboardViewModel::resumeBot,
                        onStopBot = dashboardViewModel::stopBot,
                        colors = colors,
                        currentLanguage = currentLanguage  // ✅ PASS LANGUAGE
                    )
                }

                uiState.error?.let {
                    ErrorCard(
                        error = it,
                        onDismiss = dashboardViewModel::clearError,
                        colors = colors
                    )
                }

                uiState.lastTradeResult?.let {
                    LastTradeResultCard(
                        result = it,
                        colors = colors,
                        currentCurrency = uiState.currencySettings.selectedCurrency  // ✅ ADD THIS
                    )
                }

                ThemeToggleCard(
                    isDarkMode = dashboardTheme.isDarkMode,
                    onThemeToggle = { isDark ->
                        themeViewModel.setDarkMode(isDark)
                    },
                    colors = colors
                )
            }

            if (showAssetDialog) {
                AssetSelectionDialog(
                    assets = assets,
                    isLoading = uiState.assetsLoading,
                    onAssetSelected = {
                        dashboardViewModel.selectAsset(it)
                        showAssetDialog = false
                    },
                    onDismiss = { showAssetDialog = false },
                    colors = colors,
                    onRefresh = dashboardViewModel::refreshAssets,
                    currentLanguage = currentLanguage  // ✅ PASS LANGUAGE
                )
            }

            if (showScheduleDialog) {
                ScheduleListDialog(
                    scheduledOrders = scheduledOrders,
                    onRemoveOrder = dashboardViewModel::removeScheduledOrder,
                    onClearAll = dashboardViewModel::clearAllScheduledOrders,
                    onDismiss = { showScheduleDialog = false },
                    onShowInputSignal = {  // ✅ UPDATED
                        showScheduleDialog = false
                        showMultilineScheduleDialog = true
                    },
                    colors = colors
                )
            }

            if (showMultilineScheduleDialog) {
                MultilineScheduleDialog(
                    currentInput = "",
                    scheduledOrders = scheduledOrders,
                    onInputChange = { input ->
                        dashboardViewModel.updateScheduleInput(input)
                    },
                    onAdd = {
                        dashboardViewModel.addScheduledOrders()
                        showMultilineScheduleDialog = false
                    },
                    onDismiss = { showMultilineScheduleDialog = false },
                    colors = colors
                )
            }

            if (showIndicatorSettingsDialog) {
                IndicatorSettingsDialog(
                    indicatorSettings = uiState.indicatorSettings,
                    consecutiveLossSettings = uiState.consecutiveLossSettings,
                    canModify = uiState.canModifySettings(),
                    onIndicatorTypeChange = dashboardViewModel::setIndicatorType,
                    onIndicatorPeriodChange = dashboardViewModel::setIndicatorPeriod,
                    onRSILevelsChange = dashboardViewModel::setIndicatorRSILevels,
                    onSensitivityChange = dashboardViewModel::setIndicatorSensitivity,
                    onConsecutiveLossChange = dashboardViewModel::setConsecutiveLossLimit,
                    onDismiss = { showIndicatorSettingsDialog = false },
                    colors = colors
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    isConnected: Boolean,
    connectionStatus: String,
    isDarkMode: Boolean,
    onForceReconnect: (() -> Unit)? = null,
    colors: DashboardColors,
    modifier: Modifier = Modifier
) {
    var showToast by remember { mutableStateOf(false) }
    var previousConnectionState by remember { mutableStateOf(isConnected) }

    LaunchedEffect(isConnected) {
        if (isConnected != previousConnectionState) {
            showToast = true
            previousConnectionState = isConnected
            delay(3000)
            showToast = false
        }
    }

    // Pilih header image berdasarkan mode
    val headerImageResource = if (isDarkMode) {
        R.drawable.header5
    } else {
        R.drawable.header8
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background Image - TIDAK TERPOTONG SAMA SEKALI
        Image(
            painter = painterResource(id = headerImageResource),
            contentDescription = "Header Background",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // Tinggi menyesuaikan konten gambar
            contentScale = ContentScale.FillWidth, // Lebar penuh, tinggi proporsional
            alignment = Alignment.TopCenter
        )

        // Content Layer (di atas background image)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
        }

        // Toast notification
        AnimatedVisibility(
            visible = showToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -100 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -100 }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            EnhancedConnectionToast(
                isConnected = isConnected,
                connectionStatus = connectionStatus
            )
        }
    }
}

@Composable
private fun EnhancedConnectionToast(
    isConnected: Boolean,
    connectionStatus: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .defaultMinSize(minHeight = 42.dp)
            .background(
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.wrapContentWidth()
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF67D88B) else Color(0xFFDC4D4D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = if (isConnected) "Connected to server" else "Disconnected from server",
                    color = if (isConnected) Color(0xFF67D88B) else Color(0xFFDC4D4D),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1
                )

                if (connectionStatus.isNotEmpty() && connectionStatus != "Connected" && connectionStatus != "Disconnected") {
                    Text(
                        text = connectionStatus,
                        color = if (isConnected) Color(0xFF67D88B).copy(alpha = 0.7f) else Color(0xFFDC4D4D).copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun FadingGradientLine(
    modifier: Modifier = Modifier,
    height: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFF94144),
                        Color(0xFFF3722C),
                        Color(0xFF43AA8B),
                        Color.Transparent
                    )
                )
            )
    )
}


@Composable
fun BotControlCard(
    botState: BotState,
    botStatus: String,
    canPauseBot: Boolean,
    canResumeBot: Boolean,
    canStopBot: Boolean,
    onPauseBot: () -> Unit,
    onResumeBot: () -> Unit,
    onStopBot: () -> Unit,
    colors: DashboardColors,
    currentLanguage: String = "id"  // ✅ ADD PARAMETER
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = when (botState) {
                    BotState.RUNNING -> colors.successColor.copy(alpha = 0.4f)
                    BotState.PAUSED -> colors.warningColor.copy(alpha = 0.4f)
                    BotState.STOPPED -> colors.errorColor.copy(alpha = 0.4f)
                },
                ambientColor = Color.Black.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.cardBackground,
                            colors.cardBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsManager.getBotControl(currentLanguage), // ✅ MULTILANGUAGE
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textPrimary
                    )

                    Surface(
                        modifier = Modifier
                            .wrapContentSize()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = when (botState) {
                                    BotState.RUNNING -> colors.successColor.copy(alpha = 0.6f)
                                    BotState.PAUSED -> colors.warningColor.copy(alpha = 0.6f)
                                    BotState.STOPPED -> colors.errorColor.copy(alpha = 0.6f)
                                },
                                ambientColor = Color.Black.copy(alpha = 0.2f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = when (botState) {
                            BotState.RUNNING -> colors.botStatusRunningBg
                            BotState.PAUSED -> colors.botStatusPausedBg
                            BotState.STOPPED -> colors.botStatusStoppedBg
                        },
                        border = BorderStroke(
                            0.5.dp,
                            when (botState) {
                                BotState.RUNNING -> colors.successColor.copy(alpha = 0.4f)
                                BotState.PAUSED -> colors.warningColor.copy(alpha = 0.4f)
                                BotState.STOPPED -> colors.errorColor.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (botState) {
                                            BotState.RUNNING -> colors.successColor
                                            BotState.PAUSED -> colors.warningColor
                                            BotState.STOPPED -> colors.errorColor
                                        }
                                    )
                            )
                            Text(
                                text = when (botState) {
                                    BotState.RUNNING -> StringsManager.getBotRunning(currentLanguage)
                                    BotState.PAUSED -> StringsManager.getBotPaused(currentLanguage)
                                    BotState.STOPPED -> StringsManager.getBotStopped(currentLanguage)
                                }, // ✅ MULTILANGUAGE
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // TOMBOL PAUSE/RESUME DAN STOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tombol Pause/Resume
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        // Layer bayangan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        canResumeBot -> colors.botButtonResumeShadow.copy(alpha = 0.4f)
                                        canPauseBot -> colors.botButtonPauseShadow.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .offset(y = 2.dp)
                        )

                        // Tombol utama
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = canPauseBot || canResumeBot) {
                                    if (canResumeBot) onResumeBot() else onPauseBot()
                                },
                            color = when {
                                canResumeBot -> colors.botButtonResumeBg
                                canPauseBot -> colors.botButtonPauseBg
                                else -> colors.botButtonDisabledBg
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = when {
                                    canResumeBot -> colors.botButtonResumeBg
                                    canPauseBot -> colors.botButtonPauseBg
                                    else -> colors.borderColor
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = when {
                                            canResumeBot -> Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.botButtonResumeText.copy(alpha = 0.55f),
                                                    colors.botButtonResumeText.copy(alpha = 0.55f)
                                                )
                                            )
                                            canPauseBot -> Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.botButtonPauseText.copy(alpha = 0.75f),
                                                    colors.botButtonPauseText.copy(alpha = 0.75f)
                                                )
                                            )
                                            else -> Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.textMuted.copy(alpha = 0.45f),
                                                    colors.textMuted.copy(alpha = 0.45f)
                                                )
                                            )
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (canResumeBot)
                                            StringsManager.getBotResume(currentLanguage)
                                        else
                                            StringsManager.getBotPause(currentLanguage), // ✅ MULTILANGUAGE
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = if (canStopBot) colors.TextPrimary1 else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Tombol Stop
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        // Layer bayangan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (canStopBot) colors.botButtonStopShadow else Color.Transparent
                                )
                                .offset(y = 2.dp)
                        )

                        // Tombol utama
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = canStopBot) { onStopBot() },
                            color = if (canStopBot) colors.botButtonStopText else colors.botButtonDisabledBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = if (canStopBot) colors.botButtonStopText else colors.borderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = if (canStopBot) {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.botButtonStopText.copy(alpha = 0.6f),
                                                    colors.botButtonStopText.copy(alpha = 0.6f)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.textMuted.copy(alpha = 0.45f),
                                                    colors.textMuted.copy(alpha = 0.45f)
                                                )
                                            )
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = StringsManager.getBotStop(currentLanguage), // ✅ MULTILANGUAGE
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = if (canStopBot) colors.TextPrimary1 else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (botState == BotState.RUNNING && botStatus.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        color = colors.botInfoCardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            0.5.dp,
                            colors.borderColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = colors.accentPrimary
                            )
                            Text(
                                text = botStatus,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealtimeTodayProfitCard(
    todayProfit: Long,
    isLoading: Boolean,
    isCalculating: Boolean,
    isRefreshing: Boolean = false,
    currentLanguage: String = "id", //
    onRefresh: () -> Unit = {},
    onForceRecalculate: () -> Unit = {},
    colors: DashboardColors,
    currentCurrency: CurrencyType = CurrencyType.IDR,
    modifier: Modifier = Modifier
) {
    var lastProfit by remember { mutableStateOf(todayProfit) }
    var showChangeAnimation by remember { mutableStateOf(false) }
    var profitChange by remember { mutableStateOf(0L) }

    LaunchedEffect(todayProfit) {
        if (lastProfit != todayProfit && lastProfit != 0L) {
            profitChange = todayProfit - lastProfit
            showChangeAnimation = true
            delay(3000)
            showChangeAnimation = false
        }
        lastProfit = todayProfit
    }

    val isProfit = todayProfit >= 0
    val profitColor = if (isProfit) colors.accentProfit else colors.accentSecondary
    val changeColor = if (profitChange >= 0) colors.successColor else colors.errorColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .clickable { onRefresh() }
            .combinedClickable(
                onClick = { onRefresh() },
                onLongClick = { onForceRecalculate() }
            )
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = colors.chartLine.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Language Support
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = StringsManager.getTodayProfit(currentLanguage), // ✅ MULTI-LANGUAGE
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Live indicator
                Box(
                    modifier = Modifier.size(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = colors.wifiGreen.copy(alpha = alpha),
                                shape = CircleShape
                            )
                    )
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profit Display
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = profitColor,
                    strokeWidth = 2.dp
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Main profit value
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentCurrency.symbol,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = profitColor,
                            modifier = Modifier.padding(end = 7.dp)
                        )

                        Box {
                            Text(
                                text = formatTodayProfitDisplay(todayProfit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = profitColor.copy(alpha = 0.4f),
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = profitColor.copy(alpha = 0.9f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 16f
                                    )
                                )
                            )

                            Text(
                                text = formatTodayProfitDisplay(todayProfit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = profitColor
                            )
                        }

                        Text(
                            text = currentCurrency.code,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = profitColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Change animation
                    AnimatedVisibility(
                        visible = showChangeAnimation && profitChange != 0L,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (profitChange >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = "${if (profitChange >= 0) "+" else ""}${formatCompactProfit(profitChange)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = changeColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                }
            }
        }
    }
}

// Update EnhancedStatisticsRow untuk pass language
@Composable
fun EnhancedStatisticsRow(
    todayProfit: Long,
    isLoading: Boolean,
    isCalculating: Boolean,
    isRefreshing: Boolean,
    isDemoAccount: Boolean,
    currentLanguage: String = "id", // ✅ ADD PARAMETER
    onRefreshTodayProfit: () -> Unit,
    onForceRecalculate: () -> Unit,
    todayStats: TodayStats = TodayStats(),
    isStatsLoading: Boolean = false,
    colors: DashboardColors,
    currentCurrency: CurrencyType = CurrencyType.IDR
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RealtimeTodayProfitCard(
            todayProfit = todayProfit,
            isLoading = isLoading,
            isCalculating = isCalculating,
            isRefreshing = isRefreshing,
            currentLanguage = currentLanguage, // ✅ PASS LANGUAGE
            onRefresh = onRefreshTodayProfit,
            onForceRecalculate = onForceRecalculate,
            colors = colors,
            currentCurrency = currentCurrency
        )
    }
}

@Composable
fun ToggleableStatsClockCard(
    modifier: Modifier = Modifier,
    selectedAsset: Asset?,
    canModify: Boolean,
    onAddAsset: () -> Unit,
    isLoading: Boolean,
    isDemoAccount: Boolean,
    localStats: LocalStatsTracker.LocalTradingStats = LocalStatsTracker.LocalTradingStats(),
    isStatsLoading: Boolean = false,
    colors: DashboardColors,
    currentLanguage: String = "id",
    isBotRunning: Boolean
) {
    var showClock by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showClock) {
                DigitalClockRow(
                    colors = colors,
                    currentLanguage = currentLanguage,
                    isBotRunning = isBotRunning
                )
            } else {
                LocalStatsContent(
                    localStats = localStats,
                    isLoading = isStatsLoading,
                    isDemoAccount = isDemoAccount,
                    colors = colors,
                    currentLanguage = currentLanguage,
                    isBotRunning = isBotRunning  // ✅ PASS PARAMETER
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsManager.getAssets(currentLanguage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { showClock = !showClock },
                        shape = CircleShape,
                        color = if (showClock) colors.successColor.copy(alpha = 0.1f) else colors.errorColor.copy(alpha = 0.1f),
                        border = BorderStroke(
                            1.dp,
                            if (showClock) colors.successColor.copy(alpha = 0.5f) else colors.errorColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showClock) Icons.Default.BarChart else Icons.Default.AccessTime,
                                contentDescription = if (showClock)
                                    StringsManager.getShowStats(currentLanguage)
                                else
                                    StringsManager.getShowClock(currentLanguage),
                                tint = if (showClock) colors.successColor else colors.errorColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onAddAsset,
                        enabled = canModify,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = StringsManager.getAddAsset(currentLanguage),
                            tint = if (canModify) colors.accentPrimary5 else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            TradingLineChart(
                selectedAsset = selectedAsset,
                colors = colors
            )
        }
    }
}

@Composable
fun LocalStatCard(
    modifier: Modifier = Modifier,
    value: Int,
    label: String,
    color: Color,
    isLoading: Boolean,
    colors: DashboardColors,
    isDemoAccount: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        border = BorderStroke(
            0.4.dp,
            if (isDemoAccount) colors.warningColor else colors.successColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = color,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,  // ✅ Label sudah langsung "Win", "Draw", "Lose"
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.textSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LocalStatsContent(
    localStats: LocalStatsTracker.LocalTradingStats,
    isLoading: Boolean,
    isDemoAccount: Boolean,
    colors: DashboardColors,
    currentLanguage: String = "id",
    isBotRunning: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, colors.borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsManager.getStatistics(currentLanguage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .shadow(
                                elevation = if (isBotRunning) 6.dp else 0.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .background(
                                color = if (isBotRunning)
                                    Color(0xFF00FF00).copy(alpha = pulseAlpha)
                                else
                                    Color(0xFFFF0000),
                                shape = CircleShape
                            )
                    )

                    Text(
                        text = if (isDemoAccount)
                            StringsManager.getDemoAccount(currentLanguage)
                        else
                            StringsManager.getRealAccount(currentLanguage),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDemoAccount) colors.warningColor else colors.successColor,
                        modifier = Modifier
                            .background(
                                color = if (isDemoAccount) colors.warningColor.copy(alpha = 0.2f) else colors.successColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ✅ HARDCODE label dalam bahasa Inggris
                LocalStatCard(
                    modifier = Modifier.weight(1f),
                    value = localStats.winCount,
                    label = "Win",  // ✅ Tetap "Win"
                    color = colors.wifiGreen,
                    isLoading = isLoading,
                    colors = colors,
                    isDemoAccount = isDemoAccount
                )

                LocalStatCard(
                    modifier = Modifier.weight(1f),
                    value = localStats.drawCount,
                    label = "Draw",  // ✅ Tetap "Draw"
                    color = colors.accentWarning,
                    isLoading = isLoading,
                    colors = colors,
                    isDemoAccount = isDemoAccount
                )

                LocalStatCard(
                    modifier = Modifier.weight(1f),
                    value = localStats.loseCount,
                    label = "Lose",  // ✅ Tetap "Lose"
                    color = colors.accentSecondary,
                    isLoading = isLoading,
                    colors = colors,
                    isDemoAccount = isDemoAccount
                )
            }
        }
    }
}

@Composable
fun AssetAndClockCard(
    modifier: Modifier = Modifier,
    selectedAsset: Asset?,
    canModify: Boolean,
    onAddAsset: () -> Unit,
    isLoading: Boolean,
    isDemoAccount: Boolean,
    localStats: LocalStatsTracker.LocalTradingStats = LocalStatsTracker.LocalTradingStats(),
    isStatsLoading: Boolean = false,
    colors: DashboardColors,
    currentLanguage: String = "id",
    isBotRunning: Boolean
) {
    ToggleableStatsClockCard(
        modifier = modifier,
        selectedAsset = selectedAsset,
        canModify = canModify,
        onAddAsset = onAddAsset,
        isLoading = isLoading,
        isDemoAccount = isDemoAccount,
        localStats = localStats,
        isStatsLoading = isStatsLoading,
        colors = colors,
        currentLanguage = currentLanguage,
        isBotRunning = isBotRunning
    )
}

@Composable
fun DigitalClockRow(
    colors: DashboardColors,
    currentLanguage: String = "id",
    isBotRunning: Boolean = false
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis() + ServerTimeService.cachedServerTimeOffset
            delay(1000)
        }
    }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    val locale = remember(currentLanguage) {
        when (currentLanguage) {
            "id" -> Locale("id", "ID")
            "en" -> Locale("en", "US")
            "es" -> Locale("es", "ES")
            "vi" -> Locale("vi", "VN")
            "tr" -> Locale("tr", "TR")
            "hi" -> Locale("hi", "IN")
            "ms" -> Locale("ms", "MY")
            else -> Locale.getDefault()
        }
    }

    val dateFormat = remember(locale) {
        SimpleDateFormat("EEE, dd MMM yyyy", locale).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    val formattedTime = timeFormat.format(Date(currentTime))
    val formattedDate = dateFormat.format(Date(currentTime))

    val userTimeZone = TimeZone.getDefault()
    val offsetMs = userTimeZone.getOffset(currentTime)
    val offsetHours = offsetMs / (1000 * 60 * 60)
    val offsetMinutes = (kotlin.math.abs(offsetMs) / (1000 * 60)) % 60
    val utcOffsetString = String.format("UTC %+d:%02d", offsetHours, offsetMinutes)

    val digitalClockFontFamily = FontFamily(Font(R.font.digital2))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = colors.bgclock),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, colors.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 4.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ⏰ Jam digital
            Surface(
                modifier = Modifier.wrapContentSize(),
                color = colors.cardBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = digitalClockFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = colors.textPrimary.copy(alpha = 0.3f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                )
            }

            // 📅 Baris tanggal + UTC + notif bulat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 📆 Tanggal di kiri
                Text(
                    text = formattedDate,
                    fontSize = 8.sp,
                    color = colors.textSecondary
                )

                // 🌍 UTC + bulatan status bot di kanan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = utcOffsetString,
                        fontSize = 8.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 🔹 Bulatan status bot kecil dengan efek glow lembut
                    val infiniteTransition = rememberInfiniteTransition(label = "")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .shadow(
                                elevation = if (isBotRunning) 6.dp else 0.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .background(
                                color = if (isBotRunning)
                                    Color(0xFF00FF00).copy(alpha = pulseAlpha)
                                else
                                    Color(0xFFFF0000),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}



private fun getLocalizedDateFormat(currentTime: Long, currentLanguage: String): Pair<String, String> {
    val date = Date(currentTime)

    return when (currentLanguage) {
        "id" -> {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "TANGGAL LOKAL")
        }
        "en" -> {
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale("en", "US"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "LOCAL DATE")
        }
        "es" -> {
            val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "FECHA LOCAL")
        }
        "vi" -> {
            val dateFormat = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM 'năm' yyyy", Locale("vi", "VN"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "NGÀY ĐỊA PHƯƠNG")
        }
        "tr" -> {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("tr", "TR"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "YEREL TARİH")
        }
        "hi" -> {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "स्थानीय तारीख")
        }
        "ms" -> {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("ms", "MY"))
            val formatted = dateFormat.format(date)
            Pair(formatted, "TARIKH TEMPATAN")
        }
        else -> {
            // Fallback to English
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            val formatted = dateFormat.format(date)
            Pair(formatted, "LOCAL DATE")
        }
    }
}

@Composable
fun SynchronizedWinLoseStatsRow(
    todayStats: TodayStats,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            value = todayStats.winCount,
            label = "Win",
            color = WifiGreen,
            isLoading = isLoading
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Remove,
            value = todayStats.drawCount,
            label = "Draw",
            color = AccentWarning,
            isLoading = isLoading
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingDown,
            value = todayStats.loseCount,
            label = "Lose",
            color = AccentSecondary,
            isLoading = isLoading
        )
    }
}

@Composable
fun ThemeToggleCard(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    colors: DashboardColors,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.darkBackground
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = colors.accentPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isDarkMode) "Dark Theme Active" else "Light Theme Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = colors.textSecondary
                    )
                }
            }

            Switch(
                checked = isDarkMode,
                onCheckedChange = onThemeToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.textSecondary,
                    checkedTrackColor = colors.surface,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surface,
                    checkedBorderColor = colors.accentPrimary,
                    uncheckedBorderColor = colors.borderColor
                )
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: Int,
    label: String,
    color: Color,
    isLoading: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = color,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AssetAndStatsCard(
    modifier: Modifier = Modifier,
    selectedAsset: Asset?,
    canModify: Boolean,
    onAddAsset: () -> Unit,
    todayStats: TodayStats,
    isLoading: Boolean,
    isDemoAccount: Boolean,
    colors: DashboardColors
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        ),
        border = BorderStroke(0.5.dp, Color(0xFF4A4A4A)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WinLoseStatsRow(
                todayStats = todayStats,
                isLoading = isLoading,
                isDemoAccount = isDemoAccount
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asset",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = onAddAsset,
                    enabled = canModify,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Asset",
                        tint = if (canModify) AccentPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (selectedAsset != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
                ) {
                }
            }

            TradingLineChart(
                selectedAsset = selectedAsset,
                colors = colors
            )
        }
    }
}

@Composable
fun IndicatorInfoCard(
    indicatorSettings: IndicatorSettings,
    isIndicatorModeActive: Boolean,
    indicatorOrders: List<IndicatorOrder>,
    predictionInfo: Map<String, Any> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Only show when indicator mode is active or there are recent orders
    if (!isIndicatorModeActive && indicatorOrders.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indicator Analysis",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = Color.White
                )

                OutlinedCard(
                    modifier = Modifier.wrapContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isIndicatorModeActive)
                            Color(0xFF1A0D2E) else Color(0xFF2A2A2A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isIndicatorModeActive)
                            Color(0x809C27B0) else Color(0xFF666666)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = if (isIndicatorModeActive) Color(0xFF9C27B0) else Color(0xFF666666),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isIndicatorModeActive) "Active" else "Inactive",
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

            // Indicator Settings Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Configuration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9C27B0)
                    )

                    // Configuration Table
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IndicatorInfoRow(
                            label = "Indicator Type",
                            value = indicatorSettings.getDisplayName(),
                            icon = Icons.Default.TrendingUp
                        )

                        Divider(color = Color(0xFF374151), thickness = 0.5.dp)

                        IndicatorInfoRow(
                            label = "Period",
                            value = "${indicatorSettings.period}",
                            icon = Icons.Default.Timeline
                        )

                        Divider(color = Color(0xFF374151), thickness = 0.5.dp)

                        IndicatorInfoRow(
                            label = "Sensitivity",
                            value = String.format("%.1f", indicatorSettings.sensitivity),
                            icon = Icons.Default.Tune
                        )

                        if (indicatorSettings.type == IndicatorType.RSI) {
                            Divider(color = Color(0xFF374151), thickness = 0.5.dp)

                            IndicatorInfoRow(
                                label = "RSI Levels",
                                value = "${indicatorSettings.rsiOverbought.toInt()}/${indicatorSettings.rsiOversold.toInt()}",
                                icon = Icons.Default.Speed
                            )
                        }
                    }
                }
            }

            // Price Predictions Section
            if (isIndicatorModeActive && predictionInfo.isNotEmpty()) {
                val predictions = predictionInfo["predictions"] as? List<Map<String, Any>> ?: emptyList()

                if (predictions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Price Predictions",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF60A5FA)
                                )
                            }

                            // Show active predictions
                            val activePredictions = predictions.filter { !(it["is_triggered"] as? Boolean ?: true) }

                            if (activePredictions.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activePredictions.take(5)) { prediction ->
                                        PredictionTargetCard(prediction = prediction)
                                    }
                                }

                                // Summary info
                                Divider(color = Color(0xFF374151), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active Targets",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                    Text(
                                        text = "${activePredictions.size} / ${predictions.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF60A5FA)
                                    )
                                }

                                val highConfidenceCount = activePredictions.count {
                                    val confidence = it["confidence"] as? String ?: "0%"
                                    val confValue = confidence.replace("%", "").toFloatOrNull() ?: 0f
                                    confValue >= 70f
                                }

                                if (highConfidenceCount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "High Confidence",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                        Text(
                                            text = "$highConfidenceCount targets (â‰¥70%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            } else {
                                // No active predictions
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "All predictions triggered",
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Orders Statistics (if there are orders)
            if (indicatorOrders.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Signal Statistics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF60A5FA)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Total Orders
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${indicatorOrders.size}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA)
                                )
                                Text(
                                    text = "Total Signals",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }

                            // Executed Orders
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val executed = indicatorOrders.count { it.isExecuted }
                                Text(
                                    text = "$executed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Executed",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }

                            // Pending Orders
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val pending = indicatorOrders.count { !it.isExecuted && !it.isSkipped }
                                Text(
                                    text = "$pending",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                                Text(
                                    text = "Pending",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        // Signal Distribution
                        if (indicatorOrders.isNotEmpty()) {
                            Divider(color = Color(0xFF374151), thickness = 0.5.dp)

                            val callSignals = indicatorOrders.count { it.trend.lowercase() == "buy" }
                            val putSignals = indicatorOrders.count { it.trend.lowercase() == "sell" }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$callSignals",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Text(
                                        text = "Buy Signals",
                                        fontSize = 9.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$putSignals",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    Text(
                                        text = "Sell Signals",
                                        fontSize = 9.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Status
            if (isIndicatorModeActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A0D2E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(16.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Analyzing Market Data",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF9C27B0)
                            )
                            Text(
                                text = "Monitoring ${indicatorSettings.type.name} signals for trading opportunities",
                                fontSize = 10.sp,
                                color = Color(0xFFD1D5DB),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionTargetCard(prediction: Map<String, Any>) {
    val predictionType = prediction["type"] as? String ?: "Unknown"
    val targetPrice = prediction["target_price"] as? String ?: "0.00000"
    val recommendedTrend = prediction["recommended_trend"] as? String ?: "call"
    val confidence = prediction["confidence"] as? String ?: "0%"
    val distanceFromCurrent = prediction["distance_from_current"] as? String ?: "0%"

    val confidenceValue = confidence.replace("%", "").toFloatOrNull() ?: 0f
    val confidenceColor = when {
        confidenceValue >= 80f -> Color(0xFF10B981)
        confidenceValue >= 70f -> Color(0xFFFBBF24)
        confidenceValue >= 60f -> Color(0xFF60A5FA)
        else -> Color(0xFF9CA3AF)
    }

    val trendColor = if (recommendedTrend == "buy") Color(0xFF10B981) else Color(0xFFEF4444)
    val trendIcon = if (recommendedTrend == "buy") Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val trendText = if (recommendedTrend == "buy") "BUY" else "SELL"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2D2E30),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, confidenceColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with prediction type and confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = predictionType.replace("_", " "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = confidenceColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = confidence,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = confidenceColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Target price and trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Target Price",
                        fontSize = 9.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = targetPrice,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = trendColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = trendText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }
            }

            // Distance from current price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distance from current",
                    fontSize = 9.sp,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    text = distanceFromCurrent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF60A5FA)
                )
            }
        }
    }
}

@Composable
private fun IndicatorInfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Normal
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun MaxStepSelectionDialog(
    currentMaxSteps: Int,
    martingaleSettings: MartingaleState,
    onMaxStepsSelected: (Int) -> Unit,
    onMultiplierTypeChange: (MultiplierType) -> Unit,
    onMultiplierValueChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    colors: DashboardColors
) {
    var customStepsInput by remember(currentMaxSteps) { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var showMultiplierTypeDropdown by remember { mutableStateOf(false) }

    var multiplierInput by remember(key1 = martingaleSettings.multiplierType, key2 = martingaleSettings.multiplierValue) {
        mutableStateOf(
            when (martingaleSettings.multiplierType) {
                MultiplierType.FIXED -> martingaleSettings.multiplierValue.toString()
                MultiplierType.PERCENTAGE -> martingaleSettings.multiplierValue.toInt().toString()
            }
        )
    }

    var localMaxSteps by remember(currentMaxSteps) { mutableStateOf(currentMaxSteps) }
    var localMultiplierType by remember(martingaleSettings.multiplierType) { mutableStateOf(martingaleSettings.multiplierType) }
    var localMultiplierValue by remember(martingaleSettings.multiplierValue) { mutableStateOf(martingaleSettings.multiplierValue) }

    fun sanitizeDecimalInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val parts = filtered.split('.')
        return if (parts.size > 2) {
            "${parts[0]}.${parts.drop(1).joinToString("")}"
        } else {
            filtered
        }
    }

    fun isValidMultiplierValue(value: Double, type: MultiplierType): Boolean {
        return when (type) {
            MultiplierType.FIXED -> value >= 1.0 && value <= 50.0
            MultiplierType.PERCENTAGE -> value >= 1.0 && value <= 5000.0
        }
    }

    fun getValidationError(input: String, type: MultiplierType): String? {
        if (input.isEmpty()) return null

        val sanitized = input.replace(",", ".")
        val value = sanitized.toDoubleOrNull()
        return when {
            value == null -> "Format angka tidak valid"
            type == MultiplierType.FIXED && value < 1.0 -> "Minimum 1.0x"
            type == MultiplierType.FIXED && value > 50.0 -> "Maksimum 50.0x"
            type == MultiplierType.PERCENTAGE && value < 1.0 -> "Minimum 1%"
            type == MultiplierType.PERCENTAGE && value > 5000.0 -> "Maksimum 5000%"
            else -> null
        }
    }

    val handleMaxStepsSelected = remember<(Int) -> Unit> {
        { steps ->
            localMaxSteps = steps
            onMaxStepsSelected(steps)
        }
    }

    val handleMultiplierTypeChange = remember<(MultiplierType) -> Unit> {
        { type ->
            localMultiplierType = type
            onMultiplierTypeChange(type)
            multiplierInput = when (type) {
                MultiplierType.FIXED -> if (isValidMultiplierValue(localMultiplierValue, type)) {
                    localMultiplierValue.toString()
                } else {
                    "2.0"
                }
                MultiplierType.PERCENTAGE -> if (isValidMultiplierValue(localMultiplierValue, type)) {
                    localMultiplierValue.toInt().toString()
                } else {
                    "100"
                }
            }
        }
    }

    val handleMultiplierValueChange = remember<(Double) -> Unit> {
        { value ->
            localMultiplierValue = value
            onMultiplierValueChange(value)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = colors.dialogBackground
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.dialogBorder.copy(alpha = 0.5f),
                        colors.dialogBorder.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.dialogHeaderGradientStart,
                                    colors.dialogHeaderGradientMid,
                                    colors.dialogHeaderGradientEnd
                                )
                            )
                        ),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Martingale Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.01).sp
                            )
                            Text(
                                text = "Configure strategy parameters",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                letterSpacing = 0.sp
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = colors.dialogCloseButtonBg,
                            onClick = onDismiss,
                            border = BorderStroke(
                                width = 1.dp,
                                color = colors.dialogCloseButtonBorder
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dialogBackground)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Maximum Steps Section
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Maximum Steps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                letterSpacing = 0.sp
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(listOf(1, 2, 3, 4, 5)) { steps ->
                                    val isSelected = steps == localMaxSteps

                                    OutlinedButton(
                                        onClick = {
                                            handleMaxStepsSelected(steps)
                                        },
                                        modifier = Modifier
                                            .height(42.dp)
                                            .wrapContentWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected)
                                                colors.dialogButtonSelected else colors.dialogButtonUnselected,
                                            contentColor = if (isSelected)
                                                colors.dialogButtonSelectedText else colors.dialogButtonUnselectedText
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected)
                                                colors.dialogButtonSelectedBorder else colors.dialogButtonBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = colors.dialogButtonSelectedText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = "$steps steps",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Custom Steps Section
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Custom Steps (1-10)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Normal
                                )

                                Button(
                                    onClick = {
                                        showCustomInput = !showCustomInput
                                        customStepsInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showCustomInput)
                                            colors.dialogCancelButtonBg else colors.dialogCustomButtonBg,
                                        contentColor = if (showCustomInput)
                                            colors.dialogCancelButtonText else colors.dialogCustomButtonText
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (showCustomInput) "Cancel" else "Custom",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showCustomInput,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customStepsInput,
                                        onValueChange = { input ->
                                            val sanitized = input.filter { it.isDigit() }
                                            if (sanitized.length <= 2) {
                                                customStepsInput = sanitized
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = "1-10",
                                                fontSize = 11.sp,
                                                color = colors.dialogInputPlaceholder
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 12.sp,
                                            color = colors.dialogInputText,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.dialogInputBorderFocused,
                                            unfocusedBorderColor = colors.dialogInputBorder,
                                            errorBorderColor = colors.errorColor,
                                            focusedContainerColor = colors.dialogInputBgFocused,
                                            unfocusedContainerColor = colors.dialogInputBg,
                                            cursorColor = colors.dialogInputBorderFocused,
                                            focusedTextColor = colors.dialogInputText,
                                            unfocusedTextColor = colors.dialogInputText
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        isError = customStepsInput.isNotEmpty() &&
                                                (customStepsInput.toIntOrNull()?.let { it !in 1..10 } ?: true)
                                    )

                                    Button(
                                        onClick = {
                                            val steps = customStepsInput.toIntOrNull()
                                            if (steps != null && steps in 1..10) {
                                                handleMaxStepsSelected(steps)
                                                showCustomInput = false
                                                customStepsInput = ""
                                            }
                                        },
                                        enabled = customStepsInput.toIntOrNull()?.let { it in 1..10 } == true,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.successColor,
                                            contentColor = Color.White,
                                            disabledContainerColor = colors.controls
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Set",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Divider
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            colors.dialogDivider,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Multiplier Configuration
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Multiplier Configuration",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Multiplier Type",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Normal
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            showMultiplierTypeDropdown = !showMultiplierTypeDropdown
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.dialogSurface,
                                            contentColor = colors.textPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(24.dp),
                                                    shape = CircleShape,
                                                    color = colors.dialogPreviewAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = null,
                                                            tint = colors.dialogPreviewAccent,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = when (localMultiplierType) {
                                                        MultiplierType.FIXED -> "Fixed Multiplier"
                                                        MultiplierType.PERCENTAGE -> "Percentage Based"
                                                    },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMultiplierTypeDropdown,
                                        onDismissRequest = { showMultiplierTypeDropdown = false },
                                        modifier = Modifier
                                            .background(
                                                colors.dialogDropdownBg,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                colors.dialogDropdownBorder,
                                                RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.size(20.dp),
                                                        shape = CircleShape,
                                                        color = colors.dialogMultiplierFixedBg
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "×",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.dialogMultiplierFixedIcon
                                                            )
                                                        }
                                                    }
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                                    ) {
                                                        Text(
                                                            "Fixed Multiplier",
                                                            color = colors.textPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            "Direct multiplication (2x, 3x)",
                                                            color = colors.textSecondary,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    if (localMultiplierType == MultiplierType.FIXED) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = colors.successColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                handleMultiplierTypeChange(MultiplierType.FIXED)
                                                if (!isValidMultiplierValue(localMultiplierValue, MultiplierType.FIXED)) {
                                                    handleMultiplierValueChange(2.0)
                                                }
                                                showMultiplierTypeDropdown = false
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.size(20.dp),
                                                        shape = CircleShape,
                                                        color = colors.dialogMultiplierPercentBg
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "%",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.dialogMultiplierPercentIcon
                                                            )
                                                        }
                                                    }
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                                    ) {
                                                        Text(
                                                            "Percentage Based",
                                                            color = colors.textPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            "Add percentage (100%, 150%)",
                                                            color = colors.textSecondary,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    if (localMultiplierType == MultiplierType.PERCENTAGE) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = colors.successColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                handleMultiplierTypeChange(MultiplierType.PERCENTAGE)
                                                if (!isValidMultiplierValue(localMultiplierValue, MultiplierType.PERCENTAGE)) {
                                                    handleMultiplierValueChange(100.0)
                                                }
                                                showMultiplierTypeDropdown = false
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Multiplier Value Input
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = multiplierInput,
                                onValueChange = { input ->
                                    val sanitized = sanitizeDecimalInput(input)

                                    val finalInput = when (localMultiplierType) {
                                        MultiplierType.FIXED -> {
                                            val parts = sanitized.split('.')
                                            if (parts.size == 2 && parts[1].length > 1) {
                                                "${parts[0]}.${parts[1].take(1)}"
                                            } else sanitized
                                        }
                                        MultiplierType.PERCENTAGE -> {
                                            sanitized.split('.')[0]
                                        }
                                    }

                                    multiplierInput = finalInput

                                    val value = finalInput.toDoubleOrNull()
                                    if (value != null && isValidMultiplierValue(value, localMultiplierType)) {
                                        handleMultiplierValueChange(value)
                                    }
                                },
                                label = {
                                    Text(
                                        text = when (localMultiplierType) {
                                            MultiplierType.FIXED -> "Multiplier (1.0 - 50.0)"
                                            MultiplierType.PERCENTAGE -> "Percentage (1 - 5000)"
                                        },
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = CircleShape,
                                        color = when (localMultiplierType) {
                                            MultiplierType.FIXED -> colors.dialogMultiplierFixedBg
                                            MultiplierType.PERCENTAGE -> colors.dialogMultiplierPercentBg
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = when (localMultiplierType) {
                                                    MultiplierType.FIXED -> "×"
                                                    MultiplierType.PERCENTAGE -> "%"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (localMultiplierType) {
                                                    MultiplierType.FIXED -> colors.dialogMultiplierFixedIcon
                                                    MultiplierType.PERCENTAGE -> colors.dialogMultiplierPercentIcon
                                                }
                                            )
                                        }
                                    }
                                },
                                suffix = {
                                    Text(
                                        text = when (localMultiplierType) {
                                            MultiplierType.FIXED -> "x"
                                            MultiplierType.PERCENTAGE -> "%"
                                        },
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.dialogInputBorderFocused,
                                    unfocusedBorderColor = colors.dialogInputBorder,
                                    errorBorderColor = colors.errorColor,
                                    focusedContainerColor = colors.dialogInputBgFocused,
                                    unfocusedContainerColor = colors.dialogInputBg,
                                    cursorColor = colors.dialogInputBorderFocused,
                                    focusedLabelColor = colors.dialogInputBorderFocused,
                                    unfocusedLabelColor = colors.textSecondary,
                                    focusedTextColor = colors.dialogInputText,
                                    unfocusedTextColor = colors.dialogInputText
                                ),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    color = colors.dialogInputText,
                                    fontWeight = FontWeight.Medium
                                ),
                                isError = getValidationError(multiplierInput, localMultiplierType) != null
                            )

                            val errorMessage = getValidationError(multiplierInput, localMultiplierType)
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage,
                                    fontSize = 10.sp,
                                    color = colors.errorColor,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    // Quick Presets
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick Presets",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(
                                    when (localMultiplierType) {
                                        MultiplierType.FIXED -> listOf(1.5, 2.0, 2.5, 3.0, 4.0, 5.0)
                                        MultiplierType.PERCENTAGE -> listOf(50.0, 100.0, 150.0, 200.0, 300.0, 500.0)
                                    }
                                ) { value ->
                                    val isSelected = kotlin.math.abs(localMultiplierValue - value) < 0.001

                                    OutlinedButton(
                                        onClick = {
                                            handleMultiplierValueChange(value)
                                            multiplierInput = when (localMultiplierType) {
                                                MultiplierType.FIXED -> value.toString()
                                                MultiplierType.PERCENTAGE -> value.toInt().toString()
                                            }
                                        },
                                        modifier = Modifier.height(38.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected)
                                                colors.dialogButtonSelected else colors.dialogButtonUnselected,
                                            contentColor = if (isSelected)
                                                colors.dialogButtonSelectedText else colors.dialogButtonUnselectedText
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected)
                                                colors.dialogButtonSelectedBorder else colors.dialogButtonBorder
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = when (localMultiplierType) {
                                                MultiplierType.FIXED -> "${value}x"
                                                MultiplierType.PERCENTAGE -> "${value.toInt()}%"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

// Preview Section
                    item {
                        val previewData = remember(localMaxSteps, localMultiplierType, localMultiplierValue, multiplierInput) {
                            try {
                                val inputValue = multiplierInput.toDoubleOrNull()
                                val valueToUse = if (inputValue != null && isValidMultiplierValue(inputValue, localMultiplierType)) {
                                    inputValue
                                } else {
                                    localMultiplierValue
                                }

                                val previewSettings = martingaleSettings.copy(
                                    maxSteps = localMaxSteps,
                                    multiplierType = localMultiplierType,
                                    multiplierValue = valueToUse
                                )

                                val formattedSequence = try {
                                    previewSettings.getFormattedSequence()
                                } catch (e: Exception) {
                                    val amounts = mutableListOf<String>()
                                    var currentAmount = previewSettings.baseAmount

                                    for (step in 1..localMaxSteps) {
                                        amounts.add(formatIndonesianCurrencyNew(currentAmount))
                                        currentAmount = when (localMultiplierType) {
                                            MultiplierType.FIXED -> (currentAmount * valueToUse).toLong()
                                            MultiplierType.PERCENTAGE -> (currentAmount * (1 + valueToUse/100)).toLong()
                                        }
                                    }
                                    amounts.joinToString(" → ")
                                }

                                val formattedTotalRisk = try {
                                    previewSettings.getFormattedTotalRisk()
                                } catch (e: Exception) {
                                    var totalRisk = 0L
                                    var currentAmount = previewSettings.baseAmount

                                    for (step in 1..localMaxSteps) {
                                        totalRisk += currentAmount
                                        currentAmount = when (localMultiplierType) {
                                            MultiplierType.FIXED -> (currentAmount * valueToUse).toLong()
                                            MultiplierType.PERCENTAGE -> (currentAmount * (1 + valueToUse/100)).toLong()
                                        }
                                    }
                                    formatIndonesianCurrencyNew(totalRisk)
                                }

                                Triple(true, formattedSequence, formattedTotalRisk)
                            } catch (e: Exception) {
                                Triple(true, "Custom: ${localMaxSteps} steps", "Calculating...")
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = colors.dialogPreviewAccent.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Preview,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = colors.dialogPreviewAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "Sequence Preview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.dialogPreviewBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    colors.dialogPreviewBorder.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Amount Progression:",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Surface(
                                            color = colors.dialogPreviewAmountBg,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = previewData.second,
                                                fontSize = 11.sp,
                                                color = colors.textPrimary,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                lineHeight = 14.sp,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                colors.dialogPreviewRiskBg,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = colors.dialogPreviewAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Total Risk:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Text(
                                            text = previewData.third,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.dialogPreviewAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Information Section
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = colors.dialogPreviewAccent.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = colors.dialogPreviewAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "How Martingale Works",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.dialogInfoBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.dialogInfoBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CompactInfoItem(
                                        icon = Icons.Default.TrendingUp,
                                        title = "Fixed Multiplier",
                                        description = "Direct multiplication (2.0x = double)",
                                        colors = colors
                                    )

                                    Divider(color = colors.dialogDivider, thickness = 0.5.dp)

                                    CompactInfoItem(
                                        icon = Icons.Default.Percent,
                                        title = "Percentage Based",
                                        description = "Add percentage (100% = double)",
                                        colors = colors
                                    )

                                    Divider(color = colors.dialogDivider, thickness = 0.5.dp)

                                    CompactInfoItem(
                                        icon = Icons.Default.Layers,
                                        title = "Max Steps",
                                        description = "Maximum attempts before reset",
                                        colors = colors
                                    )

                                    Divider(color = colors.dialogDivider, thickness = 0.5.dp)

                                    CompactInfoItem(
                                        icon = Icons.Default.Refresh,
                                        title = "Reset Condition",
                                        description = "Resets after win or reaching max steps",
                                        colors = colors
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.dialogSuccessBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.dialogSuccessBorder.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = CircleShape,
                                        color = colors.dialogSuccessIcon.copy(alpha = 0.2f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = colors.dialogSuccessIcon,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Flexible Configuration",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.dialogSuccessText
                                        )
                                        Text(
                                            text = "Full flexibility enabled. Configure any combination that suits your trading strategy. No artificial limits on risk parameters.",
                                            fontSize = 10.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactInfoItem(
    icon: ImageVector,
    title: String,
    description: String,
    colors: DashboardColors
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = CircleShape,
            color = colors.dialogInfoIconBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = colors.textSecondary,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun TradingSettingsCard(
    isDemoAccount: Boolean,
    martingaleSettings: MartingaleState,
    currencySettings: CurrencySettings,
    canModify: Boolean,
    onAccountTypeChange: (Boolean) -> Unit,
    onCurrencyChange: (CurrencyType) -> Unit,
    onMartingaleStepsChange: (Int) -> Unit,
    onBaseAmountChange: (Long) -> Unit,
    onMartingaleToggle: (Boolean) -> Unit,
    onMultiplierTypeChange: (MultiplierType) -> Unit,
    onMultiplierValueChange: (Double) -> Unit,
    colors: DashboardColors,
    currentLanguage: String = "id"  // ✅ ADD PARAMETER
) {
    var showQuickAmountDropdown by remember { mutableStateOf(false) }
    var showMaxStepDialog by remember { mutableStateOf(false) }
    var amountInput by remember {
        mutableStateOf(formatToIndonesianCurrencyWithoutDecimal(martingaleSettings.baseAmount))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = StringsManager.getTradingSettings(currentLanguage), // ✅ MULTILANGUAGE
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = colors.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = StringsManager.getAccountConfiguration(currentLanguage), // ✅ MULTILANGUAGE
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var showAccountDropdown by remember { mutableStateOf(false) }

                        OutlinedCard(
                            onClick = { if (canModify) showAccountDropdown = true },
                            enabled = canModify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isDemoAccount)
                                    colors.warningColor.copy(alpha = 0.15f) else colors.successColor.copy(alpha = 0.15f),
                                contentColor = colors.textPrimary,
                                disabledContainerColor = colors.controls,
                                disabledContentColor = colors.textMuted
                            ),
                            border = BorderStroke(
                                0.8.dp,
                                if (isDemoAccount) colors.warningColor.copy(alpha = 0.5f) else colors.successColor.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDemoAccount) Icons.Default.School else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDemoAccount) colors.warningColor else colors.successColor
                                    )
                                    Text(
                                        text = if (isDemoAccount)
                                            StringsManager.getDemoAccount(currentLanguage)
                                        else
                                            StringsManager.getRealAccount(currentLanguage), // ✅ MULTILANGUAGE
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colors.textPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false },
                            modifier = Modifier
                                .background(
                                    colors.cardBackground,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .wrapContentWidth()
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = colors.warningColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            StringsManager.getDemoAccountFull(currentLanguage), // ✅ MULTILANGUAGE
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isDemoAccount) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.accentPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onAccountTypeChange(true)
                                    showAccountDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = colors.successColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            StringsManager.getRealAccountFull(currentLanguage), // ✅ MULTILANGUAGE
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (!isDemoAccount) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.accentPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onAccountTypeChange(false)
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        var showDurationDropdown by remember { mutableStateOf(false) }
                        var selectedDuration by remember { mutableStateOf("AUTO") }

                        OutlinedCard(
                            onClick = { if (canModify) showDurationDropdown = true },
                            enabled = canModify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = colors.accentPrimary5.copy(alpha = 0.1f),
                                contentColor = colors.textPrimary,
                                disabledContainerColor = colors.controls,
                                disabledContentColor = colors.textMuted
                            ),
                            border = BorderStroke(0.8.dp, colors.accentPrimary5.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.accentPrimary5
                                    )
                                    Text(
                                        text = selectedDuration,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colors.textPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDurationDropdown,
                            onDismissRequest = { showDurationDropdown = false },
                            modifier = Modifier
                                .background(
                                    colors.cardBackground,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            listOf(
                                "AUTO" to StringsManager.getAutoDuration(currentLanguage), // ✅ MULTILANGUAGE
                                "1M" to StringsManager.getOneMinute(currentLanguage)  // ✅ MULTILANGUAGE
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = colors.accentPrimary5,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = label,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (value == selectedDuration) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = colors.accentPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedDuration = value
                                        showDurationDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = StringsManager.getCurrencySelection(currentLanguage), // ✅ MULTILANGUAGE
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = colors.textPrimary
                )

                Box {
                    var showCurrencyDropdown by remember { mutableStateOf(false) }
                    val currentCurrency = currencySettings.selectedCurrency

                    OutlinedCard(
                        onClick = { if (canModify) showCurrencyDropdown = true },
                        enabled = canModify,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = colors.accentPrimary.copy(alpha = 0.1f),
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.controls
                        ),
                        border = BorderStroke(0.8.dp, colors.accentPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.accentPrimary
                                )
                                Text(
                                    text = "${currentCurrency.code} (${currentCurrency.flag})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.textPrimary
                            )
                        }
                    }


                    DropdownMenu(
                        expanded = showCurrencyDropdown,
                        onDismissRequest = { showCurrencyDropdown = false },
                        modifier = Modifier
                            .background(colors.cardBackground, RoundedCornerShape(12.dp))
                            .heightIn(max = 500.dp)
                            .widthIn(min = 280.dp)
                    ) {
                        // Group currencies by region
                        val currenciesByRegion = CurrencyType.getAllRegions().map { region ->
                            region to CurrencyType.getCurrenciesByRegion(region)
                        }

                        currenciesByRegion.forEachIndexed { regionIndex, (region, currencies) ->
                            // Region Header
                            if (regionIndex > 0) {
                                Divider(
                                    color = colors.borderColor.copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = colors.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = region,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Currency items in this region
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Currency Symbol
                                            Surface(
                                                modifier = Modifier.size(36.dp),
                                                shape = CircleShape,
                                                color = if (currency == currentCurrency)
                                                    colors.accentPrimary.copy(alpha = 0.2f)
                                                else
                                                    colors.surface,
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (currency == currentCurrency)
                                                        colors.accentPrimary
                                                    else
                                                        colors.borderColor.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(
                                                        text = currency.flag,
                                                        color = if (currency == currentCurrency)
                                                            colors.accentPrimary
                                                        else
                                                            colors.textPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Currency Info
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = currency.code,
                                                        color = colors.textPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (currency == currentCurrency) {
                                                        Surface(
                                                            color = colors.successColor.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.successColor,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Min: ${currency.formatAmount(currency.minAmountInCents)}",
                                                    color = colors.textMuted,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Check icon for selected currency
                                            if (currency == currentCurrency) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = colors.successColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onCurrencyChange(currency)
                                        showCurrencyDropdown = false
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsManager.getTradeAmount(currentLanguage), // ✅ MULTILANGUAGE
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = colors.textPrimary
                    )

                    val currentCurrency = currencySettings.selectedCurrency
                    Text(
                        text = "${StringsManager.getMinimum(currentLanguage)}: ${currentCurrency.formatAmount(currentCurrency.minAmountInCents)}", // ✅ MULTILANGUAGE
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        fontSize = 10.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var rawAmountInput by remember { mutableStateOf("") }
                    var placeholderAmount by remember { mutableStateOf(currencySettings.baseAmountInCents) }
                    val currentCurrency = currencySettings.selectedCurrency

                    LaunchedEffect(currencySettings.baseAmountInCents) {
                        if (rawAmountInput.isEmpty()) {
                            placeholderAmount = currencySettings.baseAmountInCents
                        }
                    }

                    OutlinedTextField(
                        value = rawAmountInput,
                        onValueChange = { input ->
                            rawAmountInput = input

                            val amount = currentCurrency.parseUserInput(input)
                            if (amount != null && amount >= currentCurrency.minAmountInCents) {
                                onBaseAmountChange(amount)
                                placeholderAmount = amount
                            }
                        },
                        placeholder = {
                            Text(
                                text = "${currentCurrency.formatAmount(currentCurrency.minAmountInCents)}",
                                fontSize = 13.sp,
                                color = colors.placeholderText
                            )
                        },
                        enabled = canModify,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentPrimary,
                            unfocusedBorderColor = colors.borderColor,
                            disabledBorderColor = colors.textMuted,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accentPrimary,
                            focusedLabelColor = colors.accentPrimary,
                            unfocusedLabelColor = colors.textPrimary,
                            unfocusedContainerColor = colors.surface,
                            focusedContainerColor = colors.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val amount = currentCurrency.parseUserInput(rawAmountInput)
                                if (amount != null) {
                                    rawAmountInput = currentCurrency.formatAmount(amount)
                                }
                            }
                        )
                    )

                    Box {
                        FilledTonalButton(
                            onClick = { if (canModify) showQuickAmountDropdown = true },
                            enabled = canModify,
                            modifier = Modifier.height(54.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.accentPrimary5.copy(alpha = 0.2f),
                                contentColor = colors.textPrimary,
                                disabledContainerColor = colors.controls,
                                disabledContentColor = colors.textMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, colors.accentPrimary5.copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.accentPrimary5
                                )
                                Text(
                                    text = StringsManager.getQuick(currentLanguage), // ✅ MULTILANGUAGE
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showQuickAmountDropdown,
                            onDismissRequest = { showQuickAmountDropdown = false },
                            modifier = Modifier
                                .background(
                                    colors.cardBackground,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            val quickAmounts = getQuickAmountsForCurrency(currentCurrency)

                            quickAmounts.forEach { (amount, display) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MonetizationOn,
                                                contentDescription = null,
                                                tint = colors.successColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = display,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        onBaseAmountChange(amount)
                                        rawAmountInput = currentCurrency.formatAmount(amount)
                                        placeholderAmount = amount
                                        showQuickAmountDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = StringsManager.getMartingaleStrategy(currentLanguage), // ✅ MULTILANGUAGE
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = {
                            if (canModify) {
                                onMartingaleToggle(!martingaleSettings.isEnabled)
                            }
                        },
                        enabled = canModify,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (martingaleSettings.isEnabled)
                                colors.successColor.copy(alpha = 0.15f) else colors.surface,
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.controls,
                            disabledContentColor = colors.textMuted
                        ),
                        border = BorderStroke(
                            0.8.dp,
                            if (martingaleSettings.isEnabled) colors.successColor.copy(alpha = 0.5f) else colors.borderColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (martingaleSettings.isEnabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (martingaleSettings.isEnabled) colors.successColor else colors.textMuted
                            )
                            Text(
                                text = StringsManager.getMartingale(currentLanguage), // ✅ MULTILANGUAGE
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textPrimary
                            )
                        }
                    }

                    OutlinedCard(
                        onClick = {
                            if (canModify && martingaleSettings.isEnabled) {
                                showMaxStepDialog = true
                            }
                        },
                        enabled = canModify && martingaleSettings.isEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (martingaleSettings.isEnabled)
                                colors.surface else colors.surface,
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.controls,
                        ),
                        border = BorderStroke(
                            0.8.dp,
                            if (martingaleSettings.isEnabled) colors.warningColor.copy(alpha = 0.4f) else colors.borderColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = StringsManager.getMaxSteps(currentLanguage), // ✅ MULTILANGUAGE
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = martingaleSettings.maxSteps.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                if (martingaleSettings.isEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = colors.accentWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMaxStepDialog) {
        MaxStepSelectionDialog(
            currentMaxSteps = martingaleSettings.maxSteps,
            martingaleSettings = martingaleSettings,
            onMaxStepsSelected = { steps ->
                onMartingaleStepsChange(steps)
                showMaxStepDialog = false
            },
            onMultiplierTypeChange = onMultiplierTypeChange,
            onMultiplierValueChange = onMultiplierValueChange,
            colors = colors,
            onDismiss = { showMaxStepDialog = false }
        )
    }
}

@Composable
fun StopLossProfitCard(
    stopLossSettings: StopLossSettings,
    stopProfitSettings: StopProfitSettings,
    tradingSession: TradingSession,
    canModify: Boolean,
    onStopLossEnabledChange: (Boolean) -> Unit,
    onStopLossMaxAmountChange: (Long) -> Unit,
    onStopProfitEnabledChange: (Boolean) -> Unit,
    onStopProfitTargetAmountChange: (Long) -> Unit,
    onResetSession: () -> Unit,
    colors: DashboardColors,
    currentCurrency: CurrencyType = CurrencyType.IDR,
    currentLanguage: String = "id",  // ✅ ADD PARAMETER
    modifier: Modifier = Modifier
) {
    var showStopLossInput by remember { mutableStateOf(false) }
    var showStopProfitInput by remember { mutableStateOf(false) }

    var stopLossAmountInput by remember(stopLossSettings.maxLossAmount) {
        mutableStateOf(
            if (stopLossSettings.maxLossAmount > 0)
                formatInputAmount(stopLossSettings.maxLossAmount)
            else ""
        )
    }

    var stopProfitAmountInput by remember(stopProfitSettings.targetProfitAmount) {
        mutableStateOf(
            if (stopProfitSettings.targetProfitAmount > 0)
                formatInputAmount(stopProfitSettings.targetProfitAmount)
            else ""
        )
    }

    fun parseFlexibleInput(input: String): Long? {
        if (input.trim().isEmpty()) return null

        val cleanInput = input.trim().uppercase()

        return try {
            when {
                cleanInput.endsWith("B") -> {
                    val number = cleanInput.dropLast(1).toDoubleOrNull()
                    number?.let { (it * 1_000_000_000 * 100).toLong() }
                }
                cleanInput.endsWith("M") -> {
                    val number = cleanInput.dropLast(1).toDoubleOrNull()
                    number?.let { (it * 1_000_000 * 100).toLong() }
                }
                cleanInput.endsWith("K") -> {
                    val number = cleanInput.dropLast(1).toDoubleOrNull()
                    number?.let { (it * 1_000 * 100).toLong() }
                }
                else -> {
                    val numberStr = cleanInput.replace("[^\\d.,]".toRegex(), "")
                        .replace(",", ".")
                    val number = numberStr.toDoubleOrNull()
                    number?.let { (it * 100).toLong() }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.cardBackground,
                            colors.cardBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsManager.getStopLossProfit(currentLanguage), // ✅ MULTILANGUAGE
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textPrimary
                    )

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = canModify) { onResetSession() },
                        color = if (canModify) colors.warningColor.copy(alpha = 0.1f) else colors.controls,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            0.8.dp,
                            if (canModify) colors.warningColor.copy(alpha = 0.4f) else colors.borderColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = StringsManager.getResetSession(currentLanguage), // ✅ MULTILANGUAGE
                                modifier = Modifier.size(18.dp),
                                tint = if (canModify) colors.warningColor else colors.textMuted
                            )
                        }
                    }
                }

                // Statistics Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.darkBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(0.8.dp, colors.borderColor.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Net Profit
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = StringsManager.getNetProfit(currentLanguage), // ✅ MULTILANGUAGE
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val netProfit = tradingSession.getNetProfit()
                            Text(
                                text = currentCurrency.formatAmount(netProfit),
                                color = if (netProfit >= 0) colors.successColor else colors.errorColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentCurrency.formatAmount(stopProfitSettings.targetProfitAmount),
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(60.dp)
                                .background(colors.borderColor.copy(alpha = 0.5f))
                        )

                        // Total Loss
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = StringsManager.getTotalLoss(currentLanguage), // ✅ MULTILANGUAGE
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = currentCurrency.formatAmount(tradingSession.totalLoss),
                                color = colors.errorColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentCurrency.formatAmount(stopLossSettings.maxLossAmount),
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stop Loss Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (stopLossSettings.isEnabled && canModify)
                                        colors.errorColor.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .offset(y = 2.dp)
                        )

                        Surface(
                            onClick = {
                                onStopLossEnabledChange(!stopLossSettings.isEnabled)
                                if (!stopLossSettings.isEnabled) {
                                    showStopLossInput = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            enabled = canModify,
                            color = if (stopLossSettings.isEnabled)
                                colors.errorColor.copy(alpha = 0.4f)
                            else
                                colors.botButtonDisabledBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (stopLossSettings.isEnabled)
                                    colors.borderColor.copy(alpha = 0.5f)
                                else
                                    colors.borderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = if (stopLossSettings.isEnabled) {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.errorColor.copy(alpha = 0.15f),
                                                    colors.errorColor.copy(alpha = 0.05f)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.textMuted.copy(alpha = 0.4f),
                                                    colors.textMuted.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = StringsManager.getStopLoss(currentLanguage), // ✅ MULTILANGUAGE
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp,
                                    color = if (stopLossSettings.isEnabled)
                                        colors.textPrimary
                                    else
                                        colors.textSecondary
                                )
                            }
                        }
                    }

                    // Target Profit Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (stopProfitSettings.isEnabled && canModify)
                                        colors.successColor.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .offset(y = 2.dp)
                        )

                        Surface(
                            onClick = {
                                onStopProfitEnabledChange(!stopProfitSettings.isEnabled)
                                if (!stopProfitSettings.isEnabled) {
                                    showStopProfitInput = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            enabled = canModify,
                            color = if (stopProfitSettings.isEnabled)
                                colors.successColor.copy(alpha = 0.4f)
                            else
                                colors.botButtonDisabledBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (stopLossSettings.isEnabled)
                                    colors.borderColor.copy(alpha = 0.5f)
                                else
                                    colors.borderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = if (stopProfitSettings.isEnabled) {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.successColor.copy(alpha = 0.15f),
                                                    colors.successColor.copy(alpha = 0.05f)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.textMuted.copy(alpha = 0.4f),
                                                    colors.textMuted.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = StringsManager.getTargetProfit(currentLanguage), // ✅ MULTILANGUAGE
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp,
                                    color = if (stopLossSettings.isEnabled)
                                        colors.textPrimary
                                    else
                                        colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Stop Loss Input Section
                AnimatedVisibility(
                    visible = stopLossSettings.isEnabled && showStopLossInput,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.darkBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.errorColor.copy(alpha = 0.5f))
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
                                Text(
                                    text = StringsManager.getStopLossSettings(currentLanguage), // ✅ MULTILANGUAGE
                                    color = colors.errorColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showStopLossInput = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = StringsManager.getClose(currentLanguage), // ✅ MULTILANGUAGE
                                        tint = colors.errorColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = stopLossAmountInput,
                                onValueChange = { input ->
                                    stopLossAmountInput = input
                                },
                                label = {
                                    Text(
                                        StringsManager.getMaxLossAmount(currentLanguage), // ✅ MULTILANGUAGE
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                placeholder = {
                                    Text(
                                        StringsManager.getPlaceholderAmount(currentLanguage), // ✅ MULTILANGUAGE
                                        color = colors.placeholderText,
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = colors.errorColor,
                                    unfocusedBorderColor = colors.borderColor,
                                    focusedLabelColor = colors.errorColor,
                                    unfocusedLabelColor = colors.textMuted,
                                    cursorColor = colors.errorColor
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        parseFlexibleInput(stopLossAmountInput)?.let { parsedAmount ->
                                            onStopLossMaxAmountChange(parsedAmount)
                                            showStopLossInput = false
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            if (stopLossSettings.maxLossAmount > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = StringsManager.getCurrentMaxLoss(currentLanguage), // ✅ MULTILANGUAGE
                                            color = colors.textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentCurrency.formatAmount(stopLossSettings.maxLossAmount),
                                            color = colors.errorColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = StringsManager.getAmountFormatHint(currentLanguage), // ✅ MULTILANGUAGE
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                // Target Profit Input Section
                AnimatedVisibility(
                    visible = stopProfitSettings.isEnabled && showStopProfitInput,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.darkBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.successColor.copy(alpha = 0.5f))
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
                                Text(
                                    text = StringsManager.getTargetProfitSettings(currentLanguage), // ✅ MULTILANGUAGE
                                    color = colors.successColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showStopProfitInput = false },
                                    modifier = Modifier.size(28.dp)
                                ) {Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = StringsManager.getClose(currentLanguage), // ✅ MULTILANGUAGE
                                    tint = colors.successColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                }
                            }

                            OutlinedTextField(
                                value = stopProfitAmountInput,
                                onValueChange = { input ->
                                    stopProfitAmountInput = input
                                },
                                label = {
                                    Text(
                                        StringsManager.getTargetProfitAmount(currentLanguage), // ✅ MULTILANGUAGE
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                placeholder = {
                                    Text(
                                        StringsManager.getPlaceholderAmount(currentLanguage), // ✅ MULTILANGUAGE
                                        color = colors.placeholderText,
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = colors.successColor,
                                    unfocusedBorderColor = colors.borderColor,
                                    focusedLabelColor = colors.successColor,
                                    unfocusedLabelColor = colors.textMuted,
                                    cursorColor = colors.successColor
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        parseFlexibleInput(stopProfitAmountInput)?.let { parsedAmount ->
                                            onStopProfitTargetAmountChange(parsedAmount)
                                            showStopProfitInput = false
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            if (stopProfitSettings.targetProfitAmount > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = StringsManager.getCurrentTargetProfit(currentLanguage), // ✅ MULTILANGUAGE
                                            color = colors.textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentCurrency.formatAmount(stopProfitSettings.targetProfitAmount),
                                            color = colors.successColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = StringsManager.getAmountFormatHint(currentLanguage), // ✅ MULTILANGUAGE
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatInputAmount(serverAmount: Long): String {
    val rupiahValue = serverAmount / 100.0

    return when {
        rupiahValue >= 1_000_000_000.0 -> {
            val milyarValue = rupiahValue / 1_000_000_000.0
            if (milyarValue == milyarValue.toInt().toDouble()) {
                "${milyarValue.toInt()}B"
            } else {
                "${String.format("%.1f", milyarValue)}B"
            }
        }
        rupiahValue >= 1_000_000.0 -> {
            val jutaValue = rupiahValue / 1_000_000.0
            if (jutaValue == jutaValue.toInt().toDouble()) {
                "${jutaValue.toInt()}M"
            } else {
                "${String.format("%.1f", jutaValue)}M"
            }
        }
        rupiahValue >= 1_000.0 -> {
            val ribuValue = rupiahValue / 1_000.0
            if (ribuValue == ribuValue.toInt().toDouble()) {
                "${ribuValue.toInt()}K"
            } else {
                "${String.format("%.1f", ribuValue)}K"
            }
        }
        else -> {
            if (rupiahValue == rupiahValue.toInt().toDouble()) {
                "${rupiahValue.toInt()}"
            } else {
                "${String.format("%.0f", rupiahValue)}"
            }
        }
    }
}

@Composable
fun WinLoseStatsRow(
    todayStats: TodayStats,
    isLoading: Boolean,
    isDemoAccount: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            value = todayStats.winCount,
            label = "Win",
            color = WifiGreen,
            isLoading = isLoading
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Remove,
            value = todayStats.drawCount,
            label = "Draw",
            color = AccentWarning,
            isLoading = isLoading
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingDown,
            value = todayStats.loseCount,
            label = "Loss",
            color = AccentSecondary,
            isLoading = isLoading
        )
    }
}


@Composable
fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    colors: DashboardColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.accentSecondary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.accentSecondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.accentSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = "Error",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = colors.accentSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LastTradeResultCard(
    result: TradeResult,
    colors: DashboardColors,
    currentCurrency: CurrencyType = CurrencyType.IDR
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            ),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (result.success)
                colors.successColor.copy(alpha = 0.2f)
            else
                colors.errorColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (result.success) {
                                listOf(
                                    colors.successColor.copy(alpha = 0.08f),
                                    colors.successColor.copy(alpha = 0.04f)
                                )
                            } else {
                                listOf(
                                    colors.errorColor.copy(alpha = 0.08f),
                                    colors.errorColor.copy(alpha = 0.04f)
                                )
                            }
                        )
                    ),
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = if (result.success)
                                colors.successColor.copy(alpha = 0.15f)
                            else
                                colors.errorColor.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (result.success) colors.successColor else colors.errorColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Last Trade Result",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.01).sp
                            )

                            Text(
                                text = if (result.success) "Trade Successful" else "Trade Failed",
                                fontSize = 13.sp,
                                color = if (result.success) colors.successColor else colors.errorColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        color = if (result.success)
                            colors.successColor.copy(alpha = 0.12f)
                        else
                            colors.errorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (result.success)
                                colors.successColor.copy(alpha = 0.25f)
                            else
                                colors.errorColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (result.success) colors.successColor else colors.errorColor,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (result.success) "SUCCESS" else "FAILED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (result.success) colors.successColor else colors.errorColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Trade Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = result.message,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            result.details?.let { details ->
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Transaction Information",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailRow(
                                label = "Asset",
                                value = details["asset"] as? String ?: "Unknown",
                                icon = Icons.Default.TrendingUp,
                                colors = colors
                            )
                            Divider(color = colors.borderColor)
                            DetailRow(
                                label = "Amount",
                                value = currentCurrency.formatAmount(details["amount"] as? Long ?: 0L),
                                icon = Icons.Default.AttachMoney,
                                valueColor = colors.accentProfit,
                                colors = colors
                            )
                            Divider(color = colors.borderColor)

                            // 🔥 UPDATED: Convert CALL/PUT to BUY/SELL
                            val rawTrend = details["trend"] as? String ?: "unknown"
                            val displayTrend = when (rawTrend.uppercase()) {
                                "CALL" -> "BUY"
                                "PUT" -> "SELL"
                                else -> rawTrend.uppercase()
                            }
                            val isBuy = displayTrend == "BUY"

                            DetailRow(
                                label = "Trend Direction",
                                value = displayTrend,
                                icon = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                valueColor = if (isBuy) colors.successColor else colors.errorColor,
                                showBadge = true,
                                colors = colors
                            )

                            if (result.isMartingaleAttempt) {
                                Divider(color = colors.borderColor)
                                DetailRow(
                                    label = "Martingale Step",
                                    value = result.martingaleStep.toString(),
                                    icon = Icons.Default.Layers,
                                    valueColor = colors.warningColor,
                                    colors = colors
                                )
                                Divider(color = colors.borderColor)
                                DetailRow(
                                    label = "Total Loss",
                                    value = currentCurrency.formatAmount(result.martingaleTotalLoss),
                                    icon = Icons.Default.TrendingDown,
                                    valueColor = colors.errorColor,
                                    colors = colors
                                )
                            }

                            val timestamp = details["timestamp"] as? Long ?: System.currentTimeMillis()
                            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            Divider(color = colors.borderColor)
                            DetailRow(
                                label = "Executed At",
                                value = timeFormat.format(Date(timestamp + 2000)),
                                icon = Icons.Default.Schedule,
                                valueColor = colors.textSecondary,
                                colors = colors
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color? = null,
    showBadge: Boolean = false,
    colors: DashboardColors
) {
    val finalValueColor = valueColor ?: colors.textPrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = colors.controls
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 13.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.1.sp
            )
        }

        if (showBadge) {
            Surface(
                color = finalValueColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = finalValueColor.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = finalValueColor,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    ),
                    letterSpacing = 0.2.sp
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = finalValueColor,
                textAlign = TextAlign.End,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
fun MultilineScheduleDialog(
    currentInput: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    colors: DashboardColors,
    scheduledOrders: List<ScheduledOrder> = emptyList()
) {
    var multilineInput by remember(scheduledOrders) {
        mutableStateOf(
            if (currentInput.isEmpty() && scheduledOrders.isNotEmpty()) {
                scheduledOrders
                    .filter { !it.isExecuted && !it.isSkipped }
                    .sortedBy { it.timeInMillis }
                    .joinToString("\n") { order ->
                        val trendSymbol = when (order.trend.lowercase()) {
                            "call", "buy" -> "B"
                            "put", "sell" -> "S"
                            else -> order.trend.uppercase().take(1)
                        }
                        "${order.time} $trendSymbol"
                    }
            } else {
                currentInput
            }
        )
    }

    val placeholderText = remember(scheduledOrders, multilineInput) {
        if (scheduledOrders.isEmpty()) {
            "Paste your schedule here:\n\n00:00 B\n00:03 S\n00:06 B\n00:09 B\n00:12 S\n00:15 B\n00:18 B\n00:21 B\n00:24 B\n00:27 B"
        } else if (multilineInput.isEmpty()) {
            "Your schedules will appear here automatically...\n\nClick to edit or paste new schedules"
        } else {
            "Current schedules loaded.\n\nYou can modify them or paste additional schedules below."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulk Schedule Input",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Format Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.accentPrimary.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Supported Formats:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )

                        Text(
                            text = "• Single line: 10:30 B 11:00 S 12:15 B\n" +
                                    "• Multiple lines:\n" +
                                    "  10:30 B\n" +
                                    "  11:00 S\n" +
                                    "  12:15 B\n" +
                                    "• Mixed format: Copy-paste any combination",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }

                // TextField
                OutlinedTextField(
                    value = multilineInput,
                    onValueChange = { multilineInput = it },
                    placeholder = {
                        Text(
                            text = placeholderText,
                            fontSize = 11.sp,
                            color = colors.placeholderText,
                            lineHeight = 16.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 16.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentPrimary,
                        unfocusedBorderColor = colors.borderColor,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 20
                )

                // ✅ Action Buttons - DIPINDAH KE ATAS PREVIEW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { multilineInput = "" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentSecondary,
                            contentColor = colors.TextPrimary1
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onInputChange(multilineInput)
                            onAdd()
                        },
                        enabled = multilineInput.isNotBlank(),
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentSecondary,
                            contentColor = colors.TextPrimary1,
                            disabledContainerColor = colors.controls
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Schedules", fontSize = 12.sp)
                    }
                }

                // ✅ Preview - SEKARANG DI BAWAH TOMBOL
                if (multilineInput.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Preview (${getPreviewCount(multilineInput)} orders):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )

                            LazyColumn(
                                modifier = Modifier.height(80.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(getPreviewOrders(multilineInput)) { preview ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = preview.first,
                                            fontSize = 10.sp,
                                            color = colors.textSecondary,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (preview.second == "B") colors.accentPrimary.copy(alpha = 0.2f) else colors.accentSecondary.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = preview.second,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (preview.second == "B") colors.accentPrimary else colors.accentSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions (sudah ada di file Anda)
private fun getPreviewCount(input: String): Int {
    return try {
        val cleanedInput = input.trim()
            .replace(Regex("[\\u3000\\u00A0]"), " ")
            .replace(Regex("[:]"), ":")
            .replace(Regex("\\s+"), " ")
            .replace("\n", " ")
            .replace("\r", " ")

        val parts = cleanedInput.split(" ").filter { it.isNotBlank() }
        parts.size / 2
    } catch (e: Exception) {
        0
    }
}

private fun getPreviewOrders(input: String): List<Pair<String, String>> {
    return try {
        val cleanedInput = input.trim()
            .replace(Regex("\\s+"), " ")
            .replace("\n", " ")
            .replace("\r", " ")

        val parts = cleanedInput.split(" ").filter { it.isNotBlank() }
        val orders = mutableListOf<Pair<String, String>>()

        for (i in parts.indices step 2) {
            if (i + 1 < parts.size) {
                val time = parts[i].trim()
                val trend = parts[i + 1].trim().uppercase()

                val isValidTime = time.matches(Regex("\\d{1,2}([:.])\\d{2}"))
                val isValidTrend = trend in listOf("B", "S", "BUY", "SELL")

                if (isValidTime && isValidTrend) {
                    orders.add(Pair(time, trend))
                }
            }
        }

        orders.take(10)
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun AssetSelectionDialog(
    assets: List<Asset>,
    isLoading: Boolean,
    onAssetSelected: (Asset) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    colors: DashboardColors,
    currentLanguage: String = "id"  // ✅ ADD PARAMETER
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(700.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = colors.background
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                width = 1.dp,
                color = colors.borderColor
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.surface,
                                    colors.cardBackground
                                )
                            )
                        ),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = StringsManager.getSelectAsset(currentLanguage), // ✅ MULTILANGUAGE
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.02).sp
                            )
                            Text(
                                text = StringsManager.getChooseFromAvailable(currentLanguage), // ✅ MULTILANGUAGE
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.sp
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = colors.surface,
                            onClick = onRefresh,
                            border = BorderStroke(
                                width = 1.dp,
                                color = colors.borderColor
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = StringsManager.getRefresh(currentLanguage), // ✅ MULTILANGUAGE
                                    tint = colors.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = colors.accentPrimary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = StringsManager.getLoadingAssets(currentLanguage), // ✅ MULTILANGUAGE
                                            fontSize = 18.sp,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.01).sp
                                        )
                                        Text(
                                            text = StringsManager.getPleaseWaitFetchingAssets(currentLanguage), // ✅ MULTILANGUAGE
                                            fontSize = 14.sp,
                                            color = colors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 0.sp
                                        )
                                    }
                                }
                            }
                        }

                        assets.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(88.dp),
                                        shape = CircleShape,
                                        color = colors.surface,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = colors.borderColor
                                        )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = colors.warningColor,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = StringsManager.getNoAssetsAvailable(currentLanguage), // ✅ MULTILANGUAGE
                                            fontSize = 20.sp,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = (-0.01).sp
                                        )

                                        Text(
                                            text = StringsManager.getNoAssetsToDisplay(currentLanguage), // ✅ MULTILANGUAGE
                                            fontSize = 15.sp,
                                            color = colors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp,
                                            letterSpacing = 0.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = onRefresh,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accentPrimary
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 32.dp,
                                            vertical = 16.dp
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 4.dp,
                                            pressedElevation = 8.dp,
                                            hoveredElevation = 6.dp
                                        ),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = colors.accentPrimary2,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = StringsManager.getRefreshAssets(currentLanguage), // ✅ MULTILANGUAGE
                                                color = colors.accentPrimary2,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp,
                                                letterSpacing = 0.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = assets,
                                    key = { asset -> asset.hashCode() }
                                ) { asset ->
                                    AssetListItem(
                                        asset = asset,
                                        onClick = { onAssetSelected(asset) },
                                        colors = colors,
                                        currentLanguage = currentLanguage  // ✅ PASS LANGUAGE
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetListItem(
    asset: Asset,
    onClick: () -> Unit,
    colors: DashboardColors,
    currentLanguage: String = "id"  // ✅ ADD PARAMETER
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = asset.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.15.sp
                )

                Text(
                    text = asset.ric,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.4.sp
                )

                Surface(
                    color = colors.controls,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = asset.typeName,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 2.dp
                        ),
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val profitColor = when {
                    asset.profitRate > 0 -> colors.successColor
                    asset.profitRate < 0 -> colors.errorColor
                    else -> colors.textMuted
                }

                val profitIcon = when {
                    asset.profitRate > 0 -> "↑"
                    asset.profitRate < 0 -> "↓"
                    else -> ""
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (profitIcon.isNotEmpty()) {
                        Text(
                            text = profitIcon,
                            fontSize = 12.sp,
                            color = profitColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${asset.profitRate.toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = profitColor,
                        letterSpacing = 0.1.sp
                    )
                }

                Text(
                    text = StringsManager.getProfit(currentLanguage), // ✅ MULTILANGUAGE
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun ScheduleListDialog(
    scheduledOrders: List<ScheduledOrder>,
    onRemoveOrder: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    onShowInputSignal: () -> Unit,
    colors: DashboardColors
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(600.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, colors.borderColor),
            colors = CardDefaults.cardColors(containerColor = colors.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header (tidak berubah)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.surface, colors.cardBackground)
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Scheduled Orders",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    letterSpacing = (-0.02).sp
                                )

                                if (scheduledOrders.isNotEmpty()) {
                                    Surface(
                                        color = colors.cardBackground,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${scheduledOrders.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                shape = CircleShape,
                                color = colors.surface,
                                onClick = onDismiss,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = colors.borderColor
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Manage your scheduled trades",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )

                        // Action buttons
                        if (scheduledOrders.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.successColor.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, colors.successColor.copy(alpha = 0.3f)),
                                    onClick = onShowInputSignal
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Input Signal",
                                            tint = colors.successColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Input Signal",
                                            color = colors.successColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.errorColor.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, colors.errorColor.copy(alpha = 0.2f)),
                                    onClick = onClearAll
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Clear All",
                                            tint = colors.errorColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Clear All",
                                            color = colors.errorColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ✅ Content - TAMPILKAN SEMUA ORDER (termasuk completed)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    if (scheduledOrders.isEmpty()) {
                        // Empty state (tidak berubah)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(88.dp),
                                shape = CircleShape,
                                color = colors.surface.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, colors.borderColor)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No Scheduled Orders",
                                    fontSize = 20.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.01).sp
                                )
                                Text(
                                    text = "Schedule orders to execute automatically\nat your specified times.",
                                    fontSize = 15.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    } else {
                        // ✅ TAMPILKAN SEMUA ORDER - sort berdasarkan waktu
                        val sortedOrders = remember(scheduledOrders) {
                            scheduledOrders.sortedBy { it.timeInMillis }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(sortedOrders, key = { _, order -> order.id }) { index, order ->
                                ScheduleListItem(
                                    order = order,
                                    index = index + 1,
                                    onRemove = { onRemoveOrder(order.id) },
                                    colors = colors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleListItem(
    order: ScheduledOrder,
    index: Int,
    onRemove: () -> Unit,
    colors: DashboardColors
) {
    // ✅ FIXED: Tentukan warna berdasarkan status dengan benar
    val cardBackgroundColor = when {
        order.isSkipped -> colors.warningColor.copy(alpha = 0.08f)
        order.isExecuted && order.martingaleState.isCompleted -> {
            // Cek hasil martingale atau hasil trade biasa
            when (order.martingaleState.finalResult) {
                "WIN", "MENANG" -> colors.successColor.copy(alpha = 0.08f)
                "LOSE", "LOSS", "KALAH" -> colors.errorColor.copy(alpha = 0.08f)
                else -> colors.successColor.copy(alpha = 0.08f)
            }
        }
        order.isExecuted -> colors.successColor.copy(alpha = 0.08f)
        else -> colors.surface.copy(alpha = 0.4f)
    }

    val cardBorderColor = when {
        order.isSkipped -> colors.warningColor.copy(alpha = 0.2f)
        order.isExecuted && order.martingaleState.isCompleted -> {
            when (order.martingaleState.finalResult) {
                "WIN", "MENANG" -> colors.successColor.copy(alpha = 0.2f)
                "LOSE", "LOSS", "KALAH" -> colors.errorColor.copy(alpha = 0.2f)
                else -> colors.successColor.copy(alpha = 0.2f)
            }
        }
        order.isExecuted -> colors.successColor.copy(alpha = 0.2f)
        else -> colors.borderColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = cardBorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index number
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = colors.controls
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Time and Trend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.time,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        val displayTrend = when (order.trend.uppercase()) {
                            "CALL" -> "BUY"
                            "PUT" -> "SELL"
                            "B" -> "BUY"
                            "S" -> "SELL"
                            else -> order.trend.uppercase()
                        }

                        Surface(
                            color = if (displayTrend == "BUY") colors.successColor else colors.errorColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = displayTrend,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }

                    // ✅ FIXED: Status display - HARUS TAMPIL UNTUK SEMUA ORDER
                    val statusText = when {
                        order.isSkipped -> order.skipReason ?: "Skipped"
                        order.martingaleState.isCompleted -> {
                            val result = order.martingaleState.finalResult ?: "UNKNOWN"
                            val step = order.martingaleState.currentStep

                            if (step > 1) {
                                when (result.uppercase()) {
                                    "WIN", "MENANG" -> "WIN"
                                    "LOSE", "LOSS", "KALAH" -> "LOSS"
                                    else -> "Completed (Step $step)"
                                }
                            } else {
                                when (result.uppercase()) {
                                    "WIN", "MENANG" -> "WIN"
                                    "LOSE", "LOSS", "KALAH" -> "LOSS"
                                    else -> "Completed"
                                }
                            }
                        }
                        order.martingaleState.isActive -> {
                            val step = order.martingaleState.currentStep
                            if (step > 0) "Martingale Step $step In Progress"
                            else "Martingale Starting..."
                        }
                        order.isExecuted -> "Monitoring"
                        else -> "Pending..."  // ✅ DEFAULT untuk order yang belum dieksekusi
                    }

                    val statusColor = when {
                        order.isSkipped -> colors.warningColor
                        order.martingaleState.isCompleted -> {
                            when (order.martingaleState.finalResult) {
                                "WIN", "MENANG" -> colors.successColor
                                "LOSE", "LOSS", "KALAH" -> colors.errorColor
                                else -> colors.textMuted
                            }
                        }
                        order.martingaleState.isActive -> colors.warningColor
                        order.isExecuted -> colors.successColor
                        else -> colors.textSecondary  // ✅ Warna untuk "Pending..."
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete button - hanya tampil jika belum executed/skipped
            if (!order.isExecuted && !order.isSkipped) {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    color = colors.errorColor.copy(alpha = 0.1f),
                    onClick = onRemove
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = colors.errorColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildStatusText(order: ScheduledOrder): String {
    return when {
        // Order di-skip
        order.isSkipped -> {
            order.skipReason ?: "Skipped"
        }

        // Martingale completed (WIN atau LOSS)
        order.martingaleState.isCompleted -> {
            val result = order.martingaleState.finalResult ?: "UNKNOWN"
            val step = order.martingaleState.currentStep

            if (step > 1) {
                when (result.uppercase()) {
                    "WIN", "MENANG" -> "Martingale WIN at Step $step"
                    "LOSE", "LOSS", "KALAH" -> "Martingale FAILED at Step $step"
                    else -> "Completed (Step $step)"
                }
            } else {
                when (result.uppercase()) {
                    "WIN", "MENANG" -> "Trade WIN"
                    "LOSE", "LOSS", "KALAH" -> "Trade LOSS"
                    else -> "Completed"
                }
            }
        }

        // Martingale sedang berjalan
        order.martingaleState.isActive -> {
            val step = order.martingaleState.currentStep
            if (step > 0) {
                "Martingale Step $step"
            } else {
                "Martingale Starting..."
            }
        }

        // Trade executed (tidak perlu parameter tambahan)
        order.isExecuted -> {
            "Trade Executed"
        }

        // Default (pending)
        else -> ""
    }
}

@Composable
private fun TradingModeSelector(
    currentMode: TradingMode,
    onModeChange: (TradingMode) -> Unit,
    canModify: Boolean,
    isFollowModeActive: Boolean,
    isIndicatorModeActive: Boolean,
    isCTCModeActive: Boolean,
    botState: BotState,
    colors: DashboardColors,
    currentLanguage: String = "id",
    isModeSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showModeDropdown by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { if (canModify) showModeDropdown = true },
            enabled = canModify,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = colors.surface,
                contentColor = colors.textPrimary,
                disabledContainerColor = colors.surface.copy(alpha = 0.5f),
                disabledContentColor = colors.textMuted
            ),
            border = BorderStroke(
                0.8.dp,
                colors.chartLine.copy(alpha = 0.4f)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tampilkan "Pilih Mode" jika belum dipilih
                Text(
                    text = if (isModeSelected) getModeDisplayName(currentMode)
                    else StringsManager.getSelectTradingMode(currentLanguage),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (canModify) colors.textPrimary else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = showModeDropdown,
            onDismissRequest = { showModeDropdown = false },
            modifier = Modifier
                .background(colors.cardBackground, shape = RoundedCornerShape(8.dp))
                .border(1.dp, colors.borderColor, RoundedCornerShape(8.dp))
                .wrapContentWidth()
        ) {
            // ✅ Header "Pilih Mode" di dalam dropdown
            DropdownMenuItem(
                text = {
                    Text(
                        text = StringsManager.getSignalMode(currentLanguage),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                },
                onClick = { }, // Tidak bisa diklik
                enabled = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            // ✅ Divider setelah header
            Divider(
                color = colors.borderColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Signal Mode
            val canSelectSignal = !isFollowModeActive && !isIndicatorModeActive && !isCTCModeActive
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Signal Mode",
                            color = if (canSelectSignal) colors.textPrimary else colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentMode == TradingMode.SCHEDULE) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.accentProfit,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    if (canSelectSignal) onModeChange(TradingMode.SCHEDULE)
                    showModeDropdown = false
                },
                enabled = canSelectSignal,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )

            // Fastrade FTT Mode
            val canSelectFTT = botState == BotState.STOPPED && !isIndicatorModeActive && !isCTCModeActive
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Fastrade FTT Mode",
                            color = if (canSelectFTT) colors.textPrimary else colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentMode == TradingMode.FOLLOW_ORDER) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    if (canSelectFTT) onModeChange(TradingMode.FOLLOW_ORDER)
                    showModeDropdown = false
                },
                enabled = canSelectFTT,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )

            // Momentum Mode (Indicator)
            val canSelectMomentum = botState == BotState.STOPPED && !isFollowModeActive && !isCTCModeActive
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Analysis Strategy Mode",
                            color = if (canSelectMomentum) colors.textPrimary else colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentMode == TradingMode.INDICATOR_ORDER) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.accentWarning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    if (canSelectMomentum) onModeChange(TradingMode.INDICATOR_ORDER)
                    showModeDropdown = false
                },
                enabled = canSelectMomentum,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )

            // Fastrade CTC Mode
            val canSelectCTC = botState == BotState.STOPPED && !isFollowModeActive && !isIndicatorModeActive
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Fastrade CTC Mode",
                            color = if (canSelectCTC) colors.textPrimary else colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentMode == TradingMode.CTC_ORDER) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    if (canSelectCTC) onModeChange(TradingMode.CTC_ORDER)
                    showModeDropdown = false
                },
                enabled = canSelectCTC,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}

private fun getModeDisplayName(mode: TradingMode): String {
    return when (mode) {
        TradingMode.SCHEDULE -> "Signal Mode"
        TradingMode.FOLLOW_ORDER -> "Fastrade FTT Mode"
        TradingMode.INDICATOR_ORDER -> "Analysis Strategy Mode"
        TradingMode.CTC_ORDER -> "Fastrade CTC Mode"
    }
}

@Composable
private fun CTCContent(
    isActive: Boolean,
    ctcOrders: List<CTCOrder>,
    canModify: Boolean,
    onStartCTC: () -> Unit,
    onStopCTC: () -> Unit,
    colors: DashboardColors
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) colors.successColor.copy(alpha = 0.15f) else colors.surface
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, if (isActive) colors.successColor.copy(alpha = 0.4f) else colors.borderColor)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isActive) colors.wifiGreen else Color(0xFFFF6B35),
                                    CircleShape
                                )
                        )
                        Text(
                            text = "CTC Ultra-Fast",
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = if (isActive) colors.successColor.copy(alpha = 0.18f)
                        else colors.errorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "INACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) colors.accentProfit else colors.accentSecondary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isActive && ctcOrders.isNotEmpty()) {
                    Divider(color = colors.borderColor.copy(alpha = 0.6f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Orders: ${ctcOrders.size}",
                            fontSize = 8.sp,
                            color = colors.textSecondary
                        )
                    }

                    val pending = ctcOrders.count { !it.isExecuted }
                    if (pending > 0) {
                        Text(
                            text = "Pending: $pending",
                            fontSize = 8.sp,
                            color = colors.warningColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isActive) {
                    Text(
                        text = "Ready for ultra-fast execution",
                        fontSize = 8.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartCTC,
                enabled = !isActive && canModify,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35),  // Orange untuk CTC
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Start", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onStopCTC,
                enabled = isActive,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun IndicatorContent(
    isActive: Boolean,
    indicatorOrders: List<IndicatorOrder>,
    indicatorSettings: IndicatorSettings,
    indicatorOrderStatus: String,
    canModify: Boolean,
    onStartIndicator: () -> Unit,
    onStopIndicator: () -> Unit,
    onShowSettings: () -> Unit,
    colors: DashboardColors
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) colors.successColor.copy(alpha = 0.15f) else colors.surface
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, if (isActive) colors.successColor.copy(alpha = 0.4f) else colors.borderColor)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isActive) colors.wifiGreen else colors.accentWarning,
                                    CircleShape
                                )
                        )
                        Text(
                            text = "Indicator Setup",
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = if (isActive) colors.successColor.copy(alpha = 0.18f)
                        else colors.errorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "INACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) colors.accentProfit else colors.accentSecondary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = indicatorSettings.getDisplayName(),
                        fontSize = 8.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isActive && indicatorOrders.isNotEmpty()) {
                    Divider(color = colors.borderColor.copy(alpha = 0.6f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Orders: ${indicatorOrders.size}",
                            fontSize = 8.sp,
                            color = colors.textSecondary
                        )
                    }

                    val pending = indicatorOrders.count { !it.isExecuted && !it.isSkipped }
                    if (pending > 0) {
                        Text(
                            text = "Pending: $pending",
                            fontSize = 8.sp,
                            color = colors.warningColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isActive) {
                    Text(
                        text = "Ready for analysis",
                        fontSize = 8.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = indicatorOrderStatus.take(40) + if (indicatorOrderStatus.length > 40) "..." else "",
                        fontSize = 7.sp,
                        color = colors.accentProfit,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 9.sp
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartIndicator,
                enabled = !isActive && canModify,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.successColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Start", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onStopIndicator,
                enabled = isActive,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowSettings,
                    enabled = canModify,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (canModify) colors.successColor else colors.borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (canModify) colors.accentProfit else colors.textMuted,
                        disabledContentColor = colors.textMuted
                    )
                ) {
                    Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun FollowContent(
    isActive: Boolean,
    followOrders: List<FollowOrder>,
    canModify: Boolean,
    onStartFollow: () -> Unit,
    onStopFollow: () -> Unit,
    colors: DashboardColors
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) colors.successColor.copy(alpha = 0.15f) else colors.surface
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, if (isActive) colors.successColor.copy(alpha = 0.5f) else colors.borderColor)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isActive) colors.wifiGreen else colors.accentProfit,
                                    CircleShape
                                )
                        )
                        Text(
                            text = "Follow Candle",
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = if (isActive) colors.successColor.copy(alpha = 0.18f)
                        else colors.errorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "INACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) colors.accentProfit else colors.accentSecondary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isActive && followOrders.isNotEmpty()) {
                    Divider(color = colors.borderColor.copy(alpha = 0.6f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Orders: ${followOrders.size}",
                            fontSize = 8.sp,
                            maxLines = 1,
                            color = colors.textSecondary
                        )
                    }

                    val pending = followOrders.count { !it.isExecuted }
                    if (pending > 0) {
                        Text(
                            text = "Pending: $pending",
                            fontSize = 8.sp,
                            color = colors.warningColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isActive) {
                    Text(
                        text = "Ready for signals",
                        fontSize = 8.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartFollow,
                enabled = !isActive && canModify,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.successColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Start", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onStopFollow,
                enabled = isActive,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.controls,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    scheduleInput: String,
    scheduledOrders: List<ScheduledOrder>,
    canModify: Boolean,
    canStartBot: Boolean,
    botState: BotState,
    onScheduleInputChange: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onViewSchedules: () -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onShowMultilineDialog: () -> Unit,
    colors: DashboardColors,
    onRemoveOrder: (String) -> Unit,
) {
    val isBotRunning = botState == BotState.RUNNING

    // âœ… State untuk menyimpan ID order yang sudah dihide
    var hiddenOrderIds by remember { mutableStateOf(setOf<String>()) }

    val sortedOrders = remember(scheduledOrders) {
        scheduledOrders.sortedBy { it.timeInMillis }
    }

    fun isOrderCompleted(order: ScheduledOrder): Boolean {
        if (order.isExecuted && !order.result.isNullOrEmpty()) {
            val result = order.result.uppercase().trim()
            if (result in listOf("WIN", "LOSE", "LOSS", "DRAW", "MENANG", "KALAH")) {
                return true
            }
        }

        if (order.martingaleState.isCompleted) {
            val finalResult = order.martingaleState.finalResult?.uppercase()?.trim()
            if (!finalResult.isNullOrEmpty() &&
                finalResult in listOf("WIN", "MENANG", "LOSE", "LOSS", "KALAH")) {
                return true
            }
        }

        return false
    }

    val currentRunningOrder = remember(sortedOrders, hiddenOrderIds) {
        sortedOrders.firstOrNull {
            !it.isExecuted &&
                    !it.isSkipped &&
                    !hiddenOrderIds.contains(it.id)
        }
    }

    val currentRunningOrderId = currentRunningOrder?.id

    val isCurrentOrderInMonitoring = remember(sortedOrders, hiddenOrderIds) {
        sortedOrders.any { order ->
            order.isExecuted &&
                    !isOrderCompleted(order) &&
                    !hiddenOrderIds.contains(order.id)
        }
    }

    // âœ… Filter untuk DISPLAY - exclude hidden orders
    val displayedOrders = remember(sortedOrders, hiddenOrderIds) {
        sortedOrders.filter { order ->
            !order.isSkipped && !hiddenOrderIds.contains(order.id)
        }
    }

    val limitedDisplayedOrders = remember(displayedOrders) {
        displayedOrders.take(5)
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(currentRunningOrderId, limitedDisplayedOrders, isCurrentOrderInMonitoring) {
        if (currentRunningOrderId != null &&
            limitedDisplayedOrders.isNotEmpty() &&
            !isCurrentOrderInMonitoring) {

            val index = limitedDisplayedOrders.indexOfFirst { it.id == currentRunningOrderId }
            if (index != -1) {
                delay(300)
                lazyListState.animateScrollToItem(
                    index = index,
                    scrollOffset = -10
                )
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isBotRunning && sortedOrders.isNotEmpty()) {
            val allCompleted = displayedOrders.all { order ->
                isOrderCompleted(order)
            }

            if (allCompleted && displayedOrders.isEmpty()) {
                // Placeholder - semua selesai
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(119.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.borderColor.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.successColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "All orders completed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            val executedCount = scheduledOrders.count { it.isExecuted }
                            val skippedCount = scheduledOrders.count { it.isSkipped }

                            Text(
                                text = "$executedCount executed${if (skippedCount > 0) ", $skippedCount skipped" else ""}",
                                fontSize = 9.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.chartLine2.copy(alpha = 0.4f))
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = limitedDisplayedOrders,
                            key = { it.id }
                        ) { order ->
                            RunningScheduleItem(
                                order = order,
                                colors = colors,
                                isCurrentlyRunning = order.id == currentRunningOrderId && !isCurrentOrderInMonitoring,
                                isCurrentOrderInMonitoring = isCurrentOrderInMonitoring,
                                onHideCompleted = { orderId ->
                                    // âœ… Callback untuk hide order
                                    hiddenOrderIds = hiddenOrderIds + orderId
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // TextField
            OutlinedTextField(
                value = scheduleInput,
                onValueChange = onScheduleInputChange,
                enabled = canModify && !isBotRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 10.sp,
                    color = colors.textPrimary
                ),
                placeholder = {
                    Text(
                        text = "12:30 B\n12:40 S\n13:00 s\n13:45 b",
                        fontSize = 9.sp,
                        color = colors.textMuted,
                        lineHeight = 20.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.chartLine.copy(alpha = 0.4f),
                    unfocusedBorderColor = colors.borderColor.copy(alpha = 0.6f),
                    unfocusedContainerColor = colors.darkBackgroundClock,
                    focusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.chartLine.copy(alpha = 0.2f),
                    disabledTextColor = colors.textMuted,
                    disabledBorderColor = colors.borderColor.copy(alpha = 0.6f),
                    disabledContainerColor = colors.darkBackgroundClock
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4,
            )
        }

        // Tombol List
        if (scheduledOrders.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable(enabled = canModify && !isBotRunning) { onShowMultilineDialog() },
                colors = CardDefaults.cardColors(
                    containerColor = if (canModify && !isBotRunning) colors.surface2 else colors.surface2.copy(alpha = 0.8f),
                    contentColor = if (canModify && !isBotRunning) colors.accentPrimary4 else colors.textMuted
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    0.5.dp,
                    colors.accentPrimary1.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "List Schedule Input",
                            modifier = Modifier.size(14.dp),
                            tint = if (canModify && !isBotRunning) colors.accentPrimary4 else colors.textMuted
                        )
                        Text(
                            text = "List",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canModify && !isBotRunning) colors.accentPrimary4 else colors.textMuted
                        )
                    }
                }
            }
        }

        // Tombol View
        if (scheduledOrders.isNotEmpty()) {
            val pendingOrders = sortedOrders.filter { !it.isExecuted && !it.isSkipped }

            OutlinedButton(
                onClick = onViewSchedules,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary3,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.botButtonDisabledBg,
                    disabledContentColor = colors.TextPrimary1,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View (${scheduledOrders.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (isBotRunning && pendingOrders.isNotEmpty()) {
                        Surface(
                            color = colors.successColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${pendingOrders.size} active",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.successColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tombol Add
        Button(
            onClick = onAddSchedule,
            enabled = canModify && scheduleInput.isNotBlank() && !isBotRunning,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.successColor,
                contentColor = colors.TextPrimary1,
                disabledContainerColor = colors.botButtonDisabledBg,
                disabledContentColor = colors.textMuted
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                0.8.dp,
                colors.borderColor.copy(alpha = 0.6f)
            )
        ) {
            Text("Add", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Tombol Start/Stop
        if (!isBotRunning) {
            Button(
                onClick = onStartBot,
                enabled = canStartBot,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorColor,
                    contentColor = colors.TextPrimary1,
                    disabledContainerColor = colors.botButtonDisabledBg,
                    disabledContentColor = colors.textMuted,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Start", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onStopBot,
                enabled = true,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorColor,
                    contentColor = colors.TextPrimary1,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    0.8.dp,
                    colors.borderColor.copy(alpha = 0.6f)
                )
            ) {
                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RunningScheduleItem(
    order: ScheduledOrder,
    colors: DashboardColors,
    isCurrentlyRunning: Boolean,
    isCurrentOrderInMonitoring: Boolean,
    onHideCompleted: (String) -> Unit
) {
    val isCompleted = order.isExecuted &&
            order.martingaleState.isCompleted &&
            !order.martingaleState.finalResult.isNullOrEmpty()

    // ✅ State untuk tracking kapan order menjadi completed
    var completedTimestamp by remember { mutableStateOf<Long?>(null) }

    // ✅ Track perubahan status completed
    LaunchedEffect(isCompleted) {
        if (isCompleted && completedTimestamp == null) {
            // Set timestamp saat pertama kali completed
            completedTimestamp = System.currentTimeMillis()
            println("⏱️ Order ${order.id} completed at ${completedTimestamp}")
        }
    }

    // ✅ Auto-hide setelah 1 menit dari completed timestamp
    LaunchedEffect(completedTimestamp) {
        completedTimestamp?.let { timestamp ->
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - timestamp

            if (elapsedTime >= 60000L) {
                // Sudah lewat 1 menit, langsung hide
                println("⏱️ Order ${order.id} elapsed time already > 1 min, hiding immediately")
                onHideCompleted(order.id)
            } else {
                // Belum 1 menit, tunggu sisa waktu
                val remainingTime = 60000L - elapsedTime
                println("⏱️ Order ${order.id} waiting ${remainingTime}ms before hiding")
                delay(remainingTime)
                println("⏱️ Order ${order.id} hiding after ${remainingTime}ms delay")
                onHideCompleted(order.id)
            }
        }
    }

    val statusText = when {
        isCompleted -> {
            val result = order.martingaleState.finalResult ?: ""
            val step = order.martingaleState.currentStep

            when (result.uppercase()) {
                "WIN", "MENANG" -> if (step > 1) "WIN" else "WIN"
                "LOSE", "LOSS", "KALAH" -> if (step > 1) "LOSS" else "LOSS"
                else -> "Completed"
            }
        }
        order.martingaleState.isActive -> {
            val step = order.martingaleState.currentStep
            "Step $step"
        }
        order.isExecuted && isCurrentOrderInMonitoring -> "Monitoring"
        order.isExecuted -> "Monitoring"
        isCurrentlyRunning -> "Pending..."
        else -> ""
    }

    val statusColor = when {
        isCompleted -> {
            when (order.martingaleState.finalResult?.uppercase()) {
                "WIN", "MENANG" -> colors.successColor
                "LOSE", "LOSS", "KALAH" -> colors.errorColor
                else -> colors.textMuted
            }
        }
        order.martingaleState.isActive -> colors.warningColor
        order.isExecuted && isCurrentOrderInMonitoring -> colors.accentWarning
        isCurrentlyRunning -> colors.errorColor
        else -> colors.textMuted
    }

    val cardBorderColor = when {
        isCompleted -> {
            when (order.martingaleState.finalResult?.uppercase()) {
                "WIN", "MENANG" -> colors.successColor.copy(alpha = 0.4f)
                "LOSE", "LOSS", "KALAH" -> colors.errorColor.copy(alpha = 0.4f)
                else -> colors.borderColor.copy(alpha = 0.4f)
            }
        }
        order.martingaleState.isActive -> colors.warningColor.copy(alpha = 0.4f)
        order.isExecuted && isCurrentOrderInMonitoring -> colors.accentWarning.copy(alpha = 0.4f)
        isCurrentlyRunning -> colors.errorColor.copy(alpha = 0.4f)
        else -> colors.borderColor.copy(alpha = 0.4f)
    }

    val shouldShowStatus = statusText.isNotEmpty()
    val cardHeight = if (shouldShowStatus) 40.dp else 28.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        colors = CardDefaults.cardColors(
            containerColor = colors.darkBackground
        ),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = if (shouldShowStatus)
                Arrangement.Center
            else
                Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.time,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    lineHeight = 11.sp
                )

                Spacer(modifier = Modifier.width(3.dp))

                val displayTrend = when (order.trend.uppercase()) {
                    "CALL", "B" -> "B"
                    "PUT", "S" -> "S"
                    else -> order.trend.uppercase().take(1)
                }

                val trendColor = if (displayTrend == "B") {
                    Color(0xFF14CA75)
                } else {
                    Color(0xFFE2605D)
                }

                Surface(
                    color = trendColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = displayTrend,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = trendColor,
                        letterSpacing = 0.2.sp,
                        lineHeight = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            if (shouldShowStatus) {
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 0.15.sp,
                    lineHeight = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Helper data class tetap sama
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun IndicatorSettingsDialog(
    indicatorSettings: IndicatorSettings,
    consecutiveLossSettings: ConsecutiveLossSettings,
    canModify: Boolean,
    onIndicatorTypeChange: (IndicatorType) -> Unit,
    onIndicatorPeriodChange: (Int) -> Unit,
    onRSILevelsChange: (BigDecimal, BigDecimal) -> Unit,
    onSensitivityChange: (BigDecimal) -> Unit,
    onConsecutiveLossChange: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
    colors: DashboardColors
) {
    var selectedType by remember(indicatorSettings.type) { mutableStateOf(indicatorSettings.type) }
    var periodInput by remember(indicatorSettings.period) { mutableStateOf(indicatorSettings.period.toString()) }
    var sensitivityInput by remember(indicatorSettings.sensitivity) { mutableStateOf(indicatorSettings.sensitivity.toPlainString()) }
    var overboughtInput by remember(indicatorSettings.rsiOverbought) { mutableStateOf(indicatorSettings.rsiOverbought.toPlainString()) }
    var oversoldInput by remember(indicatorSettings.rsiOversold) { mutableStateOf(indicatorSettings.rsiOversold.toPlainString()) }
    var consecutiveLossEnabled by remember(consecutiveLossSettings.isEnabled) { mutableStateOf(consecutiveLossSettings.isEnabled) }
    var maxLossesInput by remember(consecutiveLossSettings.maxConsecutiveLosses) { mutableStateOf(consecutiveLossSettings.maxConsecutiveLosses.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = colors.background
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.borderColor.copy(alpha = 0.5f),
                        colors.borderColor.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.surface,
                                    colors.cardBackground,
                                    colors.background
                                )
                            )
                        ),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.75f)
                        ) {
                            Text(
                                text = "Indicator Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.01).sp
                            )
                            Text(
                                text = "Configure technical indicators with BigDecimal precision",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                letterSpacing = 0.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Current Settings Display
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF9C27B0).copy(alpha = 0.2f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = Color(0xFF9C27B0),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Current Configuration",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF9C27B0)
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Indicator",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        Surface(
                                            color = colors.surface,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = indicatorSettings.type.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Divider(color = Color(0xFF9C27B0).copy(alpha = 0.2f), thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Period",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = "${indicatorSettings.period}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }

                                    Divider(color = Color(0xFF9C27B0).copy(alpha = 0.2f), thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sensitivity",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = "${indicatorSettings.sensitivity.toPlainString()} (${indicatorSettings.getSensitivityDisplayText()})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }

                                    if (indicatorSettings.type == IndicatorType.RSI) {
                                        Divider(color = Color(0xFF9C27B0).copy(alpha = 0.2f), thickness = 0.5.dp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "RSI Levels",
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = colors.errorColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${indicatorSettings.rsiOverbought.toPlainString()}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.errorColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "/",
                                                    fontSize = 10.sp,
                                                    color = colors.textMuted
                                                )
                                                Surface(
                                                    color = colors.successColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${indicatorSettings.rsiOversold.toPlainString()}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.successColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Divider(color = Color(0xFF9C27B0).copy(alpha = 0.2f), thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Consecutive Loss Limit",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (consecutiveLossSettings.isEnabled) {
                                                Surface(
                                                    color = colors.warningColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${consecutiveLossSettings.maxConsecutiveLosses}x",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.warningColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = if (consecutiveLossSettings.isEnabled)
                                                    colors.successColor.copy(alpha = 0.2f)
                                                else
                                                    colors.textMuted.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (consecutiveLossSettings.isEnabled) "ON" else "OFF",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (consecutiveLossSettings.isEnabled)
                                                        colors.successColor
                                                    else
                                                        colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Indicator Type Selection
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Indicator Type",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(IndicatorType.values()) { type ->
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (type == selectedType)
                                            Color(0xFF9C27B0) else colors.surface,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (type == selectedType)
                                                Color(0xFFBA68C8) else colors.borderColor
                                        ),
                                        onClick = {
                                            selectedType = type
                                            onIndicatorTypeChange(type)
                                        },
                                        enabled = canModify
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = type.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (type == selectedType)
                                                        Color.White else colors.textSecondary
                                                )
                                                Text(
                                                    text = when (type) {
                                                        IndicatorType.SMA -> "Simple MA"
                                                        IndicatorType.EMA -> "Exponential MA"
                                                        IndicatorType.RSI -> "Momentum"
                                                    },
                                                    fontSize = 9.sp,
                                                    color = if (type == selectedType)
                                                        Color.White.copy(alpha = 0.7f) else colors.textMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Period Input
                    item {
                        OutlinedTextField(
                            value = periodInput,
                            onValueChange = { input ->
                                if (canModify) {
                                    periodInput = input
                                    input.toIntOrNull()?.let { period ->
                                        if (period in 2..200) {
                                            onIndicatorPeriodChange(period)
                                        }
                                    }
                                }
                            },
                            label = { Text("Period (2-200)", fontSize = 11.sp, color = colors.textSecondary) },
                            enabled = canModify,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9C27B0),
                                unfocusedBorderColor = colors.borderColor,
                                disabledBorderColor = colors.borderColor,
                                focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                                unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
                                disabledContainerColor = colors.surface.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF9C27B0),
                                focusedLabelColor = Color(0xFF9C27B0),
                                unfocusedLabelColor = colors.textSecondary,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                disabledTextColor = colors.textMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    // RSI Levels (only for RSI type)
                    if (selectedType == IndicatorType.RSI) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "RSI Levels (BigDecimal Precision)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = overboughtInput,
                                        onValueChange = { input ->
                                            if (canModify) {
                                                overboughtInput = input
                                                try {
                                                    val overbought = BigDecimal(input)
                                                    val oversold = BigDecimal(oversoldInput)
                                                    if (overbought > oversold &&
                                                        overbought >= IndicatorSettings.RSI_MIN_OVERBOUGHT &&
                                                        overbought <= IndicatorSettings.RSI_MAX_OVERBOUGHT) {
                                                        onRSILevelsChange(overbought, oversold)
                                                    }
                                                } catch (e: NumberFormatException) {
                                                }
                                            }
                                        },
                                        label = { Text("Overbought (50-95)", fontSize = 10.sp, color = colors.textSecondary) },
                                        enabled = canModify,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.errorColor,
                                            unfocusedBorderColor = colors.borderColor,
                                            disabledBorderColor = colors.borderColor,
                                            focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                                            unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
                                            disabledContainerColor = colors.surface.copy(alpha = 0.2f),
                                            cursorColor = colors.errorColor,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary,
                                            disabledTextColor = colors.textMuted
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 12.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        )
                                    )

                                    OutlinedTextField(
                                        value = oversoldInput,
                                        onValueChange = { input ->
                                            if (canModify) {
                                                oversoldInput = input
                                                try {
                                                    val overbought = BigDecimal(overboughtInput)
                                                    val oversold = BigDecimal(input)
                                                    if (overbought > oversold &&
                                                        oversold >= IndicatorSettings.RSI_MIN_OVERSOLD &&
                                                        oversold <= IndicatorSettings.RSI_MAX_OVERSOLD) {
                                                        onRSILevelsChange(overbought, oversold)
                                                    }
                                                } catch (e: NumberFormatException) {
                                                }
                                            }
                                        },
                                        label = { Text("Oversold (5-50)", fontSize = 10.sp, color = colors.textSecondary) },
                                        enabled = canModify,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.successColor,
                                            unfocusedBorderColor = colors.borderColor,
                                            disabledBorderColor = colors.borderColor,
                                            focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                                            unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
                                            disabledContainerColor = colors.surface.copy(alpha = 0.2f),
                                            cursorColor = colors.successColor,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary,
                                            disabledTextColor = colors.textMuted
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 12.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Sensitivity Input
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sensitivity: ${indicatorSettings.getSensitivityDisplayText()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )

                            OutlinedTextField(
                                value = sensitivityInput,
                                onValueChange = { input ->
                                    if (canModify) {
                                        sensitivityInput = input
                                        try {
                                            val sensitivity = BigDecimal(input)
                                            if (sensitivity >= IndicatorSettings.SENSITIVITY_MIN &&
                                                sensitivity <= IndicatorSettings.SENSITIVITY_MAX) {
                                                onSensitivityChange(sensitivity)
                                            }
                                        } catch (e: NumberFormatException) {
                                        }
                                    }
                                },
                                label = { Text("Sensitivity (0.1-2.0)", fontSize = 11.sp, color = colors.textSecondary) },
                                enabled = canModify,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accentPrimary,
                                    unfocusedBorderColor = colors.borderColor,
                                    disabledBorderColor = colors.borderColor,
                                    focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
                                    disabledContainerColor = colors.surface.copy(alpha = 0.2f),
                                    cursorColor = colors.accentPrimary,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    disabledTextColor = colors.textMuted
                                ),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                )
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(IndicatorUtils.getSensitivityPresets()) { (value, label) ->
                                    val isSelected = indicatorSettings.sensitivity.compareTo(value) == 0

                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected)
                                            colors.accentPrimary.copy(alpha = 0.2f) else colors.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected)
                                                colors.accentPrimary else colors.borderColor
                                        ),
                                        onClick = {
                                            if (canModify) {
                                                onSensitivityChange(value)
                                                sensitivityInput = value.toPlainString()
                                            }
                                        },
                                        enabled = canModify
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected)
                                                    colors.accentPrimary else colors.textSecondary
                                            )
                                            Text(
                                                text = value.toPlainString(),
                                                fontSize = 8.sp,
                                                color = if (isSelected)
                                                    colors.accentPrimary.copy(alpha = 0.8f) else colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.accentPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Sensitivity Control (Full Range):",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accentPrimary
                                    )
                                    Text(
                                        text = "• Ultra Low (0.01): Very loose triggers, maximum trades\n• Low (0.1): Loose triggers, more trades\n• Medium (0.5): Balanced sensitivity\n• High (1.0+): Tight triggers, fewer precise trades\n• Custom: Any value >0 for ultimate flexibility",
                                        fontSize = 9.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Consecutive Loss Limit
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consecutive Loss Limit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )

                                Switch(
                                    checked = consecutiveLossEnabled,
                                    onCheckedChange = { enabled ->
                                        if (canModify) {
                                            consecutiveLossEnabled = enabled
                                            val maxLosses = maxLossesInput.toIntOrNull() ?: 5
                                            onConsecutiveLossChange(enabled, maxLosses)
                                        }
                                    },
                                    enabled = canModify,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.warningColor,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = colors.surface,
                                        disabledCheckedThumbColor = colors.textMuted,
                                        disabledCheckedTrackColor = colors.surface,
                                        disabledUncheckedThumbColor = colors.textMuted.copy(alpha = 0.5f),
                                        disabledUncheckedTrackColor = colors.surface.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            if (consecutiveLossEnabled) {
                                OutlinedTextField(
                                    value = maxLossesInput,
                                    onValueChange = { input ->
                                        if (canModify) {
                                            maxLossesInput = input
                                            input.toIntOrNull()?.let { maxLosses ->
                                                if (maxLosses in 1..20) {
                                                    onConsecutiveLossChange(true, maxLosses)
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("Max Consecutive Losses (1-20)", fontSize = 11.sp, color = colors.textSecondary) },
                                    enabled = canModify && consecutiveLossEnabled,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.warningColor,
                                        unfocusedBorderColor = colors.borderColor,
                                        disabledBorderColor = colors.borderColor,
                                        focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                                        unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
                                        disabledContainerColor = colors.surface.copy(alpha = 0.2f),
                                        cursorColor = colors.warningColor,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary,
                                        disabledTextColor = colors.textMuted
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }
                        }
                    }

                    // Information Section
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = CircleShape,
                                        color = colors.accentPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = colors.accentPrimary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "BigDecimal Precision: ${selectedType.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                }

                                Text(
                                    text = when (selectedType) {
                                        IndicatorType.SMA -> "Simple Moving Average with BigDecimal precision calculates the exact average price over the specified period. Sensitivity controls how close price must be to support/resistance levels to trigger trades."
                                        IndicatorType.EMA -> "Exponential Moving Average with BigDecimal precision gives more weight to recent prices. Sensitivity affects how tight the prediction triggers are - lower values create more trades, higher values create more precise but fewer trades."
                                        IndicatorType.RSI -> "Relative Strength Index with BigDecimal precision measures price momentum from 0-100. All RSI thresholds use exact decimal values. Sensitivity adjusts prediction tolerance for more or less frequent trade triggers."
                                    },
                                    fontSize = 10.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 14.sp
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.successColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.successColor.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = colors.successColor.copy(alpha = 0.2f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "✓",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.successColor
                                                )
                                            }
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "BigDecimal Precision Mode",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.successColor
                                            )
                                            Text(
                                                text = "All calculations use exact decimal arithmetic for maximum precision in price predictions and sensitivity adjustments. No floating-point rounding errors.",
                                                fontSize = 9.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.errorColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.errorColor.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = colors.errorColor.copy(alpha = 0.2f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = colors.errorColor,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Risk Warning",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.errorColor
                                            )
                                            Text(
                                                text = "High precision calculations do not guarantee market prediction accuracy. Technical indicators are probabilistic tools, not certainties. Use appropriate risk management.",
                                                fontSize = 9.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

enum class TradingModeSelectable {
    NONE,
    SCHEDULE,
    FOLLOW_ORDER,
    INDICATOR_ORDER,
    CTC_ORDER
}

@Composable
fun TradingModeCard(
    modifier: Modifier = Modifier,
    scheduleInput: String,
    scheduledOrders: List<ScheduledOrder>,
    canModify: Boolean,
    botState: BotState,
    canStartBot: Boolean,
    onScheduleInputChange: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onViewSchedules: () -> Unit,
    onClearAll: () -> Unit,
    onRemoveOrder: (String) -> Unit,  // ✅ TAMBAHKAN PARAMETER INI
    onStartBot: () -> Unit,
    isFollowModeActive: Boolean,
    followOrders: List<FollowOrder>,
    martingaleSettings: MartingaleState,
    onStartFollow: () -> Unit,
    onStopFollow: () -> Unit,
    currentMode: TradingMode,
    onModeChange: (TradingMode) -> Unit,
    isIndicatorModeActive: Boolean,
    indicatorOrders: List<IndicatorOrder>,
    indicatorSettings: IndicatorSettings,
    indicatorOrderStatus: String,
    onStartIndicator: () -> Unit,
    onStopIndicator: () -> Unit,
    onShowIndicatorSettings: () -> Unit,
    isCTCModeActive: Boolean,
    ctcOrders: List<CTCOrder>,
    onStartCTC: () -> Unit,
    onStopCTC: () -> Unit,
    colors: DashboardColors,
    currentLanguage: String = "id",
    isTradingModeSelected: Boolean = false,
    onStopBot: () -> Unit,
) {
    // State untuk mengatur dialog mana yang terbuka
    var showMultilineDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.45f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        border = BorderStroke(0.6.dp, colors.chartLine.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TradingModeSelector(
                currentMode = currentMode,
                onModeChange = { mode ->
                    onModeChange(mode)
                },
                canModify = canModify,
                isFollowModeActive = isFollowModeActive,
                isIndicatorModeActive = isIndicatorModeActive,
                isCTCModeActive = isCTCModeActive,
                botState = botState,
                colors = colors,
                currentLanguage = currentLanguage,
                isModeSelected = isTradingModeSelected
            )

            if (isTradingModeSelected) {
                when (currentMode) {
                    TradingMode.SCHEDULE -> {
                        ScheduleContent(
                            scheduleInput = scheduleInput,
                            scheduledOrders = scheduledOrders,
                            canModify = canModify,
                            canStartBot = canStartBot,
                            botState = botState,
                            onScheduleInputChange = onScheduleInputChange,
                            onAddSchedule = onAddSchedule,
                            onViewSchedules = { showViewDialog = true },
                            onStartBot = onStartBot,
                            onStopBot = onStopBot,
                            onShowMultilineDialog = { showMultilineDialog = true },
                            onRemoveOrder = onRemoveOrder,  // ✅ PASS CALLBACK
                            colors = colors
                        )
                    }
                    TradingMode.FOLLOW_ORDER -> {
                        FollowContent(
                            isActive = isFollowModeActive,
                            followOrders = followOrders,
                            canModify = canModify,
                            onStartFollow = onStartFollow,
                            onStopFollow = onStopFollow,
                            colors = colors
                        )
                    }
                    TradingMode.INDICATOR_ORDER -> {
                        IndicatorContent(
                            isActive = isIndicatorModeActive,
                            indicatorOrders = indicatorOrders,
                            indicatorSettings = indicatorSettings,
                            indicatorOrderStatus = indicatorOrderStatus,
                            canModify = canModify,
                            onStartIndicator = onStartIndicator,
                            onStopIndicator = onStopIndicator,
                            onShowSettings = onShowIndicatorSettings,
                            colors = colors
                        )
                    }
                    TradingMode.CTC_ORDER -> {
                        CTCContent(
                            isActive = isCTCModeActive,
                            ctcOrders = ctcOrders,
                            canModify = canModify,
                            onStartCTC = onStartCTC,
                            onStopCTC = onStopCTC,
                            colors = colors
                        )
                    }
                }
            } else {
                // Placeholder ketika mode belum dipilih
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = StringsManager.getSelectTradingMode(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = StringsManager.getAdjustToYourTradingStyle(currentLanguage),
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // ✅ Dialog View dengan callback remove individual
    if (showViewDialog && currentMode == TradingMode.SCHEDULE) {
        ScheduleListDialog(
            scheduledOrders = scheduledOrders,
            onRemoveOrder = onRemoveOrder,  // ✅ GUNAKAN CALLBACK
            onClearAll = {
                onClearAll()
                showViewDialog = false
                showMultilineDialog = true
            },
            onDismiss = { showViewDialog = false },
            onShowInputSignal = {
                showViewDialog = false
                showMultilineDialog = true
            },
            colors = colors
        )
    }

    // Dialog Bulk Input dengan callback ke View setelah Add
    if (showMultilineDialog && currentMode == TradingMode.SCHEDULE) {
        MultilineScheduleDialog(
            currentInput = scheduleInput,
            onInputChange = onScheduleInputChange,
            onAdd = {
                onAddSchedule()
                showMultilineDialog = false
                showViewDialog = true
            },
            onDismiss = { showMultilineDialog = false },
            colors = colors,
            scheduledOrders = scheduledOrders
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFBAC1CB)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun BotStatusIndicatorSimple(
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isRunning) Color(0xFF00FF00) else Color(0xFFFF0000) // hijau / merah
    Box(
        modifier = modifier
            .size(14.dp)
            .background(color = color, shape = CircleShape)
            .shadow(2.dp, CircleShape)
    )
}

private fun formatAmount(amount: Long): String {
    return formatIndonesianCurrencyNew(amount)
}

private fun formatToIndonesianCurrencyWithoutDecimal(amount: Long): String {
    return formatAmount(amount)
}

fun formatIndonesianCurrencyNew(serverAmount: Long): String {
    val amountInRupiah = serverAmount / 100
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amountInRupiah)
}

private fun formatCompactProfit(amount: Long): String {
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
    })

    val profitInRupiah = kotlin.math.abs(amount) / 100.0

    return formatter.format(profitInRupiah.toLong())
}


private fun formatTodayProfitDisplay(amount: Long): String {
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
    })

    val profitInRupiah = kotlin.math.abs(amount) / 100.0
    return formatter.format(profitInRupiah.toLong())
}


private fun isTradeCompletedForStats(trade: TradingHistoryNew): Boolean {
    val status = trade.status.lowercase().trim()

    val completedStatuses = setOf(
        "won", "win",
        "lost", "lose", "loss", "failed",
        "stand", "draw", "tie", "draw_trade"
    )

    val runningStatuses = setOf(
        "opened", "open", "pending", "running",
        "active", "executing", "processing"
    )

    return when {
        completedStatuses.contains(status) -> true
        runningStatuses.contains(status) -> false
        else -> {
            println("   Stats: Unknown status '$status' for trade ${trade.uuid}")
            false
        }
    }
}




private fun isTradeCompletedSync(trade: TradingHistoryNew): Boolean {
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
                return false
            }
            true
        }
        runningStatuses.contains(status) -> {
            false
        }
        else -> {
            println("Trade ${trade.id}: Unknown status '$status' - excluding from calculation")
            false
        }
    }
}

private fun getQuickAmountsForCurrency(currency: CurrencyType): List<Pair<Long, String>> {
    return when (currency) {
        CurrencyType.IDR -> listOf(
            20000000L to "200K",
            50000000L to "500K",
            100000000L to "1JT",
            200000000L to "2JT",
            500000000L to "5JT"
        )
        CurrencyType.USD -> listOf(
            1000L to "$10",
            2000L to "$20",
            5000L to "$50",
            10000L to "$100",
            20000L to "$200"
        )
        CurrencyType.EUR -> listOf(
            950L to "€10",
            1900L to "€20",
            4750L to "€50",
            9500L to "€100",
            19000L to "€200"
        )
        CurrencyType.GBP -> listOf(
            800L to "£10",
            1600L to "£20",
            4000L to "£50",
            8000L to "£100",
            16000L to "£200"
        )
        CurrencyType.JPY -> listOf(
            150000L to "¥1.5K",
            300000L to "¥3K",
            750000L to "¥7.5K",
            1500000L to "¥15K",
            3000000L to "¥30K"
        )
        CurrencyType.AUD -> listOf(
            1500L to "A$15",
            3000L to "A$30",
            7500L to "A$75",
            15000L to "A$150",
            30000L to "A$300"
        )
        CurrencyType.CAD -> listOf(
            1350L to "C$13.5",
            2700L to "C$27",
            6750L to "C$67.5",
            13500L to "C$135",
            27000L to "C$270"
        )
        CurrencyType.SGD -> listOf(
            1350L to "S$13.5",
            2700L to "S$27",
            6750L to "S$67.5",
            13500L to "S$135",
            27000L to "S$270"
        )
        CurrencyType.MYR -> listOf(
            4650L to "RM46.5",
            9300L to "RM93",
            23250L to "RM232.5",
            46500L to "RM465",
            93000L to "RM930"
        )
        CurrencyType.THB -> listOf(
            35000L to "฿350",
            70000L to "฿700",
            175000L to "฿1.75K",
            350000L to "฿3.5K",
            700000L to "฿7K"
        )
        CurrencyType.PHP -> listOf(
            56000L to "₱560",
            112000L to "₱1.1K",
            280000L to "₱2.8K",
            560000L to "₱5.6K",
            1120000L to "₱11.2K"
        )
        CurrencyType.VND -> listOf(
            24500000L to "₫245K",
            49000000L to "₫490K",
            122500000L to "₫1.2M",
            245000000L to "₫2.5M",
            490000000L to "₫4.9M"
        )
        CurrencyType.KRW -> listOf(
            1330000L to "₩13.3K",
            2660000L to "₩26.6K",
            6650000L to "₩66.5K",
            13300000L to "₩133K",
            26600000L to "₩266K"
        )
        CurrencyType.CNY -> listOf(
            7250L to "¥72.5",
            14500L to "¥145",
            36250L to "¥362.5",
            72500L to "¥725",
            145000L to "¥1.45K"
        )
        CurrencyType.HKD -> listOf(
            7800L to "HK$78",
            15600L to "HK$156",
            39000L to "HK$390",
            78000L to "HK$780",
            156000L to "HK$1.56K"
        )
        CurrencyType.TWD -> listOf(
            31500L to "NT$315",
            63000L to "NT$630",
            157500L to "NT$1.57K",
            315000L to "NT$3.15K",
            630000L to "NT$6.3K"
        )
        CurrencyType.INR -> listOf(
            83000L to "₹830",
            166000L to "₹1.66K",
            415000L to "₹4.15K",
            830000L to "₹8.3K",
            1660000L to "₹16.6K"
        )
        CurrencyType.PKR -> listOf(
            280000L to "₨2.8K",
            560000L to "₨5.6K",
            1400000L to "₨14K",
            2800000L to "₨28K",
            5600000L to "₨56K"
        )
        CurrencyType.BDT -> listOf(
            110000L to "৳1.1K",
            220000L to "৳2.2K",
            550000L to "৳5.5K",
            1100000L to "৳11K",
            2200000L to "৳22K"
        )
        CurrencyType.LKR -> listOf(
            325000L to "Rs3.25K",
            650000L to "Rs6.5K",
            1625000L to "Rs16.25K",
            3250000L to "Rs32.5K",
            6500000L to "Rs65K"
        )
        CurrencyType.CHF -> listOf(
            900L to "Fr9",
            1800L to "Fr18",
            4500L to "Fr45",
            9000L to "Fr90",
            18000L to "Fr180"
        )
        CurrencyType.NZD -> listOf(
            1650L to "NZ$16.5",
            3300L to "NZ$33",
            8250L to "NZ$82.5",
            16500L to "NZ$165",
            33000L to "NZ$330"
        )
        CurrencyType.MXN -> listOf(
            17000L to "Mex$170",
            34000L to "Mex$340",
            85000L to "Mex$850",
            170000L to "Mex$1.7K",
            340000L to "Mex$3.4K"
        )
        CurrencyType.BRL -> listOf(
            5000L to "R$50",
            10000L to "R$100",
            25000L to "R$250",
            50000L to "R$500",
            100000L to "R$1K"
        )
        CurrencyType.ARS -> listOf(
            350000L to "$3.5K",
            700000L to "$7K",
            1750000L to "$17.5K",
            3500000L to "$35K",
            7000000L to "$70K"
        )
        CurrencyType.CLP -> listOf(
            900000L to "$9K",
            1800000L to "$18K",
            4500000L to "$45K",
            9000000L to "$90K",
            18000000L to "$180K"
        )
        CurrencyType.COP -> listOf(
            4000000L to "$40K",
            8000000L to "$80K",
            20000000L to "$200K",
            40000000L to "$400K",
            80000000L to "$800K"
        )
        CurrencyType.AED -> listOf(
            3670L to "د.إ36.7",
            7340L to "د.إ73.4",
            18350L to "د.إ183.5",
            36700L to "د.إ367",
            73400L to "د.إ734"
        )
        CurrencyType.SAR -> listOf(
            3750L to "﷼37.5",
            7500L to "﷼75",
            18750L to "﷼187.5",
            37500L to "﷼375",
            75000L to "﷼750"
        )
        CurrencyType.TRY -> listOf(
            28000L to "₺280",
            56000L to "₺560",
            140000L to "₺1.4K",
            280000L to "₺2.8K",
            560000L to "₺5.6K"
        )
        CurrencyType.EGP -> listOf(
            31000L to "£310",
            62000L to "£620",
            155000L to "£1.55K",
            310000L to "£3.1K",
            620000L to "£6.2K"
        )
        CurrencyType.ZAR -> listOf(
            18500L to "R185",
            37000L to "R370",
            92500L to "R925",
            185000L to "R1.85K",
            370000L to "R3.7K"
        )
        CurrencyType.NGN -> listOf(
            800000L to "₦8K",
            1600000L to "₦16K",
            4000000L to "₦40K",
            8000000L to "₦80K",
            16000000L to "₦160K"
        )
        CurrencyType.RUB -> listOf(
            92000L to "₽920",
            184000L to "₽1.84K",
            460000L to "₽4.6K",
            920000L to "₽9.2K",
            1840000L to "₽18.4K"
        )
        CurrencyType.PLN -> listOf(
            4000L to "zł40",
            8000L to "zł80",
            20000L to "zł200",
            40000L to "zł400",
            80000L to "zł800"
        )
        CurrencyType.CZK -> listOf(
            23000L to "Kč230",
            46000L to "Kč460",
            115000L to "Kč1.15K",
            230000L to "Kč2.3K",
            460000L to "Kč4.6K"
        )
        CurrencyType.HUF -> listOf(
            360000L to "Ft3.6K",
            720000L to "Ft7.2K",
            1800000L to "Ft18K",
            3600000L to "Ft36K",
            7200000L to "Ft72K"
        )
        CurrencyType.SEK -> listOf(
            10500L to "kr105",
            21000L to "kr210",
            52500L to "kr525",
            105000L to "kr1.05K",
            210000L to "kr2.1K"
        )
        CurrencyType.NOK -> listOf(
            10800L to "kr108",
            21600L to "kr216",
            54000L to "kr540",
            108000L to "kr1.08K",
            216000L to "kr2.16K"
        )
        CurrencyType.DKK -> listOf(
            7000L to "kr70",
            14000L to "kr140",
            35000L to "kr350",
            70000L to "kr700",
            140000L to "kr1.4K"
        )
    }
}