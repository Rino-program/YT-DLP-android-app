package com.example.ytdlpapp.model

/**
 * バイナリの種類
 */
enum class BinaryType(val fileName: String, val displayName: String) {
    YTDLP("yt-dlp", "yt-dlp"),
    FFMPEG("ffmpeg", "FFmpeg")
}

/**
 * バイナリのインストール状態
 */
data class BinaryState(
    val type: BinaryType,
    val isInstalled: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val version: String? = null,
    val error: String? = null
)

/**
 * ダウンロードの状態
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val speed: String = "", val eta: String = "") : DownloadState()
    data class Processing(val message: String = "処理中...") : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * ダウンロードオプション
 */
data class DownloadOptions(
    val format: String = "best",
    val audioOnly: Boolean = false,
    val audioFormat: String = "mp3",
    val customOptions: String = ""
)
