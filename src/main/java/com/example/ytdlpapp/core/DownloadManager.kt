package com.example.ytdlpapp.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.ytdlpapp.model.DownloadOptions
import com.example.ytdlpapp.model.DownloadState
import com.example.ytdlpapp.model.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile

/**
 * yt-dlpを使用したダウンロード管理
 */
class DownloadManager(
    private val context: Context,
    private val binaryManager: BinaryManager
) {
    companion object {
        private const val TAG = "DownloadManager"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _logOutput = MutableStateFlow("")
    val logOutput: StateFlow<String> = _logOutput

    private var currentProcess: Process? = null

    /**
     * ELFバイナリがPIE(Position Independent Executable)形式かどうかをチェック
     * PIE形式(e_type: 3 = ET_DYN)の場合のみlinkerで実行可能
     */
    private fun isPieExecutable(filePath: String): Boolean {
        return try {
            RandomAccessFile(File(filePath), "r").use { raf ->
                // ELFマジックナンバーをチェック (0x7F 'E' 'L' 'F')
                val magic = ByteArray(4)
                raf.read(magic)
                if (magic[0] != 0x7F.toByte() || magic[1] != 'E'.code.toByte() ||
                    magic[2] != 'L'.code.toByte() || magic[3] != 'F'.code.toByte()) {
                    return false
                }
                
                // e_type フィールドを読み取る (オフセット 0x10)
                raf.seek(0x10)
                val eType = raf.readShort().toInt() and 0xFFFF
                // ET_DYN (3) = PIE実行可能ファイルまたは共有オブジェクト
                eType == 3
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if binary is PIE: ${e.message}")
            false
        }
    }

    fun getDefaultOutputDir(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "YtDlp").apply { mkdirs() }
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun isPlaylist(url: String): Boolean {
        return url.contains("list=")
    }

    private fun appendLog(text: String) {
        _logOutput.value += text
    }

    /**
     * ダウンロードを実行
     */
    suspend fun download(
        url: String,
        options: DownloadOptions = DownloadOptions()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ytdlpPath = binaryManager.getYtdlpPath()
            if (ytdlpPath == null) {
                val error = "yt-dlp がインストールされていません"
                _downloadState.value = DownloadState.Error(error)
                appendLog("✗ エラー: $error\n")
                return@withContext Result.failure(Exception(error))
            }

            val ytdlpFile = File(ytdlpPath)
            if (!ytdlpFile.exists()) {
                val error = "yt-dlp ファイルが見つかりません: $ytdlpPath"
                _downloadState.value = DownloadState.Error(error)
                appendLog("✗ エラー: $error\n")
                return@withContext Result.failure(Exception(error))
            }

            
            // 実行権限を確実に設定（BinaryManagerを経由）
            Log.d(TAG, "Ensuring executable permission for yt-dlp...")
            if (!ytdlpFile.canExecute()) {
                Log.w(TAG, "yt-dlp lacks execution permission, requesting BinaryManager to fix...")
                // BinaryManagerのsetExecutablePermissionを呼び出すためのワークアラウンド
                try {
                    // Os.chmod を直接使用（Android 6.0+）
                    android.system.Os.chmod(ytdlpPath, 0x1ED) // 0755
                    Log.d(TAG, "Applied chmod 0755 via Os.chmod")
                } catch (e: Exception) {
                    Log.w(TAG, "Os.chmod failed, trying 0777: ${e.message}")
                    try {
                        android.system.Os.chmod(ytdlpPath, 0x1FF) // 0777
                    } catch (e2: Exception) {
                        Log.e(TAG, "Os.chmod 0777 also failed: ${e2.message}")
                    }
                }
                
                // Java APIでも試行
                ytdlpFile.setExecutable(true, false)
                ytdlpFile.setReadable(true, false)
                ytdlpFile.setWritable(true, false)
            }
            
            // 古いchmodコード（後方互換性のため残す）
            try {
                val chmodResult = Runtime.getRuntime().exec(arrayOf("chmod", "777", ytdlpPath))
                chmodResult.waitFor()
                Log.d(TAG, "Pre-execution chmod exit code: ${chmodResult.exitValue()}")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-execution chmod failed: ${e.message}")
            }

            if (!ytdlpFile.canExecute()) {
                val error = "yt-dlp を実行できません (Permission denied)。バイナリ管理で yt-dlp を再インストールしてください。"
                _downloadState.value = DownloadState.Error(error)
                appendLog("✗ エラー: $error\n")
                appendLog("パス: ${ytdlpFile.absolutePath} (canExecute=${ytdlpFile.canExecute()}, canRead=${ytdlpFile.canRead()}, canWrite=${ytdlpFile.canWrite()})\n")
                return@withContext Result.failure(Exception(error))
            }
            
            Log.d(TAG, "yt-dlp executable check: canExecute=${ytdlpFile.canExecute()}, canRead=${ytdlpFile.canRead()}")

            // 出力ディレクトリ
            val outputDir = if (options.outputDir.isNotBlank()) {
                File(options.outputDir).apply { mkdirs() }
            } else {
                getDefaultOutputDir()
            }

            // FFmpegパスを取得
            val ffmpegPath = binaryManager.getFfmpegPath()

            Log.d(TAG, "Starting download: $url")
            Log.d(TAG, "Output directory: ${outputDir.absolutePath}")
            Log.d(TAG, "yt-dlp path: $ytdlpPath")
            Log.d(TAG, "ffmpeg path: $ffmpegPath")

            _downloadState.value = DownloadState.Downloading(0)
            _logOutput.value = ""

            // コマンドを構築（引数リスト）
            val args = buildArgs(ytdlpPath, url, outputDir, options, ffmpegPath)
            
            appendLog("=== ダウンロード開始 ===\n")
            appendLog("URL: $url\n")
            appendLog("出力先: ${outputDir.absolutePath}\n")
            appendLog("コマンド: ${args.joinToString(" ")}\n\n")

            // Android 10+ W^X制限対応: リンカー経由で実行
            // /system/bin/linker64 <binary> <args...>
            val linker = if (android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
                "/system/bin/linker64"
            } else {
                "/system/bin/linker"
            }
            
            val command: List<String>
            val executionMode: String
            
            // まずリンカー経由で試行（Android 10+対応）
            if (File(linker).exists()) {
                // linker64 /path/to/yt-dlp arg1 arg2 ...
                command = listOf(linker) + args
                executionMode = "linker wrapper ($linker)"
                Log.d(TAG, "Using linker wrapper execution")
            } else {
                // フォールバック: sh -c経由
                Log.d(TAG, "Linker not found, falling back to shell wrapper")
                val shellCommand = args.joinToString(" ") { arg ->
                    "'" + arg.replace("'", "'\\''") + "'"
                }
                command = listOf("sh", "-c", shellCommand)
                executionMode = "shell wrapper (sh -c)"
            }

            val processBuilder = ProcessBuilder(command)
                .directory(context.filesDir) // 作業ディレクトリはアプリ内部
                .redirectErrorStream(true)

            // 環境変数を設定
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PATH"] = "${File(ytdlpPath).parent}:/system/bin:/system/xbin:/system/bin"
            env["LD_LIBRARY_PATH"] = "/system/lib64:/system/lib"
            
            Log.d(TAG, "Environment: HOME=${env["HOME"]}, TMPDIR=${env["TMPDIR"]}, PATH=${env["PATH"]}")

            Log.d(TAG, "Executing command: ${command.joinToString(" ")}")
            appendLog("実行モード: $executionMode\n\n")

            currentProcess = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))

            var lastFilePath: String? = null

            reader.forEachLine { line ->
                appendLog(line + "\n")
                Log.d(TAG, "yt-dlp: $line")

                // 進捗を解析
                parseProgress(line)?.let { progress ->
                    _downloadState.value = DownloadState.Downloading(
                        progress = progress.percent,
                        speed = progress.speed,
                        eta = progress.eta
                    )
                }

                // 出力ファイルパスを取得
                if (line.contains("[download] Destination:") || line.contains("[Merger]")) {
                    val pathMatch = Regex("""(?:Destination:|Merging formats into "?)(.+\.[\w]+)"?""").find(line)
                    pathMatch?.groupValues?.getOrNull(1)?.let {
                        lastFilePath = it.trim().removeSuffix("\"")
                    }
                }
                
                // 処理中のステータス
                if (line.contains("[download]") && line.contains("100%")) {
                    _downloadState.value = DownloadState.Processing("ファイルを処理中...")
                }
                if (line.contains("[Merger]") || line.contains("[ExtractAudio]")) {
                    _downloadState.value = DownloadState.Processing("変換中...")
                }
            }

            val exitCode = currentProcess!!.waitFor()
            currentProcess = null

            Log.d(TAG, "Process exited with code: $exitCode")

            if (exitCode == 0) {
                val filePath = lastFilePath ?: outputDir.absolutePath
                _downloadState.value = DownloadState.Completed(filePath)
                appendLog("\n✓ ダウンロード完了: $filePath\n")
                Result.success(filePath)
            } else {
                val error = "ダウンロード失敗 (exit code: $exitCode)"
                _downloadState.value = DownloadState.Error(error)
                appendLog("\n✗ $error\n")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            val errorMsg = e.message ?: "不明なエラー"
            _downloadState.value = DownloadState.Error(errorMsg)
            appendLog("\n✗ エラー: $errorMsg\n")
            Result.failure(e)
        }
    }

    private fun buildArgs(
        ytdlpPath: String,
        url: String,
        outputDir: File,
        options: DownloadOptions,
        ffmpegPath: String?
    ): List<String> {
        val args = mutableListOf<String>()
        
        args.add(ytdlpPath)
        
        // FFmpegパスを指定（インストールされている場合）
        if (ffmpegPath != null) {
            args.addAll(listOf("--ffmpeg-location", File(ffmpegPath).parent ?: ffmpegPath))
        }
        
        // 基本オプション
        args.addAll(listOf("--no-mtime", "--newline"))
        
        // 出力テンプレート
        val outputTemplate = if (isPlaylist(url)) {
            val padDigits = options.playlistPadding
            "${outputDir.absolutePath}/%(playlist_index)0${padDigits}d - %(title)s.%(ext)s"
        } else {
            "${outputDir.absolutePath}/%(title)s.%(ext)s"
        }
        args.addAll(listOf("-o", outputTemplate))
        
        // プレイリスト設定
        if (!isPlaylist(url)) {
            args.add("--no-playlist")
        }
        
        // フォーマット指定
        when (options.format) {
            OutputFormat.MP3 -> {
                args.addAll(listOf("-x", "--audio-format", "mp3", "--audio-quality", "0"))
            }
            OutputFormat.M4A -> {
                args.addAll(listOf("-x", "--audio-format", "m4a", "--audio-quality", "0"))
            }
            OutputFormat.BEST -> {
                // デフォルト（最高品質）
            }
            else -> {
                // 動画フォーマット
                if (isYouTubeUrl(url) && options.excludeAv1) {
                    // YouTube用：AV1を除外（互換性重視）
                    args.addAll(listOf(
                        "-f", "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/bestvideo[ext=mp4]+bestaudio/best[ext=mp4]/best"
                    ))
                }
                
                // 出力形式
                when (options.format) {
                    OutputFormat.MP4 -> args.addAll(listOf("--merge-output-format", "mp4"))
                    OutputFormat.WEBM -> args.addAll(listOf("--merge-output-format", "webm"))
                    OutputFormat.MKV -> args.addAll(listOf("--merge-output-format", "mkv"))
                    else -> {}
                }
            }
        }
        
        // カスタムオプション
        if (options.customOptions.isNotBlank()) {
            args.addAll(options.customOptions.split(" ").filter { it.isNotBlank() })
        }
        
        // URL
        args.add(url)
        
        return args
    }

    private data class Progress(
        val percent: Int,
        val speed: String,
        val eta: String
    )

    private fun parseProgress(line: String): Progress? {
        // [download]  45.2% of 10.50MiB at  2.50MiB/s ETA 00:03
        val regex = Regex("""\[download\]\s+(\d+\.?\d*)%.*?(?:at\s+)?(\d+\.?\d*\s*\w+/s)?.*?(?:ETA\s+)?(\d{2}:\d{2})?""")
        val match = regex.find(line) ?: return null
        
        val percent = match.groupValues[1].toFloatOrNull()?.toInt() ?: return null
        val speed = match.groupValues.getOrNull(2)?.trim() ?: ""
        val eta = match.groupValues.getOrNull(3)?.trim() ?: ""
        
        return Progress(percent, speed, eta)
    }

    fun cancel() {
        currentProcess?.destroyForcibly()
        currentProcess = null
        _downloadState.value = DownloadState.Error("キャンセルされました")
        appendLog("\n⚠ ダウンロードがキャンセルされました\n")
    }

    fun reset() {
        _downloadState.value = DownloadState.Idle
        _logOutput.value = ""
    }
}
