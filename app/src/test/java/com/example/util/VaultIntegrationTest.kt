package com.example.util

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.IndexedFile
import com.example.data.repository.FileRepository
import com.example.ui.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class VaultIntegrationTest {

    private lateinit var context: Application
    private lateinit var database: AppDatabase
    private lateinit var repository: FileRepository
    private lateinit var viewModel: FileManagerViewModel
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FileRepository(database.fileDao())
        viewModel = FileManagerViewModel(context, repository)
        
        tempDir = File(context.filesDir, "test_vault_dir")
        tempDir.mkdirs()
    }

    @After
    fun teardown() {
        database.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun testVaultEndToEnd() = runBlocking {
        val originalText = "Highly sensitive vault document"
        val physicalFile = File(tempDir, "sensitive.txt")
        physicalFile.writeText(originalText)
        
        val originalHash = CryptoUtils.calculateSHA256(physicalFile)
        
        val fileId = repository.insertFile(
            IndexedFile(
                name = physicalFile.name,
                path = physicalFile.absolutePath,
                mimeType = "text/plain",
                size = physicalFile.length(),
                timestamp = System.currentTimeMillis(),
                isVault = false,
                classification = "Documents",
                hash = originalHash
            )
        )
        
        var indexedFile = repository.allFiles.first().find { it.id == fileId.toInt() }!!
        
        val pin = "1234"
        viewModel.setupVaultPin(pin)
        
        viewModel.toggleVaultStatus(indexedFile)
        Thread.sleep(500)
        
        var vaultFile = repository.allFiles.first().find { it.id == fileId.toInt() }!!
        assertTrue("File should be marked as vault in DB", vaultFile.isVault)
        assertFalse("Original physical file should be deleted", physicalFile.exists())
        
        val vaultDir = File(context.filesDir, "secure_vault")
        val encryptedFile = File(vaultDir, "file_${vaultFile.id}.enc")
        
        assertTrue("Encrypted file should exist at ${encryptedFile.path}", encryptedFile.exists())
        val encryptedContent = encryptedFile.readText()
        assertNotEquals(originalText, encryptedContent)
        
        viewModel.toggleVaultStatus(vaultFile)
        Thread.sleep(500)
        
        var restoredFile = repository.allFiles.first().find { it.id == fileId.toInt() }!!
        assertFalse("File should no longer be marked as vault in DB", restoredFile.isVault)
        assertFalse("Encrypted file should be deleted", encryptedFile.exists())
        
        val restoredPhysicalFile = File(restoredFile.path)
        assertTrue("Restored physical file should exist", restoredPhysicalFile.exists())
        val restoredContent = restoredPhysicalFile.readText()
        assertEquals("Restored content must match original", originalText, restoredContent)
        
        val restoredHash = CryptoUtils.calculateSHA256(restoredPhysicalFile)
        assertEquals("Restored file hash must match original hash", originalHash, restoredHash)
    }
}
