package com.autotrade.finalstc.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_COUNTRY = "selected_country"
    }

    private val _currentLanguage = MutableStateFlow(getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _currentCountry = MutableStateFlow(getCountry())
    val currentCountry: StateFlow<String> = _currentCountry.asStateFlow()

    fun saveLanguage(languageCode: String, countryCode: String) {
        prefs.edit().apply {
            putString(KEY_LANGUAGE, languageCode)
            putString(KEY_COUNTRY, countryCode)
            apply()
        }

        _currentLanguage.value = languageCode
        _currentCountry.value = countryCode
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "id") ?: "id"
    }

    fun getCountry(): String {
        return prefs.getString(KEY_COUNTRY, "ID") ?: "ID"
    }

    fun getLanguageDisplay(): String {
        val lang = getLanguage()
        val country = getCountry()
        return when(lang) {
            "id" -> "🇮🇩 Indonesia"
            "en" -> when(country) {
                "NG" -> "🇳🇬 Nigeria"
                "ZA" -> "🇿🇦 South Africa"
                "KE" -> "🇰🇪 Kenya"
                "GH" -> "🇬🇭 Ghana"
                "UG" -> "🇺🇬 Uganda"
                "TZ" -> "🇹🇿 Tanzania"
                "ET" -> "🇪🇹 Ethiopia"
                "PH" -> "🇵🇭 Philippines"
                "SG" -> "🇸🇬 Singapore"
                "HK" -> "🇭🇰 Hong Kong"
                "GB" -> "🇬🇧 United Kingdom"
                "UA" -> "🇺🇦 Ukraine"
                "PL" -> "🇵🇱 Poland"
                "RO" -> "🇷🇴 Romania"
                "CZ" -> "🇨🇿 Czech Republic"
                else -> "🇬🇧 $country"
            }
            "es" -> when(country) {
                "MX" -> "🇲🇽 México"
                "AR" -> "🇦🇷 Argentina"
                "CL" -> "🇨🇱 Chile"
                "CO" -> "🇨🇴 Colombia"
                "PE" -> "🇵🇪 Perú"
                "VE" -> "🇻🇪 Venezuela"
                "CR" -> "🇨🇷 Costa Rica"
                "EC" -> "🇪🇨 Ecuador"
                "UY" -> "🇺🇾 Uruguay"
                "PY" -> "🇵🇾 Paraguay"
                "BO" -> "🇧🇴 Bolivia"
                "SV" -> "🇸🇻 El Salvador"
                "GT" -> "🇬🇹 Guatemala"
                "HN" -> "🇭🇳 Honduras"
                "PA" -> "🇵🇦 Panamá"
                "DO" -> "🇩🇴 República Dominicana"
                "CU" -> "🇨🇺 Cuba"
                else -> "🇪🇸 $country"
            }
            "vi" -> when(country) {
                "VN" -> "🇻🇳 Việt Nam"
                "LA" -> "🇱🇦 Laos"
                "TH" -> "🇹🇭 Thailand"
                "KH" -> "🇰🇭 Cambodia"
                else -> "🇻🇳 $country"
            }
            "tr" -> when(country) {
                "TR" -> "🇹🇷 Türkiye"
                "CY" -> "🇨🇾 Cyprus"
                else -> "🇹🇷 Türkiye"
            }
            "hi" -> when(country) {
                "IN" -> "🇮🇳 India"
                "NP" -> "🇳🇵 Nepal"
                "FJ" -> "🇫🇯 Fiji"
                else -> "🇮🇳 India"
            }
            "ms" -> when(country) {
                "MY" -> "🇲🇾 Malaysia"
                "BN" -> "🇧🇳 Brunei"
                else -> "🇲🇾 Malaysia"
            }
            "bn" -> when(country) {
                "BD" -> "🇧🇩 Bangladesh"
                "PK" -> "🇵🇰 Pakistan"
                else -> "🇧🇩 Bangladesh"
            }
            "ru" -> when(country) {
                "RU" -> "🇷🇺 Russia"
                "KZ" -> "🇰🇿 Kazakhstan"
                "BY" -> "🇧🇾 Belarus"
                "UA" -> "🇺🇦 Ukraine"
                "KG" -> "🇰🇬 Kyrgyzstan"
                else -> "🇷🇺 $country"
            }
            else -> "🇮🇩 Indonesia"
        }
    }
}

data class LanguageGroup(
    val code: String,
    val name: String,
    val flag: String,
    val countries: List<Country>
)

data class Country(
    val code: String,
    val name: String,
    val flag: String
)

object LanguageData {
    val languages = listOf(
        LanguageGroup(
            code = "id",
            name = "Bahasa Indonesia",
            flag = "🇮🇩",
            countries = listOf(
                Country("ID", "Indonesia", "🇮🇩")
            )
        ),
        LanguageGroup(
            code = "en",
            name = "English",
            flag = "🇬🇧",
            countries = listOf(
                Country("NG", "Nigeria", "🇳🇬"),
                Country("ZA", "South Africa", "🇿🇦"),
                Country("KE", "Kenya", "🇰🇪"),
                Country("GH", "Ghana", "🇬🇭"),
                Country("UG", "Uganda", "🇺🇬"),
                Country("TZ", "Tanzania", "🇹🇿"),
                Country("ET", "Ethiopia", "🇪🇹"),
                Country("PH", "Philippines", "🇵🇭"),
                Country("SG", "Singapore", "🇸🇬"),
                Country("HK", "Hong Kong", "🇭🇰"),
                Country("GB", "United Kingdom", "🇬🇧"),
                Country("UA", "Ukraine", "🇺🇦"),
                Country("PL", "Poland", "🇵🇱"),
                Country("RO", "Romania", "🇷🇴"),
                Country("CZ", "Czech Republic", "🇨🇿")
            )
        ),
        LanguageGroup(
            code = "es",
            name = "Español",
            flag = "🇪🇸",
            countries = listOf(
                Country("MX", "México", "🇲🇽"),
                Country("AR", "Argentina", "🇦🇷"),
                Country("CL", "Chile", "🇨🇱"),
                Country("CO", "Colombia", "🇨🇴"),
                Country("PE", "Perú", "🇵🇪"),
                Country("VE", "Venezuela", "🇻🇪"),
                Country("CR", "Costa Rica", "🇨🇷"),
                Country("EC", "Ecuador", "🇪🇨"),
                Country("UY", "Uruguay", "🇺🇾"),
                Country("PY", "Paraguay", "🇵🇾"),
                Country("BO", "Bolivia", "🇧🇴"),
                Country("SV", "El Salvador", "🇸🇻"),
                Country("GT", "Guatemala", "🇬🇹"),
                Country("HN", "Honduras", "🇭🇳"),
                Country("PA", "Panamá", "🇵🇦"),
                Country("DO", "República Dominicana", "🇩🇴"),
                Country("CU", "Cuba", "🇨🇺")
            )
        ),
        LanguageGroup(
            code = "vi",
            name = "Tiếng Việt",
            flag = "🇻🇳",
            countries = listOf(
                Country("VN", "Việt Nam", "🇻🇳"),
                Country("LA", "Laos", "🇱🇦"),
                Country("TH", "Thailand", "🇹🇭"),
                Country("KH", "Cambodia", "🇰🇭")
            )
        ),
        LanguageGroup(
            code = "tr",
            name = "Türkçe",
            flag = "🇹🇷",
            countries = listOf(
                Country("TR", "Türkiye", "🇹🇷"),
                Country("CY", "Cyprus", "🇨🇾")
            )
        ),
        LanguageGroup(
            code = "hi",
            name = "हिन्दी",
            flag = "🇮🇳",
            countries = listOf(
                Country("IN", "India", "🇮🇳"),
                Country("NP", "Nepal", "🇳🇵"),
                Country("FJ", "Fiji", "🇫🇯")
            )
        ),
        LanguageGroup(
            code = "ms",
            name = "Bahasa Melayu",
            flag = "🇲🇾",
            countries = listOf(
                Country("MY", "Malaysia", "🇲🇾"),
                Country("BN", "Brunei", "🇧🇳")
            )
        ),
        LanguageGroup(
            code = "bn",
            name = "বাংলা",
            flag = "🇧🇩",
            countries = listOf(
                Country("BD", "Bangladesh", "🇧🇩"),
                Country("PK", "Pakistan", "🇵🇰")
            )
        ),
        LanguageGroup(
            code = "ru",
            name = "Русский",
            flag = "🇷🇺",
            countries = listOf(
                Country("RU", "Russia", "🇷🇺"),
                Country("KZ", "Kazakhstan", "🇰🇿"),
                Country("BY", "Belarus", "🇧🇾"),
                Country("UA", "Ukraine", "🇺🇦"),
                Country("KG", "Kyrgyzstan", "🇰🇬")
            )
        )
    )
}