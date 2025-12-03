package com.autotrade.finalstc.presentation.login

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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
import kotlinx.coroutines.delay

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

    // Track loading progress steps
    val loadingSteps = remember { mutableStateOf(1) }
    val totalSteps = 3

    // Update loading steps based on loading message
    LaunchedEffect(uiState.loadingMessage) {
        when {
            uiState.loadingMessage.contains("Stockity", ignoreCase = true) -> loadingSteps.value = 1
            uiState.loadingMessage.contains("Auth", ignoreCase = true) -> loadingSteps.value = 2
            uiState.loadingMessage.contains("Access", ignoreCase = true) -> loadingSteps.value = 3
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetSuccess()
        }
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = lang,
            currentCountry = currentCountry,
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

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = StringsManager.getLoginTitle(lang),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF202124),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

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

                    // Enhanced Loading Card
                    EnhancedLoadingCard(
                        isLoading = uiState.isLoading,
                        loadingMessage = uiState.loadingMessage,
                        currentStep = loadingSteps.value,
                        totalSteps = totalSteps
                    )

                    if (uiState.isLoading) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isStockityLoginSuccess)
                                    Color(0xFFFFF3E0)
                                else
                                    Color(0xFFFCE8E6)
                            ),
                            shape = RoundedCornerShape(12.dp)
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

                    // Shimmer Loading Effect for Login Button
                    ShimmerLoadingEffect(
                        isLoading = uiState.isLoading,
                        contentAfterLoading = {
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
                                Text(
                                    text = StringsManager.getLoginButton(lang),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                .weight(0.7f)
                                .fillMaxHeight(),
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.showLanguageDialog(true) },
                            modifier = Modifier
                                .weight(0.3f)
                                .fillMaxHeight(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF000000)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = getFlagEmoji(currentCountry),
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = when(lang) {
                                    "id" -> "ID"
                                    "en" -> "EN"
                                    "es" -> "ES"
                                    "vi" -> "VI"
                                    "tr" -> "TR"
                                    "hi" -> "HI"
                                    "ms" -> "MS"
                                    "bn" -> "BN"
                                    "ru" -> "RU"
                                    else -> "ID"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

fun getFlagEmoji(countryCode: String): String {
    return when(countryCode) {
        "ID" -> "\uD83C\uDDEE\uD83C\uDDE9"
        "NG" -> "\uD83C\uDDF3\uD83C\uDDEC"
        "ZA" -> "\uD83C\uDDFF\uD83C\uDDE6"
        "KE" -> "\uD83C\uDDF0\uD83C\uDDEA"
        "GH" -> "\uD83C\uDDEC\uD83C\uDDED"
        "UG" -> "\uD83C\uDDFA\uD83C\uDDEC"
        "TZ" -> "\uD83C\uDDF9\uD83C\uDDFF"
        "ET" -> "\uD83C\uDDEA\uD83C\uDDF9"
        "PH" -> "\uD83C\uDDF5\uD83C\uDDED"
        "SG" -> "\uD83C\uDDF8\uD83C\uDDEC"
        "HK" -> "\uD83C\uDDED\uD83C\uDDF0"
        "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
        "UA" -> "\uD83C\uDDFA\uD83C\uDDE6"
        "PL" -> "\uD83C\uDDF5\uD83C\uDDF1"
        "RO" -> "\uD83C\uDDF7\uD83C\uDDF4"
        "CZ" -> "\uD83C\uDDE8\uD83C\uDDFF"
        "MX" -> "\uD83C\uDDF2\uD83C\uDDFD"
        "AR" -> "\uD83C\uDDE6\uD83C\uDDF7"
        "CL" -> "\uD83C\uDDE8\uD83C\uDDF1"
        "CO" -> "\uD83C\uDDE8\uD83C\uDDF4"
        "PE" -> "\uD83C\uDDF5\uD83C\uDDEA"
        "VE" -> "\uD83C\uDDFB\uD83C\uDDEA"
        "CR" -> "\uD83C\uDDE8\uD83C\uDDF7"
        "EC" -> "\uD83C\uDDEA\uD83C\uDDE8"
        "UY" -> "\uD83C\uDDFA\uD83C\uDDFE"
        "PY" -> "\uD83C\uDDF5\uD83C\uDDFE"
        "BO" -> "\uD83C\uDDE7\uD83C\uDDF4"
        "SV" -> "\uD83C\uDDF8\uD83C\uDDFB"
        "GT" -> "\uD83C\uDDEC\uD83C\uDDF9"
        "HN" -> "\uD83C\uDDED\uD83C\uDDF3"
        "PA" -> "\uD83C\uDDF5\uD83C\uDDE6"
        "DO" -> "\uD83C\uDDE9\uD83C\uDDF4"
        "CU" -> "\uD83C\uDDE8\uD83C\uDDFA"
        "VN" -> "\uD83C\uDDFB\uD83C\uDDF3"
        "LA" -> "\uD83C\uDDF1\uD83C\uDDE6"
        "TH" -> "\uD83C\uDDF9\uD83C\uDDED"
        "KH" -> "\uD83C\uDDF0\uD83C\uDDED"
        "TR" -> "\uD83C\uDDF9\uD83C\uDDF7"
        "CY" -> "\uD83C\uDDE8\uD83C\uDDFE"
        "IN" -> "\uD83C\uDDEE\uD83C\uDDF3"
        "NP" -> "\uD83C\uDDF3\uD83C\uDDF5"
        "FJ" -> "\uD83C\uDDEB\uD83C\uDDEF"
        "MY" -> "\uD83C\uDDF2\uD83C\uDDFE"
        "BN" -> "\uD83C\uDDE7\uD83C\uDDF3"
        "BD" -> "\uD83C\uDDE7\uD83C\uDDE9"
        "PK" -> "\uD83C\uDDF5\uD83C\uDDF0"
        "BR" -> "\uD83C\uDDE7\uD83C\uDDF7"
        "RU" -> "\uD83C\uDDF7\uD83C\uDDFA"
        "KZ" -> "\uD83C\uDDF0\uD83C\uDDFF"
        "BY" -> "\uD83C\uDDE7\uD83C\uDDFE"
        "KG" -> "\uD83C\uDDF0\uD83C\uDDEC"
        else -> "\uD83C\uDF0D" // Globe emoji as fallback
    }
}

// Shimmer Effect Composable
@Composable
fun ShimmerLoadingEffect(
    isLoading: Boolean,
    contentAfterLoading: @Composable () -> Unit
) {
    if (isLoading) {
        val transition = rememberInfiniteTransition()
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )

        val brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE8F5E8),
                Color(0xFF2D8A15),
                Color(0xFFE8F5E8)
            ),
            start = Offset(translateAnim - 500, 0f),
            end = Offset(translateAnim, 0f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = brush,
                            size = size
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    } else {
        contentAfterLoading()
    }
}

// Enhanced Loading Card with Progress Steps
@Composable
fun EnhancedLoadingCard(
    isLoading: Boolean,
    loadingMessage: String,
    currentStep: Int,
    totalSteps: Int
) {
    if (isLoading) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = true
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D8A15)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Progress Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Authenticating",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$currentStep/$totalSteps",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Progress Bar
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Loading Message with Typing Animation
                var displayedText by remember { mutableStateOf("") }
                var currentIndex by remember { mutableStateOf(0) }

                LaunchedEffect(loadingMessage, isLoading) {
                    if (isLoading) {
                        while (currentIndex < loadingMessage.length) {
                            displayedText = loadingMessage.substring(0, currentIndex + 1)
                            currentIndex++
                            delay(50) // Typing speed
                        }
                    } else {
                        displayedText = ""
                        currentIndex = 0
                    }
                }

                Text(
                    text = displayedText,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pulsing Dot Animation
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Processing",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                Color.White.copy(alpha = alpha),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}

