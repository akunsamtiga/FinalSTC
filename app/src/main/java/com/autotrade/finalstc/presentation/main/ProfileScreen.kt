package com.autotrade.finalstc.presentation.main.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrade.finalstc.presentation.main.MainViewModel
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.local.LanguageManager
import com.autotrade.finalstc.utils.StringsManager
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autotrade.finalstc.BuildConfig
import com.autotrade.finalstc.presentation.main.history.HistoryViewModel
import com.autotrade.finalstc.presentation.main.dashboard.DashboardViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.autotrade.finalstc.presentation.login.components.LanguageSelectorDialog

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
private val StatusBlue = Color(0xFF64B5F6)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()



    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    val currentLanguage: StateFlow<String> = languageManager.currentLanguage
    val currentCountry: StateFlow<String> = languageManager.currentCountry

    init {
    }

    fun checkAdminStatus(email: String) {
        viewModelScope.launch {
            _isSuperAdmin.value = firebaseRepository.checkIsSuperAdmin(email)
            _isAdmin.value = _isSuperAdmin.value || firebaseRepository.checkIsAdmin(email)
        }
    }

    fun toggleLanguageDialog(show: Boolean) {
        _showLanguageDialog.value = show
    }

    fun updateLanguage(languageCode: String, countryCode: String) {
        viewModelScope.launch {
            languageManager.saveLanguage(languageCode, countryCode)
            _showLanguageDialog.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: MainViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val userSession = viewModel.getUserSession()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    val isAdmin by profileViewModel.isAdmin.collectAsStateWithLifecycle()
    val isSuperAdmin by profileViewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val lang by profileViewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentCountry by profileViewModel.currentCountry.collectAsStateWithLifecycle()
    val showLanguageDialog by profileViewModel.showLanguageDialog.collectAsStateWithLifecycle()

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val currentCurrency = dashboardUiState.currencySettings.selectedCurrency

    LaunchedEffect(userSession?.email) {
        userSession?.email?.let { email ->
            profileViewModel.checkAdminStatus(email)
        }
    }

    val historyList by historyViewModel.historyList.collectAsStateWithLifecycle()
    val isDemoAccount by historyViewModel.currentAccountType.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        historyViewModel.loadTradingHistory(isDemoAccount)
    }

    val totalTrades = remember(historyList, isDemoAccount) {
        historyList.count { it.isDemoAccount == isDemoAccount }
    }

    val totalPortfolioCents = remember(historyList, isDemoAccount) {
        historyList.filter { it.isDemoAccount == isDemoAccount }.sumOf { trade ->
            when (trade.status.lowercase()) {
                "won", "win" -> trade.win - trade.amount
                "lost", "lose", "loss" -> -trade.amount
                else -> 0L
            }
        }
    }

    val formattedPortfolio = remember(totalPortfolioCents) {
        val value = totalPortfolioCents / 100.0
        when {
            kotlin.math.abs(value) >= 1_000_000 -> {
                String.format("%.1fM", value / 1_000_000)
            }
            kotlin.math.abs(value) >= 10_000 -> {
                String.format("%.1fK", value / 1_000)
            }
            else -> {
                String.format("%.2f", value)
            }
        }
    }

    val activeDays = remember(historyList, isDemoAccount) {
        historyList.filter { it.isDemoAccount == isDemoAccount }
            .map { it.createdAt.substring(0, 10) }
            .distinct()
            .count()
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = lang,
            currentCountry = currentCountry,
            onDismiss = { profileViewModel.toggleLanguageDialog(false) },
            onLanguageSelected = { languageCode, countryCode ->
                profileViewModel.updateLanguage(languageCode, countryCode)
            }
        )
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
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(800))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "gradient_animation")
                    val gradientOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "gradient_offset"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        DarkSurface.copy(alpha = 0.8f + gradientOffset * 0.2f),
                                        DarkBackground.copy(alpha = 0.6f + gradientOffset * 0.3f),
                                        DarkBackground.copy(alpha = 0.9f)
                                    ),
                                    radius = 800f + gradientOffset * 200f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    StatusBlue.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                ),
                                                radius = 48f
                                            ),
                                            shape = CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .border(
                                            width = 2.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(StatusBlue, WifiGreen)
                                            ),
                                            shape = CircleShape
                                        )
                                        .background(
                                            DarkSurface.copy(alpha = 0.8f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        StatusBlue.copy(alpha = 0.2f),
                                                        WifiGreen.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text(
                                    text = extractNameFromEmail(userSession?.email ?: "User"),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = when {
                                        isSuperAdmin -> StringsManager.getSuperAdminAccount(lang)
                                        isAdmin -> StringsManager.getAdminAccount(lang)
                                        else -> StringsManager.getStockityAccount(lang)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        isSuperAdmin -> AccentWarning
                                        isAdmin -> AccentWarning
                                        else -> StatusBlue
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier,
                                    colors = CardDefaults.cardColors(
                                        containerColor = WifiGreen.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        val pulseAnimation by infiniteTransition.animateFloat(
                                            initialValue = 0.7f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1500),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "pulse"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    WifiGreen.copy(alpha = pulseAnimation),
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = StringsManager.getOnline(lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WifiGreen
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickStatCard(
                                icon = Icons.Outlined.TrendingUp,
                                value = totalTrades.toString(),
                                label = StringsManager.getTrades(lang),
                                color = WifiGreen
                            )
                            QuickStatCard(
                                icon = Icons.Outlined.Wallet,
                                value = formattedPortfolio,
                                label = StringsManager.getPortfolio(lang),
                                color = StatusBlue
                            )
                            QuickStatCard(
                                icon = Icons.Outlined.Schedule,
                                value = "${activeDays}d",
                                label = StringsManager.getActive(lang),
                                color = AccentWarning
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (isAdmin) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                                focusedElevation = 4.dp,
                                hoveredElevation = 3.dp
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
                        ) {
                            Button(
                                onClick = onNavigateToAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentWarning,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(24.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isSuperAdmin)
                                        StringsManager.getSuperAdminPanel(lang)
                                    else
                                        StringsManager.getAdminPanel(lang),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    PremiumCard(
                        title = StringsManager.getProfileInformation(lang),
                        icon = Icons.Outlined.Person,
                        iconColor = StatusBlue
                    ) {
                        userSession?.let { session ->
                            ProfileDetailItem(
                                icon = Icons.Outlined.Badge,
                                label = StringsManager.getFullName(lang),
                                value = extractNameFromEmail(session.email),
                                iconColor = StatusBlue
                            )
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ProfileDetailItem(
                                icon = Icons.Outlined.Fingerprint,
                                label = StringsManager.getUserId(lang),
                                value = session.userId,
                                iconColor = WifiGreen
                            )
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ProfileDetailItem(
                                icon = Icons.Outlined.Email,
                                label = StringsManager.getEmailAddress(lang),
                                value = session.email,
                                iconColor = AccentSecondary
                            )
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ProfileDetailItem(
                                icon = Icons.Outlined.Public,
                                label = StringsManager.getTimezone(lang),
                                value = session.userTimezone,
                                iconColor = AccentWarning
                            )
                        }
                    }

                    PremiumCard(
                        title = StringsManager.getDeviceInformation(lang),
                        icon = Icons.Outlined.Devices,
                        iconColor = WifiGreen
                    ) {
                        userSession?.let { session ->
                            ProfileDetailItem(
                                icon = Icons.Outlined.Smartphone,
                                label = StringsManager.getDeviceId(lang),
                                value = "${session.deviceId.take(8)}••••••••",
                                iconColor = StatusBlue
                            )
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ProfileDetailItem(
                                icon = Icons.Outlined.Web,
                                label = StringsManager.getBrowser(lang),
                                value = "com.android.chrome",
                                iconColor = AccentWarning
                            )
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ProfileDetailItem(
                                icon = Icons.Outlined.Security,
                                label = StringsManager.getSecurityStatus(lang),
                                value = StringsManager.getVerified(lang),
                                iconColor = WifiGreen
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp,
                            focusedElevation = 4.dp,
                            hoveredElevation = 3.dp
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
                    ) {
                        OutlinedButton(
                            onClick = { profileViewModel.toggleLanguageDialog(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = BorderStroke(0.dp, Color.Transparent),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = StatusBlue
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = when(lang) {
                                                "id" -> "Bahasa"
                                                "en" -> "Language"
                                                "es" -> "Idioma"
                                                "vi" -> "Ngôn Ngữ"
                                                "tr" -> "Dil"
                                                "hi" -> "भाषा"
                                                "ms" -> "Bahasa"
                                                else -> "Language"
                                            },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = when(lang) {
                                                "id" -> "Bahasa Indonesia"
                                                "en" -> "English"
                                                "es" -> "Español"
                                                "vi" -> "Tiếng Việt"
                                                "tr" -> "Türkçe"
                                                "hi" -> "हिन्दी"
                                                "ms" -> "Bahasa Melayu"
                                                else -> "Language"
                                            },
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (currentCountry) {
                                            "ID" -> "🇮🇩"
                                            "NG" -> "🇳🇬"
                                            "PH" -> "🇵🇭"
                                            "ZA" -> "🇿🇦"
                                            "KE" -> "🇰🇪"
                                            "GB" -> "🇬🇧"
                                            "UA" -> "🇺🇦"
                                            "MX" -> "🇲🇽"
                                            "CL" -> "🇨🇱"
                                            "CO" -> "🇨🇴"
                                            "CR" -> "🇨🇷"
                                            "DO" -> "🇩🇴"
                                            "EC" -> "🇪🇨"
                                            "SV" -> "🇸🇻"
                                            "GT" -> "🇬🇹"
                                            "HN" -> "🇭🇳"
                                            "PA" -> "🇵🇦"
                                            "PY" -> "🇵🇾"
                                            "PE" -> "🇵🇪"
                                            "UY" -> "🇺🇾"
                                            "VE" -> "🇻🇪"
                                            "BR" -> "🇧🇷"
                                            "VN" -> "🇻🇳"
                                            "LA" -> "🇱🇦"
                                            "TH" -> "🇹🇭"
                                            "TR" -> "🇹🇷"
                                            "IN" -> "🇮🇳"
                                            "MY" -> "🇲🇾"
                                            else -> "🌍"
                                        },
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp,
                            focusedElevation = 4.dp,
                            hoveredElevation = 3.dp
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(0.5.dp, AccentSecondary.copy(alpha = 0.3f))
                    ) {
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSecondary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = StringsManager.getLogoutFromAccount(lang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Automated Trading Bot v${BuildConfig.VERSION_NAME}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "© 2025 STCAutoTrade. All rights reserved.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Developer support@stcbroker.id",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = AccentWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = StringsManager.getLogoutConfirmation(lang),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = StringsManager.getAreYouSureLogout(lang),
                        color = TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = StringsManager.getNeedLoginAgain(lang),
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSecondary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        StringsManager.getYesLogout(lang),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, TextMuted),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        StringsManager.getCancel(lang),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = Modifier.size(width = 90.dp, height = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PremiumCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, Color(0xFF4A4A4A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = iconColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            content()
        }
    }
}

@Composable
private fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    iconColor.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = 0.2.sp
            )
        }
    }
}

private fun extractNameFromEmail(email: String): String {
    return email.substringBefore("@").split(".", "_", "-")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}