package com.autotrade.finalstc.presentation.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.presentation.main.dashboard.DashboardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(DashboardTheme())
    val currentTheme: StateFlow<DashboardTheme> = _currentTheme.asStateFlow()

    init {
        viewModelScope.launch {
            val savedIsDarkMode = themeRepository.getThemePreference()
            _currentTheme.value = DashboardTheme(isDarkMode = savedIsDarkMode)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val newTheme = _currentTheme.value.copy(
                isDarkMode = !_currentTheme.value.isDarkMode
            )
            _currentTheme.value = newTheme

            // Save theme preference
            themeRepository.saveThemePreference(newTheme.isDarkMode)
        }
    }

    fun setDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            val newTheme = _currentTheme.value.copy(isDarkMode = isDarkMode)
            _currentTheme.value = newTheme

            // Save theme preference
            themeRepository.saveThemePreference(isDarkMode)
        }
    }
}

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        THEME_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun getThemePreference(): Boolean {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getBoolean(KEY_IS_DARK_MODE, true)
        }
    }

    suspend fun saveThemePreference(isDarkMode: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putBoolean(KEY_IS_DARK_MODE, isDarkMode)
                .apply()
        }
    }

    companion object {
        private const val THEME_PREFS_NAME = "theme_preferences"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
    }
}