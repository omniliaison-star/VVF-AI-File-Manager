package com.example.data.repository

import com.example.data.local.FileDao
import com.example.data.model.IndexedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FileRepository(private val fileDao: FileDao) {

    val allFiles: Flow<List<IndexedFile>> = fileDao.getAllFiles()
    val nonVaultFiles: Flow<List<IndexedFile>> = fileDao.getNonVaultFiles()
    val vaultFiles: Flow<List<IndexedFile>> = fileDao.getVaultFiles()

    suspend fun insertFile(file: IndexedFile): Long {
        return fileDao.insertFile(file)
    }

    suspend fun updateFile(file: IndexedFile) {
        fileDao.updateFile(file)
    }

    suspend fun deleteFile(file: IndexedFile) {
        fileDao.deleteFile(file)
    }

    suspend fun deleteFileById(id: Int) {
        fileDao.deleteFileById(id)
    }

    suspend fun clearAll() {
        fileDao.clearDatabase()
    }

    suspend fun getFileByPath(path: String): IndexedFile? {
        return fileDao.getFileByPath(path)
    }
}
