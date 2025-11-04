package com.autotrade.finalstc.presentation.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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

        CountryLanguage("NG", "Nigeria", "🇳🇬", "en", "English"),
        CountryLanguage("ZA", "South Africa", "🇿🇦", "en", "English"),
        CountryLanguage("KE", "Kenya", "🇰🇪", "en", "English"),
        CountryLanguage("GH", "Ghana", "🇬🇭", "en", "English"),
        CountryLanguage("UG", "Uganda", "🇺🇬", "en", "English"),
        CountryLanguage("TZ", "Tanzania", "🇹🇿", "en", "English"),
        CountryLanguage("ET", "Ethiopia", "🇪🇹", "en", "English"),

        CountryLanguage("PH", "Philippines", "🇵🇭", "en", "English"),
        CountryLanguage("SG", "Singapore", "🇸🇬", "en", "English"),
        CountryLanguage("HK", "Hong Kong", "🇭🇰", "en", "English"),
        CountryLanguage("BD", "Bangladesh", "🇧🇩", "en", "English"),
        CountryLanguage("PK", "Pakistan", "🇵🇰", "en", "English"),

        CountryLanguage("GB", "United Kingdom", "🇬🇧", "en", "English"),
        CountryLanguage("UA", "Ukraine", "🇺🇦", "en", "English"),
        CountryLanguage("PL", "Poland", "🇵🇱", "en", "English"),
        CountryLanguage("RO", "Romania", "🇷🇴", "en", "English"),
        CountryLanguage("CZ", "Czech Republic", "🇨🇿", "en", "English"),

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

        CountryLanguage("VN", "Việt Nam", "🇻🇳", "vi", "Tiếng Việt"),
        CountryLanguage("LA", "Laos", "🇱🇦", "vi", "Tiếng Việt"),
        CountryLanguage("TH", "Thailand", "🇹🇭", "vi", "Tiếng Việt"),
        CountryLanguage("KH", "Cambodia", "🇰🇭", "vi", "Tiếng Việt"),

        CountryLanguage("TR", "Türkiye", "🇹🇷", "tr", "Türkçe"),
        CountryLanguage("CY", "Cyprus", "🇨🇾", "tr", "Türkçe"),

        CountryLanguage("IN", "India", "🇮🇳", "hi", "हिन्दी"),
        CountryLanguage("NP", "Nepal", "🇳🇵", "hi", "हिन्दी"),
        CountryLanguage("FJ", "Fiji", "🇫🇯", "hi", "हिन्दी"),

        CountryLanguage("MY", "Malaysia", "🇲🇾", "ms", "Bahasa Melayu"),
        CountryLanguage("BN", "Brunei", "🇧🇳", "ms", "Bahasa Melayu"),
    )
}

@Composable
fun LanguageSelectorDialog(
    currentLanguage: String,
    currentCountry: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            else -> "Pilih Negara & Bahasa"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF5F6368)
                        )
                    }
                }

                Divider(color = Color(0xFFDADCE0))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(AllCountriesData.allCountries) { countryLang ->
                        CountryLanguageItem(
                            countryLanguage = countryLang,
                            isSelected = countryLang.countryCode == currentCountry &&
                                    countryLang.languageCode == currentLanguage,
                            onClick = {
                                onLanguageSelected(countryLang.languageCode, countryLang.countryCode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountryLanguageItem(
    countryLanguage: CountryLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFFE8F5E9)
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = countryLanguage.flag,
            fontSize = 32.sp,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = countryLanguage.countryName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF202124)
            )
            Text(
                text = countryLanguage.languageName,
                fontSize = 13.sp,
                color = Color(0xFF5F6368),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF2D8A15),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}