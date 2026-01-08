package com.example.ytdlpapp.core

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * yt-dlpとffmpegバイナリの管理
 */
class BinaryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BinaryManager"
        
        // yt-dlp (Python zipapp - Android用)
        private const val YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        
        // ffmpeg-android-java用のURL (arm64-v8a用)
        private fun getFfmpegUrl(): String {
            val arch = when {
                Build.SUPPORTED_ABIS.any { it.contains("arm64") } -> "arm64-v8a"
                Build.SUPPORTED_ABIS.any { it.contains("armeabi") } -> "armeabi-v7a"
                Build.SUPPORTED_ABIS.any { it.contains("x86_64") } -> "x86_64"
                else -> "x86"
            }
            // ffmpeg-kit から静的ビルドを取得
            return "https://github.com/AdrianAndroid/FFmpegForAndroid/releases/download/v1.0/ffmpeg-$arch.zip"
        }
    }

    private val binDir = File(context.filesDir, "bin")
    
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _ytdlpState = MutableStateFlow(BinaryInstallState())
    val ytdlpState: StateFlow<BinaryInstallState> = _ytdlpState

    private val _ffmpegState = MutableStateFlow(BinaryInstallState())
    val ffmpegState: StateFlow<BinaryInstallState> = _ffmpegState

    data class BinaryInstallState(
        val isInstalled: Boolean = false,
        val isDownloading: Boolean = false,
        val progress: Int = 0,
        val error: String? = null
    )

    init {
        binDir.mkdirs()
        checkInstallStatus()
    }

    private fun checkInstallStatus() {
        val ytdlpFile = File(binDir, "yt-dlp")
        val ffmpegFile = File(binDir, "ffmpeg")
        
        _ytdlpState.value = BinaryInstallState(isInstalled = ytdlpFile.exists() && ytdlpFile.canExecute())
        _ffmpegState.value = BinaryInstallState(isInstalled = ffmpegFile.exists() && ffmpegFile.canExecute())
    }

    fun getYtdlpPath(): String? {
        val file = File(binDir, "yt-dlp")
        return if (file.exists() && file.canExecute()) file.absolutePath else null
    }

    fun getFfmpegPath(): String? {
        val file = File(binDir, "ffmpeg")
        return if (file.exists() && file.canExecute()) file.absolutePath else null
    }

    /**
     * yt-dlpをインストール
     */
    suspend fun installYtdlp(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _ytdlpState.value = BinaryInstallState(isDownloading = true, progress = 0)
            
            val ytdlpFile = File(binDir, "yt-dlp")
            
            downloadFile(YTDLP_URL, ytdlpFile) { progress ->
                _ytdlpState.value = BinaryInstallState(isDownloading = true, progress = progress)
            }
            
            // 実行権限を付与
            ytdlpFile.setExecutable(true, false)
            ytdlpFile.setReadable(true, false)
            
            _ytdlpState.value = BinaryInstallState(isInstalled = true)
            Log.i(TAG, "yt-dlp installed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install yt-dlp", e)
            _ytdlpState.value = BinaryInstallState(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * ffmpegをインストール
     */
    suspend fun installFfmpeg(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = 0)
            
            val ffmpegFile = File(binDir, "ffmpeg")
            val tempZip = File(binDir, "ffmpeg_temp.zip")
            
            try {
                downloadFile(getFfmpegUrl(), tempZip) { progress ->
                    _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = progress)
                }
                
                // ZIPを解凍
                extractZip(tempZip, binDir)
                
                // 実行権限を付与
                ffmpegFile.setExecutable(true, false)
                ffmpegFile.setReadable(true, false)
                
            } finally {
                tempZip.delete()
            }
            
            _ffmpegState.value = BinaryInstallState(isInstalled = ffmpegFile.exists() && ffmpegFile.canExecute())
            
            if (_ffmpegState.value.isInstalled) {
                Log.i(TAG, "ffmpeg installed successfully")
                Result.success(Unit)
            } else {
                throw Exception("ffmpeg installation failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install ffmpeg", e)
            _ffmpegState.value = BinaryInstallState(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * バイナリを削除
     */
    fun uninstallYtdlp() {
        File(binDir, "yt-dlp").delete()
        _ytdlpState.value = BinaryInstallState(isInstalled = false)
    }

    fun uninstallFfmpeg() {
        File(binDir, "ffmpeg").delete()
        _ffmpegState.value = BinaryInstallState(isInstalled = false)
    }

    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "YtDlpApp/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response")
            val contentLength = body.contentLength()
            var downloaded = 0L

            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            onProgress((downloaded * 100 / contentLength).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
        }
    }

    private fun extractZip(zipFile: File, outputDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
