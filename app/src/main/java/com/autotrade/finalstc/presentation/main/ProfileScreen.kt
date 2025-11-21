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
import android.os.Build
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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog

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
private val DangerRed = Color(0xFFE53935)

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

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    val currentLanguage: StateFlow<String> = languageManager.currentLanguage
    val currentCountry: StateFlow<String> = languageManager.currentCountry

    sealed class DeleteAccountState {
        object Idle : DeleteAccountState()
        object Loading : DeleteAccountState()
        data class Success(val message: String) : DeleteAccountState()
        data class Error(val message: String) : DeleteAccountState()
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

    fun deleteAccount(userId: String, email: String) {
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading

            try {
                // Cari user di whitelist berdasarkan userId
                val user = firebaseRepository.getWhitelistUserByUserId(userId)

                if (user != null) {
                    // Hapus user dari Firestore
                    firebaseRepository.deleteWhitelistUser(user.id)
                    _deleteAccountState.value = DeleteAccountState.Success(
                        "Account deleted successfully. You will be logged out."
                    )
                } else {
                    _deleteAccountState.value = DeleteAccountState.Error(
                        "User not found in whitelist. Cannot delete account."
                    )
                }
            } catch (e: Exception) {
                _deleteAccountState.value = DeleteAccountState.Error(
                    "Failed to delete account: ${e.message}"
                )
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
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
    val context = LocalContext.current
    val userSession = viewModel.getUserSession()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    val isAdmin by profileViewModel.isAdmin.collectAsStateWithLifecycle()
    val isSuperAdmin by profileViewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val lang by profileViewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentCountry by profileViewModel.currentCountry.collectAsStateWithLifecycle()
    val showLanguageDialog by profileViewModel.showLanguageDialog.collectAsStateWithLifecycle()
    val deleteAccountState by profileViewModel.deleteAccountState.collectAsStateWithLifecycle()

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

    // Handle delete account state
    LaunchedEffect(deleteAccountState) {
        when (deleteAccountState) {
            is ProfileViewModel.DeleteAccountState.Success -> {
                delay(1500)
                viewModel.logout()
                onLogout()
            }
            else -> { /* Do nothing */ }
        }
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
                        .wrapContentHeight()
                ) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "gradient_animation")
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
                                        DarkSurface.copy(alpha = 0.9f + gradientOffset * 0.4f),
                                        DarkBackground.copy(alpha = 0.8f + gradientOffset * 0.5f),
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
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    StatusBlue.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                ),
                                                radius = 40f
                                            ),
                                            shape = CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
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
                                            .size(64.dp)
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
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = extractNameFromEmail(userSession?.email ?: "User"),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = when {
                                        isSuperAdmin -> StringsManager.getSuperAdminAccount(lang)
                                        isAdmin -> StringsManager.getAdminAccount(lang)
                                        else -> StringsManager.getStockityAccount(lang)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        isSuperAdmin -> AccentWarning
                                        isAdmin -> AccentWarning
                                        else -> StatusBlue
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = WifiGreen.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        val infiniteTransition =
                                            rememberInfiniteTransition(label = "pulse_animation")
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
                                                .size(6.dp)
                                                .background(
                                                    WifiGreen.copy(alpha = pulseAnimation),
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = StringsManager.getOnline(lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WifiGreen
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
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

                        if (isAdmin) {
                            Button(
                                onClick = onNavigateToAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentWarning,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 3.dp,
                                    pressedElevation = 6.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isSuperAdmin)
                                        StringsManager.getSuperAdminPanel(lang)
                                    else
                                        StringsManager.getAdminPanel(lang),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
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
                                label = "OS",
                                value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
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

                            // ✅ Tombol Hapus Akun dipindahkan ke sini
                            Divider(
                                color = BorderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Button(
                                onClick = { showDeleteAccountDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DangerRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 6.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when(lang) {
                                        "id" -> "Hapus Akun"
                                        "en" -> "Delete Account"
                                        "es" -> "Eliminar Cuenta"
                                        "vi" -> "Xóa Tài Khoản"
                                        "tr" -> "Hesabı Sil"
                                        "hi" -> "खाता हटाएं"
                                        "ms" -> "Padam Akaun"
                                        else -> "Delete Account"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
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
                                            else -> "🌐"
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:support@stcbroker.id")
                                            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle if no email client
                                        }
                                    }
                                    .padding(top = 2.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = "Developer ",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "support@stcbroker.id",
                                    fontSize = 11.sp,
                                    color = StatusBlue,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Logout Dialog
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

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            userEmail = userSession?.email ?: "",
            userId = userSession?.userId ?: "",
            lang = lang,
            deleteAccountState = deleteAccountState,
            onDismiss = {
                showDeleteAccountDialog = false
                profileViewModel.resetDeleteAccountState()
            },
            onConfirmDelete = { userId, email ->
                profileViewModel.deleteAccount(userId, email)
            }
        )
    }
}

// ✅ NEW: Delete Account Dialog Composable
@Composable
private fun DeleteAccountDialog(
    userEmail: String,
    userId: String,
    lang: String,
    deleteAccountState: ProfileViewModel.DeleteAccountState,
    onDismiss: () -> Unit,
    onConfirmDelete: (String, String) -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    val isDeleteEnabled = confirmationText.equals("DELETE", ignoreCase = true)

    Dialog(onDismissRequest = {
        if (deleteAccountState !is ProfileViewModel.DeleteAccountState.Loading) {
            onDismiss()
        }
    }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                DangerRed.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = when(lang) {
                            "id" -> "Hapus Akun"
                            "en" -> "Delete Account"
                            "es" -> "Eliminar Cuenta"
                            "vi" -> "Xóa Tài Khoản"
                            "tr" -> "Hesabı Sil"
                            "hi" -> "खाता हटाएं"
                            "ms" -> "Padam Akaun"
                            else -> "Delete Account"
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                }

                Divider(
                    color = BorderColor,
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Warning Message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DangerRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = when(lang) {
                                "id" -> "⚠️ Peringatan Penting"
                                "en" -> "⚠️ Important Warning"
                                "es" -> "⚠️ Advertencia Importante"
                                "vi" -> "⚠️ Cảnh Báo Quan Trọng"
                                "tr" -> "⚠️ Önemli Uyarı"
                                "hi" -> "⚠️ महत्वपूर्ण चेतावनी"
                                "ms" -> "⚠️ Amaran Penting"
                                else -> "⚠️ Important Warning"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = when(lang) {
                                "id" -> "Menghapus akun akan:\n\n" +
                                        "• Menghapus akses Anda dari aplikasi ini\n" +
                                        "• Menghapus data whitelist Anda dari database\n" +
                                        "• Memerlukan admin untuk mendaftar ulang jika ingin menggunakan aplikasi lagi\n\n" +
                                        "Akun Stockity Anda TIDAK akan terhapus, hanya akses ke aplikasi bot ini."
                                "en" -> "Deleting your account will:\n\n" +
                                        "• Remove your access from this application\n" +
                                        "• Delete your whitelist data from the database\n" +
                                        "• Require admin registration to use the app again\n\n" +
                                        "Your Stockity account will NOT be deleted, only your access to this bot app."
                                "es" -> "Eliminar tu cuenta:\n\n" +
                                        "• Eliminará tu acceso a esta aplicación\n" +
                                        "• Eliminará tus datos de la lista blanca\n" +
                                        "• Requerirá registro de administrador para usar la aplicación nuevamente\n\n" +
                                        "Tu cuenta de Stockity NO se eliminará, solo tu acceso a esta aplicación de bot."
                                "vi" -> "Xóa tài khoản sẽ:\n\n" +
                                        "• Xóa quyền truy cập của bạn khỏi ứng dụng này\n" +
                                        "• Xóa dữ liệu danh sách trắng của bạn\n" +
                                        "• Yêu cầu đăng ký quản trị viên để sử dụng lại\n\n" +
                                        "Tài khoản Stockity của bạn sẽ KHÔNG bị xóa, chỉ quyền truy cập vào ứng dụng bot này."
                                "tr" -> "Hesabınızı silmek:\n\n" +
                                        "• Bu uygulamadan erişiminizi kaldıracak\n" +
                                        "• Beyaz liste verilerinizi silecek\n" +
                                        "• Tekrar kullanmak için yönetici kaydı gerektirecek\n\n" +
                                        "Stockity hesabınız SİLİNMEYECEK, sadece bu bot uygulamasına erişiminiz."
                                "hi" -> "खाता हटाने से:\n\n" +
                                        "• इस एप्लिकेशन से आपकी पहुंच हट जाएगी\n" +
                                        "• आपका व्हाइटलिस्ट डेटा डिलीट हो जाएगा\n" +
                                        "• दोबारा उपयोग के लिए एडमिन पंजीकरण की आवश्यकता होगी\n\n" +
                                        "आपका Stockity खाता हटाया नहीं जाएगा, केवल इस बॉट ऐप तक पहुंच।"
                                "ms" -> "Memadam akaun akan:\n\n" +
                                        "• Membuang akses anda dari aplikasi ini\n" +
                                        "• Memadam data senarai putih anda\n" +
                                        "• Memerlukan pendaftaran admin untuk guna semula\n\n" +
                                        "Akaun Stockity anda TIDAK akan dipadam, hanya akses ke aplikasi bot ini."
                                else -> "Deleting your account will:\n\n" +
                                        "• Remove your access from this application\n" +
                                        "• Delete your whitelist data from the database\n" +
                                        "• Require admin registration to use the app again\n\n" +
                                        "Your Stockity account will NOT be deleted, only your access to this bot app."
                            },
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // User Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = when(lang) {
                                "id" -> "Akun yang akan dihapus:"
                                "en" -> "Account to be deleted:"
                                "es" -> "Cuenta a eliminar:"
                                "vi" -> "Tài khoản sẽ bị xóa:"
                                "tr" -> "Silinecek hesap:"
                                "hi" -> "हटाया जाने वाला खाता:"
                                "ms" -> "Akaun yang akan dipadam:"
                                else -> "Account to be deleted:"
                            },
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "User ID: $userId",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Confirmation Input
                Text(
                    text = when(lang) {
                        "id" -> "Ketik DELETE untuk konfirmasi:"
                        "en" -> "Type DELETE to confirm:"
                        "es" -> "Escribe DELETE para confirmar:"
                        "vi" -> "Gõ DELETE để xác nhận:"
                        "tr" -> "Onaylamak için DELETE yazın:"
                        "hi" -> "पुष्टि के लिए DELETE लिखें:"
                        "ms" -> "Taip DELETE untuk mengesahkan:"
                        else -> "Type DELETE to confirm:"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = {
                        Text(
                            "DELETE",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDeleteEnabled) DangerRed else StatusBlue,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = DangerRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    enabled = deleteAccountState !is ProfileViewModel.DeleteAccountState.Loading
                )

                // State Messages
                when (deleteAccountState) {
                    is ProfileViewModel.DeleteAccountState.Loading -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = StatusBlue.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = StatusBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when(lang) {
                                        "id" -> "Menghapus akun..."
                                        "en" -> "Deleting account..."
                                        "es" -> "Eliminando cuenta..."
                                        "vi" -> "Đang xóa tài khoản..."
                                        "tr" -> "Hesap siliniyor..."
                                        "hi" -> "खाता हटाया जा रहा है..."
                                        "ms" -> "Memadam akaun..."
                                        else -> "Deleting account..."
                                    },
                                    fontSize = 13.sp,
                                    color = StatusBlue
                                )
                            }
                        }
                    }
                    is ProfileViewModel.DeleteAccountState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = WifiGreen.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = WifiGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when(lang) {
                                        "id" -> "Akun berhasil dihapus. Logging out..."
                                        "en" -> "Account deleted successfully. Logging out..."
                                        "es" -> "Cuenta eliminada. Cerrando sesión..."
                                        "vi" -> "Đã xóa tài khoản. Đăng xuất..."
                                        "tr" -> "Hesap silindi. Çıkış yapılıyor..."
                                        "hi" -> "खाता हटाया गया। लॉगआउट..."
                                        "ms" -> "Akaun dipadam. Log keluar..."
                                        else -> "Account deleted successfully. Logging out..."
                                    },
                                    fontSize = 13.sp,
                                    color = WifiGreen
                                )
                            }
                        }
                    }
                    is ProfileViewModel.DeleteAccountState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = DangerRed.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Error,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = deleteAccountState.message,
                                    fontSize = 13.sp,
                                    color = DangerRed,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    else -> { /* Idle state */ }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = deleteAccountState !is ProfileViewModel.DeleteAccountState.Loading
                    ) {
                        Text(
                            when(lang) {
                                "id" -> "Batal"
                                "en" -> "Cancel"
                                "es" -> "Cancelar"
                                "vi" -> "Hủy"
                                "tr" -> "İptal"
                                "hi" -> "रद्द करें"
                                "ms" -> "Batal"
                                else -> "Cancel"
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { onConfirmDelete(userId, userEmail) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White,
                            disabledContainerColor = DangerRed.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isDeleteEnabled && deleteAccountState !is ProfileViewModel.DeleteAccountState.Loading
                    ) {
                        if (deleteAccountState is ProfileViewModel.DeleteAccountState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                when(lang) {
                                    "id" -> "Hapus"
                                    "en" -> "Delete"
                                    "es" -> "Eliminar"
                                    "vi" -> "Xóa"
                                    "tr" -> "Sil"
                                    "hi" -> "हटाएं"
                                    "ms" -> "Padam"
                                    else -> "Delete"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
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

private fun getAndroidVersionName(): String {
    val versionName = when (Build.VERSION.SDK_INT) {
        Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Android 14"
        Build.VERSION_CODES.TIRAMISU -> "Android 13"
        Build.VERSION_CODES.S_V2 -> "Android 12L"
        Build.VERSION_CODES.S -> "Android 12"
        Build.VERSION_CODES.R -> "Android 11"
        Build.VERSION_CODES.Q -> "Android 10"
        Build.VERSION_CODES.P -> "Android 9 Pie"
        Build.VERSION_CODES.O_MR1 -> "Android 8.1 Oreo"
        Build.VERSION_CODES.O -> "Android 8.0 Oreo"
        Build.VERSION_CODES.N_MR1 -> "Android 7.1 Nougat"
        Build.VERSION_CODES.N -> "Android 7.0 Nougat"
        else -> "Android ${Build.VERSION.RELEASE}"
    }
    return "$versionName (API ${Build.VERSION.SDK_INT})"
}