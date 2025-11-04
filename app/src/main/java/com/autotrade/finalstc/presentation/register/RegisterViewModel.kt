package com.autotrade.finalstc.presentation.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.data.api.UserProfileApiService
import com.autotrade.finalstc.data.local.SessionManager
import com.autotrade.finalstc.data.model.UserSession
import com.autotrade.finalstc.data.model.WhitelistUser
import com.autotrade.finalstc.data.repository.CurrencyRepository
import com.autotrade.finalstc.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val registrationUrl: String = "https://stockity.id/registered?a=25db72fbbc00",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSavingToWhitelist: Boolean = false,
    val saveToWhitelistSuccess: Boolean = false,
    val saveToWhitelistError: String? = null,
    val isUserBlocked: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val userProfileApiService: UserProfileApiService,
    private val sessionManager: SessionManager,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _whatsappNumber = MutableStateFlow("6285959860015")
    val whatsappNumber: StateFlow<String> = _whatsappNumber.asStateFlow()

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    init {
        loadRegistrationConfig()
        loadWhatsappNumber()
    }

    private fun loadWhatsappNumber() {
        viewModelScope.launch {
            try {
                val config = firebaseRepository.getRegistrationConfig()
                _whatsappNumber.value = config.whatsappHelpNumber
                Log.d(TAG, "WhatsApp number loaded: ${config.whatsappHelpNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading WhatsApp number: ${e.message}")
            }
        }
    }

    private fun loadRegistrationConfig() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading registration config")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val config = firebaseRepository.getRegistrationConfig()
                Log.d(TAG, "Registration config loaded: ${config.registrationUrl}")

                _uiState.value = _uiState.value.copy(
                    registrationUrl = config.registrationUrl,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading registration config: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load registration config, using default URL"
                )
            }
        }
    }

    fun saveUserToWhitelistAndLogin(
        authToken: String,
        deviceId: String,
        email: String
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== START SAVE TO WHITELIST PROCESS ===")
                _uiState.value = _uiState.value.copy(
                    isSavingToWhitelist = true,
                    saveToWhitelistError = null,
                    saveToWhitelistSuccess = false,
                    isUserBlocked = false
                )

                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

                Log.d(TAG, "Fetching user profile from API")
                val response = userProfileApiService.getUserProfile(
                    deviceId = deviceId,
                    authorizationToken = authToken,
                    userAgent = userAgent
                )

                if (!response.isSuccessful || response.body()?.data == null) {
                    val errorMsg = "Gagal mengambil data profil pengguna (${response.code()})"
                    Log.e(TAG, errorMsg)
                    _uiState.value = _uiState.value.copy(
                        isSavingToWhitelist = false,
                        saveToWhitelistError = errorMsg
                    )
                    return@launch
                }

                val userProfile = response.body()!!.data!!
                Log.d(TAG, "User profile fetched successfully:")
                Log.d(TAG, "  Email: ${userProfile.email}")
                Log.d(TAG, "  UserID: ${userProfile.id}")
                Log.d(TAG, "  Name: ${userProfile.getFullName()}")

                Log.d(TAG, "=== STEP 1: Checking by EMAIL (${userProfile.email}) ===")
                val existingUserByEmail = firebaseRepository.getWhitelistUserByEmail(userProfile.email)

                if (existingUserByEmail != null) {
                    Log.d(TAG, "User found by email: ${existingUserByEmail.name}")
                    Log.d(TAG, "  isActive: ${existingUserByEmail.isActive}")
                    Log.d(TAG, "  userId: ${existingUserByEmail.userId}")

                    if (!existingUserByEmail.isActive) {
                        Log.w(TAG, "USER IS BLOCKED (isActive = false)")
                        _uiState.value = _uiState.value.copy(
                            isSavingToWhitelist = false,
                            isUserBlocked = true,
                            saveToWhitelistError = "Akun kamu belum aktif. Silahkan hubungi admin untuk aktivasi STC Autotrade."
                        )
                        return@launch
                    }

                    Log.d(TAG, "User is ACTIVE, proceeding with login")
                    firebaseRepository.updateLastLogin(existingUserByEmail.userId)

                    saveUserSession(authToken, deviceId, userProfile.email, existingUserByEmail.userId, userAgent)

                    _uiState.value = _uiState.value.copy(
                        isSavingToWhitelist = false,
                        saveToWhitelistSuccess = true
                    )

                    Log.d(TAG, "=== LOGIN SUCCESSFUL (existing user by email) ===")
                    return@launch
                }

                Log.d(TAG, "User NOT found by email")

                Log.d(TAG, "=== STEP 2: Checking by USER ID (${userProfile.id}) ===")
                val existingUserByUserId = firebaseRepository.getWhitelistUserByUserId(userProfile.id.toString())

                if (existingUserByUserId != null) {
                    Log.d(TAG, "User found by userId: ${existingUserByUserId.name}")
                    Log.d(TAG, "  isActive: ${existingUserByUserId.isActive}")
                    Log.d(TAG, "  email: ${existingUserByUserId.email}")

                    if (!existingUserByUserId.isActive) {
                        Log.w(TAG, "USER IS BLOCKED (isActive = false)")
                        _uiState.value = _uiState.value.copy(
                            isSavingToWhitelist = false,
                            isUserBlocked = true,
                            saveToWhitelistError = "Anda diblokir. Hubungi administrator untuk informasi lebih lanjut."
                        )
                        return@launch
                    }

                    Log.d(TAG, "User is ACTIVE, proceeding with login")
                    firebaseRepository.updateLastLogin(existingUserByUserId.userId)

                    saveUserSession(authToken, deviceId, userProfile.email, existingUserByUserId.userId, userAgent)

                    _uiState.value = _uiState.value.copy(
                        isSavingToWhitelist = false,
                        saveToWhitelistSuccess = true
                    )

                    Log.d(TAG, "=== LOGIN SUCCESSFUL (existing user by userId) ===")
                    return@launch
                }

                Log.d(TAG, "User NOT found by userId")

                Log.d(TAG, "=== STEP 3: Creating NEW USER ===")
                Log.d(TAG, "User does not exist in database, creating new entry")

                val newWhitelistUser = WhitelistUser(
                    id = "",
                    email = userProfile.email,
                    name = userProfile.getFullName(),
                    userId = userProfile.id.toString(),
                    deviceId = deviceId,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis(),
                    addedBy = "web_registration"
                )

                Log.d(TAG, "New user details:")
                Log.d(TAG, "  Name: ${newWhitelistUser.name}")
                Log.d(TAG, "  Email: ${newWhitelistUser.email}")
                Log.d(TAG, "  UserId: ${newWhitelistUser.userId}")
                Log.d(TAG, "  isActive: ${newWhitelistUser.isActive}")

                firebaseRepository.addWhitelistUser(newWhitelistUser, addedBy = "web_registration")

                Log.d(TAG, "New user successfully saved to whitelist")

                saveUserSession(authToken, deviceId, userProfile.email, userProfile.id.toString(), userAgent)

                _uiState.value = _uiState.value.copy(
                    isSavingToWhitelist = false,
                    saveToWhitelistSuccess = true,
                    saveToWhitelistError = null
                )

                Log.d(TAG, "=== AUTO-REGISTRATION COMPLETED SUCCESSFULLY ===")

            } catch (e: Exception) {
                Log.e(TAG, "=== ERROR DURING SAVE TO WHITELIST ===")
                Log.e(TAG, "Error message: ${e.message}", e)

                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Koneksi timeout. Silakan coba lagi."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Masalah jaringan. Periksa koneksi internet Anda."
                    else -> "Gagal Menghubungkan: ${e.message}"
                }

                _uiState.value = _uiState.value.copy(
                    isSavingToWhitelist = false,
                    saveToWhitelistError = errorMsg,
                    saveToWhitelistSuccess = false
                )
            }
        }
    }

    private fun saveUserSession(
        authToken: String,
        deviceId: String,
        email: String,
        userId: String,
        userAgent: String
    ) {
        var userSession = UserSession(
            authtoken = authToken,
            userId = userId,
            deviceId = deviceId,
            email = email,
            userTimezone = "Asia/Bangkok",
            userAgent = userAgent,
            deviceType = "web",
            currency = "IDR"
        )
        sessionManager.saveUserSession(userSession)
        Log.d(TAG, "User session saved successfully")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching user currency after registration")
                val currencyResult = currencyRepository.fetchUserCurrency()

                if (currencyResult.isSuccess) {
                    val currencyData = currencyResult.getOrNull()
                    Log.d(TAG, "Currency fetched: ${currencyData?.current}")

                    userSession = userSession.copy(
                        currency = currencyData?.current ?: "IDR"
                    )
                    sessionManager.saveUserSession(userSession)
                } else {
                    Log.w(TAG, "Failed to fetch currency, using default IDR")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching currency: ${e.message}", e)
            }
        }
    }

    fun retryLoadConfig() {
        Log.d(TAG, "Retrying to load registration config")
        loadRegistrationConfig()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(
            saveToWhitelistError = null,
            saveToWhitelistSuccess = false,
            isUserBlocked = false
        )
    }
}