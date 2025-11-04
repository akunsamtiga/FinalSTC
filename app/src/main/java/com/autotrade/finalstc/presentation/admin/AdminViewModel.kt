package com.autotrade.finalstc.presentation.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrade.finalstc.data.repository.FirebaseRepository
import com.autotrade.finalstc.data.model.WhitelistUser
import com.autotrade.finalstc.data.model.RegistrationConfig
import com.autotrade.finalstc.data.model.AdminUser
import com.autotrade.finalstc.utils.FileExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val whitelistUsers: List<WhitelistUser> = emptyList(),
    val adminUsers: List<AdminUser> = emptyList(),
    val registrationConfig: RegistrationConfig = RegistrationConfig(),
    val isLoading: Boolean = false,
    val isLoadingConfig: Boolean = false,
    val isLoadingAdmins: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentUserEmail: String = "",
    val isSuperAdmin: Boolean = false
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val fileExportHelper = FileExportHelper(context)

    init {
        // Whitelist users akan di-load setelah setCurrentUserEmail dipanggil

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingConfig = true)
            try {
                firebaseRepository.getRegistrationConfigFlow()
                    .collect { config ->
                        _uiState.value = _uiState.value.copy(
                            registrationConfig = config,
                            isLoadingConfig = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingConfig = false,
                    error = "Gagal memuat konfigurasi: ${e.message}"
                )
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAdmins = true)
            try {
                firebaseRepository.getAdminUsers()
                    .collect { admins ->
                        _uiState.value = _uiState.value.copy(
                            adminUsers = admins,
                            isLoadingAdmins = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAdmins = false,
                    error = "Gagal memuat daftar admin: ${e.message}"
                )
            }
        }
    }

    fun setCurrentUserEmail(email: String) {
        viewModelScope.launch {
            val isSuperAdmin = firebaseRepository.checkIsSuperAdmin(email)
            _uiState.value = _uiState.value.copy(
                currentUserEmail = email,
                isSuperAdmin = isSuperAdmin
            )

            // Load whitelist users dengan filter berdasarkan role
            loadWhitelistUsers(email, isSuperAdmin)
        }
    }

    private fun loadWhitelistUsers(email: String, isSuperAdmin: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                firebaseRepository.getWhitelistUsers(
                    adminEmail = email,
                    isSuperAdmin = isSuperAdmin
                ).collect { users ->
                    _uiState.value = _uiState.value.copy(
                        whitelistUsers = users,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal memuat pengguna: ${e.message}"
                )
            }
        }
    }

    // ===== WHITELIST USER METHODS =====

    fun addUser(user: WhitelistUser) {
        viewModelScope.launch {
            try {
                val currentEmail = _uiState.value.currentUserEmail
                firebaseRepository.addWhitelistUser(user, currentEmail)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pengguna berhasil ditambahkan"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal menambah pengguna: ${e.message}"
                )
            }
        }
    }

    fun getUsersAddedByAdmin(adminEmail: String) {
        viewModelScope.launch {
            try {
                val users = firebaseRepository.getUsersAddedByAdmin(adminEmail)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ditemukan ${users.size} pengguna yang ditambahkan oleh $adminEmail"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal mengambil data: ${e.message}"
                )
            }
        }
    }

    fun updateUser(user: WhitelistUser) {
        viewModelScope.launch {
            try {
                firebaseRepository.updateWhitelistUser(user)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pengguna berhasil diperbarui"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memperbarui pengguna: ${e.message}"
                )
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                firebaseRepository.deleteWhitelistUser(userId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pengguna berhasil dihapus"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal menghapus pengguna: ${e.message}"
                )
            }
        }
    }

    fun toggleUserStatus(user: WhitelistUser) {
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(isActive = !user.isActive)
                firebaseRepository.updateWhitelistUser(updatedUser)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Status pengguna berhasil diperbarui"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memperbarui status pengguna: ${e.message}"
                )
            }
        }
    }

    // ===== ADMIN USER METHODS (SUPER ADMIN ONLY) =====

    fun addAdmin(admin: AdminUser) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat menambah admin"
            )
            return
        }

        viewModelScope.launch {
            try {
                val currentEmail = _uiState.value.currentUserEmail
                firebaseRepository.addAdminUser(admin, currentEmail)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Admin berhasil ditambahkan"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = if (e.message?.contains("sudah terdaftar") == true)
                        "Email admin sudah terdaftar"
                    else "Gagal menambah admin: ${e.message}"
                )
            }
        }
    }

    fun updateAdmin(admin: AdminUser) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat mengupdate admin"
            )
            return
        }

        viewModelScope.launch {
            try {
                firebaseRepository.updateAdminUser(admin)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Admin berhasil diperbarui"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memperbarui admin: ${e.message}"
                )
            }
        }
    }

    fun deleteAdmin(adminId: String) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat menghapus admin"
            )
            return
        }

        viewModelScope.launch {
            try {
                firebaseRepository.deleteAdminUser(adminId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Admin berhasil dihapus"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal menghapus admin: ${e.message}"
                )
            }
        }
    }

    fun toggleAdminStatus(admin: AdminUser) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat mengubah status admin"
            )
            return
        }

        viewModelScope.launch {
            try {
                val updatedAdmin = admin.copy(isActive = !admin.isActive)
                firebaseRepository.updateAdminUser(updatedAdmin)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Status admin berhasil diperbarui"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Gagal memperbarui status admin: ${e.message}"
                )
            }
        }
    }

    // ===== REGISTRATION CONFIG METHODS (SUPER ADMIN ONLY) =====

    fun updateRegistrationUrl(newUrl: String) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat mengubah URL registrasi"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingConfig = true)
                val success = firebaseRepository.updateRegistrationUrl(newUrl, "admin")

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        successMessage = "URL registrasi berhasil diperbarui"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        error = "Gagal memperbarui URL registrasi"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingConfig = false,
                    error = "Error memperbarui URL: ${e.message}"
                )
            }
        }
    }

    fun validateRegistrationUrl(url: String): Boolean {
        return try {
            val urlPattern = Regex(
                "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
                RegexOption.IGNORE_CASE
            )
            urlPattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }

    fun validateEmail(email: String): Boolean {
        return try {
            val emailPattern = Regex(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            )
            emailPattern.matches(email)
        } catch (e: Exception) {
            false
        }
    }

    // ===== EXPORT/IMPORT METHODS =====

    fun exportWhitelist(format: String = "json") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val exportData = when (format) {
                    "csv" -> firebaseRepository.exportWhitelistAsCsv()
                    else -> firebaseRepository.exportWhitelistAsJson()
                }

                val success = if (format == "csv") {
                    fileExportHelper.exportCsvToDownload(exportData)
                } else {
                    fileExportHelper.exportJsonToDownload(exportData)
                }

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "File berhasil didownload ke folder Downloads"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal mendownload file"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal export: ${e.message}"
                )
            }
        }
    }

    fun shareWhitelist(format: String = "json") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val exportData = when (format) {
                    "csv" -> firebaseRepository.exportWhitelistAsCsv()
                    else -> firebaseRepository.exportWhitelistAsJson()
                }

                val fileName = if (format == "csv") "whitelist_export.csv" else "whitelist_export.json"
                val mimeType = if (format == "csv") "text/csv" else "application/json"

                val success = fileExportHelper.shareFile(exportData, fileName, mimeType)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (success) "Membuka dialog berbagi..." else null,
                    error = if (!success) "Gagal membuka dialog berbagi" else null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal export: ${e.message}"
                )
            }
        }
    }

    fun copyToClipboard(format: String = "json") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val exportData = when (format) {
                    "csv" -> firebaseRepository.exportWhitelistAsCsv()
                    else -> firebaseRepository.exportWhitelistAsJson()
                }

                val success = fileExportHelper.copyToClipboard(exportData, "Whitelist Export")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (success) "Data berhasil disalin ke clipboard" else null,
                    error = if (!success) "Gagal menyalin data" else null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal export: ${e.message}"
                )
            }
        }
    }

    fun importWhitelist(jsonData: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val successCount = firebaseRepository.importWhitelistFromJson(jsonData)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Import berhasil: $successCount user ditambahkan"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal import: ${e.message}"
                )
            }
        }
    }

    fun updateWhatsappNumber(newNumber: String) {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "Akses ditolak: Hanya Super Admin yang dapat mengubah nomor WhatsApp"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingConfig = true)

                // Clean the number (remove spaces, dashes, etc)
                val cleanNumber = newNumber.replace(Regex("[^0-9]"), "")

                val success = firebaseRepository.updateWhatsappNumber(cleanNumber, "admin")

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        successMessage = "Nomor WhatsApp berhasil diperbarui"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        error = "Gagal memperbarui nomor WhatsApp"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingConfig = false,
                    error = "Error memperbarui nomor WhatsApp: ${e.message}"
                )
            }
        }
    }

    fun validateWhatsappNumber(number: String): Boolean {
        return firebaseRepository.validateWhatsappNumber(number)
    }
    // ===== UTILITY METHODS =====

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}