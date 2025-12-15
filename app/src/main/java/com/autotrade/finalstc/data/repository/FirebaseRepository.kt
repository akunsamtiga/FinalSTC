package com.autotrade.finalstc.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.autotrade.finalstc.data.model.WhitelistUser
import com.autotrade.finalstc.data.model.RegistrationConfig
import com.autotrade.finalstc.data.model.AdminUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val whitelistCollection = firestore.collection("whitelist_users")
    private val configCollection = firestore.collection("app_config")
    private val adminCollection = firestore.collection("admin_users")

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val COLLECTION_WHITELIST = "whitelist_users"
        private const val COLLECTION_CONFIG = "app_config"
        private const val COLLECTION_ADMIN = "admin_users"
        private const val CONFIG_REGISTRATION_ID = "registration_config"
        const val SUPER_ADMIN_EMAIL = "aryasis87@gmail.com"
    }

    // ===== EXISTING WHITELIST METHODS =====
    fun getWhitelistUsers(adminEmail: String = "", isSuperAdmin: Boolean = true): Flow<List<WhitelistUser>> {
        Log.d(TAG, "Mengambil data pengguna whitelist dari koleksi: $COLLECTION_WHITELIST")
        Log.d(TAG, "Admin email: $adminEmail, Is Super Admin: $isSuperAdmin")

        return if (isSuperAdmin || adminEmail.isEmpty()) {
            // Super admin melihat semua users
            whitelistCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    Log.d(TAG, "Snapshot diterima dengan ${snapshot.size()} dokumen (Super Admin - All users)")

                    if (snapshot.isEmpty) {
                        Log.d(TAG, "Snapshot kosong - tidak ada dokumen ditemukan")
                        return@map emptyList<WhitelistUser>()
                    }

                    snapshot.documents.mapNotNull { document ->
                        try {
                            val user = document.toObject<WhitelistUser>()?.copy(id = document.id)
                            if (user != null) {
                                Log.d(TAG, "Berhasil konversi dokumen: ${document.id} - ${user.name} (UserID: ${user.userId}, aktif: ${user.isActive})")
                            } else {
                                Log.w(TAG, "Dokumen ${document.id} konversi ke null")
                            }
                            user
                        } catch (e: Exception) {
                            Log.e(TAG, "Error konversi dokumen ${document.id}: ${e.message}", e)
                            null
                        }
                    }
                }
                .catch { exception ->
                    Log.e(TAG, "Error dalam getWhitelistUsers flow", exception)
                    throw exception
                }
        } else {
            // Admin biasa hanya melihat users yang mereka tambahkan sendiri
            whitelistCollection
                .whereEqualTo("addedBy", adminEmail)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    Log.d(TAG, "Snapshot diterima dengan ${snapshot.size()} dokumen (Regular Admin - Filtered by addedBy: $adminEmail)")

                    if (snapshot.isEmpty) {
                        Log.d(TAG, "Snapshot kosong - tidak ada dokumen yang ditambahkan oleh $adminEmail")
                        return@map emptyList<WhitelistUser>()
                    }

                    snapshot.documents.mapNotNull { document ->
                        try {
                            val user = document.toObject<WhitelistUser>()?.copy(id = document.id)
                            if (user != null) {
                                Log.d(TAG, "Berhasil konversi dokumen: ${document.id} - ${user.name} (UserID: ${user.userId}, aktif: ${user.isActive}, addedBy: ${user.addedBy})")
                            } else {
                                Log.w(TAG, "Dokumen ${document.id} konversi ke null")
                            }
                            user
                        } catch (e: Exception) {
                            Log.e(TAG, "Error konversi dokumen ${document.id}: ${e.message}", e)
                            null
                        }
                    }
                }
                .catch { exception ->
                    Log.e(TAG, "Error dalam getWhitelistUsers flow", exception)
                    throw exception
                }
        }
    }

    suspend fun addWhitelistUser(user: WhitelistUser, addedBy: String = ""): String {
        return try {
            Log.d(TAG, "Menambah pengguna baru: ${user.name} (${user.email}) dengan UserID: ${user.userId}, aktif: ${user.isActive}, ditambahkan oleh: $addedBy")

            val userWithId = user.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis(),
                addedBy = addedBy,  // Set admin yang menambahkan
                isActive = true
            )

            val userData = mapOf(
                "id" to userWithId.id,
                "email" to userWithId.email,
                "name" to userWithId.name,
                "userId" to userWithId.userId,
                "deviceId" to userWithId.deviceId,
                "isActive" to userWithId.isActive,
                "createdAt" to userWithId.createdAt,
                "lastLogin" to userWithId.lastLogin,
                "addedBy" to userWithId.addedBy,      // Field baru
                "addedAt" to userWithId.addedAt       // Field baru
            )

            val documentRef = whitelistCollection.document(userWithId.id)
            documentRef.set(userData).await()

            Log.d(TAG, "Berhasil menambah pengguna dengan ID: ${userWithId.id} oleh: $addedBy")

            val verifyDoc = documentRef.get().await()
            if (verifyDoc.exists()) {
                Log.d(TAG, "Verifikasi: Dokumen ada dengan data: ${verifyDoc.data}")
                val rawData = verifyDoc.data
                Log.d(TAG, "Verifikasi: Field addedBy: ${rawData?.get("addedBy")}")
                Log.d(TAG, "Verifikasi: Field addedAt: ${rawData?.get("addedAt")}")
            } else {
                Log.e(TAG, "Verifikasi gagal: Dokumen tidak ada")
            }

            userWithId.id
        } catch (e: Exception) {
            Log.e(TAG, "Error menambah pengguna: ${e.message}", e)
            throw e
        }
    }


    suspend fun updateWhitelistUser(user: WhitelistUser) {
        try {
            if (user.id.isEmpty()) {
                throw IllegalArgumentException("ID pengguna tidak boleh kosong")
            }

            Log.d(TAG, "Memperbarui pengguna: ${user.id} - ${user.name}, aktif: ${user.isActive}")

            val userData = mapOf(
                "id" to user.id,
                "email" to user.email,
                "name" to user.name,
                "userId" to user.userId,
                "deviceId" to user.deviceId,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt,
                "lastLogin" to user.lastLogin,
                "addedBy" to user.addedBy,  // Pertahankan siapa yang menambahkan
                "addedAt" to user.addedAt   // Pertahankan kapan ditambahkan
            )

            whitelistCollection.document(user.id)
                .set(userData)
                .await()

            Log.d(TAG, "Berhasil memperbarui pengguna: ${user.id}")

            val verifyDoc = whitelistCollection.document(user.id).get().await()
            if (verifyDoc.exists()) {
                val rawData = verifyDoc.data
                Log.d(TAG, "Verifikasi: Field isActive: ${rawData?.get("isActive")}")
                Log.d(TAG, "Verifikasi: Field addedBy: ${rawData?.get("addedBy")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error memperbarui pengguna ${user.id}: ${e.message}", e)
            throw e
        }
    }

    suspend fun getUsersAddedByAdmin(adminEmail: String): List<WhitelistUser> {
        return try {
            Log.d(TAG, "Mengambil pengguna yang ditambahkan oleh: $adminEmail")

            val result = whitelistCollection
                .whereEqualTo("addedBy", adminEmail)
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val users = result.documents.mapNotNull { document ->
                document.toObject<WhitelistUser>()?.copy(id = document.id)
            }

            Log.d(TAG, "Ditemukan ${users.size} pengguna yang ditambahkan oleh $adminEmail")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil pengguna berdasarkan admin: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAdminUserStatistics(): Map<String, Int> {
        return try {
            Log.d(TAG, "Mengambil statistik penambahan user per admin")

            val allUsers = exportWhitelistUsers()
            val statistics = allUsers.groupingBy { it.addedBy }
                .eachCount()

            Log.d(TAG, "Statistik: $statistics")
            statistics
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil statistik: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun deleteWhitelistUser(userId: String) {
        try {
            if (userId.isEmpty()) {
                throw IllegalArgumentException("ID pengguna tidak boleh kosong")
            }

            Log.d(TAG, "Menghapus pengguna: $userId")

            whitelistCollection.document(userId)
                .delete()
                .await()

            Log.d(TAG, "Berhasil menghapus pengguna: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error menghapus pengguna $userId: ${e.message}", e)
            throw e
        }
    }

    suspend fun getWhitelistUserById(userId: String): WhitelistUser? {
        return try {
            Log.d(TAG, "Mengambil pengguna berdasarkan ID: $userId")

            val document = whitelistCollection.document(userId).get().await()
            val user = document.toObject<WhitelistUser>()?.copy(id = document.id)

            if (user != null) {
                Log.d(TAG, "Pengguna ditemukan: ${user.name}, aktif: ${user.isActive}")
            } else {
                Log.d(TAG, "Pengguna tidak ditemukan: $userId")
            }

            user
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil pengguna $userId: ${e.message}", e)
            null
        }
    }

    suspend fun checkUserInWhitelist(email: String, userId: String, deviceId: String): Boolean {
        return try {
            Log.d(TAG, "Memeriksa whitelist untuk: $email, userID: $userId, deviceID: $deviceId")

            val query = whitelistCollection
                .whereEqualTo("email", email)
                .whereEqualTo("userId", userId)
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val isWhitelisted = !query.isEmpty
            Log.d(TAG, "Hasil pemeriksaan whitelist pengguna: $isWhitelisted")

            isWhitelisted
        } catch (e: Exception) {
            Log.e(TAG, "Error memeriksa whitelist: ${e.message}", e)
            false
        }
    }

    suspend fun debugCheckUserIdExists(userId: String): Boolean {
        return try {
            Log.d(TAG, "Memeriksa userId: '$userId'")

            val queries = listOf(
                "userId" to userId,
                "user_id" to userId,
                "UserId" to userId,
                "USER_ID" to userId
            )

            for ((fieldName, value) in queries) {
                Log.d(TAG, "Mencoba field: '$fieldName' dengan nilai: '$value'")

                val query = whitelistCollection
                    .whereEqualTo(fieldName, value)
                    .get()
                    .await()

                Log.d(TAG, "Hasil query untuk '$fieldName': ${query.size()} dokumen")

                if (!query.isEmpty) {
                    query.documents.forEach { doc ->
                        Log.d(TAG, "Dokumen ditemukan: ${doc.id}, data: ${doc.data}")
                        val user = doc.toObject<WhitelistUser>()
                        Log.d(TAG, "Pengguna terkonversi: Nama=${user?.name}, aktif=${user?.isActive}")
                    }
                    return true
                }
            }

            Log.d(TAG, "userId '$userId' tidak ditemukan di field manapun")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error dalam debugCheckUserIdExists: ${e.message}", e)
            false
        }
    }

    suspend fun debugCheckAllDocuments(): List<WhitelistUser> {
        return try {
            Log.d(TAG, "Mengambil semua dokumen")

            val result = whitelistCollection.get().await()
            Log.d(TAG, "Total dokumen ditemukan: ${result.size()}")

            val users = result.documents.mapIndexedNotNull { index, document ->
                Log.d(TAG, "Dokumen $index:")
                Log.d(TAG, "  ID: ${document.id}")
                Log.d(TAG, "  Data mentah: ${document.data}")

                try {
                    val user = document.toObject<WhitelistUser>()?.copy(id = document.id)
                    Log.d(TAG, "  Pengguna terkonversi: Nama=${user?.name}, UserID='${user?.userId}', aktif=${user?.isActive}")
                    user
                } catch (e: Exception) {
                    Log.e(TAG, "  Error konversi dokumen: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Selesai mengambil semua dokumen - Ditemukan ${users.size} pengguna valid")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error dalam debugCheckAllDocuments: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun checkUserInWhitelistByUserId(userId: String): Boolean {
        return try {
            Log.d(TAG, "Memeriksa whitelist untuk userID: '$userId'")

            val userExists = debugCheckUserIdExists(userId)
            Log.d(TAG, "Hasil pemeriksaan debug: $userExists")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            Log.d(TAG, "Hasil query utama: ${query.size()} dokumen")

            if (query.isEmpty) {
                Log.w(TAG, "Tidak ada dokumen aktif ditemukan untuk userId: '$userId'")

                val fallbackQuery = whitelistCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                Log.d(TAG, "Query cadangan (tanpa filter aktif): ${fallbackQuery.size()} dokumen")

                if (!fallbackQuery.isEmpty) {
                    fallbackQuery.documents.forEach { doc ->
                        val user = doc.toObject<WhitelistUser>()
                        Log.w(TAG, "Ditemukan pengguna tidak aktif: ${user?.name}, aktif: ${user?.isActive}")
                        if (user?.isActive == false) {
                            Log.w(TAG, "Pengguna ada tetapi TIDAK AKTIF")
                        }
                    }
                    return false
                }

                Log.w(TAG, "Pengguna tidak ditemukan di whitelist sama sekali")
                return false
            }

            query.documents.forEach { doc ->
                val user = doc.toObject<WhitelistUser>()
                Log.d(TAG, "Ditemukan pengguna aktif: ${user?.name} (${user?.email}) - UserID: ${user?.userId}, aktif: ${user?.isActive}")
            }

            val isWhitelisted = !query.isEmpty
            Log.d(TAG, "Hasil pemeriksaan utama: $isWhitelisted")

            isWhitelisted
        } catch (e: Exception) {
            Log.e(TAG, "Error memeriksa whitelist untuk userID '$userId': ${e.message}", e)
            false
        }
    }

    suspend fun updateLastLogin(userId: String) {
        try {
            Log.d(TAG, "Memperbarui login terakhir untuk pengguna: $userId")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (!query.isEmpty) {
                val document = query.documents.first()
                document.reference.update("lastLogin", System.currentTimeMillis()).await()
                Log.d(TAG, "Berhasil memperbarui login terakhir untuk userID: $userId")
            } else {
                Log.w(TAG, "Tidak ada dokumen ditemukan untuk userID: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error memperbarui login terakhir untuk userID $userId: ${e.message}", e)
        }
    }

    suspend fun searchUsers(query: String): List<WhitelistUser> {
        return try {
            Log.d(TAG, "Mencari pengguna dengan query: $query")

            val nameResults = whitelistCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()

            val emailResults = whitelistCollection
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThanOrEqualTo("email", query + "\uf8ff")
                .get()
                .await()

            val allResults = mutableSetOf<WhitelistUser>()

            nameResults.documents.forEach { document ->
                document.toObject<WhitelistUser>()?.let { user ->
                    allResults.add(user.copy(id = document.id))
                }
            }

            emailResults.documents.forEach { document ->
                document.toObject<WhitelistUser>()?.let { user ->
                    allResults.add(user.copy(id = document.id))
                }
            }

            val results = allResults.toList().sortedByDescending { it.createdAt }
            Log.d(TAG, "Pencarian mengembalikan ${results.size} hasil")

            results
        } catch (e: Exception) {
            Log.e(TAG, "Error mencari pengguna: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getActiveUsersCount(): Int {
        return try {
            Log.d(TAG, "Mengambil jumlah pengguna aktif")

            val result = whitelistCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val count = result.size()
            Log.d(TAG, "Jumlah pengguna aktif: $count")

            count
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil jumlah pengguna aktif: ${e.message}", e)
            0
        }
    }

    suspend fun getRecentLoginUsers(hours: Int = 24): List<WhitelistUser> {
        val timeThreshold = System.currentTimeMillis() - (hours * 60 * 60 * 1000)

        return try {
            Log.d(TAG, "Mengambil pengguna login terbaru ($hours jam terakhir)")

            val result = whitelistCollection
                .whereGreaterThan("lastLogin", timeThreshold)
                .orderBy("lastLogin", Query.Direction.DESCENDING)
                .get()
                .await()

            val users = result.documents.mapNotNull { document ->
                document.toObject<WhitelistUser>()?.copy(id = document.id)
            }

            Log.d(TAG, "Ditemukan ${users.size} pengguna dengan login terbaru")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil pengguna login terbaru: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun bulkUpdateUserStatus(userIds: List<String>, isActive: Boolean) {
        try {
            Log.d(TAG, "Memperbarui status ${userIds.size} pengguna ke aktif: $isActive")

            val batch = firestore.batch()

            userIds.forEach { userId ->
                val docRef = whitelistCollection.document(userId)
                batch.update(docRef, "isActive", isActive)
            }

            batch.commit().await()
            Log.d(TAG, "Berhasil menyelesaikan pembaruan massal")
        } catch (e: Exception) {
            Log.e(TAG, "Error dalam pembaruan massal: ${e.message}", e)
            throw e
        }
    }

    suspend fun exportWhitelistUsers(): List<WhitelistUser> {
        return try {
            Log.d(TAG, "Mengekspor semua pengguna whitelist")

            val result = whitelistCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val users = result.documents.mapNotNull { document ->
                document.toObject<WhitelistUser>()?.copy(id = document.id)
            }

            Log.d(TAG, "Mengekspor ${users.size} pengguna")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error mengekspor pengguna: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun testFirestoreConnection(): Boolean {
        return try {
            Log.d(TAG, "Menguji koneksi Firestore")

            val testDoc = firestore.collection("test").document("connection")
            testDoc.set(mapOf("timestamp" to System.currentTimeMillis())).await()
            testDoc.delete().await()

            Log.d(TAG, "Tes koneksi Firestore berhasil")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Tes koneksi Firestore gagal: ${e.message}", e)
            false
        }
    }

    // ===== REGISTRATION CONFIG METHODS =====

    suspend fun getRegistrationConfig(): RegistrationConfig {
        return try {
            Log.d(TAG, "Mengambil konfigurasi registrasi")

            val document = configCollection.document(CONFIG_REGISTRATION_ID).get().await()

            if (document.exists()) {
                val config = document.toObject<RegistrationConfig>()
                if (config != null) {
                    Log.d(TAG, "Konfigurasi registrasi ditemukan: ${config.registrationUrl}")
                    return config
                }
            }

            Log.d(TAG, "Konfigurasi registrasi tidak ditemukan, membuat default")
            val defaultConfig = RegistrationConfig()
            saveRegistrationConfig(defaultConfig)

            defaultConfig
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil konfigurasi registrasi: ${e.message}", e)
            RegistrationConfig()
        }
    }

    suspend fun saveRegistrationConfig(config: RegistrationConfig) {
        try {
            Log.d(TAG, "Menyimpan konfigurasi registrasi: ${config.registrationUrl}")

            val configData = mapOf(
                "id" to CONFIG_REGISTRATION_ID,
                "registrationUrl" to config.registrationUrl,
                "whatsappHelpUrl" to config.whatsappHelpUrl, // ✅ CHANGED
                "isActive" to config.isActive,
                "description" to config.description,
                "createdAt" to config.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to config.updatedBy
            )

            configCollection.document(CONFIG_REGISTRATION_ID)
                .set(configData)
                .await()

            Log.d(TAG, "Berhasil menyimpan konfigurasi registrasi")

        } catch (e: Exception) {
            Log.e(TAG, "Error menyimpan konfigurasi registrasi: ${e.message}", e)
            throw e
        }
    }


    suspend fun updateRegistrationUrl(newUrl: String, updatedBy: String = "admin"): Boolean {
        return try {
            Log.d(TAG, "Memperbarui URL registrasi ke: $newUrl")

            val currentConfig = getRegistrationConfig()
            val updatedConfig = currentConfig.copy(
                registrationUrl = newUrl,
                updatedAt = System.currentTimeMillis(),
                updatedBy = updatedBy
            )

            saveRegistrationConfig(updatedConfig)

            Log.d(TAG, "Berhasil memperbarui URL registrasi")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error memperbarui URL registrasi: ${e.message}", e)
            false
        }
    }

    fun getRegistrationConfigFlow(): Flow<RegistrationConfig> {
        Log.d(TAG, "Mengambil flow konfigurasi registrasi")

        return configCollection.document(CONFIG_REGISTRATION_ID)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject<RegistrationConfig>() ?: RegistrationConfig()
                } else {
                    val defaultConfig = RegistrationConfig()
                    saveRegistrationConfig(defaultConfig)
                    defaultConfig
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error dalam getRegistrationConfigFlow", exception)
                emit(RegistrationConfig())
            }
    }

    // ===== ADMIN USER METHODS =====

    fun getAdminUsers(): Flow<List<AdminUser>> {
        Log.d(TAG, "Mengambil data admin dari koleksi: $COLLECTION_ADMIN")

        return adminCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                Log.d(TAG, "Snapshot admin diterima dengan ${snapshot.size()} dokumen")

                if (snapshot.isEmpty) {
                    Log.d(TAG, "Snapshot admin kosong")
                    return@map emptyList<AdminUser>()
                }

                snapshot.documents.mapNotNull { document ->
                    try {
                        val admin = document.toObject<AdminUser>()?.copy(id = document.id)
                        if (admin != null) {
                            Log.d(TAG, "Admin ditemukan: ${admin.email} - Role: ${admin.role}, Aktif: ${admin.isActive}")
                        }
                        admin
                    } catch (e: Exception) {
                        Log.e(TAG, "Error konversi dokumen admin ${document.id}: ${e.message}", e)
                        null
                    }
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error dalam getAdminUsers flow", exception)
                throw exception
            }
    }

    suspend fun addAdminUser(admin: AdminUser, createdBy: String): String {
        return try {
            Log.d(TAG, "Menambah admin baru: ${admin.name} (${admin.email})")

            val existingAdmin = adminCollection
                .whereEqualTo("email", admin.email)
                .get()
                .await()

            if (!existingAdmin.isEmpty) {
                throw IllegalArgumentException("Email admin sudah terdaftar")
            }

            val adminWithId = admin.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                createdBy = createdBy,
                isActive = true
            )

            val adminData = mapOf(
                "id" to adminWithId.id,
                "email" to adminWithId.email,
                "name" to adminWithId.name,
                "userId" to adminWithId.userId,  // ✅ TAMBAHKAN INI
                "role" to adminWithId.role,
                "isActive" to adminWithId.isActive,
                "createdAt" to adminWithId.createdAt,
                "createdBy" to adminWithId.createdBy,
                "lastLogin" to adminWithId.lastLogin
            )

            adminCollection.document(adminWithId.id)
                .set(adminData)
                .await()

            Log.d(TAG, "Berhasil menambah admin dengan ID: ${adminWithId.id}")
            adminWithId.id
        } catch (e: Exception) {
            Log.e(TAG, "Error menambah admin: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateAdminUser(admin: AdminUser) {
        try {
            if (admin.id.isEmpty()) {
                throw IllegalArgumentException("ID admin tidak boleh kosong")
            }

            Log.d(TAG, "Memperbarui admin: ${admin.id} - ${admin.name}")

            val adminData = mapOf(
                "id" to admin.id,
                "email" to admin.email,
                "name" to admin.name,
                "userId" to admin.userId,  // ✅ TAMBAHKAN INI
                "role" to admin.role,
                "isActive" to admin.isActive,
                "createdAt" to admin.createdAt,
                "createdBy" to admin.createdBy,
                "lastLogin" to admin.lastLogin
            )

            adminCollection.document(admin.id)
                .set(adminData)
                .await()

            Log.d(TAG, "Berhasil memperbarui admin: ${admin.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error memperbarui admin ${admin.id}: ${e.message}", e)
            throw e
        }
    }


    suspend fun deleteAdminUser(adminId: String) {
        try {
            if (adminId.isEmpty()) {
                throw IllegalArgumentException("ID admin tidak boleh kosong")
            }

            Log.d(TAG, "Menghapus admin: $adminId")

            adminCollection.document(adminId)
                .delete()
                .await()

            Log.d(TAG, "Berhasil menghapus admin: $adminId")
        } catch (e: Exception) {
            Log.e(TAG, "Error menghapus admin $adminId: ${e.message}", e)
            throw e
        }
    }

    suspend fun checkIsAdmin(email: String): Boolean {
        return try {
            // Super admin check
            if (email == SUPER_ADMIN_EMAIL) {
                Log.d(TAG, "Super admin detected: $email")
                return true
            }

            Log.d(TAG, "Memeriksa admin untuk email: $email")

            val query = adminCollection
                .whereEqualTo("email", email)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val isAdmin = !query.isEmpty
            Log.d(TAG, "Hasil pemeriksaan admin: $isAdmin")

            isAdmin
        } catch (e: Exception) {
            Log.e(TAG, "Error memeriksa admin: ${e.message}", e)
            false
        }
    }

    suspend fun checkIsSuperAdmin(email: String): Boolean {
        return try {
            // Check hardcoded super admin first
            if (email == SUPER_ADMIN_EMAIL) {
                Log.d(TAG, "Super admin detected from constant: $email")
                return true
            }

            // Check from database
            Log.d(TAG, "Checking super admin from database for email: $email")

            val query = adminCollection
                .whereEqualTo("email", email)
                .whereEqualTo("role", "super_admin")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val isSuperAdmin = !query.isEmpty
            Log.d(TAG, "Database check - Is Super Admin: $isSuperAdmin")

            isSuperAdmin
        } catch (e: Exception) {
            Log.e(TAG, "Error checking super admin: ${e.message}", e)
            // Fallback to hardcoded check
            email == SUPER_ADMIN_EMAIL
        }
    }

    suspend fun getAdminByEmail(email: String): AdminUser? {
        return try {
            Log.d(TAG, "Mengambil admin berdasarkan email: $email")

            val query = adminCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            if (query.isEmpty) {
                Log.d(TAG, "Admin tidak ditemukan: $email")
                return null
            }

            val admin = query.documents.first().toObject<AdminUser>()?.copy(
                id = query.documents.first().id
            )

            if (admin != null) {
                Log.d(TAG, "Admin ditemukan: ${admin.name}, aktif: ${admin.isActive}")
            }

            admin
        } catch (e: Exception) {
            Log.e(TAG, "Error mengambil admin $email: ${e.message}", e)
            null
        }
    }

    suspend fun exportWhitelistAsJson(): String {
        return try {
            Log.d(TAG, "Mengekspor whitelist sebagai JSON")

            val users = exportWhitelistUsers()
            val jsonString = com.google.gson.Gson().toJson(users)

            Log.d(TAG, "Export JSON berhasil: ${jsonString.length} karakter")
            jsonString
        } catch (e: Exception) {
            Log.e(TAG, "Error export JSON: ${e.message}", e)
            throw e
        }
    }

    suspend fun exportWhitelistAsCsv(): String {
        return try {
            Log.d(TAG, "Mengekspor whitelist sebagai CSV")

            val users = exportWhitelistUsers()
            val csvBuilder = StringBuilder()

            // Header dengan field baru
            csvBuilder.append("ID,Name,Email,UserID,DeviceID,IsActive,CreatedAt,AddedBy,AddedAt\n")

            // Data
            users.forEach { user ->
                val createdDate = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(user.createdAt))

                val addedDate = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(user.addedAt))

                csvBuilder.append(
                    "\"${user.id}\",\"${user.name}\",\"${user.email}\",\"${user.userId}\",\"${user.deviceId}\",${user.isActive},\"$createdDate\",\"${user.addedBy}\",\"$addedDate\"\n"
                )
            }

            Log.d(TAG, "Export CSV berhasil")
            csvBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error export CSV: ${e.message}", e)
            throw e
        }
    }

    // 2. Import method
    suspend fun importWhitelistFromJson(jsonString: String): Int {
        return try {
            Log.d(TAG, "Mengimport whitelist dari JSON")

            val gson = com.google.gson.Gson()
            val users = gson.fromJson(jsonString, Array<WhitelistUser>::class.java).toList()

            var successCount = 0
            users.forEach { user ->
                try {
                    // Generate ID baru jika kosong
                    val userToAdd = if (user.id.isEmpty()) {
                        user.copy(id = java.util.UUID.randomUUID().toString())
                    } else {
                        user
                    }

                    addWhitelistUser(userToAdd)
                    successCount++
                    Log.d(TAG, "Berhasil import user: ${user.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal import user ${user.name}: ${e.message}")
                }
            }

            Log.d(TAG, "Import selesai: $successCount dari ${users.size} user berhasil")
            successCount
        } catch (e: Exception) {
            Log.e(TAG, "Error import JSON: ${e.message}", e)
            throw e
        }
    }

    /**
     * Check if user exists in whitelist by email
     * @param email Email address to check
     * @return true if user exists and is active, false otherwise
     */
    suspend fun checkUserInWhitelistByEmail(email: String): Boolean {
        return try {
            Log.d(TAG, "Checking whitelist for email: '$email'")

            val query = whitelistCollection
                .whereEqualTo("email", email)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            Log.d(TAG, "Query result for email: ${query.size()} documents")

            if (query.isEmpty) {
                Log.w(TAG, "No active user found for email: '$email'")

                // Check if user exists but inactive
                val fallbackQuery = whitelistCollection
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!fallbackQuery.isEmpty) {
                    fallbackQuery.documents.forEach { doc ->
                        val user = doc.toObject<WhitelistUser>()
                        Log.w(TAG, "Found inactive user: ${user?.name}, active: ${user?.isActive}")
                    }
                    return false
                }

                Log.w(TAG, "User with email '$email' not found in whitelist at all")
                return false
            }

            query.documents.forEach { doc ->
                val user = doc.toObject<WhitelistUser>()
                Log.d(TAG, "Found active user: ${user?.name} (${user?.email}) - UserID: ${user?.userId}, active: ${user?.isActive}")
            }

            val isWhitelisted = !query.isEmpty
            Log.d(TAG, "Email check result: $isWhitelisted")

            isWhitelisted
        } catch (e: Exception) {
            Log.e(TAG, "Error checking whitelist for email '$email': ${e.message}", e)
            false
        }
    }

    /**
     * Get whitelist user by email
     * @param email Email address to search
     * @return WhitelistUser if found, null otherwise
     */
    suspend fun getWhitelistUserByEmail(email: String): WhitelistUser? {
        return try {
            Log.d(TAG, "Getting whitelist user by email: $email")

            val query = whitelistCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            if (query.isEmpty) {
                Log.d(TAG, "User not found for email: $email")
                return null
            }

            val document = query.documents.first()
            val user = document.toObject<WhitelistUser>()?.copy(id = document.id)

            if (user != null) {
                Log.d(TAG, "User found: ${user.name}, active: ${user.isActive}")
            }

            user
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by email $email: ${e.message}", e)
            null
        }
    }

    /**
     * Update user's device ID if needed
     * @param userId User ID to update
     * @param newDeviceId New device ID
     */
    suspend fun updateUserDeviceId(userId: String, newDeviceId: String) {
        try {
            Log.d(TAG, "Updating device ID for userId: $userId")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (!query.isEmpty) {
                val document = query.documents.first()
                document.reference.update(
                    mapOf(
                        "deviceId" to newDeviceId,
                        "lastLogin" to System.currentTimeMillis()
                    )
                ).await()
                Log.d(TAG, "Successfully updated device ID for userId: $userId")
            } else {
                Log.w(TAG, "No document found for userId: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device ID for userId $userId: ${e.message}", e)
        }
    }

    suspend fun updateWhatsappUrl(newUrl: String, updatedBy: String = "admin"): Boolean {
        return try {
            Log.d(TAG, "Memperbarui URL WhatsApp ke: $newUrl")

            val currentConfig = getRegistrationConfig()
            val updatedConfig = currentConfig.copy(
                whatsappHelpUrl = newUrl, // ✅ CHANGED
                updatedAt = System.currentTimeMillis(),
                updatedBy = updatedBy
            )

            saveRegistrationConfig(updatedConfig)

            Log.d(TAG, "Berhasil memperbarui URL WhatsApp")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error memperbarui URL WhatsApp: ${e.message}", e)
            false
        }
    }


    fun validateWhatsappUrl(url: String): Boolean {
        return try {
            // ✅ CHANGED: Validasi URL WhatsApp
            val whatsappPattern = Regex(
                "^https://wa\\.me/[0-9]{10,15}(\\?text=.*)?$",
                RegexOption.IGNORE_CASE
            )
            whatsappPattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }


    suspend fun getWhitelistUserByUserId(userId: String): WhitelistUser? {
        return try {
            Log.d(TAG, "Getting whitelist user by userId: $userId")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (query.isEmpty) {
                Log.d(TAG, "User not found for userId: $userId")
                return null
            }

            val document = query.documents.first()
            val user = document.toObject<WhitelistUser>()?.copy(id = document.id)

            if (user != null) {
                Log.d(TAG, "User found: ${user.name}, active: ${user.isActive}")
            }

            user
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by userId $userId: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserFCMToken(userId: String, fcmToken: String): Boolean {
        return try {
            Log.d(TAG, "Updating FCM token for userId: $userId")
            Log.d(TAG, "Token preview: ${fcmToken.take(20)}...${fcmToken.takeLast(20)}")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "No user found for userId: $userId")
                return false
            }

            val document = query.documents.first()
            document.reference.update(
                mapOf(
                    "fcmToken" to fcmToken,
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ FCM token updated successfully for userId: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token for userId $userId: ${e.message}", e)
            false
        }
    }

    /**
     * Get user's FCM token by userId
     */
    suspend fun getUserFCMToken(userId: String): String? {
        return try {
            Log.d(TAG, "Getting FCM token for userId: $userId")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "No user found for userId: $userId")
                return null
            }

            val document = query.documents.first()
            val user = document.toObject<WhitelistUser>()
            val token = user?.fcmToken

            if (!token.isNullOrEmpty()) {
                Log.d(TAG, "✅ FCM token found: ${token.take(20)}...${token.takeLast(20)}")
            } else {
                Log.w(TAG, "⚠️ No FCM token found for userId: $userId")
            }

            token

        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token for userId $userId: ${e.message}", e)
            null
        }
    }

    /**
     * Clear user's FCM token (on logout or token change)
     */
    suspend fun clearUserFCMToken(userId: String): Boolean {
        return try {
            Log.d(TAG, "Clearing FCM token for userId: $userId")

            val query = whitelistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "No user found for userId: $userId")
                return false
            }

            val document = query.documents.first()
            document.reference.update(
                mapOf(
                    "fcmToken" to "",
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ FCM token cleared for userId: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token for userId $userId: ${e.message}", e)
            false
        }
    }

    /**
     * Get all users with valid FCM tokens (for admin/testing)
     */
    suspend fun getUsersWithFCMTokens(): List<WhitelistUser> {
        return try {
            Log.d(TAG, "Getting all users with FCM tokens")

            val result = whitelistCollection
                .whereNotEqualTo("fcmToken", "")
                .orderBy("fcmToken")
                .orderBy("fcmTokenUpdatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val users = result.documents.mapNotNull { document ->
                document.toObject<WhitelistUser>()?.copy(id = document.id)
            }

            Log.d(TAG, "Found ${users.size} users with FCM tokens")
            users

        } catch (e: Exception) {
            Log.e(TAG, "Error getting users with FCM tokens: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateAdminFCMToken(email: String, fcmToken: String): Boolean {
        return try {
            Log.d(TAG, "Updating FCM token for admin: $email")
            Log.d(TAG, "Token preview: ${fcmToken.take(20)}...${fcmToken.takeLast(20)}")

            val query = adminCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "No admin found for email: $email")
                return false
            }

            val document = query.documents.first()
            document.reference.update(
                mapOf(
                    "fcmToken" to fcmToken,
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ FCM token updated successfully for admin: $email")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token for admin $email: ${e.message}", e)
            false
        }
    }

    /**
     * Get admin's FCM token by email
     */
    suspend fun getAdminFCMToken(email: String): String? {
        return try {
            Log.d(TAG, "Getting FCM token for admin: $email")

            // ✅ FIXED: Gunakan get().await() instead of stream()
            val query = adminCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()  // ✅ Changed from stream()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "⚠️ No admin found for email: $email")
                return null
            }

            val document = query.documents.firstOrNull()
            val admin = document?.toObject<AdminUser>()
            val token = admin?.fcmToken

            if (!token.isNullOrEmpty()) {
                Log.d(TAG, "✅ FCM token found: ${token.take(20)}...${token.takeLast(20)}")
            } else {
                Log.w(TAG, "⚠️ No FCM token found for admin: $email")
            }

            token

        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token for admin $email: ${e.message}", e)
            null
        }
    }

    /**
     * Clear admin's FCM token (on logout)
     */
    suspend fun clearAdminFCMToken(email: String): Boolean {
        return try {
            Log.d(TAG, "Clearing FCM token for admin: $email")

            val query = adminCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            if (query.isEmpty) {
                Log.w(TAG, "No admin found for email: $email")
                return false
            }

            val document = query.documents.first()
            document.reference.update(
                mapOf(
                    "fcmToken" to "",
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ FCM token cleared for admin: $email")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token for admin $email: ${e.message}", e)
            false
        }
    }

    /**
     * Get all admins with valid FCM tokens
     */
    suspend fun getAdminsWithFCMTokens(): List<AdminUser> {
        return try {
            Log.d(TAG, "Getting all admins with FCM tokens")

            val result = adminCollection
                .whereNotEqualTo("fcmToken", "")
                .orderBy("fcmToken")
                .orderBy("lastLogin", Query.Direction.DESCENDING)
                .get()
                .await()

            val admins = result.documents.mapNotNull { document ->
                document.toObject<AdminUser>()?.copy(id = document.id)
            }

            Log.d(TAG, "Found ${admins.size} admins with FCM tokens")
            admins

        } catch (e: Exception) {
            Log.e(TAG, "Error getting admins with FCM tokens: ${e.message}", e)
            emptyList()
        }
    }
}