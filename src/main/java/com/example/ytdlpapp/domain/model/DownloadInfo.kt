package com.example.ytdlpapp.domain.model

data class DownloadInfo(
    val id: Int = 0,
    val url: String = "",
    val title: String = "",
    val format: String = "",
    val outputPath: String = "",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}
