package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indexed_files")
data class IndexedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val mimeType: String,
    val size: Long,
    val timestamp: Long,
    val isVault: Boolean = false,
    val classification: String, // "Images", "Videos", "Audios", "Documents"
    val hash: String = "", // For duplicates checking
    val contentSummary: String? = null,
    val summaryKeywords: String? = null
)
