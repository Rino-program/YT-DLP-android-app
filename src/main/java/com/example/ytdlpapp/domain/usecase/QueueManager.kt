package com.example.ytdlpapp.domain.usecase

import android.content.Context
import android.util.Log
import com.example.ytdlpapp.data.repository.DownloadQueueRepository
import com.example.ytdlpapp.data.repository.ProxyRepository
import com.example.ytdlpapp.data.repository.StatisticsRepository
import com.example.ytdlpapp.domain.model.QueuedDownload
import com.example.ytdlpapp.domain.model.QueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class QueueManager(
    private val context: Context,
    private val queueRepository: DownloadQueueRepository,
    private val proxyRepository: ProxyRepository,
    private val statisticsRepository: StatisticsRepository,
    private val downloadEngine: DownloadEngine,
    private val coroutineScope: CoroutineScope
) {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentDownload = MutableStateFlow<QueuedDownload?>(null)
    val currentDownload: StateFlow<QueuedDownload?> = _currentDownload.asStateFlow()

    private val _queueProgress = MutableStateFlow(0)
    val queueProgress: StateFlow<Int> = _queueProgress.asStateFlow()

    private var processingJob: Job? = null

    /**
     * ダウンロードをキューに追加
     */
    suspend fun addToQueue(download: QueuedDownload): Int {
        val id = queueRepository.addToQueue(download).toInt()
        Log.d("QueueManager", "Added to queue: $id")
        return id
    }

    /**
     * 複数のURLをプレイリストとして追加
     */
    suspend fun addPlaylistToQueue(
        url: String,
        format: String = "best",
        outputPath: String,
        ytdlpOptions: String = "",
        ffmpegOptions: String = ""
    ) {
        // yt-dlpがプレイリストと認識したら複数アイテムとして追加
        val download = QueuedDownload(
            url = url,
            format = format,
            outputPath = outputPath,
            ytdlpOptions = ytdlpOptions,
            ffmpegOptions = ffmpegOptions,
            isPlaylist = true
        )
        queueRepository.addToQueue(download)
        Log.d("QueueManager", "Added playlist: $url")
    }

    /**
     * 複数のURLをテキストファイルから読み込み
     */
    suspend fun addFromFile(
        filePath: String,
        format: String = "best",
        outputPath: String,
        ytdlpOptions: String = "",
        ffmpegOptions: String = ""
    ): Int {
        var count = 0
        try {
            val file = File(filePath)
            val lines = file.readLines()
            for (line in lines) {
                val url = line.trim()
                if (url.isNotEmpty() && !url.startsWith("#")) {
                    val download = QueuedDownload(
                        url = url,
                        format = format,
                        outputPath = outputPath,
                        ytdlpOptions = ytdlpOptions,
                        ffmpegOptions = ffmpegOptions
                    )
                    queueRepository.addToQueue(download)
                    count++
                }
            }
            Log.d("QueueManager", "Added $count items from file")
        } catch (e: Exception) {
            Log.e("QueueManager", "Failed to read file: ${e.message}")
        }
        return count
    }

    /**
     * キュー処理を開始
     */
    fun startProcessing() {
        if (_isProcessing.value) return

        processingJob = coroutineScope.launch {
            _isProcessing.value = true
            try {
                while (isActive && _isProcessing.value) {
                    val nextDownload = queueRepository.getNextInQueue()
                    if (nextDownload != null) {
                        _currentDownload.value = nextDownload
                        processDownload(nextDownload)
                    } else {
                        _isProcessing.value = false
                        _currentDownload.value = null
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("QueueManager", "Error in queue processing", e)
                _isProcessing.value = false
            }
        }
    }

    /**
     * キュー処理を停止
     */
    fun stopProcessing() {
        _isProcessing.value = false
        processingJob?.cancel()
    }

    /**
     * 単一のダウンロード処理
     */
    private suspend fun processDownload(download: QueuedDownload) {
        try {
            queueRepository.updateProgress(download.id, 0, QueueStatus.DOWNLOADING)

            // プロキシ設定をチェック
            val proxy = proxyRepository.getProxySettings()
            var ytdlpOptions = download.ytdlpOptions
            if (proxy?.enabled == true) {
                val proxyUrl = proxy.toProxyUrl()
                if (proxyUrl != null) {
                    ytdlpOptions += " --proxy \"$proxyUrl\""
                }
            }

            val result = downloadEngine.downloadWithYtdlp(
                url = download.url,
                format = download.format,
                outputPath = download.outputPath,
                ytdlpOptions = ytdlpOptions,
                ffmpegOptions = download.ffmpegOptions
            )

            result.onSuccess { downloadInfo ->
                queueRepository.updateQueue(
                    download.copy(
                        status = QueueStatus.COMPLETED,
                        progress = 100,
                        title = downloadInfo.title
                    )
                )
                statisticsRepository.recordSuccess(downloadInfo.outputPath.length.toLong())
                Log.d("QueueManager", "Download completed: ${download.url}")
            }.onFailure { error ->
                queueRepository.updateQueue(
                    download.copy(
                        status = QueueStatus.FAILED,
                        errorMessage = error.message,
                        progress = 0
                    )
                )
                statisticsRepository.recordFailure()
                Log.e("QueueManager", "Download failed: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e("QueueManager", "Error processing download", e)
            queueRepository.updateQueue(
                download.copy(
                    status = QueueStatus.FAILED,
                    errorMessage = e.message
                )
            )
            statisticsRepository.recordFailure()
        }
    }

    /**
     * すべてのキューをクリア
     */
    suspend fun clearQueue() {
        queueRepository.deleteAll()
        stopProcessing()
    }

    /**
     * 完了したダウンロードを削除
     */
    suspend fun removeCompleted() {
        queueRepository.deleteCompleted()
    }

    /**
     * キューの順序を並べ替え（優先度でソート）
     */
    suspend fun reprioritizeQueue(id: Int, newPriority: Int) {
        val download = queueRepository.getNextInQueue()
        if (download != null && download.id == id) {
            queueRepository.updateQueue(download.copy(priority = newPriority))
        }
    }
}
