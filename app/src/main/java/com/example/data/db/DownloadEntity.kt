package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val mediaUri: String,
    val filePath: String,
    val thumbnailUrl: String,
    val fileSizeBytes: Long,
    val durationFormatted: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val downloadStatus: String = "COMPLETED"
)
