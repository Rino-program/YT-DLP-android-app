package com.example.ytdlpapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpapp.data.repository.DownloadQueueRepository
import com.example.ytdlpapp.domain.model.QueuedDownload
import com.example.ytdlpapp.domain.model.QueueStatus
import com.example.ytdlpapp.domain.usecase.QueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueManagerUiState(
    val queueItems: List<QueuedDownload> = emptyList(),
    val format: String = "best",
    val outputPath: String = "",
    val isProcessing: Boolean = false,
    val statusMessage: String = "",
    val queueCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val currentDownload: QueuedDownload? = null
)

class QueueManagerViewModel(
    private val queueManager: QueueManager,
    private val queueRepository: DownloadQueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueManagerUiState())
    val uiState: StateFlow<QueueManagerUiState> = _uiState.asStateFlow()

    init {
        loadQueueItems()
    }

    private fun loadQueueItems() {
        viewModelScope.launch {
            queueRepository.getAllQueued().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        queueItems = items,
                        queueCount = items.count { it.status == QueueStatus.QUEUED },
                        completedCount = items.count { it.status == QueueStatus.COMPLETED },
                        failedCount = items.count { it.status == QueueStatus.FAILED }
                    )
                }
            }
        }
    }

    fun addSingleUrl(url: String, format: String, outputPath: String) {
        viewModelScope.launch {
            try {
                val download = QueuedDownload(
                    url = url,
                    format = format,
                    outputPath = outputPath
                )
                queueRepository.addToQueue(download)
                _uiState.update {
                    it.copy(statusMessage = "✓ URLをキューに追加しました")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "✗ エラー: ${e.message}")
                }
            }
        }
    }

    fun addFromFile(filePath: String, outputPath: String) {
        viewModelScope.launch {
            try {
                val count = queueManager.addFromFile(
                    filePath,
                    "best",
                    outputPath
                )
                _uiState.update {
                    it.copy(statusMessage = "✓ $count URLs imported")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "✗ エラー: ${e.message}")
                }
            }
        }
    }

    fun startProcessing() {
        queueManager.startProcessing()
        _uiState.update { it.copy(isProcessing = true) }
    }

    fun stopProcessing() {
        queueManager.stopProcessing()
        _uiState.update { it.copy(isProcessing = false) }
    }

    fun clearQueue() {
        viewModelScope.launch {
            queueRepository.deleteAll()
            _uiState.update { it.copy(statusMessage = "✓ キューをクリアしました") }
        }
    }
}
