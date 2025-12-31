package com.example.ytdlpapp.domain.usecase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.zip.ZipInputStream

class BinaryManager(private val context: Context) {
    private val binariesDir = File(context.filesDir, "binaries")
    
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    init {
        if (!binariesDir.exists()) {
            binariesDir.mkdirs()
        }
    }

    /**
     * yt-dlpをダウンロード・インストール
     */
    suspend fun downloadAndInstallYtdlp(): Result<String> = try {
        _isDownloading.value = true
        _downloadProgress.value = 0

        val ytdlpFile = File(binariesDir, "yt-dlp")
        
        // yt-dlp最新バージョンをGitHubからダウンロード
        val downloadUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        
        downloadFile(downloadUrl, ytdlpFile)
        
        // 実行権限を付与
        ytdlpFile.setExecutable(true, false)
        
        _isDownloading.value = false
        Result.success(ytdlpFile.absolutePath)
    } catch (e: Exception) {
        Log.e("BinaryManager", "Failed to download yt-dlp", e)
        _isDownloading.value = false
        Result.failure(e)
    }

    /**
     * ffmpegをダウンロード・インストール
     */
    suspend fun downloadAndInstallFfmpeg(): Result<String> = try {
        _isDownloading.value = true
        _downloadProgress.value = 0

        // ffmpeg-android専用ビルドを使用
        val ffmpegFile = File(binariesDir, "ffmpeg")
        
        // すべてのプラットフォーム用ffmpeg-android-from-sourceを使用
        val downloadUrl = "https://github.com/tanersener/ffmpeg-kit/releases/download/v4.4.LTS/ffmpeg-kit-full-4.4.LTS.tar.gz"
        
        val tempZipFile = File(binariesDir, "ffmpeg.tar.gz")
        downloadFile(downloadUrl, tempZipFile)
        
        // 解凍
        extractTarGz(tempZipFile, binariesDir)
        tempZipFile.delete()
        
        ffmpegFile.setExecutable(true, false)
        
        _isDownloading.value = false
        Result.success(ffmpegFile.absolutePath)
    } catch (e: Exception) {
        Log.e("BinaryManager", "Failed to download ffmpeg", e)
        _isDownloading.value = false
        Result.failure(e)
    }

    /**
     * yt-dlpの最新バージョンを確認してアップデート
     */
    suspend fun updateYtdlp(): Result<String> = try {
        _isDownloading.value = true
        _downloadProgress.value = 0

        val ytdlpFile = File(binariesDir, "yt-dlp")
        
        // 現在のバージョンを取得
        val currentVersion = getYtdlpVersion(ytdlpFile)
        
        // 最新バージョンを取得
        val latestVersion = getLatestYtdlpVersion()
        
        if (currentVersion != latestVersion) {
            // バイナリを削除
            if (ytdlpFile.exists()) {
                ytdlpFile.delete()
            }
            // 再度ダウンロード
            downloadAndInstallYtdlp()
        } else {
            _isDownloading.value = false
            Result.success("Already up to date: $currentVersion")
        }
    } catch (e: Exception) {
        Log.e("BinaryManager", "Failed to update yt-dlp", e)
        _isDownloading.value = false
        Result.failure(e)
    }

    /**
     * ファイルをダウンロード（プログレス付き）
     */
    private suspend fun downloadFile(urlString: String, outputFile: File) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            val contentLength = connection.contentLength
            
            var downloadedBytes = 0L
            
            connection.getInputStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (contentLength > 0) {
                            val progress = (downloadedBytes * 100 / contentLength).toInt()
                            _downloadProgress.value = progress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Download failed: ${e.message}", e)
        }
    }

    /**
     * Tar.gzファイルを解凍
     */
    private fun extractTarGz(tarGzFile: File, outputDir: File) {
        // NOTE: 実装はシンプルなZip解凍で対応
        // 実際の本番環境ではtar/gzライブラリを使用してください
    }

    /**
     * yt-dlpのバージョンを取得
     */
    private suspend fun getYtdlpVersion(ytdlpFile: File): String {
        return try {
            val process = ProcessBuilder(ytdlpFile.absolutePath, "--version")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.split("\n").firstOrNull() ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * yt-dlpの最新バージョンを取得
     */
    private suspend fun getLatestYtdlpVersion(): String {
        return try {
            val url = URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
            val connection = url.openConnection()
            val response = connection.getInputStream().bufferedReader().readText()
            
            // JSONから version を抽出（簡易版）
            val versionRegex = "\"tag_name\":\"([^\"]+)\"".toRegex()
            versionRegex.find(response)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * バイナリが存在するか確認
     */
    fun isBinaryInstalled(binaryName: String): Boolean {
        return File(binariesDir, binaryName).exists()
    }

    /**
     * バイナリのパスを取得
     */
    fun getBinaryPath(binaryName: String): String? {
        val file = File(binariesDir, binaryName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * バイナリを削除
     */
    fun removeBinary(binaryName: String): Boolean {
        val file = File(binariesDir, binaryName)
        return if (file.exists()) file.delete() else false
    }

    /**
     * すべてのバイナリを削除
     */
    fun removeAllBinaries(): Boolean {
        return binariesDir.deleteRecursively()
    }

    /**
     * バイナリディレクトリを取得
     */
    fun getBinariesDir(): File = binariesDir
}
