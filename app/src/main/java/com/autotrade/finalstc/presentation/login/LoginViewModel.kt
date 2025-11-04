package com.autotrade.finalstc.presentation.login

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.data.repository.LoginRepository
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.local.LanguageManager
import com.autotrade.finalstc.utils.StringsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val showValidationErrors: Boolean = false,
    val loadingMessage: String = "Sedang memuat...",
    val isStockityLoginSuccess: Boolean = false,
    val showLanguageDialog: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val sessionManager: SessionManager,
    private val languageManager: LanguageManager,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> = _uiState

    // Expose languageManager flows directly untuk reactive updates
    val currentLanguage: StateFlow<String> = languageManager.currentLanguage
    val currentCountry: StateFlow<String> = languageManager.currentCountry

    private val _whatsappNumber = mutableStateOf("6285959860015")
    val whatsappNumber: State<String> = _whatsappNumber

    init {
        loadSavedCredentials()
        loadWhatsappNumber()
    }

    private fun loadWhatsappNumber() {
        viewModelScope.launch {
            try {
                val config = firebaseRepository.getRegistrationConfig()
                _whatsappNumber.value = config.whatsappHelpNumber
                Log.d("LoginViewModel", "WhatsApp number loaded: ${config.whatsappHelpNumber}")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error loading WhatsApp number: ${e.message}")
            }
        }
    }

    private fun loadSavedCredentials() {
        val (savedEmail, savedPassword, rememberMe) = sessionManager.getSavedCredentials()
        _uiState.value = _uiState.value.copy(
            email = savedEmail,
            password = savedPassword,
            rememberMe = rememberMe
        )
    }

    fun updateEmail(email: String) {
        val isValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null,
            isEmailValid = isValid,
            showValidationErrors = false
        )
    }

    fun updatePassword(password: String) {
        val isValid = password.isEmpty() || password.length >= 6
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null,
            isPasswordValid = isValid,
            showValidationErrors = false
        )
    }

    fun updateRememberMe(rememberMe: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = rememberMe)
    }

    fun showLanguageDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLanguageDialog = show)
    }

    fun updateLanguage(languageCode: String, countryCode: String) {
        viewModelScope.launch {
            languageManager.saveLanguage(languageCode, countryCode)
            _uiState.value = _uiState.value.copy(showLanguageDialog = false)
        }
    }

    fun login(email: String, password: String) {
        val lang = languageManager.getLanguage()

        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            showValidationErrors = true,
            isStockityLoginSuccess = false
        )

        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = StringsManager.getEmailEmpty(lang),
                    isEmailValid = false
                )
                return
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = StringsManager.getPasswordEmpty(lang),
                    isPasswordValid = false
                )
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = StringsManager.getEmailInvalid(lang),
                    isEmailValid = false
                )
                return
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = StringsManager.getPasswordTooShort(lang),
                    isPasswordValid = false
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isEmailValid = true,
                isPasswordValid = true,
                loadingMessage = StringsManager.getLoadingStockity(lang)
            )

            delay(300)

            _uiState.value = _uiState.value.copy(
                loadingMessage = StringsManager.getLoadingAuth(lang)
            )

            delay(500)

            _uiState.value = _uiState.value.copy(
                loadingMessage = StringsManager.getLoadingAccess(lang)
            )

            loginRepository.login(email, password)
                .onSuccess {
                    sessionManager.saveCredentials(email, password, _uiState.value.rememberMe)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null,
                        loadingMessage = StringsManager.getLoginSuccess(lang),
                        isStockityLoginSuccess = true
                    )
                }
                .onFailure { exception ->
                    val errorMessage = categorizeError(exception.message ?: "")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = errorMessage,
                        loadingMessage = StringsManager.getLoadingDefault(lang),
                        isStockityLoginSuccess = errorMessage.contains("Login Stockity berhasil")
                    )
                }
        }
    }

    private fun categorizeError(originalError: String): String {
        return when {
            originalError.contains("Email atau password salah untuk akun Stockity", ignoreCase = true) ->
                "Email atau password Stockity salah\n\nPastikan kredensial Stockity Anda benar."

            originalError.contains("Akun Stockity diblokir", ignoreCase = true) ->
                "Akun Stockity diblokir\n\nHubungi customer service Stockity."

            originalError.contains("Server Stockity error", ignoreCase = true) ->
                "Server Stockity bermasalah\n\nSilakan coba lagi dalam beberapa menit."

            originalError.contains("Login Stockity berhasil", ignoreCase = true) -> {
                when {
                    originalError.contains("tidak aktif", ignoreCase = true) ->
                        "Login Stockity berhasil!\n\nAkun tidak aktif di aplikasi\n\nHubungi admin untuk aktivasi."

                    originalError.contains("belum terdaftar", ignoreCase = true) ->
                        "Login Stockity berhasil!\n\nAkun belum terdaftar di aplikasi\n\nHubungi admin untuk registrasi."

                    else -> originalError
                }
            }

            originalError.contains("tidak dapat terhubung ke database whitelist", ignoreCase = true) ->
                "Login Stockity berhasil!\n\nTidak dapat mengecek akses aplikasi\n\nPeriksa koneksi internet Anda."

            originalError.contains("timeout", ignoreCase = true) ->
                "Koneksi timeout\n\nPeriksa koneksi internet dan coba lagi."

            originalError.contains("network", ignoreCase = true) ||
                    originalError.contains("connection", ignoreCase = true) ->
                "Masalah koneksi\n\nPeriksa internet Anda dan coba lagi."

            else -> originalError
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            showValidationErrors = false,
            isEmailValid = true,
            isPasswordValid = true
        )
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isLoginButtonEnabled(): Boolean {
        return uiState.value.email.isNotBlank() &&
                uiState.value.password.isNotBlank() &&
                !uiState.value.isLoading &&
                validateEmail(uiState.value.email) &&
                validatePassword(uiState.value.password)
    }
}