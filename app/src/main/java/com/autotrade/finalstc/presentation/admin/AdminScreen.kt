package com.autotrade.finalstc.presentation.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.autotrade.finalstc.data.model.RegistrationConfig
import kotlinx.coroutines.delay
import com.autotrade.finalstc.data.model.WhitelistUser
import java.text.SimpleDateFormat
import java.util.*

// Production Theme Colors
private val DarkBackground = Color(0xFF0B1A14)
private val DarkCard = Color(0xFF15241C)
private val DarkCardHover = Color(0xFF1A2A21)
private val AccentPrimary = Color(0xFF4ECDC4)
private val AccentSecondary = Color(0xFF26A69A)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFFCBD5E1)
private val TextTertiary = Color(0xFF94A3B8)
private val SuccessColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)
private val InfoColor = Color(0xFF3B82F6)

// Enum untuk filter type
enum class StatsFilterType {
    TOTAL, ACTIVE, INACTIVE, RECENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
    currentUserEmail: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUrlEditDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showAdminManagement by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<WhitelistUser?>(null) }
    var selectedStatsFilter by remember { mutableStateOf<StatsFilterType?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showWhatsAppEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail.isNotEmpty()) {
            viewModel.setCurrentUserEmail(currentUserEmail)
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(3000)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(3000)
            viewModel.clearSuccessMessage()
        }
    }

    val filteredUsers = remember(uiState.whitelistUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.whitelistUsers
        } else {
            uiState.whitelistUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true) ||
                        user.userId.contains(searchQuery, ignoreCase = true) ||
                        user.deviceId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkBackground.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(600))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                AdminHeader(
                    onNavigateBack = onNavigateBack,
                    onAdminManagement = { showAdminManagement = true },
                    totalUsers = uiState.whitelistUsers.size,
                    isSuperAdmin = uiState.isSuperAdmin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards
                AdminStatsRow(
                    users = uiState.whitelistUsers,
                    onStatsClick = { filterType ->
                        selectedStatsFilter = filterType
                        showStatsDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 900.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // HEADER DENGAN TOMBOL ADD USER
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        AccentPrimary.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = AccentPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Whitelist Users",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "${filteredUsers.size} of ${uiState.whitelistUsers.size} users",
                                        fontSize = 12.sp,
                                        color = TextTertiary
                                    )
                                }
                            }

                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add User",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // ===== EXPORT/IMPORT BUTTONS =====
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Download JSON Button
                            Button(
                                onClick = { viewModel.exportWhitelist("json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            // Download CSV Button
                            Button(
                                onClick = { viewModel.exportWhitelist("csv") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessColor.copy(alpha = 0.8f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            // Import Button
                            Button(
                                onClick = { showImportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InfoColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // ===== END EXPORT/IMPORT BUTTONS =====

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            placeholder = {
                                Text(
                                    "Pencarian",
                                    fontSize = 13.sp,
                                    color = TextTertiary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkCardHover,
                                cursorColor = AccentPrimary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            )
                        )

                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AccentPrimary,
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        } else if (filteredUsers.isEmpty()) {
                            if (searchQuery.isNotBlank()) {
                                EmptySearchView(searchQuery = searchQuery)
                            } else {
                                EmptyStateView()
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                filteredUsers.forEach { user ->
                                    WhitelistUserCard(
                                        user = user,
                                        onEdit = {
                                            selectedUser = user
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            selectedUser = user
                                            showDeleteDialog = true
                                        },
                                        onToggleStatus = { viewModel.toggleUserStatus(user) },
                                        searchQuery = searchQuery
                                    )
                                }
                            }
                        }
                    }
                }

                // Messages Display
                MessageDisplay(
                    error = uiState.error,
                    success = uiState.successMessage,
                    onDismissError = { viewModel.clearError() },
                    onDismissSuccess = { viewModel.clearSuccessMessage() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ===== DIALOGS =====
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAddUser = { user ->
                viewModel.addUser(user)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showEditDialog = false
                selectedUser = null
            },
            onEditUser = { user ->
                viewModel.updateUser(user)
                showEditDialog = false
                selectedUser = null
            },
            isSuperAdmin = uiState.isSuperAdmin  // Tambahkan parameter ini
        )
    }

    if (showDeleteDialog && selectedUser != null) {
        DeleteConfirmationDialog(
            user = selectedUser!!,
            onDismiss = {
                showDeleteDialog = false
                selectedUser = null
            },
            onConfirm = {
                viewModel.deleteUser(selectedUser!!.id)
                showDeleteDialog = false
                selectedUser = null
            }
        )
    }

    if (showUrlEditDialog) {
        EditRegistrationUrlDialog(
            currentUrl = uiState.registrationConfig.registrationUrl,
            isLoading = uiState.isLoadingConfig,
            onDismiss = { showUrlEditDialog = false },
            onSaveUrl = { newUrl ->
                viewModel.updateRegistrationUrl(newUrl)
                showUrlEditDialog = false
            },
            onValidateUrl = { url -> viewModel.validateRegistrationUrl(url) }
        )
    }

    if (showStatsDialog && selectedStatsFilter != null) {
        StatsDetailDialog(
            filterType = selectedStatsFilter!!,
            users = uiState.whitelistUsers,
            onDismiss = {
                showStatsDialog = false
                selectedStatsFilter = null
            },
            onEdit = { user ->
                selectedUser = user
                showStatsDialog = false
                selectedStatsFilter = null
                showEditDialog = true
            },
            onDelete = { user ->
                selectedUser = user
                showStatsDialog = false
                selectedStatsFilter = null
                showDeleteDialog = true
            },
            onToggleStatus = { user ->
                viewModel.toggleUserStatus(user)
            }
        )
    }
    
    if (showAdminManagement) {
        AdminManagementDialog(
            viewModel = viewModel,
            currentUserEmail = currentUserEmail,
            onDismiss = { showAdminManagement = false }
        )
    }

    if (showImportDialog) {
        ImportWhitelistDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonData ->
                viewModel.importWhitelist(jsonData)
                showImportDialog = false
            }
        )
    }

    if (showWhatsAppEditDialog) {
        EditWhatsAppDialog(
            currentNumber = uiState.registrationConfig.whatsappHelpNumber,
            isLoading = uiState.isLoadingConfig,
            onDismiss = { showWhatsAppEditDialog = false },
            onSaveNumber = { newNumber ->
                viewModel.updateWhatsappNumber(newNumber)
                showWhatsAppEditDialog = false
            },
            onValidateNumber = { number -> viewModel.validateWhatsappNumber(number) }
        )
    }

}


@Composable
fun ImportWhitelistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(jsonInput) {
        errorMessage = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ===== HEADER =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                InfoColor.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Upload,
                            contentDescription = null,
                            tint = InfoColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Whitelist Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tambahkan pengguna dari file JSON",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(
                    color = Color(0xFF1A2A21),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ===== INFO CARD =====
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = InfoColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = InfoColor,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = "Paste data JSON yang sudah di-export dari tombol JSON. Format harus sesuai dengan struktur whitelist users.",
                            color = InfoColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // ===== TEXT INPUT =====
                Text(
                    text = "JSON Data",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 300.dp),
                    placeholder = {
                        Text(
                            text = """[{"id":"","name":"John Doe","email":"john@example.com","userId":"12345","deviceId":"device1","isActive":true,"createdAt":1234567890,"lastLogin":0}]""",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InfoColor,
                        unfocusedBorderColor = Color(0xFF1A2A21),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        cursorColor = InfoColor,
                        errorBorderColor = ErrorColor,
                        errorLabelColor = ErrorColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        {
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                fontSize = 11.sp
                            )
                        }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Character count
                Text(
                    text = "${jsonInput.length} karakter",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ===== ERROR MESSAGE DISPLAY =====
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ===== INSTRUCTIONS =====
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A2A21)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Format JSON yang benar:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Array dari objects dengan field: id, name, email, userId, deviceId, isActive, createdAt, lastLogin",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== BUTTONS =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (jsonInput.isBlank()) {
                                errorMessage = "JSON data tidak boleh kosong"
                                return@Button
                            }

                            // Validasi JSON format
                            try {
                                if (!jsonInput.trim().startsWith("[") || !jsonInput.trim().endsWith("]")) {
                                    errorMessage = "JSON harus berupa array (dimulai dengan [ dan diakhiri ])"
                                    return@Button
                                }

                                isLoading = true
                                onImport(jsonInput)
                                keyboardController?.hide()
                            } catch (e: Exception) {
                                errorMessage = "Format JSON tidak valid: ${e.message}"
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfoColor,
                            contentColor = Color.White,
                            disabledContainerColor = InfoColor.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = jsonInput.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Text("Import", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== CLEAR BUTTON =====
                if (jsonInput.isNotBlank()) {
                    OutlinedButton(
                        onClick = { jsonInput = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hapus Isi", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsDetailDialog(
    filterType: StatsFilterType,
    users: List<WhitelistUser>,
    onDismiss: () -> Unit,
    onEdit: (WhitelistUser) -> Unit,  // Tambah parameter ini
    onDelete: (WhitelistUser) -> Unit,  // Tambah parameter ini
    onToggleStatus: (WhitelistUser) -> Unit  // Tambah parameter ini
) {
    val filteredUsers = remember(filterType, users) {
        when (filterType) {
            StatsFilterType.TOTAL -> users
            StatsFilterType.ACTIVE -> users.filter { it.isActive }
            StatsFilterType.INACTIVE -> users.filter { !it.isActive }
            StatsFilterType.RECENT -> {
                val timeThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                users.filter { it.lastLogin > timeThreshold }
            }
        }
    }

    val (title, icon, color) = when (filterType) {
        StatsFilterType.TOTAL -> Triple("Total Users", Icons.Outlined.People, InfoColor)
        StatsFilterType.ACTIVE -> Triple("Active Users", Icons.Outlined.CheckCircle, SuccessColor)
        StatsFilterType.INACTIVE -> Triple("Inactive Users", Icons.Outlined.Cancel, ErrorColor)
        StatsFilterType.RECENT -> Triple("Recent Logins (24h)", Icons.Outlined.Schedule, WarningColor)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredUsers.size} ${if (filteredUsers.size == 1) "user" else "users"}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(
                    color = DarkCardHover,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Users List
                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No users found",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredUsers.forEach { user ->
                            StatsUserCardWithActions(
                                user = user,
                                onEdit = { onEdit(user) },
                                onDelete = { onDelete(user) },
                                onToggleStatus = { onToggleStatus(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsUserCardWithActions(
    user: WhitelistUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardHover
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (user.isActive) SuccessColor.copy(alpha = 0.15f)
                            else ErrorColor.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isActive) SuccessColor else ErrorColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.lastLogin > 0) {
                        Text(
                            text = "Last login: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLogin))}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Switch Status
                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SuccessColor,
                        checkedTrackColor = SuccessColor.copy(alpha = 0.3f),
                        uncheckedThumbColor = ErrorColor,
                        uncheckedTrackColor = ErrorColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Info
                Column {
                    Text(
                        text = "ID: ${user.userId}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    // Added By Info
                    if (user.addedBy.isNotEmpty()) {
                        Text(
                            text = "Added by: ${user.addedBy.substringBefore("@")}",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = InfoColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = ErrorColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchView(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No results found",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Text(
            text = "for \"$searchQuery\"",
            fontSize = 13.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun RegistrationConfigCard(
    config: com.autotrade.finalstc.data.model.RegistrationConfig,
    isLoading: Boolean,
    isSuperAdmin: Boolean,
    onEditUrl: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            InfoColor.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = InfoColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registration URL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isSuperAdmin) "Manage registration link for new users" else "View registration link (Super Admin only)",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                if (isSuperAdmin) {
                    Button(
                        onClick = onEditUrl,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfoColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TextTertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View Only",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardHover
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Current URL:",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = config.registrationUrl,
                        fontSize = 14.sp,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last updated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(config.updatedAt))}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun EditRegistrationUrlDialog(
    currentUrl: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onValidateUrl: (String) -> Boolean
) {
    var url by remember { mutableStateOf(currentUrl) }
    var isValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            isValid = onValidateUrl(url)
            errorMessage = if (!isValid) "URL tidak valid. Gunakan format: https://example.com" else ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = InfoColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Registration URL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Registration URL", fontSize = 13.sp) },
                    placeholder = { Text("https://stockity.id/registered?a=...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValid) AccentPrimary else ErrorColor,
                        focusedLabelColor = if (isValid) AccentPrimary else ErrorColor,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        errorBorderColor = ErrorColor,
                        errorLabelColor = ErrorColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    isError = !isValid && url.isNotBlank(),
                    supportingText = if (!isValid && url.isNotBlank()) {
                        { Text(errorMessage, color = ErrorColor, fontSize = 12.sp) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = InfoColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = InfoColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "URL ini akan digunakan untuk mengarahkan pengguna baru ke halaman registrasi Stockity. Pastikan URL valid dan dapat diakses.",
                            color = InfoColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (url.isNotBlank() && isValid) {
                                onSaveUrl(url)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfoColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && url.isNotBlank() && isValid
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save URL", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageDisplay(
    error: String?,
    success: String?,
    onDismissError: () -> Unit,
    onDismissSuccess: () -> Unit
) {
    // Error Display
    error?.let { errorMsg ->
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = ErrorColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Error,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMsg,
                    color = ErrorColor,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismissError,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = ErrorColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Success Display
    success?.let { successMsg ->
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SuccessColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = SuccessColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = successMsg,
                    color = SuccessColor,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismissSuccess,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = SuccessColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminHeader(
    onNavigateBack: () -> Unit,
    onAdminManagement: () -> Unit,
    totalUsers: Int,
    isSuperAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            DarkCardHover,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Admin Panel",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isSuperAdmin) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        WarningColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Super Admin",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningColor,
                                    lineHeight = 1.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = "$totalUsers ${if (totalUsers == 1) "user" else "users"} in whitelist",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Hanya tampilkan tombol Admin Management jika Super Admin
            if (isSuperAdmin) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAdminManagement,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarningColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manage Admins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatsRow(
    users: List<WhitelistUser>,
    onStatsClick: (StatsFilterType) -> Unit
) {
    val activeUsers = users.count { it.isActive }
    val inactiveUsers = users.size - activeUsers
    val recentLogins = users.count {
        System.currentTimeMillis() - it.lastLogin < 24 * 60 * 60 * 1000
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsCard(
            icon = Icons.Outlined.People,
            value = users.size.toString(),
            label = "Total",
            color = InfoColor,
            modifier = Modifier.weight(1f),
            onClick = { onStatsClick(StatsFilterType.TOTAL) }
        )
        StatsCard(
            icon = Icons.Outlined.CheckCircle,
            value = activeUsers.toString(),
            label = "Active",
            color = SuccessColor,
            modifier = Modifier.weight(1f),
            onClick = { onStatsClick(StatsFilterType.ACTIVE) }
        )
        StatsCard(
            icon = Icons.Outlined.Cancel,
            value = inactiveUsers.toString(),
            label = "Inactive",
            color = ErrorColor,
            modifier = Modifier.weight(1f),
            onClick = { onStatsClick(StatsFilterType.INACTIVE) }
        )
        StatsCard(
            icon = Icons.Outlined.Schedule,
            value = recentLogins.toString(),
            label = "Recent",
            color = WarningColor,
            modifier = Modifier.weight(1f),
            onClick = { onStatsClick(StatsFilterType.RECENT) }
        )
    }
}

@Composable
private fun StatsCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WhitelistUserCard(
    user: WhitelistUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    searchQuery: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardHover
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (user.isActive) SuccessColor.copy(alpha = 0.15f)
                            else ErrorColor.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isActive) SuccessColor else ErrorColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.name.take(12)}...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        )
                    Text(
                        text = "${user.email.take(17)}...",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${user.userId}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SuccessColor,
                        checkedTrackColor = SuccessColor.copy(alpha = 0.3f),
                        uncheckedThumbColor = ErrorColor,
                        uncheckedTrackColor = ErrorColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Section dengan Added By
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Created: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(user.createdAt))}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = InfoColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = ErrorColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Added By Information
                if (user.addedBy.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = InfoColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = InfoColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Added by ${user.addedBy}",
                                fontSize = 11.sp,
                                color = InfoColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (user.addedAt > 0) {
                                Text(
                                    text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(user.addedAt)),
                                    fontSize = 10.sp,
                                    color = TextTertiary
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
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No users in whitelist",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Text(
            text = "Add users to get started",
            fontSize = 13.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onAddUser: (WhitelistUser) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Add New User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && userId.isNotBlank() && deviceId.isNotBlank()) {
                                onAddUser(
                                    WhitelistUser(
                                        name = name,
                                        email = email,
                                        userId = userId,
                                        deviceId = deviceId
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = name.isNotBlank() && email.isNotBlank() && userId.isNotBlank() && deviceId.isNotBlank()
                    ) {
                        Text("Add User", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: WhitelistUser,
    onDismiss: () -> Unit,
    onEditUser: (WhitelistUser) -> Unit,
    isSuperAdmin: Boolean = false  // Parameter tambahan
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var userId by remember { mutableStateOf(user.userId) }
    var deviceId by remember { mutableStateOf(user.deviceId) }
    var addedBy by remember { mutableStateOf(user.addedBy) }  // Field baru
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // Field AddedBy - hanya tampil untuk Super Admin
                if (isSuperAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = addedBy,
                        onValueChange = { addedBy = it },
                        label = { Text("Added By (Admin Email)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarningColor,
                            focusedLabelColor = WarningColor,
                            cursorColor = WarningColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.SupervisorAccount,
                                contentDescription = null,
                                tint = WarningColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = WarningColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Super Admin - Edit siapa yang menambahkan user ini",
                            fontSize = 11.sp,
                            color = WarningColor,
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCardHover
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Added by: ${user.addedBy.ifEmpty { "N/A" }}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && userId.isNotBlank() && deviceId.isNotBlank()) {
                                onEditUser(
                                    user.copy(
                                        name = name,
                                        email = email,
                                        userId = userId,
                                        deviceId = deviceId,
                                        addedBy = addedBy  // Simpan perubahan addedBy
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfoColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = name.isNotBlank() && email.isNotBlank() && userId.isNotBlank() && deviceId.isNotBlank()
                    ) {
                        Text("Update", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    user: WhitelistUser,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Delete User",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete ${user.name}?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This action cannot be undone.",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    "Delete",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    "Cancel",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    )
}

// Additional utility composables for better UX

@Composable
private fun SearchResultsBadge(
    resultCount: Int,
    totalCount: Int
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AccentPrimary.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Showing $resultCount of $totalCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AccentPrimary
            )
        }
    }
}

@Composable
private fun LoadingShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardHover.copy(alpha = alpha)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {}
        }
    }
}

@Composable
private fun WhatsAppConfigCard(
    config: RegistrationConfig,
    isLoading: Boolean,
    isSuperAdmin: Boolean,
    onEditWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            SuccessColor.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = SuccessColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WhatsApp Bantuan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isSuperAdmin) "Kelola nomor WhatsApp bantuan" else "Lihat nomor WhatsApp bantuan (Super Admin only)",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                if (isSuperAdmin) {
                    Button(
                        onClick = onEditWhatsApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TextTertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View Only",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardHover
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Nomor WhatsApp:",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "+${config.whatsappHelpNumber}",
                            fontSize = 14.sp,
                            color = SuccessColor,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SuccessColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last updated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(config.updatedAt))}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun EditWhatsAppDialog(
    currentNumber: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveNumber: (String) -> Unit,
    onValidateNumber: (String) -> Boolean
) {
    var number by remember { mutableStateOf(currentNumber) }
    var isValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(number) {
        if (number.isNotBlank()) {
            val cleanNumber = number.replace(Regex("[^0-9]"), "")
            isValid = onValidateNumber(cleanNumber)
            errorMessage = if (!isValid) {
                "Nomor tidak valid. Minimal 10 digit, maksimal 15 digit"
            } else ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = SuccessColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Nomor WhatsApp",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Nomor WhatsApp", fontSize = 13.sp) },
                    placeholder = { Text("6285959860015", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Text(
                            text = "+",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) AccentPrimary else ErrorColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValid) AccentPrimary else ErrorColor,
                        focusedLabelColor = if (isValid) AccentPrimary else ErrorColor,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        errorBorderColor = ErrorColor,
                        errorLabelColor = ErrorColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    isError = !isValid && number.isNotBlank(),
                    supportingText = if (!isValid && number.isNotBlank()) {
                        { Text(errorMessage, color = ErrorColor, fontSize = 12.sp) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = InfoColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = InfoColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Format nomor WhatsApp:",
                                color = InfoColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Gunakan kode negara (62 untuk Indonesia)\n• Tanpa tanda + di awal\n• Contoh: 6285959860015",
                                color = InfoColor,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("Batal", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (number.isNotBlank() && isValid) {
                                val cleanNumber = number.replace(Regex("[^0-9]"), "")
                                onSaveNumber(cleanNumber)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && number.isNotBlank() && isValid
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}