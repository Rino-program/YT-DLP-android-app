package com.example.ytdlpapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

// 利用可能なキー
object SettingsKey {
    const val DOWNLOAD_FOLDER = "download_folder"
    const val AUTO_UPDATE_BINARY = "auto_update_binary"
    const val YTDLP_OPTIONS = "ytdlp_options"
    const val FFMPEG_OPTIONS = "ffmpeg_options"
    const val SHOW_DEBUG_LOG = "show_debug_log"
}
