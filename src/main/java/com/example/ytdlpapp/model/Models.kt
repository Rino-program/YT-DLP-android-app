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
 * 出力フォーマット
 */
enum class OutputFormat(val extension: String, val displayName: String) {
    MP4("mp4", "MP4 (H.264)"),
    WEBM("webm", "WebM (VP9)"),
    MKV("mkv", "MKV"),
    MP3("mp3", "MP3 (音声のみ)"),
    M4A("m4a", "M4A (音声のみ)"),
    BEST("best", "最高品質（自動）")
}

/**
 * ダウンロードオプション
 */
data class DownloadOptions(
    val format: OutputFormat = OutputFormat.MP4,
    val audioOnly: Boolean = false,
    val audioFormat: String = "mp3",
    val outputDir: String = "",
    val playlistPadding: Int = 2,        // プレイリスト番号のゼロ埋め桁数
    val excludeAv1: Boolean = true,       // AV1コーデックを除外
    val customOptions: String = ""
)

/**
 * 変換の状態
 */
sealed class ConvertState {
    object Idle : ConvertState()
    data class Converting(val progress: Int, val message: String = "変換中...") : ConvertState()
    data class Completed(val outputPath: String) : ConvertState()
    data class Error(val message: String) : ConvertState()
}

/**
 * 変換オプション
 */
data class ConvertOptions(
    val outputFormat: String = "mp3",
    val audioBitrate: String = "192k",
    val audioSampleRate: String = "44100"
)
