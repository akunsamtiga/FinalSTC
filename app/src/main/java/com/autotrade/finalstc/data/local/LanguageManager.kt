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
            "en" -> "🇬🇧 $country"
            "es" -> "🇪🇸 $country"
            "vi" -> "🇻🇳 $country"
            "tr" -> "🇹🇷 Türkiye"
            "hi" -> "🇮🇳 India"
            "ms" -> "🇲🇾 Malaysia"
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
                Country("PH", "Philippines", "🇵🇭"),
                Country("ZA", "South Africa", "🇿🇦"),
                Country("KE", "Kenya", "🇰🇪"),
                Country("GB", "United Kingdom", "🇬🇧"),
                Country("UA", "Ukraine", "🇺🇦")
            )
        ),
        LanguageGroup(
            code = "es",
            name = "Español",
            flag = "🇪🇸",
            countries = listOf(
                Country("MX", "México", "🇲🇽"),
                Country("CL", "Chile", "🇨🇱"),
                Country("CO", "Colombia", "🇨🇴"),
                Country("CR", "Costa Rica", "🇨🇷"),
                Country("DO", "República Dominicana", "🇩🇴"),
                Country("EC", "Ecuador", "🇪🇨"),
                Country("SV", "El Salvador", "🇸🇻"),
                Country("GT", "Guatemala", "🇬🇹"),
                Country("HN", "Honduras", "🇭🇳"),
                Country("PA", "Panamá", "🇵🇦"),
                Country("PY", "Paraguay", "🇵🇾"),
                Country("PE", "Perú", "🇵🇪"),
                Country("UY", "Uruguay", "🇺🇾"),
                Country("VE", "Venezuela", "🇻🇪"),
                Country("BR", "Brasil", "🇧🇷")
            )
        ),
        LanguageGroup(
            code = "vi",
            name = "Tiếng Việt",
            flag = "🇻🇳",
            countries = listOf(
                Country("VN", "Việt Nam", "🇻🇳"),
                Country("LA", "Laos", "🇱🇦"),
                Country("TH", "Thailand", "🇹🇭")
            )
        ),
        LanguageGroup(
            code = "tr",
            name = "Türkçe",
            flag = "🇹🇷",
            countries = listOf(
                Country("TR", "Türkiye", "🇹🇷")
            )
        ),
        LanguageGroup(
            code = "hi",
            name = "हिन्दी",
            flag = "🇮🇳",
            countries = listOf(
                Country("IN", "India", "🇮🇳")
            )
        ),
        LanguageGroup(
            code = "ms",
            name = "Bahasa Melayu",
            flag = "🇲🇾",
            countries = listOf(
                Country("MY", "Malaysia", "🇲🇾")
            )
        )
    )
}