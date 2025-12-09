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
import com.autotrade.finalstc.presentation.components.ImprovedAvatarImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.autotrade.finalstc.R
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.autotrade.finalstc.presentation.login.components.LanguageSelectorDialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.UserProfileData
import com.autotrade.finalstc.data.repository.ProfileRepository
import com.autotrade.finalstc.presentation.main.dashboard.CurrencyType

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
    private val languageManager: LanguageManager,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _currentCurrency = MutableStateFlow(CurrencyType.IDR)
    val currentCurrency: StateFlow<CurrencyType> = _currentCurrency.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

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

    init {
        loadCurrency()
        observeCurrencyChanges()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            profileRepository.getUserProfile()
                .onSuccess { profile ->
                    _userProfile.value = profile
                    _isLoadingProfile.value = false
                }
                .onFailure { error ->
                    Log.e("ProfileViewModel", "Failed to load profile: ${error.message}")
                    _isLoadingProfile.value = false
                }
        }
    }

    private fun loadCurrency() {
        val currencyIso = sessionManager.getCurrencyIso()
        val currencyType = CurrencyType.fromCode(currencyIso)
        _currentCurrency.value = currencyType
        Log.d("ProfileViewModel", "💰 Loaded currency: ${currencyType.code}")
    }

    // ✅ TAMBAH: Observasi perubahan currency
    private fun observeCurrencyChanges() {
        viewModelScope.launch {
            sessionManager.currencyFlow.collect { newCurrencyIso ->
                val currencyType = CurrencyType.fromCode(newCurrencyIso)
                _currentCurrency.value = currencyType
                Log.d("ProfileViewModel", "💰 Currency changed to: ${currencyType.code}")
            }
        }
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

    val userProfile by profileViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by profileViewModel.isLoadingProfile.collectAsStateWithLifecycle()

    val currentCurrency by profileViewModel.currentCurrency.collectAsStateWithLifecycle()

    LaunchedEffect(userSession?.email) {
        userSession?.email?.let { email ->
            profileViewModel.checkAdminStatus(email)
            profileViewModel.loadUserProfile()
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

    val formattedPortfolio = remember(totalPortfolioCents, currentCurrency) {
        currentCurrency.formatAmount(totalPortfolioCents)
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
                                // ✅ Glow effect background
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

                                // ✅ Border ring
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
                                    ImprovedAvatarImage(
                                        avatarUrl = userProfile?.avatar,
                                        size = 64.dp,
                                        isAdmin = isAdmin,
                                        accentColor = StatusBlue,
                                        backgroundColor = DarkSurface,
                                        modifier = Modifier.size(64.dp)
                                    )
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

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Online Badge
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
                                                rememberInfiniteTransition(label = "pulse_animation_header")
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

                                    // Role Badge
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                isSuperAdmin -> AccentWarning.copy(alpha = 0.15f)
                                                isAdmin -> AccentWarning.copy(alpha = 0.15f)
                                                else -> StatusBlue.copy(alpha = 0.15f)
                                            }
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
                                            Icon(
                                                imageVector = when {
                                                    isSuperAdmin -> Icons.Default.Security
                                                    isAdmin -> Icons.Default.AdminPanelSettings
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = when {
                                                    isSuperAdmin -> AccentWarning
                                                    isAdmin -> AccentWarning
                                                    else -> StatusBlue
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when {
                                                    isSuperAdmin -> when(lang) {
                                                        "id" -> "Super Admin"
                                                        "en" -> "Super Admin"
                                                        "es" -> "Super Admin"
                                                        "vi" -> "Super Admin"
                                                        "tr" -> "Süper Admin"
                                                        "hi" -> "सुपर एडमिन"
                                                        "ms" -> "Super Admin"
                                                        else -> "Super Admin"
                                                    }
                                                    isAdmin -> when(lang) {
                                                        "id" -> "Admin"
                                                        "en" -> "Admin"
                                                        "es" -> "Admin"
                                                        "vi" -> "Quản trị"
                                                        "tr" -> "Admin"
                                                        "hi" -> "एडमिन"
                                                        "ms" -> "Admin"
                                                        else -> "Admin"
                                                    }
                                                    else -> when(lang) {
                                                        "id" -> "User"
                                                        "en" -> "User"
                                                        "es" -> "Usuario"
                                                        "vi" -> "Người dùng"
                                                        "tr" -> "Kullanıcı"
                                                        "hi" -> "उपयोगकर्ता"
                                                        "ms" -> "Pengguna"
                                                        else -> "User"
                                                    }
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    isSuperAdmin -> AccentWarning
                                                    isAdmin -> AccentWarning
                                                    else -> StatusBlue
                                                }
                                            )
                                        }
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
                            if (isLoadingProfile) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = StatusBlue,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                // Full Name
                                ProfileDetailItem(
                                    icon = Icons.Outlined.Badge,
                                    label = StringsManager.getFullName(lang),
                                    value = userProfile?.getFullName() ?: extractNameFromEmail(session.email),
                                    iconColor = StatusBlue
                                )

                                Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                // User ID
                                ProfileDetailItem(
                                    icon = Icons.Outlined.Fingerprint,
                                    label = StringsManager.getUserId(lang),
                                    value = session.userId,
                                    iconColor = WifiGreen
                                )

                                Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                // Email
                                ProfileDetailItem(
                                    icon = Icons.Outlined.Email,
                                    label = StringsManager.getEmailAddress(lang),
                                    value = session.email,
                                    iconColor = AccentSecondary
                                )

                                // Email Verified
                                userProfile?.let { profile ->
                                    Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileDetailItem(
                                        icon = Icons.Outlined.VerifiedUser,
                                        label = when(lang) {
                                            "id" -> "Status Email"
                                            "en" -> "Email Status"
                                            "es" -> "Estado de Email"
                                            "vi" -> "Trạng thái Email"
                                            "tr" -> "E-posta Durumu"
                                            "hi" -> "ईमेल स्थिति"
                                            "ms" -> "Status Email"
                                            else -> "Email Status"
                                        },
                                        value = getVerifiedStatus(profile.emailVerified, lang),
                                        iconColor = if (profile.emailVerified) WifiGreen else AccentWarning
                                    )
                                }

                                // Phone Number
                                userProfile?.phone?.let { phone ->
                                    if (phone.isNotEmpty()) {
                                        Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                        ProfileDetailItem(
                                            icon = Icons.Outlined.Phone,
                                            label = when(lang) {
                                                "id" -> "Nomor Telepon"
                                                "en" -> "Phone Number"
                                                "es" -> "Número de Teléfono"
                                                "vi" -> "Số Điện Thoại"
                                                "tr" -> "Telefon Numarası"
                                                "hi" -> "फ़ोन नंबर"
                                                "ms" -> "Nombor Telefon"
                                                else -> "Phone Number"
                                            },
                                            value = phone,
                                            iconColor = StatusBlue
                                        )
                                    }
                                }

                                // Phone Verified
                                userProfile?.let { profile ->
                                    if (!profile.phone.isNullOrEmpty()) {
                                        Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                        ProfileDetailItem(
                                            icon = Icons.Outlined.PhoneAndroid,
                                            label = when(lang) {
                                                "id" -> "Status Telepon"
                                                "en" -> "Phone Status"
                                                "es" -> "Estado de Teléfono"
                                                "vi" -> "Trạng thái Điện thoại"
                                                "tr" -> "Telefon Durumu"
                                                "hi" -> "फ़ोन स्थिति"
                                                "ms" -> "Status Telefon"
                                                else -> "Phone Status"
                                            },
                                            value = getVerifiedStatus(profile.phoneVerified, lang),
                                            iconColor = if (profile.phoneVerified) WifiGreen else AccentWarning
                                        )
                                    }
                                }

                                // Gender
                                userProfile?.gender?.let { gender ->
                                    Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileDetailItem(
                                        icon = Icons.Outlined.Person,
                                        label = when(lang) {
                                            "id" -> "Jenis Kelamin"
                                            "en" -> "Gender"
                                            "es" -> "Género"
                                            "vi" -> "Giới tính"
                                            "tr" -> "Cinsiyet"
                                            "hi" -> "लिंग"
                                            "ms" -> "Jantina"
                                            else -> "Gender"
                                        },
                                        value = getGenderDisplay(gender, lang),
                                        iconColor = AccentSecondary
                                    )
                                }

                                // Country
                                userProfile?.country?.let { country ->
                                    Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileDetailItem(
                                        icon = Icons.Outlined.Flag,
                                        label = when(lang) {
                                            "id" -> "Negara"
                                            "en" -> "Country"
                                            "es" -> "País"
                                            "vi" -> "Quốc gia"
                                            "tr" -> "Ülke"
                                            "hi" -> "देश"
                                            "ms" -> "Negara"
                                            else -> "Country"
                                        },
                                        value = getCountryDisplayName(country),
                                        iconColor = StatusBlue
                                    )
                                }

                                // Birthday
                                userProfile?.birthday?.let { birthday ->
                                    Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileDetailItem(
                                        icon = Icons.Outlined.Cake,
                                        label = when(lang) {
                                            "id" -> "Tanggal Lahir"
                                            "en" -> "Birthday"
                                            "es" -> "Fecha de Nacimiento"
                                            "vi" -> "Ngày sinh"
                                            "tr" -> "Doğum Tarihi"
                                            "hi" -> "जन्म तिथि"
                                            "ms" -> "Tarikh Lahir"
                                            else -> "Birthday"
                                        },
                                        value = formatDate(birthday),
                                        iconColor = AccentWarning
                                    )
                                }

                                // Registered At
                                userProfile?.registeredAt?.let { registeredAt ->
                                    Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileDetailItem(
                                        icon = Icons.Outlined.CalendarToday,
                                        label = when(lang) {
                                            "id" -> "Terdaftar Sejak"
                                            "en" -> "Registered Since"
                                            "es" -> "Registrado desde"
                                            "vi" -> "Đăng ký từ"
                                            "tr" -> "Kayıt Tarihi"
                                            "hi" -> "पंजीकृत तिथि"
                                            "ms" -> "Didaftarkan Sejak"
                                            else -> "Registered Since"
                                        },
                                        value = formatDate(registeredAt),
                                        iconColor = WifiGreen
                                    )
                                }

                                Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                // Timezone
                                ProfileDetailItem(
                                    icon = Icons.Outlined.Public,
                                    label = StringsManager.getTimezone(lang),
                                    value = session.userTimezone,
                                    iconColor = AccentWarning
                                )
                            }
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
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        OutlinedButton(
                            onClick = { profileViewModel.toggleLanguageDialog(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary,
                                containerColor = Color.Transparent
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
                                            text = StringsManager.getSelectLanguage(lang),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = getLanguageDisplayName(lang),
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
                                        text = getCountryFlag(currentCountry),
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

// Tambahkan helper function di bagian atas file (setelah private val declarations)
private fun getCountryFlag(countryCode: String): String {
    return when(countryCode) {
        "ID" -> "🇮🇩"
        "NG" -> "🇳🇬"
        "ZA" -> "🇿🇦"
        "KE" -> "🇰🇪"
        "GH" -> "🇬🇭"
        "UG" -> "🇺🇬"
        "TZ" -> "🇹🇿"
        "ET" -> "🇪🇹"
        "PH" -> "🇵🇭"
        "SG" -> "🇸🇬"
        "HK" -> "🇭🇰"
        "GB" -> "🇬🇧"
        "UA" -> "🇺🇦"
        "PL" -> "🇵🇱"
        "RO" -> "🇷🇴"
        "CZ" -> "🇨🇿"
        "MX" -> "🇲🇽"
        "AR" -> "🇦🇷"
        "CL" -> "🇨🇱"
        "CO" -> "🇨🇴"
        "PE" -> "🇵🇪"
        "VE" -> "🇻🇪"
        "CR" -> "🇨🇷"
        "EC" -> "🇪🇨"
        "UY" -> "🇺🇾"
        "PY" -> "🇵🇾"
        "BO" -> "🇧🇴"
        "SV" -> "🇸🇻"
        "GT" -> "🇬🇹"
        "HN" -> "🇭🇳"
        "PA" -> "🇵🇦"
        "DO" -> "🇩🇴"
        "CU" -> "🇨🇺"
        "VN" -> "🇻🇳"
        "LA" -> "🇱🇦"
        "TH" -> "🇹🇭"
        "KH" -> "🇰🇭"
        "TR" -> "🇹🇷"
        "CY" -> "🇨🇾"
        "IN" -> "🇮🇳"
        "NP" -> "🇳🇵"
        "FJ" -> "🇫🇯"
        "MY" -> "🇲🇾"
        "BN" -> "🇧🇳"
        "BD" -> "🇧🇩"
        "PK" -> "🇵🇰"
        "BR" -> "🇧🇷"
        "RU" -> "🇷🇺"
        "KZ" -> "🇰🇿"
        "BY" -> "🇧🇾"
        "KG" -> "🇰🇬"
        else -> "🌐"
    }
}
private fun getLanguageDisplayName(lang: String): String {
    return when (lang) {
        "id" -> "Bahasa Indonesia"
        "en" -> "English"
        "es" -> "Español"
        "vi" -> "Tiếng Việt"
        "tr" -> "Türkçe"
        "hi" -> "हिन्दी"
        "ms" -> "Bahasa Melayu"
        "bn" -> "বাংলা"
        "ru" -> "Русский"
        else -> "Language"
    }
}

private fun getCountryDisplayName(countryCode: String): String {
    return when (countryCode) {
        "ID" -> "Indonesia"
        "NG" -> "Nigeria"
        "ZA" -> "South Africa"
        "KE" -> "Kenya"
        "GH" -> "Ghana"
        "UG" -> "Uganda"
        "TZ" -> "Tanzania"
        "ET" -> "Ethiopia"
        "PH" -> "Philippines"
        "SG" -> "Singapore"
        "HK" -> "Hong Kong"
        "GB" -> "United Kingdom"
        "UA" -> "Ukraine"
        "PL" -> "Poland"
        "RO" -> "Romania"
        "CZ" -> "Czech Republic"
        "MX" -> "México"
        "AR" -> "Argentina"
        "CL" -> "Chile"
        "CO" -> "Colombia"
        "PE" -> "Perú"
        "VE" -> "Venezuela"
        "CR" -> "Costa Rica"
        "EC" -> "Ecuador"
        "UY" -> "Uruguay"
        "PY" -> "Paraguay"
        "BO" -> "Bolivia"
        "SV" -> "El Salvador"
        "GT" -> "Guatemala"
        "HN" -> "Honduras"
        "PA" -> "Panamá"
        "DO" -> "República Dominicana"
        "CU" -> "Cuba"
        "VN" -> "Việt Nam"
        "LA" -> "Laos"
        "TH" -> "Thailand"
        "KH" -> "Cambodia"
        "TR" -> "Türkiye"
        "CY" -> "Cyprus"
        "IN" -> "India"
        "NP" -> "Nepal"
        "FJ" -> "Fiji"
        "MY" -> "Malaysia"
        "BN" -> "Brunei"
        "BD" -> "Bangladesh"
        "PK" -> "Pakistan"
        "BR" -> "Brazil"
        "RU" -> "Russia"
        "KZ" -> "Kazakhstan"
        "BY" -> "Belarus"
        "KG" -> "Kyrgyzstan"
        else -> countryCode
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "-"

    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.getDefault())
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)

        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        outputFormat.format(date ?: return "-")
    } catch (e: Exception) {
        try {
            val simpleFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = simpleFormat.parse(dateString)
            val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            outputFormat.format(date ?: return "-")
        } catch (e: Exception) {
            dateString
        }
    }
}

private fun getGenderDisplay(gender: String?, lang: String): String {
    return when (gender?.lowercase()) {
        "male" -> when(lang) {
            "id" -> "Laki-laki"
            "en" -> "Male"
            "es" -> "Masculino"
            "vi" -> "Nam"
            "tr" -> "Erkek"
            "hi" -> "पुरुष"
            "ms" -> "Lelaki"
            else -> "Male"
        }
        "female" -> when(lang) {
            "id" -> "Perempuan"
            "en" -> "Female"
            "es" -> "Femenino"
            "vi" -> "Nữ"
            "tr" -> "Kadın"
            "hi" -> "महिला"
            "ms" -> "Perempuan"
            else -> "Female"
        }
        else -> "-"
    }
}

private fun getVerifiedStatus(isVerified: Boolean, lang: String): String {
    return if (isVerified) {
        when(lang) {
            "id" -> "Terverifikasi"
            "en" -> "Verified"
            "es" -> "Verificado"
            "vi" -> "Đã xác minh"
            "tr" -> "Doğrulandı"
            "hi" -> "सत्यापित"
            "ms" -> "Disahkan"
            else -> "Verified"
        }
    } else {
        when(lang) {
            "id" -> "Belum Terverifikasi"
            "en" -> "Not Verified"
            "es" -> "No Verificado"
            "vi" -> "Chưa xác minh"
            "tr" -> "Doğrulanmadı"
            "hi" -> "सत्यापित नहीं"
            "ms" -> "Tidak Disahkan"
            else -> "Not Verified"
        }
    }
}