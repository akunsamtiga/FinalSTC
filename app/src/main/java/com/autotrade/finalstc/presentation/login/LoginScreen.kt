package com.autotrade.finalstc.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autotrade.finalstc.R
import com.autotrade.finalstc.utils.StringsManager
import com.autotrade.finalstc.presentation.login.components.LanguageSelectorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentCountry by viewModel.currentCountry.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetSuccess()
        }
    }

    // Language Selector Dialog
    if (uiState.showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = lang,           // ✅ Dari StateFlow
            currentCountry = currentCountry,  // ✅ Dari StateFlow
            onDismiss = { viewModel.showLanguageDialog(false) },
            onLanguageSelected = { languageCode, countryCode ->
                viewModel.updateLanguage(languageCode, countryCode)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.header4),
                        contentDescription = "Logo STC",
                        modifier = Modifier.size(180.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = StringsManager.getLoginTitle(lang),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF202124),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = {
                            viewModel.updateEmail(it)
                            if (uiState.errorMessage != null) {
                                viewModel.clearError()
                            }
                        },
                        label = {
                            Text(
                                StringsManager.getEmailLabel(lang),
                                color = Color(0xFF5F6368)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF5F6368)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFDADCE0),
                            focusedLabelColor = Color(0xFF4285F4),
                            cursorColor = Color(0xFF4285F4),
                            focusedTextColor = Color(0xFF202124),
                            unfocusedTextColor = Color(0xFF202124)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = {
                            viewModel.updatePassword(it)
                            if (uiState.errorMessage != null) {
                                viewModel.clearError()
                            }
                        },
                        label = {
                            Text(
                                StringsManager.getPasswordLabel(lang),
                                color = Color(0xFF5F6368)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF5F6368)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible)
                                        StringsManager.getHidePassword(lang)
                                    else
                                        StringsManager.getShowPassword(lang),
                                    tint = Color(0xFF5F6368)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFDADCE0),
                            focusedLabelColor = Color(0xFF4285F4),
                            cursorColor = Color(0xFF4285F4),
                            focusedTextColor = Color(0xFF202124),
                            unfocusedTextColor = Color(0xFF202124)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remember Me Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.rememberMe,
                            onCheckedChange = { viewModel.updateRememberMe(it) },
                            enabled = !uiState.isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4285F4),
                                uncheckedColor = Color(0xFF5F6368),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringsManager.getRememberMe(lang),
                            color = Color(0xFF5F6368),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Loading State Indicator
                    if (uiState.isLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.loadingMessage,
                                    maxLines = 1,
                                    color = Color(0xFF1976D2),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF1976D2),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Error Message
                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isStockityLoginSuccess)
                                    Color(0xFFFFF3E0)
                                else
                                    Color(0xFFFCE8E6)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (uiState.isStockityLoginSuccess)
                                        Icons.Default.Warning
                                    else
                                        Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (uiState.isStockityLoginSuccess)
                                        Color(0xFFFF9800)
                                    else
                                        Color(0xFFD93025),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = if (uiState.isStockityLoginSuccess)
                                        Color(0xFFE65100)
                                    else
                                        Color(0xFFD93025),
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (uiState.errorMessage == null && !uiState.isLoading) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.login(uiState.email, uiState.password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !uiState.isLoading && uiState.email.isNotBlank() && uiState.password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D8A15),
                            disabledContainerColor = Color(0xFFE8F0FE)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = StringsManager.getLoginButton(lang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Register Button
                    OutlinedButton(
                        onClick = onRegisterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2D8A15)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF2D8A15)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2D8A15)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringsManager.getRegisterButton(lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFDADCE0)
                        )
                        Text(
                            text = " ${StringsManager.getOrText(lang)} ",
                            color = Color(0xFF5F6368),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFDADCE0)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            val waNumber = viewModel.whatsappNumber.value
                            val waUrl = "https://wa.me/$waNumber"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(waUrl)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2D8A15)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF2D8A15)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = StringsManager.getNeedHelp(lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Selector Button
                    OutlinedButton(
                        onClick = { viewModel.showLanguageDialog(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF5F6368)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFDADCE0)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = when(currentCountry) {  // ✅ Dari StateFlow
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
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}