package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IndexedFile
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM indexed_files ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<IndexedFile>>

    @Query("SELECT * FROM indexed_files WHERE isVault = 0 ORDER BY timestamp DESC")
    fun getNonVaultFiles(): Flow<List<IndexedFile>>

    @Query("SELECT * FROM indexed_files WHERE isVault = 1 ORDER BY timestamp DESC")
    fun getVaultFiles(): Flow<List<IndexedFile>>

    @Query("SELECT * FROM indexed_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): IndexedFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: IndexedFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<IndexedFile>)

    @Update
    suspend fun updateFile(file: IndexedFile)

    @Delete
    suspend fun deleteFile(file: IndexedFile)

    @Query("DELETE FROM indexed_files WHERE id = :id")
    suspend fun deleteFileById(id: Int)

    @Query("DELETE FROM indexed_files")
    suspend fun clearDatabase()
}
