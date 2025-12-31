package com.example.ytdlpapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_queue")
data class DownloadQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val url: String,
    val title: String = "",
    val format: String = "best",
    val outputPath: String,
    val ytdlpOptions: String = "",
    val ffmpegOptions: String = "",
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    val priority: Int = 0, // 優先度（高いほど優先）
    val progress: Int = 0,
    val errorMessage: String? = null,
    val estimatedSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val isPlaylist: Boolean = false,
    val playlistIndex: Int? = null
)
