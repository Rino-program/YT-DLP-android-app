package com.example.ytdlpapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "binary_info")
data class BinaryInfoEntity(
    @PrimaryKey
    val name: String, // "yt-dlp" or "ffmpeg"
    val version: String = "unknown",
    val installPath: String = "",
    val isInstalled: Boolean = false,
    val lastUpdateCheck: Long = 0,
    val fileSize: Long = 0
)
