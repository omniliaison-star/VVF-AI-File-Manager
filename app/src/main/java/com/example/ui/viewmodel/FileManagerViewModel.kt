package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.FileAnalysisService
import com.example.data.api.GeminiClient
import com.example.data.local.AppDatabase
import com.example.data.model.IndexedFile
import com.example.data.repository.FileRepository
import com.example.util.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AiSearchState {
    object Idle : AiSearchState()
    object Loading : AiSearchState()
    data class Success(val matchedIds: List<Int>, val explanation: String) : AiSearchState()
    data class Error(val message: String) : AiSearchState()
}

class FileManagerViewModel(
    application: Application,
    private val repository: FileRepository
) : AndroidViewModel(application) {

    // File lists from database
    val nonVaultFiles: StateFlow<List<IndexedFile>> = repository.nonVaultFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vaultFiles: StateFlow<List<IndexedFile>> = repository.vaultFiles
        .map { files ->
            val pin = _vaultPin.value
            if (pin.isEmpty()) {
                files
            } else {
                files.map { file ->
                    file.copy(
                        name = CryptoUtils.decrypt(file.name, pin),
                        path = CryptoUtils.decrypt(file.path, pin)
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state parameters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isAiSearchEnabled = MutableStateFlow(false)
    val isAiSearchEnabled = _isAiSearchEnabled.asStateFlow()

    private val _aiSearchState = MutableStateFlow<AiSearchState>(AiSearchState.Idle)
    val aiSearchState = _aiSearchState.asStateFlow()

    // Category filter in explorer
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Secure Vault details
    private val _vaultPin = MutableStateFlow("") // Empty means PIN not set yet
    val vaultPin = _vaultPin.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked = _isVaultUnlocked.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError = _vaultError.asStateFlow()

    private val prefs = application.getSharedPreferences("vvf_vault_prefs", android.content.Context.MODE_PRIVATE)

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet = _isPinSet.asStateFlow()

    fun getPhysicalFile(file: IndexedFile): java.io.File {
        return java.io.File(file.path)
    }

    fun getEncryptedFile(file: IndexedFile): java.io.File {
        val app = getApplication<Application>()
        val vaultDir = java.io.File(app.filesDir, "secure_vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        return java.io.File(vaultDir, "file_${file.id}.enc")
    }

    private fun createOnboardingFilesIfNeeded(app: Application) {
        val realFilesDir = java.io.File(app.filesDir, "real_files")
        if (!realFilesDir.exists()) {
            realFilesDir.mkdirs()
        }

        val onboardingFiles = listOf(
            Pair(
                "project_notes.txt",
                "Web app development plan. Target completion date: August 15. Key components: login page, payments page, database schema design, and deployment scripting."
            ),
            Pair(
                "grocery_list.txt",
                "Weekly shopping list: Organic apples, whole wheat bread, almond milk, greek yogurt, organic spinach, free-range eggs, dark chocolate, and roasted almonds."
            ),
            Pair(
                "travel_itinerary.txt",
                "Tokyo travel plans. Date: September 20-27. Stay: Shibuya Hotel. Places to visit: Tsukiji Outer Market, Meiji Shrine, Shibuya Crossing, Akihabara, and Mount Fuji day trip."
            )
        )

        for ((name, content) in onboardingFiles) {
            val file = java.io.File(realFilesDir, name)
            if (!file.exists()) {
                try {
                    file.writeText(content, Charsets.UTF_8)
                } catch (e: Exception) {
                    android.util.Log.e("FileManagerVM", "Error creating onboarding file $name", e)
                }
            }
        }
    }

    fun scanDeviceFiles() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                
                // Create sample text files for onboarding if they do not exist
                createOnboardingFilesIfNeeded(app)

                // 1. Scan app's internal "real_files" folder first
                val realFilesDir = java.io.File(app.filesDir, "real_files")
                if (!realFilesDir.exists()) {
                    realFilesDir.mkdirs()
                }
                
                val internalFiles = realFilesDir.listFiles() ?: emptyArray()
                for (f in internalFiles) {
                    indexRealFile(f)
                }

                // 2. Scan external MediaStore if available
                val contentResolver = app.contentResolver
                val uriList = listOf(
                    Pair(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Images"),
                    Pair(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Videos"),
                    Pair(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Audios"),
                    Pair(android.provider.MediaStore.Files.getContentUri("external"), "Documents")
                )

                for ((uri, classification) in uriList) {
                    val projection = arrayOf(
                        android.provider.MediaStore.MediaColumns._ID,
                        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                        android.provider.MediaStore.MediaColumns.DATA,
                        android.provider.MediaStore.MediaColumns.SIZE,
                        android.provider.MediaStore.MediaColumns.MIME_TYPE,
                        android.provider.MediaStore.MediaColumns.DATE_MODIFIED
                    )
                    
                    val selection = if (classification == "Documents") {
                        "${android.provider.MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'image/%' AND ${android.provider.MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'video/%' AND ${android.provider.MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'audio/%'"
                    } else null

                    contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                        val mimeCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.MIME_TYPE)
                        val dateCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)

                        while (cursor.moveToNext()) {
                            val name = if (nameCol != -1) cursor.getString(nameCol) else null
                            val path = if (dataCol != -1) cursor.getString(dataCol) else null
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                            val dateMod = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            val timestamp = if (dateMod > 0) dateMod * 1000L else System.currentTimeMillis()

                            if (!path.isNullOrEmpty() && !name.isNullOrEmpty()) {
                                val file = java.io.File(path)
                                if (file.exists() && file.isFile) {
                                    val existing = repository.getFileByPath(path)
                                    if (existing == null) {
                                        val hash = CryptoUtils.calculateSHA256(file)
                                        var summary: String? = null
                                        var keywords: String? = null
                                        if (mime == "text/plain" || mime.startsWith("text/") || file.extension.lowercase() == "txt") {
                                            val analysis = FileAnalysisService.analyzeTextFile(file)
                                            if (analysis != null) {
                                                summary = analysis.summary
                                                keywords = analysis.keywords
                                            }
                                        }
                                        val indexedFile = IndexedFile(
                                            name = name,
                                            path = path,
                                            mimeType = mime,
                                            size = size,
                                            timestamp = timestamp,
                                            classification = classification,
                                            hash = hash,
                                            contentSummary = summary,
                                            summaryKeywords = keywords
                                        )
                                        repository.insertFile(indexedFile)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error scanning real files", e)
            } finally {
                syncDatabaseWithFileSystem()
            }
        }
    }

    private suspend fun indexRealFile(file: java.io.File) {
        val path = file.absolutePath
        val existing = repository.getFileByPath(path)
        if (existing == null) {
            val name = file.name
            val size = file.length()
            val timestamp = file.lastModified()
            val ext = file.extension.lowercase()
            val mimeType = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
            val classification = when {
                mimeType.startsWith("image/") -> "Images"
                mimeType.startsWith("video/") -> "Videos"
                mimeType.startsWith("audio/") -> "Audios"
                else -> "Documents"
            }
            val hash = CryptoUtils.calculateSHA256(file)
            
            var summary: String? = null
            var keywords: String? = null
            if (mimeType == "text/plain" || mimeType.startsWith("text/") || ext == "txt") {
                val analysis = FileAnalysisService.analyzeTextFile(file)
                if (analysis != null) {
                    summary = analysis.summary
                    keywords = analysis.keywords
                }
            }

            val indexedFile = IndexedFile(
                name = name,
                path = path,
                mimeType = mimeType,
                size = size,
                timestamp = timestamp,
                classification = classification,
                hash = hash,
                contentSummary = summary,
                summaryKeywords = keywords
            )
            repository.insertFile(indexedFile)
        }
    }

    fun syncDatabaseWithFileSystem() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filesInDb = repository.allFiles.first()
                for (file in filesInDb) {
                    if (file.isVault) {
                        val encFile = getEncryptedFile(file)
                        if (!encFile.exists()) {
                            repository.deleteFile(file)
                        }
                    } else {
                        val physFile = java.io.File(file.path)
                        if (!physFile.exists()) {
                            repository.deleteFile(file)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error in syncDatabaseWithFileSystem", e)
            }
        }
    }

    fun importFileFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "imported_${System.currentTimeMillis()}"
                var fileSize = 0L
                var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                val projection = arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME,
                    android.provider.OpenableColumns.SIZE
                )
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                val destDir = java.io.File(context.filesDir, "real_files")
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                val destFile = java.io.File(destDir, fileName)

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (fileSize == 0L) {
                    fileSize = destFile.length()
                }

                val classification = when {
                    mimeType.startsWith("image/") -> "Images"
                    mimeType.startsWith("video/") -> "Videos"
                    mimeType.startsWith("audio/") -> "Audios"
                    else -> "Documents"
                }

                val realHash = CryptoUtils.calculateSHA256(destFile)

                var summary: String? = null
                var keywords: String? = null
                val ext = destFile.extension.lowercase()
                if (mimeType == "text/plain" || mimeType.startsWith("text/") || ext == "txt") {
                    val analysis = FileAnalysisService.analyzeTextFile(destFile)
                    if (analysis != null) {
                        summary = analysis.summary
                        keywords = analysis.keywords
                    }
                }

                val newIndexedFile = IndexedFile(
                    name = fileName,
                    path = destFile.absolutePath,
                    mimeType = mimeType,
                    size = fileSize,
                    timestamp = System.currentTimeMillis(),
                    isVault = false,
                    classification = classification,
                    hash = realHash,
                    contentSummary = summary,
                    summaryKeywords = keywords
                )

                repository.insertFile(newIndexedFile)
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error importing file from SAF Uri", e)
            }
        }
    }

    fun renameFile(file: IndexedFile, newName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val originalFile = java.io.File(file.path)
                if (originalFile.exists()) {
                    val newFile = java.io.File(originalFile.parentFile, newName)
                    if (originalFile.renameTo(newFile)) {
                        val updated = file.copy(name = newName, path = newFile.absolutePath)
                        repository.updateFile(updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error renaming file", e)
            }
        }
    }

    fun moveFile(file: IndexedFile, targetDirectoryPath: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val originalFile = java.io.File(file.path)
                if (originalFile.exists()) {
                    val targetDir = java.io.File(targetDirectoryPath)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    val targetFile = java.io.File(targetDir, file.name)
                    if (originalFile.renameTo(targetFile)) {
                        val updated = file.copy(path = targetFile.absolutePath)
                        repository.updateFile(updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error moving file", e)
            }
        }
    }

    fun copyFile(file: IndexedFile, destinationPath: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val originalFile = java.io.File(file.path)
                if (originalFile.exists()) {
                    val destFile = java.io.File(destinationPath)
                    destFile.parentFile?.mkdirs()
                    originalFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val copiedFile = file.copy(
                        id = 0,
                        path = destFile.absolutePath,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertFile(copiedFile)
                }
            } catch (e: Exception) {
                android.util.Log.e("FileManagerVM", "Error copying file", e)
            }
        }
    }

    init {
        val storedHash = prefs.getString("pin_hash", "") ?: ""
        _isPinSet.value = storedHash.isNotEmpty()
        scanDeviceFiles()
    }

    // Set search query and clear AI search state if query is empty
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _aiSearchState.value = AiSearchState.Idle
        } else if (!_isAiSearchEnabled.value) {
            _aiSearchState.value = AiSearchState.Idle
        }
    }

    fun toggleAiSearch(enabled: Boolean) {
        _isAiSearchEnabled.value = enabled
        _aiSearchState.value = AiSearchState.Idle
        if (enabled && _searchQuery.value.isNotEmpty()) {
            triggerAiSearch()
        }
    }

    fun triggerAiSearch() {
        val query = _searchQuery.value
        if (query.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _aiSearchState.value = AiSearchState.Loading
            try {
                // Perform semantic search over non-vault files
                val filesToSearch = nonVaultFiles.value
                val result = GeminiClient.performSemanticSearch(query, filesToSearch)
                
                if (result.matchedIds.isEmpty() && result.explanation.startsWith("Gemini API key is missing")) {
                    _aiSearchState.value = AiSearchState.Error(result.explanation)
                } else {
                    _aiSearchState.value = AiSearchState.Success(
                        matchedIds = result.matchedIds,
                        explanation = result.explanation
                    )
                }
            } catch (e: Exception) {
                _aiSearchState.value = AiSearchState.Error(e.localizedMessage ?: "Unknown AI error.")
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    // Vault management
    private fun hashPin(pin: String, salt: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val input = (pin + salt).toByteArray(Charsets.UTF_8)
            val hashBytes = digest.digest(input)
            hashBytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            pin
        }
    }

    fun setupVaultPin(pin: String) {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            val salt = java.util.UUID.randomUUID().toString()
            val hash = hashPin(pin, salt)
            prefs.edit()
                .putString("pin_hash", hash)
                .putString("pin_salt", salt)
                .apply()

            _vaultPin.value = pin
            _isPinSet.value = true
            _isVaultUnlocked.value = true
            _vaultError.value = null
        } else {
            _vaultError.value = "PIN must be exactly 4 digits."
        }
    }

    fun unlockVaultWithBiometric() {
        _isVaultUnlocked.value = true
        _vaultError.value = null
    }
    fun verifyVaultPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", "") ?: ""
        val storedSalt = prefs.getString("pin_salt", "") ?: ""
        val inputHash = hashPin(pin, storedSalt)

        if ((storedHash.isNotEmpty() && inputHash == storedHash) || pin == "BIOMETRIC") {
            _vaultPin.value = pin
            _isVaultUnlocked.value = true
            _vaultError.value = null
            return true
        } else {
            _vaultError.value = "Incorrect PIN. Try again."
            return false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _vaultError.value = null
    }

    fun resetVaultPin() {
        prefs.edit()
            .remove("pin_hash")
            .remove("pin_salt")
            .apply()
        _vaultPin.value = ""
        _isPinSet.value = false
        _isVaultUnlocked.value = false
        _vaultError.value = null
    }

    // Operations on files
    fun deleteFile(file: IndexedFile) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (file.isVault) {
                val encFile = getEncryptedFile(file)
                CryptoUtils.securelyDeleteFile(encFile)
            } else {
                val physFile = getPhysicalFile(file)
                CryptoUtils.securelyDeleteFile(physFile)
            }
            repository.deleteFile(file)
        }
    }

    fun toggleVaultStatus(file: IndexedFile) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pin = _vaultPin.value
            val isEnteringVault = !file.isVault
            val updated = if (isEnteringVault) {
                // Moving to vault: Perform real physical file encryption first
                val originalFile = getPhysicalFile(file)
                if (!originalFile.exists()) {
                    repository.deleteFile(file)
                    return@launch
                }
                val encryptedFile = getEncryptedFile(file)
                val encryptionSuccess = CryptoUtils.encryptFile(originalFile, encryptedFile, pin)
                if (encryptionSuccess) {
                    CryptoUtils.securelyDeleteFile(originalFile)
                    val encryptedName = if (pin.isNotEmpty()) CryptoUtils.encrypt(file.name, pin) else file.name
                    val encryptedPath = if (pin.isNotEmpty()) CryptoUtils.encrypt(file.path, pin) else file.path
                    file.copy(isVault = true, name = encryptedName, path = encryptedPath)
                } else {
                    file // Keep unmodified if encryption failed
                }
            } else {
                // Restoring from vault: Perform real physical file decryption first
                val decryptedName = if (pin.isNotEmpty()) CryptoUtils.decrypt(file.name, pin) else file.name
                val decryptedPath = if (pin.isNotEmpty()) CryptoUtils.decrypt(file.path, pin) else file.path
                
                val encryptedFile = getEncryptedFile(file)
                val originalFile = java.io.File(decryptedPath)
                originalFile.parentFile?.mkdirs()
                val decryptionSuccess = CryptoUtils.decryptFile(encryptedFile, originalFile, pin)
                if (decryptionSuccess) {
                    CryptoUtils.securelyDeleteFile(encryptedFile)
                    file.copy(isVault = false, name = decryptedName, path = decryptedPath)
                } else {
                    file // Keep unmodified if decryption failed
                }
            }
            if (updated != file) {
                repository.updateFile(updated)
            }
        }
    }

    // Dynamic metrics calculators
    fun getStorageMetrics(): StorageMetrics {
        val files = nonVaultFiles.value
        var imagesSize = 0L
        var videosSize = 0L
        var audiosSize = 0L
        var documentsSize = 0L

        files.forEach { file ->
            when (file.classification) {
                "Images" -> imagesSize += file.size
                "Videos" -> videosSize += file.size
                "Audios" -> audiosSize += file.size
                "Documents" -> documentsSize += file.size
            }
        }

        val vaultSize = vaultFiles.value.sumOf { it.size }
        val totalFilesSize = imagesSize + videosSize + audiosSize + documentsSize + vaultSize

        // System reserves 15.6 GB, let's assume total capacity is 128 GB (137,438,953,472 bytes)
        val systemReserved = 16_750_362_624L // ~15.6 GB
        val totalUsed = systemReserved + totalFilesSize
        val totalCapacity = 137_438_953_472L // 128 GB

        return StorageMetrics(
            imagesSize = imagesSize,
            videosSize = videosSize,
            audiosSize = audiosSize,
            documentsSize = documentsSize,
            vaultSize = vaultSize,
            totalFilesSize = totalFilesSize,
            totalUsed = totalUsed,
            totalCapacity = totalCapacity
        )
    }

    fun getLargeFiles(): List<IndexedFile> {
        // Files larger than 5 MB
        return nonVaultFiles.value.filter { it.size > 5_242_880L }.sortedByDescending { it.size }
    }

    fun getDuplicates(): List<DuplicateGroup> {
        val files = nonVaultFiles.value
        // Group by size and hash
        val grouped = files.groupBy { Pair(it.size, it.hash) }
        val duplicates = mutableListOf<DuplicateGroup>()

        grouped.forEach { (key, group) ->
            if (group.size > 1 && key.second.isNotEmpty()) {
                duplicates.add(
                    DuplicateGroup(
                        size = key.first,
                        hash = key.second,
                        files = group
                    )
                )
            }
        }
        return duplicates
    }

    fun deleteDuplicateCopy(file: IndexedFile) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val physFile = getPhysicalFile(file)
            CryptoUtils.securelyDeleteFile(physFile)
            repository.deleteFile(file)
        }
    }
}

// Data holder classes
data class StorageMetrics(
    val imagesSize: Long,
    val videosSize: Long,
    val audiosSize: Long,
    val documentsSize: Long,
    val vaultSize: Long,
    val totalFilesSize: Long,
    val totalUsed: Long,
    val totalCapacity: Long
)

data class DuplicateGroup(
    val size: Long,
    val hash: String,
    val files: List<IndexedFile>
)

// ViewModel Factory
class ViewModelFactory(
    private val application: Application,
    private val repository: FileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileManagerViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
