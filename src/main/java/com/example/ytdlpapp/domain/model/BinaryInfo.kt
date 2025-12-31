package com.example.ytdlpapp.domain.model

data class BinaryInfo(
    val name: String = "",
    val version: String = "unknown",
    val installPath: String = "",
    val isInstalled: Boolean = false,
    val lastUpdateCheck: Long = 0,
    val fileSize: Long = 0,
    val isUpdating: Boolean = false,
    val updateProgress: Int = 0,
    val updateError: String? = null
)

enum class BinaryType {
    YT_DLP, FFMPEG
}
