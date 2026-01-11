package com.example.ytdlpapp.core

import android.content.Context
import android.os.Build
import android.util.Log
import android.system.ErrnoException
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * yt-dlp / FFmpeg バイナリの管理
 */
class BinaryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BinaryManager"
        
        // yt-dlp バイナリURL
        private fun getYtdlpUrl(): String {
            val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            Log.d(TAG, "Device architecture: $arch")
            
            return when {
                arch.contains("arm64") || arch.contains("aarch64") -> 
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64"
                arch.contains("armeabi") || arch.contains("armv7") -> 
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_armv7l"
                arch.contains("x86_64") -> 
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_x86_64"
                else -> 
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64"
            }
        }
        
        // FFmpeg バイナリURL (ffmpeg-kit から)
        private fun getFfmpegUrl(): String {
            val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            // yt-dlp/FFmpeg-Buildsからのスタティックビルド
            return when {
                arch.contains("arm64") || arch.contains("aarch64") ->
                    "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
                arch.contains("armeabi") || arch.contains("armv7") ->
                    "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz"
                else ->
                    "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
            }
        }
    }

    // バイナリ保存場所: noBackupFilesDir（exec可能なディレクトリ）
    private val binDir: File = File(context.noBackupFilesDir, "bin").apply { 
        mkdirs()
        Log.d(TAG, "Binary directory: $absolutePath")
        // ディレクトリ自体の権限も設定
        setExecutable(true, false)
        setReadable(true, false)
        setWritable(true, false)
    }
    
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
        checkInstallStatus()
    }

    private fun checkInstallStatus() {
        val ytdlpFile = getYtdlpFile()
        val ffmpegFile = getFfmpegFile()
        
        // 既存のバイナリファイルの権限を再設定（アプリ更新やシステム変更で失われる可能性がある）
        if (ytdlpFile.exists() && ytdlpFile.length() > 0) {
            if (!ytdlpFile.canExecute()) {
                Log.w(TAG, "yt-dlp lacks execute permission, attempting to fix...")
                setExecutablePermission(ytdlpFile)
            }
        }
        if (ffmpegFile.exists() && ffmpegFile.length() > 0) {
            if (!ffmpegFile.canExecute()) {
                Log.w(TAG, "ffmpeg lacks execute permission, attempting to fix...")
                setExecutablePermission(ffmpegFile)
            }
        }
        
        _ytdlpState.value = BinaryInstallState(
            isInstalled = ytdlpFile.exists() && ytdlpFile.length() > 0 && ytdlpFile.canExecute()
        )
        _ffmpegState.value = BinaryInstallState(
            isInstalled = ffmpegFile.exists() && ffmpegFile.length() > 0 && ffmpegFile.canExecute()
        )
        
        Log.d(TAG, "yt-dlp: exists=${ytdlpFile.exists()}, canExecute=${ytdlpFile.canExecute()}")
        Log.d(TAG, "ffmpeg: exists=${ffmpegFile.exists()}, canExecute=${ffmpegFile.canExecute()}")
    }

    private fun getYtdlpFile(): File = File(binDir, "yt-dlp")
    private fun getFfmpegFile(): File = File(binDir, "ffmpeg")

    fun getYtdlpPath(): String? {
        val file = getYtdlpFile()
        if (!file.exists() || file.length() <= 0) return null
        if (!file.canExecute()) {
            setExecutablePermission(file)
        }
        return if (file.canExecute()) file.absolutePath else null
    }

    fun getFfmpegPath(): String? {
        val file = getFfmpegFile()
        if (!file.exists() || file.length() <= 0) return null
        if (!file.canExecute()) {
            setExecutablePermission(file)
        }
        return if (file.canExecute()) file.absolutePath else null
    }

    private fun chmodWithOs(path: String, mode: Int): Boolean {
        return try {
            // mode: 0x1ED = 493 = 0755, 0x1FF = 511 = 0777
            Os.chmod(path, mode)
            Log.d(TAG, "Os.chmod($path, ${String.format("0%o", mode)}) succeeded")
            true
        } catch (e: ErrnoException) {
            Log.w(TAG, "Os.chmod failed for $path: errno=${e.errno} msg=${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Os.chmod failed for $path: ${e.message}")
            false
        }
    }

    /**
     * バイナリに実行権限を付与（複数の方法を試行）
     */
    private fun setExecutablePermission(file: File): Boolean {
        try {
            Log.d(TAG, "Setting executable permission for: ${file.absolutePath}")

            // 0) libc chmod（外部コマンドに依存しない）- 最も確実
            // 0755 = rwxr-xr-x を使用（所有者のみ書き込み可、全員が実行・読取可）
            val osChmodSuccess = chmodWithOs(file.absolutePath, 0x1ED) // 0755 = 493 in decimal
            Log.d(TAG, "Os.chmod(0755) result: $osChmodSuccess")
            
            // 念のため0777も試す
            if (!osChmodSuccess || !file.canExecute()) {
                val osChmod777 = chmodWithOs(file.absolutePath, 0x1FF) // 0777 = 511 in decimal
                Log.d(TAG, "Os.chmod(0777) fallback result: $osChmod777")
            }
            
            // 方法1: Java API
            val javaSuccess = file.setExecutable(true, false) && 
                             file.setReadable(true, false) &&
                             file.setWritable(true, false)
            Log.d(TAG, "Java API chmod result: $javaSuccess")
            
            // 方法2: Runtime.exec で chmod（最も確実）
            try {
                val chmod = Runtime.getRuntime().exec(arrayOf("chmod", "777", file.absolutePath))
                val chmodExit = chmod.waitFor()
                Log.d(TAG, "chmod 777 via Runtime.exec exit code: $chmodExit")
                
                // chmodのエラー出力を確認
                if (chmodExit != 0) {
                    val errorReader = java.io.BufferedReader(java.io.InputStreamReader(chmod.errorStream))
                    val error = errorReader.readText()
                    Log.w(TAG, "chmod error: $error")
                }
            } catch (e: Exception) {
                Log.w(TAG, "chmod via Runtime failed: ${e.message}")
            }
            
            // 方法3: ProcessBuilderで chmod
            try {
                val pb = ProcessBuilder("chmod", "777", file.absolutePath)
                val p = pb.start()
                val pbExit = p.waitFor()
                Log.d(TAG, "chmod via ProcessBuilder exit code: $pbExit")
            } catch (e: Exception) {
                Log.w(TAG, "chmod via ProcessBuilder failed: ${e.message}")
            }
            
            // 最終確認
            val finalCheck = file.canExecute() && file.canRead()
            Log.d(TAG, "Final permission check - canExecute: ${file.canExecute()}, canRead: ${file.canRead()}, canWrite: ${file.canWrite()}")
            
            // ファイルのパーミッションを stat コマンドで確認
            try {
                val stat = Runtime.getRuntime().exec(arrayOf("stat", "-c", "%a", file.absolutePath))
                val statOutput = java.io.BufferedReader(java.io.InputStreamReader(stat.inputStream)).readLine()
                Log.d(TAG, "File permissions (octal): $statOutput")
            } catch (e: Exception) {
                Log.w(TAG, "stat command failed: ${e.message}")
            }
            
            return finalCheck
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set executable permission", e)
            return false
        }
    }

    /**
     * yt-dlpをインストール
     */
    suspend fun installYtdlp(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting yt-dlp installation...")
            _ytdlpState.value = BinaryInstallState(isDownloading = true, progress = 0)
            
            val ytdlpFile = getYtdlpFile()
            val tempFile = File(binDir, "yt-dlp.tmp")
            
            ytdlpFile.delete()
            tempFile.delete()
            
            val url = getYtdlpUrl()
            Log.d(TAG, "Downloading yt-dlp from: $url")
            
            downloadFile(url, tempFile) { progress ->
                _ytdlpState.value = BinaryInstallState(isDownloading = true, progress = progress)
            }
            
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile.renameTo(ytdlpFile)
                Log.d(TAG, "yt-dlp downloaded, size: ${ytdlpFile.length()}")
            } else {
                throw Exception("ダウンロードしたファイルが無効です")
            }
            
            // 実行権限を設定
            setExecutablePermission(ytdlpFile)
            
            Log.i(TAG, "yt-dlp installed at ${ytdlpFile.absolutePath}")
            
            _ytdlpState.value = BinaryInstallState(isInstalled = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install yt-dlp", e)
            _ytdlpState.value = BinaryInstallState(error = e.message ?: "不明なエラー")
            Result.failure(e)
        }
    }

    /**
     * FFmpegをインストール（シンプルなスタティックビルドを使用）
     */
    suspend fun installFfmpeg(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting FFmpeg installation...")
            _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = 0)
            
            val ffmpegFile = getFfmpegFile()
            ffmpegFile.delete()
            
            // John Van Sickle's static builds (より信頼性が高い)
            val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            val url = when {
                arch.contains("arm64") || arch.contains("aarch64") ->
                    "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz"
                arch.contains("x86_64") ->
                    "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz"
                else ->
                    "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz"
            }
            
            Log.d(TAG, "Downloading FFmpeg from: $url")
            
            val tempArchive = File(binDir, "ffmpeg.tar.xz")
            tempArchive.delete()
            
            downloadFile(url, tempArchive) { progress ->
                _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = progress / 2)
            }
            
            _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = 50)
            Log.d(TAG, "Extracting FFmpeg...")
            
            // tar.xz を展開
            extractTarXz(tempArchive, binDir) { progress ->
                _ffmpegState.value = BinaryInstallState(isDownloading = true, progress = 50 + progress / 2)
            }

            // extractTarXz は binDir 直下に ffmpeg を配置する実装なので、まずそれを優先的に確認
            if (ffmpegFile.exists() && ffmpegFile.length() > 0) {
                Log.d(TAG, "FFmpeg extracted directly to target path, size: ${ffmpegFile.length()}")
            } else {
                // ffmpeg-*/ffmpeg を探して binDir/ffmpeg に移動
                val extractedDir = binDir.listFiles()?.find { it.isDirectory && it.name.startsWith("ffmpeg-") }
                val extractedFfmpeg = extractedDir?.listFiles()?.find { it.name == "ffmpeg" }

                if (extractedFfmpeg != null && extractedFfmpeg.exists()) {
                    extractedFfmpeg.copyTo(ffmpegFile, overwrite = true)
                    extractedDir.deleteRecursively()
                    Log.d(TAG, "FFmpeg extracted from directory, size: ${ffmpegFile.length()}")
                } else {
                    // 直接 ffmpeg ファイルを探す（ffmpegFile と同一パスでもOK）
                    val directFfmpeg = binDir.listFiles()?.find { it.name == "ffmpeg" && it.isFile }
                    if (directFfmpeg != null && directFfmpeg.length() > 0) {
                        if (directFfmpeg.absolutePath != ffmpegFile.absolutePath) {
                            directFfmpeg.copyTo(ffmpegFile, overwrite = true)
                        }
                    } else {
                        throw Exception("FFmpegバイナリが見つかりません")
                    }
                }
            }
            
            tempArchive.delete()
            
            // 実行権限を設定
            setExecutablePermission(ffmpegFile)
            
            Log.i(TAG, "FFmpeg installed at ${ffmpegFile.absolutePath}")
            
            _ffmpegState.value = BinaryInstallState(isInstalled = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install FFmpeg", e)
            _ffmpegState.value = BinaryInstallState(error = e.message ?: "不明なエラー")
            Result.failure(e)
        }
    }

    fun uninstallYtdlp() {
        getYtdlpFile().delete()
        _ytdlpState.value = BinaryInstallState(isInstalled = false)
    }

    fun uninstallFfmpeg() {
        getFfmpegFile().delete()
        _ffmpegState.value = BinaryInstallState(isInstalled = false)
    }

    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) YtDlpApp/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response")
            val contentLength = body.contentLength()
            var downloaded = 0L

            outputFile.parentFile?.mkdirs()
            
            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            val progress = (downloaded * 100 / contentLength).toInt().coerceIn(0, 100)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }
        }
    }
    
    /**
     * tar.xz ファイルを展開
     */
    private fun extractTarXz(archive: File, destDir: File, onProgress: (Int) -> Unit) {
        try {
            // Apache Commons Compress を使用
            org.apache.commons.compress.compressors.xz.XZCompressorInputStream(
                BufferedInputStream(archive.inputStream())
            ).use { xzIn ->
                org.apache.commons.compress.archivers.tar.TarArchiveInputStream(xzIn).use { tarIn ->
                    var entry = tarIn.nextTarEntry
                    var count = 0
                    while (entry != null) {
                        val outFile = File(destDir, entry.name.substringAfterLast("/"))
                        if (!entry.isDirectory && (entry.name.endsWith("ffmpeg") || entry.name.endsWith("ffprobe"))) {
                            BufferedOutputStream(FileOutputStream(outFile)).use { out ->
                                tarIn.copyTo(out)
                            }
                            Log.d(TAG, "Extracted: ${outFile.name}")
                            chmodWithOs(outFile.absolutePath, 0x1FF)
                            outFile.setExecutable(true, false)
                        }
                        count++
                        onProgress((count * 100 / 50).coerceIn(0, 100)) // 概算
                        entry = tarIn.nextTarEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract tar.xz", e)
            throw Exception("アーカイブの展開に失敗: ${e.message}")
        }
    }
}
