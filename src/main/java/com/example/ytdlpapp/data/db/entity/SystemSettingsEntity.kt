package com.example.ytdlpapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_settings")
data class ProxySettingsEntity(
    @PrimaryKey
    val id: String = "default",
    val enabled: Boolean = false,
    val protocol: String = "http", // http, https, socks4, socks5
    val host: String = "",
    val port: Int = 0,
    val username: String? = null,
    val password: String? = null
)

@Entity(tableName = "download_statistics")
data class DownloadStatisticsEntity(
    @PrimaryKey
    val id: String = "global",
    val totalDownloads: Int = 0,
    val successfulDownloads: Int = 0,
    val failedDownloads: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val totalDuration: Long = 0L, // ミリ秒
    val averageSpeed: Float = 0f, // KB/s
    val lastResetDate: Long = System.currentTimeMillis()
)
