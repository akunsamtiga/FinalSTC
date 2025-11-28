package com.autotrade.finalstc.presentation.login.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow

data class CountryLanguage(
    val countryCode: String,
    val countryName: String,
    val flag: String,
    val languageCode: String,
    val languageName: String
)

object AllCountriesData {
    val allCountries = listOf(
        CountryLanguage("ID", "Indonesia", "🇮🇩", "id", "Bahasa Indonesia"),

        // African English-speaking countries
        CountryLanguage("NG", "Nigeria", "🇳🇬", "en", "English"),
        CountryLanguage("ZA", "South Africa", "🇿🇦", "en", "English"),
        CountryLanguage("KE", "Kenya", "🇰🇪", "en", "English"),
        CountryLanguage("GH", "Ghana", "🇬🇭", "en", "English"),
        CountryLanguage("UG", "Uganda", "🇺🇬", "en", "English"),
        CountryLanguage("TZ", "Tanzania", "🇹🇿", "en", "English"),
        CountryLanguage("ET", "Ethiopia", "🇪🇹", "en", "English"),

        // Asian English-speaking countries
        CountryLanguage("PH", "Philippines", "🇵🇭", "en", "English"),
        CountryLanguage("SG", "Singapore", "🇸🇬", "en", "English"),
        CountryLanguage("HK", "Hong Kong", "🇭🇰", "en", "English"),

        // European English-speaking countries
        CountryLanguage("GB", "United Kingdom", "🇬🇧", "en", "English"),
        CountryLanguage("UA", "Ukraine", "🇺🇦", "en", "English"),
        CountryLanguage("PL", "Poland", "🇵🇱", "en", "English"),
        CountryLanguage("RO", "Romania", "🇷🇴", "en", "English"),
        CountryLanguage("CZ", "Czech Republic", "🇨🇿", "en", "English"),

        // Spanish-speaking countries (Latin America)
        CountryLanguage("MX", "México", "🇲🇽", "es", "Español"),
        CountryLanguage("AR", "Argentina", "🇦🇷", "es", "Español"),
        CountryLanguage("CL", "Chile", "🇨🇱", "es", "Español"),
        CountryLanguage("CO", "Colombia", "🇨🇴", "es", "Español"),
        CountryLanguage("PE", "Perú", "🇵🇪", "es", "Español"),
        CountryLanguage("VE", "Venezuela", "🇻🇪", "es", "Español"),
        CountryLanguage("CR", "Costa Rica", "🇨🇷", "es", "Español"),
        CountryLanguage("EC", "Ecuador", "🇪🇨", "es", "Español"),
        CountryLanguage("UY", "Uruguay", "🇺🇾", "es", "Español"),
        CountryLanguage("PY", "Paraguay", "🇵🇾", "es", "Español"),
        CountryLanguage("BO", "Bolivia", "🇧🇴", "es", "Español"),
        CountryLanguage("SV", "El Salvador", "🇸🇻", "es", "Español"),
        CountryLanguage("GT", "Guatemala", "🇬🇹", "es", "Español"),
        CountryLanguage("HN", "Honduras", "🇭🇳", "es", "Español"),
        CountryLanguage("PA", "Panamá", "🇵🇦", "es", "Español"),
        CountryLanguage("DO", "República Dominicana", "🇩🇴", "es", "Español"),
        CountryLanguage("CU", "Cuba", "🇨🇺", "es", "Español"),

        // Vietnamese-speaking countries
        CountryLanguage("VN", "Việt Nam", "🇻🇳", "vi", "Tiếng Việt"),
        CountryLanguage("LA", "Laos", "🇱🇦", "vi", "Tiếng Việt"),
        CountryLanguage("TH", "Thailand", "🇹🇭", "vi", "Tiếng Việt"),
        CountryLanguage("KH", "Cambodia", "🇰🇭", "vi", "Tiếng Việt"),

        // Turkish-speaking countries
        CountryLanguage("TR", "Türkiye", "🇹🇷", "tr", "Türkçe"),
        CountryLanguage("CY", "Cyprus", "🇨🇾", "tr", "Türkçe"),

        // Hindi-speaking countries
        CountryLanguage("IN", "India", "🇮🇳", "hi", "हिन्दी"),
        CountryLanguage("NP", "Nepal", "🇳🇵", "hi", "हिन्दी"),
        CountryLanguage("FJ", "Fiji", "🇫🇯", "hi", "हिन्दी"),

        // Malay-speaking countries
        CountryLanguage("MY", "Malaysia", "🇲🇾", "ms", "Bahasa Melayu"),
        CountryLanguage("BN", "Brunei", "🇧🇳", "ms", "Bahasa Melayu"),

        // Bengali-speaking countries
        CountryLanguage("BD", "Bangladesh", "🇧🇩", "bn", "বাংলা"),
        CountryLanguage("PK", "Pakistan", "🇵🇰", "bn", "বাংলা"),

        // Russian-speaking countries
        CountryLanguage("RU", "Russia", "🇷🇺", "ru", "Русский"),
        CountryLanguage("KZ", "Kazakhstan", "🇰🇿", "ru", "Русский"),
        CountryLanguage("BY", "Belarus", "🇧🇾", "ru", "Русский"),
        CountryLanguage("KG", "Kyrgyzstan", "🇰🇬", "ru", "Русский"),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LanguageSelectorDialog(
    currentLanguage: String,
    currentCountry: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            AllCountriesData.allCountries
        } else {
            AllCountriesData.allCountries.filter { country ->
                country.countryName.contains(searchQuery, ignoreCase = true) ||
                        country.languageName.contains(searchQuery, ignoreCase = true) ||
                        country.countryCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 650.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = true
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header dengan animasi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2D8A15),
                                    Color(0xFF4CAF50)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(currentLanguage) {
                                "id" -> "Pilih Negara & Bahasa"
                                "en" -> "Select Country & Language"
                                "es" -> "Seleccionar País e Idioma"
                                "vi" -> "Chọn Quốc Gia & Ngôn Ngữ"
                                "tr" -> "Ülke ve Dil Seçin"
                                "hi" -> "देश और भाषा चुनें"
                                "ms" -> "Pilih Negara & Bahasa"
                                "bn" -> "देश और ভাষा चुनें"
                                "ru" -> "Выберите страну и язык"
                                else -> "Pilih Negara & Bahasa"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        // Animated close button
                        var isCloseHovered by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(
                            targetValue = if (isCloseHovered) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .scale(closeScale)
                                .graphicsLayer {
                                    rotationZ = if (isCloseHovered) 90f else 0f
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Search Bar dengan animasi
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF5F6368),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                placeholder = {
                                    Text(
                                        text = when(currentLanguage) {
                                            "id" -> "Cari negara atau bahasa..."
                                            "en" -> "Search country or language..."
                                            "es" -> "Buscar país o idioma..."
                                            "vi" -> "Tìm kiếm quốc gia hoặc ngôn ngữ..."
                                            "tr" -> "Ülke veya dil ara..."
                                            "hi" -> "देश या भाषा खोजें..."
                                            "ms" -> "Cari negara atau bahasa..."
                                            "bn" -> "দেশ বা ভাষা খুঁজুন..."
                                            "ru" -> "Поиск страны или языка..."
                                            else -> "Cari negara atau bahasa..."
                                        },
                                        color = Color(0xFF9AA0A6)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            if (searchQuery.isNotEmpty()) {
                                // Animated clear button
                                var isClearHovered by remember { mutableStateOf(false) }
                                val clearScale by animateFloatAsState(
                                    targetValue = if (isClearHovered) 1.2f else 1f
                                )

                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.scale(clearScale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF5F6368),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Country list dengan animasi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries) { countryLang ->
                        AnimatedCountryLanguageItem(
                            countryLanguage = countryLang,
                            isSelected = countryLang.countryCode == currentCountry &&
                                    countryLang.languageCode == currentLanguage,
                            onClick = {
                                onLanguageSelected(countryLang.languageCode, countryLang.countryCode)
                            },
                            searchQuery = searchQuery
                        )
                    }
                }

                // Footer dengan current selection
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentCountryData = AllCountriesData.allCountries.find {
                                it.countryCode == currentCountry && it.languageCode == currentLanguage
                            }

                            Text(
                                text = currentCountryData?.flag ?: "🇮🇩",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentCountryData?.countryName ?: "Indonesia",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D8A15)
                                )
                                Text(
                                    text = currentCountryData?.languageName ?: "Bahasa Indonesia",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2D8A15).copy(alpha = 0.7f)
                                )
                            }

                            Text(
                                text = "Selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2D8A15),
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF2D8A15).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
fun AnimatedCountryLanguageItem(
    countryLanguage: CountryLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    searchQuery: String
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(12.dp)
            )
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Remove default ripple to use our custom animation
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated flag with bounce effect
            var isFlagHovered by remember { mutableStateOf(false) }
            val flagScale by animateFloatAsState(
                targetValue = if (isFlagHovered) 1.1f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f)
            )

            Text(
                text = countryLanguage.flag,
                fontSize = 28.sp,
                modifier = Modifier
                    .scale(flagScale)
                    .padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = countryLanguage.countryName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color(0xFF2D8A15) else Color(0xFF202124),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = countryLanguage.languageName,
                    fontSize = 13.sp,
                    color = if (isSelected) Color(0xFF2D8A15).copy(alpha = 0.8f)
                    else Color(0xFF5F6368),
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Animated checkmark with scale and fade
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D8A15)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Country code badge for non-selected items
            AnimatedVisibility(
                visible = !isSelected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = countryLanguage.countryCode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5F6368),
                    modifier = Modifier
                        .background(
                            color = Color(0xFFF1F3F4),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}