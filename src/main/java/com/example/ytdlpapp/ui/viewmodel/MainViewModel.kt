package com.example.ytdlpapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpapp.data.repository.DownloadHistoryRepository
import com.example.ytdlpapp.data.repository.SettingsRepository
import com.example.ytdlpapp.domain.model.DownloadInfo
import com.example.ytdlpapp.domain.usecase.DownloadEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val url: String = "",
    val format: String = "best",
    val outputPath: String = "",
    val isDownloading: Boolean = false,
    val currentDownload: DownloadInfo? = null,
    val downloadHistory: List<DownloadInfo> = emptyList(),
    val errorMessage: String? = null,
    val logs: List<String> = emptyList()
)

class MainViewModel(
    private val downloadEngine: DownloadEngine,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: DownloadHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDownloadHistory()
        loadSettings()
    }

    fun setUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun setFormat(format: String) {
        _uiState.update { it.copy(format = format) }
    }

    fun setOutputPath(path: String) {
        _uiState.update { it.copy(outputPath = path) }
    }

    fun addLog(message: String) {
        _uiState.update { state ->
            state.copy(logs = state.logs + message)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun startDownload(
        ytdlpOptions: String = "",
        ffmpegOptions: String = ""
    ) {
        val state = _uiState.value
        if (state.url.isEmpty() || state.outputPath.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "URLと出力先フォルダを指定してください")
            }
            return
        }

        _uiState.update { it.copy(isDownloading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = downloadEngine.downloadWithYtdlp(
                url = state.url,
                format = state.format,
                outputPath = state.outputPath,
                ytdlpOptions = ytdlpOptions,
                ffmpegOptions = ffmpegOptions
            )

            result.onSuccess { downloadInfo ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        currentDownload = downloadInfo,
                        errorMessage = null
                    )
                }
                addLog("✓ ダウンロード完了: ${downloadInfo.title}")
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = error.message
                    )
                }
                addLog("✗ エラー: ${error.message}")
            }
        }
    }

    private fun loadDownloadHistory() {
        viewModelScope.launch {
            downloadEngine.getDownloadHistory().collect { history ->
                _uiState.update { it.copy(downloadHistory = history) }
            }
        }
    }

    private fun loadSettings() {
        val settings = settingsRepository.getSettings()
        _uiState.update {
            it.copy(outputPath = settings.downloadFolder)
        }
    }

    fun deleteDownloadHistory(id: Int) {
        viewModelScope.launch {
            historyRepository.deleteDownload(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.deleteAll()
        }
    }
}
