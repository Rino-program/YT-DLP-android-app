package com.example.ytdlpapp.domain.model

data class QueuedDownload(
    val id: Int = 0,
    val url: String = "",
    val title: String = "",
    val format: String = "best",
    val outputPath: String = "",
    val ytdlpOptions: String = "",
    val ffmpegOptions: String = "",
    val status: QueueStatus = QueueStatus.QUEUED,
    val priority: Int = 0,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val estimatedSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val isPlaylist: Boolean = false
)

enum class QueueStatus {
    QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}
