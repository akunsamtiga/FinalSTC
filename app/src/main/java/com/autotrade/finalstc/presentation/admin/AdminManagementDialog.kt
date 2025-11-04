package com.autotrade.finalstc.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.autotrade.finalstc.data.model.AdminUser
import java.text.SimpleDateFormat
import java.util.*

// Colors
private val DarkBackground = Color(0xFF0B1A14)
private val DarkCard = Color(0xFF15241C)
private val DarkCardHover = Color(0xFF1A2A21)
private val AccentPrimary = Color(0xFF4ECDC4)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFFCBD5E1)
private val TextTertiary = Color(0xFF94A3B8)
private val SuccessColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)
private val InfoColor = Color(0xFF3B82F6)

@Composable
fun AdminManagementDialog(
    viewModel: AdminViewModel,
    currentUserEmail: String,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSuperAdmin = uiState.isSuperAdmin

    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showEditAdminDialog by remember { mutableStateOf(false) }
    var showDeleteAdminDialog by remember { mutableStateOf(false) }
    var showUrlEditDialog by remember { mutableStateOf(false) }
    var showWhatsAppEditDialog by remember { mutableStateOf(false) }
    var selectedAdmin by remember { mutableStateOf<AdminUser?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAdmins = remember(uiState.adminUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.adminUsers
        } else {
            uiState.adminUsers.filter { admin ->
                admin.name.contains(searchQuery, ignoreCase = true) ||
                        admin.email.contains(searchQuery, ignoreCase = true) ||
                        admin.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())  // ✅ SELURUH dialog bisa scroll
                    .padding(20.dp)
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
                                WarningColor.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = WarningColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSuperAdmin) "Admin Management" else "View Admins",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.adminUsers.size} ${if (uiState.adminUsers.size == 1) "admin" else "admins"}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Add Admin Button - Only for Super Admin
                if (isSuperAdmin) {
                    Button(
                        onClick = { showAddAdminDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add New Admin",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Info card for regular admin
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = InfoColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Anda dapat melihat daftar admin, namun hanya Super Admin yang dapat menambah, mengubah, atau menghapus admin.",
                                color = InfoColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search by name, email, or role...",
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

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = DarkCardHover, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Configuration Cards - Hanya untuk Super Admin
                if (isSuperAdmin) {
                    // Registration URL Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCardHover
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            InfoColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = InfoColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Registration URL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { showUrlEditDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = InfoColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    enabled = !uiState.isLoadingConfig
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit", fontSize = 10.sp)
                                }
                            }

                            Text(
                                text = uiState.registrationConfig.registrationUrl,
                                fontSize = 11.sp,
                                color = AccentPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // WhatsApp Help Number Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCardHover
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            SuccessColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SuccessColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "WhatsApp Bantuan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { showWhatsAppEditDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SuccessColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    enabled = !uiState.isLoadingConfig
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit", fontSize = 10.sp)
                                }
                            }

                            Text(
                                text = "+${uiState.registrationConfig.whatsappHelpNumber}",
                                fontSize = 11.sp,
                                color = SuccessColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Divider(color = DarkCardHover, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (uiState.isLoadingAdmins) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),  // ✅ Fixed height, tanpa weight
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentPrimary,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (filteredAdmins.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),  // ✅ Fixed height, tanpa weight
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No admins found" else "No admins yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "for \"$searchQuery\"",
                                    fontSize = 13.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                } else {
                    // ✅ TANPA Box wrapper, TANPA weight, TANPA nested scroll
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredAdmins.forEach { admin ->
                            AdminCard(
                                admin = admin,
                                currentUserEmail = currentUserEmail,
                                isSuperAdmin = isSuperAdmin,
                                onEdit = {
                                    selectedAdmin = admin
                                    showEditAdminDialog = true
                                },
                                onDelete = {
                                    selectedAdmin = admin
                                    showDeleteAdminDialog = true
                                },
                                onToggleStatus = { viewModel.toggleAdminStatus(admin) }
                            )
                        }
                        // Spacer di akhir untuk memberikan ruang scroll
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Add Admin Dialog - Only for Super Admin
    if (showAddAdminDialog && isSuperAdmin) {
        AddAdminDialog(
            onDismiss = { showAddAdminDialog = false },
            onAddAdmin = { admin ->
                viewModel.addAdmin(admin)
                showAddAdminDialog = false
            },
            onValidateEmail = { email -> viewModel.validateEmail(email) }
        )
    }

    // Edit Admin Dialog - Only for Super Admin
    if (showEditAdminDialog && selectedAdmin != null && isSuperAdmin) {
        EditAdminDialog(
            admin = selectedAdmin!!,
            currentUserEmail = currentUserEmail,
            onDismiss = {
                showEditAdminDialog = false
                selectedAdmin = null
            },
            onEditAdmin = { admin ->
                viewModel.updateAdmin(admin)
                showEditAdminDialog = false
                selectedAdmin = null
            },
            onValidateEmail = { email -> viewModel.validateEmail(email) }
        )
    }

    // Delete Admin Dialog - Only for Super Admin
    if (showDeleteAdminDialog && selectedAdmin != null && isSuperAdmin) {
        DeleteAdminDialog(
            admin = selectedAdmin!!,
            currentUserEmail = currentUserEmail,
            onDismiss = {
                showDeleteAdminDialog = false
                selectedAdmin = null
            },
            onConfirm = {
                viewModel.deleteAdmin(selectedAdmin!!.id)
                showDeleteAdminDialog = false
                selectedAdmin = null
            }
        )
    }

    // Edit Registration URL Dialog
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

    // Edit WhatsApp Dialog
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
private fun AdminCard(
    admin: AdminUser,
    currentUserEmail: String,
    isSuperAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isAdminSuperAdmin = admin.role == "super_admin"
    val isCurrentUser = admin.email == currentUserEmail
    val canModify = isSuperAdmin && !isAdminSuperAdmin && !isCurrentUser

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
                // Avatar with role indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isAdminSuperAdmin) WarningColor.copy(alpha = 0.15f)
                            else if (admin.isActive) InfoColor.copy(alpha = 0.15f)
                            else ErrorColor.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdminSuperAdmin) Icons.Default.SupervisorAccount
                        else Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (isAdminSuperAdmin) WarningColor
                        else if (admin.isActive) InfoColor
                        else ErrorColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = admin.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "You",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = admin.email,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Role Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAdminSuperAdmin) WarningColor.copy(alpha = 0.15f)
                            else InfoColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isAdminSuperAdmin) "Super Admin" else "Admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAdminSuperAdmin) WarningColor else InfoColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (admin.isActive) SuccessColor.copy(alpha = 0.15f)
                            else ErrorColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (admin.isActive) "Active" else "Inactive",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (admin.isActive) SuccessColor else ErrorColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Toggle Status Switch - Only for Super Admin modifying other admins
                if (canModify) {
                    Switch(
                        checked = admin.isActive,
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Admin Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Created: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(admin.createdAt))}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    if (admin.lastLogin > 0) {
                        Text(
                            text = "Last login: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(admin.lastLogin))}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Action Buttons - Only for Super Admin modifying other admins
                if (canModify) {
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
                } else if (!isSuperAdmin) {
                    // Show "View Only" badge for regular admins
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TextTertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "View Only",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAdminDialog(
    onDismiss: () -> Unit,
    onAddAdmin: (AdminUser) -> Unit,
    onValidateEmail: (String) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("admin") }
    var emailError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            emailError = if (!onValidateEmail(email)) "Email tidak valid" else null
        } else {
            emailError = null
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
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = WarningColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add New Admin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

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
                        focusedBorderColor = if (emailError != null) ErrorColor else AccentPrimary,
                        focusedLabelColor = if (emailError != null) ErrorColor else AccentPrimary,
                        cursorColor = AccentPrimary,
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
                    isError = emailError != null,
                    supportingText = if (emailError != null) {
                        { Text(emailError!!, color = ErrorColor, fontSize = 12.sp) }
                    } else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Selection
                Column {
                    Text(
                        text = "Role",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "admin",
                            onClick = { role = "admin" },
                            label = { Text("Admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InfoColor.copy(alpha = 0.2f),
                                selectedLabelColor = InfoColor,
                                selectedLeadingIconColor = InfoColor
                            )
                        )
                        FilterChip(
                            selected = role == "super_admin",
                            onClick = { role = "super_admin" },
                            label = { Text("Super Admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.SupervisorAccount,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningColor.copy(alpha = 0.2f),
                                selectedLabelColor = WarningColor,
                                selectedLeadingIconColor = WarningColor
                            )
                        )
                    }
                }

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
                            text = "Admin dapat mengelola whitelist users. Super Admin memiliki akses penuh ke semua fitur termasuk mengelola admin lainnya dan URL registrasi.",
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && emailError == null) {
                                onAddAdmin(
                                    AdminUser(
                                        name = name,
                                        email = email,
                                        role = role
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = name.isNotBlank() && email.isNotBlank() && emailError == null
                    ) {
                        Text("Add Admin", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAdminDialog(
    admin: AdminUser,
    currentUserEmail: String,
    onDismiss: () -> Unit,
    onEditAdmin: (AdminUser) -> Unit,
    onValidateEmail: (String) -> Boolean
) {
    var name by remember { mutableStateOf(admin.name) }
    var email by remember { mutableStateOf(admin.email) }
    var role by remember { mutableStateOf(admin.role) }
    var emailError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(email) {
        if (email.isNotBlank() && email != admin.email) {
            emailError = if (!onValidateEmail(email)) "Email tidak valid" else null
        } else {
            emailError = null
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
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = InfoColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Admin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

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
                        focusedBorderColor = if (emailError != null) ErrorColor else AccentPrimary,
                        focusedLabelColor = if (emailError != null) ErrorColor else AccentPrimary,
                        cursorColor = AccentPrimary,
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
                    isError = emailError != null,
                    supportingText = if (emailError != null) {
                        { Text(emailError!!, color = ErrorColor, fontSize = 12.sp) }
                    } else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Selection
                Column {
                    Text(
                        text = "Role",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "admin",
                            onClick = { role = "admin" },
                            label = { Text("Admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InfoColor.copy(alpha = 0.2f),
                                selectedLabelColor = InfoColor,
                                selectedLeadingIconColor = InfoColor
                            )
                        )
                        FilterChip(
                            selected = role == "super_admin",
                            onClick = { role = "super_admin" },
                            label = { Text("Super Admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.SupervisorAccount,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningColor.copy(alpha = 0.2f),
                                selectedLabelColor = WarningColor,
                                selectedLeadingIconColor = WarningColor
                            )
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && emailError == null) {
                                onEditAdmin(
                                    admin.copy(
                                        name = name,
                                        email = email,
                                        role = role
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
                        enabled = name.isNotBlank() && email.isNotBlank() && emailError == null
                    ) {
                        Text("Update", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteAdminDialog(
    admin: AdminUser,
    currentUserEmail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Delete Admin",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete ${admin.name}?",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This admin will lose all access to the admin panel. This action cannot be undone.",
                    color = TextTertiary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    "Delete",
                    fontWeight = FontWeight.SemiBold,
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
                border = BorderStroke(1.dp, TextTertiary),
                shape = RoundedCornerShape(12.dp),
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