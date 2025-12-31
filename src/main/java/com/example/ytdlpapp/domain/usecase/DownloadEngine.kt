package com.example.ytdlpapp.domain.usecase

import android.content.Context
import android.util.Log
import com.example.ytdlpapp.data.repository.DownloadHistoryRepository
import com.example.ytdlpapp.domain.model.DownloadInfo
import com.example.ytdlpapp.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class DownloadEngine(
    private val context: Context,
    private val binaryManager: BinaryManager,
    private val repository: DownloadHistoryRepository
) {
    private val _downloadState = MutableStateFlow<DownloadInfo?>(null)
    val downloadState: StateFlow<DownloadInfo?> = _downloadState

    /**
     * yt-dlpを使用してダウンロード実行
     */
    suspend fun downloadWithYtdlp(
        url: String,
        format: String = "best",
        outputPath: String,
        ytdlpOptions: String = "",
        ffmpegOptions: String = ""
    ): Result<DownloadInfo> = withContext(Dispatchers.Default) {
        try {
            val ytdlpPath = binaryManager.getBinaryPath("yt-dlp")
                ?: return@withContext Result.failure(Exception("yt-dlp not installed"))

            val downloadInfo = DownloadInfo(
                url = url,
                format = format,
                outputPath = outputPath,
                status = DownloadStatus.DOWNLOADING
            )

            // データベースに保存
            val id = repository.insertDownload(downloadInfo).toInt()
            val updatedInfo = downloadInfo.copy(id = id)
            _downloadState.value = updatedInfo
            repository.updateDownload(updatedInfo)

            // コマンドを構築
            val command = mutableListOf(
                ytdlpPath,
                "-f", format,
                "-o", "${outputPath}/%(title)s.%(ext)s"
            )

            // カスタムオプションを追加
            if (ytdlpOptions.isNotEmpty()) {
                command.addAll(ytdlpOptions.split("\\s+".toRegex()))
            }

            // ffmpeg使用時のオプション
            if (ffmpegOptions.isNotEmpty()) {
                command.add("--postprocessor-args")
                command.add(ffmpegOptions)
            }

            command.add(url)

            // プロセスを実行
            val processBuilder = ProcessBuilder(command)
                .directory(File(outputPath))
                .redirectErrorStream(true)

            val process = processBuilder.start()
            val inputStream = process.inputStream.bufferedReader()

            var progressLine = ""
            while (true) {
                val line = inputStream.readLine() ?: break
                progressLine = line
                Log.d("DownloadEngine", line)

                // プログレス情報を抽出
                val progressMatch = Regex("""\[download\]\s+(\d+\.?\d*)%""").find(line)
                if (progressMatch != null) {
                    val progress = progressMatch.groupValues[1].toDoubleOrNull()?.toInt() ?: 0
                    val info = updatedInfo.copy(progress = progress)
                    _downloadState.value = info
                    repository.updateDownload(info)
                }
            }

            process.waitFor()
            val exitCode = process.exitValue()

            if (exitCode == 0) {
                val completedInfo = updatedInfo.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100
                )
                _downloadState.value = completedInfo
                repository.updateDownload(completedInfo)
                Result.success(completedInfo)
            } else {
                val failedInfo = updatedInfo.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "Download failed with exit code: $exitCode. Last output: $progressLine"
                )
                _downloadState.value = failedInfo
                repository.updateDownload(failedInfo)
                Result.failure(Exception(failedInfo.errorMessage))
            }
        } catch (e: Exception) {
            Log.e("DownloadEngine", "Download error", e)
            val errorInfo = DownloadInfo(
                url = url,
                format = format,
                outputPath = outputPath,
                status = DownloadStatus.FAILED,
                errorMessage = e.message
            )
            val id = repository.insertDownload(errorInfo).toInt()
            val withId = errorInfo.copy(id = id)
            _downloadState.value = withId
            repository.updateDownload(withId)
            Result.failure(e)
        }
    }

    /**
     * 複数のファイルをffmpegで処理
     */
    suspend fun processWithFfmpeg(
        inputFiles: List<String>,
        outputFile: String,
        ffmpegOptions: String
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            val ffmpegPath = binaryManager.getBinaryPath("ffmpeg")
                ?: return@withContext Result.failure(Exception("ffmpeg not installed"))

            val command = mutableListOf(ffmpegPath)

            // 入力ファイルを追加
            inputFiles.forEach { inputFile ->
                command.add("-i")
                command.add(inputFile)
            }

            // カスタムオプションを追加
            if (ffmpegOptions.isNotEmpty()) {
                command.addAll(ffmpegOptions.split("\\s+".toRegex()))
            }

            command.add(outputFile)

            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                Result.success(outputFile)
            } else {
                Result.failure(Exception("ffmpeg processing failed: $output"))
            }
        } catch (e: Exception) {
            Log.e("DownloadEngine", "ffmpeg error", e)
            Result.failure(e)
        }
    }

    /**
     * ダウンロードをキャンセル
     */
    fun cancelDownload(id: Int) {
        // キャンセル実装（プロセス終了など）
    }

    /**
     * ダウンロード履歴を取得
     */
    fun getDownloadHistory() = repository.getAll()
}
