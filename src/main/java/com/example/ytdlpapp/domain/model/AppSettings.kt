package com.example.ytdlpapp.domain.model

data class AppSettings(
    val downloadFolder: String = "",
    val autoUpdateBinary: Boolean = true,
    val ytdlpOptions: String = "",
    val ffmpegOptions: String = "",
    val showDebugLog: Boolean = false
)
