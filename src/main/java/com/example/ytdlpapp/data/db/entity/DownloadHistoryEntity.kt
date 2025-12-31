package com.example.ytdlpapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val url: String,
    val title: String,
    val format: String,
    val outputPath: String,
    val status: String, // "pending", "downloading", "completed", "failed"
    val progress: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
