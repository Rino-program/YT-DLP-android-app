package com.example.ytdlpapp.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.ytdlpapp.model.DownloadOptions
import com.example.ytdlpapp.model.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * ダウンロード処理を管理
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
     * ダウンロードを実行
     */
    suspend fun download(
        url: String,
        options: DownloadOptions = DownloadOptions()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ytdlpPath = binaryManager.getYtdlpPath()
                ?: return@withContext Result.failure(Exception("yt-dlp がインストールされていません"))

            // 出力ディレクトリ
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "YtDlp"
            ).also { it.mkdirs() }

            _downloadState.value = DownloadState.Downloading(0)
            _logOutput.value = ""

            // コマンドを構築
            val command = buildCommand(ytdlpPath, url, outputDir, options)
            Log.d(TAG, "Command: ${command.joinToString(" ")}")
            appendLog("実行コマンド: ${command.joinToString(" ")}\n")

            // プロセスを実行
            val processBuilder = ProcessBuilder(command)
                .directory(outputDir)
                .redirectErrorStream(true)

            // 環境変数を設定（ffmpegパス）
            binaryManager.getFfmpegPath()?.let { ffmpegPath ->
                val ffmpegDir = File(ffmpegPath).parentFile?.absolutePath
                processBuilder.environment()["PATH"] = "$ffmpegDir:${processBuilder.environment()["PATH"]}"
            }

            currentProcess = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))

            var lastFilePath: String? = null

            reader.forEachLine { line ->
                appendLog(line + "\n")
                Log.d(TAG, line)

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

                // 後処理中
                if (line.contains("[ExtractAudio]") || line.contains("[Merger]") || line.contains("[ffmpeg]")) {
                    _downloadState.value = DownloadState.Processing(
                        if (line.contains("[ExtractAudio]")) "音声を抽出中..."
                        else if (line.contains("[Merger]")) "動画を結合中..."
                        else "後処理中..."
                    )
                }
            }

            val exitCode = currentProcess!!.waitFor()
            currentProcess = null

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

    /**
     * ダウンロードをキャンセル
     */
    fun cancel() {
        currentProcess?.destroy()
        currentProcess = null
        _downloadState.value = DownloadState.Error("キャンセルされました")
    }

    /**
     * 状態をリセット
     */
    fun reset() {
        _downloadState.value = DownloadState.Idle
        _logOutput.value = ""
    }

    private fun buildCommand(
        ytdlpPath: String,
        url: String,
        outputDir: File,
        options: DownloadOptions
    ): List<String> {
        val cmd = mutableListOf(
            ytdlpPath,
            "--no-mtime",
            "--newline",
            "-o", "${outputDir.absolutePath}/%(title)s.%(ext)s"
        )

        // ffmpegパスを指定
        binaryManager.getFfmpegPath()?.let { ffmpegPath ->
            cmd.add("--ffmpeg-location")
            cmd.add(File(ffmpegPath).parentFile?.absolutePath ?: ffmpegPath)
        }

        // 音声のみ
        if (options.audioOnly) {
            cmd.add("-x")
            cmd.add("--audio-format")
            cmd.add(options.audioFormat)
        } else {
            // フォーマット指定
            if (options.format.isNotEmpty() && options.format != "best") {
                cmd.add("-f")
                cmd.add(options.format)
            }
        }

        // カスタムオプション
        if (options.customOptions.isNotBlank()) {
            cmd.addAll(options.customOptions.split("\\s+".toRegex()).filter { it.isNotBlank() })
        }

        cmd.add(url)
        return cmd
    }

    private data class ProgressInfo(
        val percent: Int,
        val speed: String,
        val eta: String
    )

    private fun parseProgress(line: String): ProgressInfo? {
        // [download]  45.3% of 100.00MiB at  5.00MiB/s ETA 00:10
        val regex = Regex("""\[download\]\s+(\d+(?:\.\d+)?)%.*?(?:at\s+(\S+))?\s*(?:ETA\s+(\S+))?""")
        val match = regex.find(line) ?: return null
        
        return ProgressInfo(
            percent = match.groupValues[1].toDoubleOrNull()?.toInt() ?: 0,
            speed = match.groupValues.getOrNull(2) ?: "",
            eta = match.groupValues.getOrNull(3) ?: ""
        )
    }

    private fun appendLog(text: String) {
        _logOutput.value = _logOutput.value + text
    }
}
