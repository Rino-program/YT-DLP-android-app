package com.example.ytdlpapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ytdlpapp.data.repository.SettingsRepository
import com.example.ytdlpapp.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val downloadFolder: String = "",
    val autoUpdateBinary: Boolean = true,
    val ytdlpOptions: String = "",
    val ffmpegOptions: String = "",
    val showDebugLog: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val settings = settingsRepository.getSettings()
        _uiState.value = SettingsUiState(
            downloadFolder = settings.downloadFolder,
            autoUpdateBinary = settings.autoUpdateBinary,
            ytdlpOptions = settings.ytdlpOptions,
            ffmpegOptions = settings.ffmpegOptions,
            showDebugLog = settings.showDebugLog
        )
    }

    fun setDownloadFolder(path: String) {
        _uiState.update { it.copy(downloadFolder = path) }
        settingsRepository.setDownloadFolder(path)
    }

    fun setAutoUpdateBinary(enabled: Boolean) {
        _uiState.update { it.copy(autoUpdateBinary = enabled) }
        settingsRepository.setAutoUpdateBinary(enabled)
    }

    fun setYtdlpOptions(options: String) {
        _uiState.update { it.copy(ytdlpOptions = options) }
        settingsRepository.setYtdlpOptions(options)
    }

    fun setFfmpegOptions(options: String) {
        _uiState.update { it.copy(ffmpegOptions = options) }
        settingsRepository.setFfmpegOptions(options)
    }

    fun setShowDebugLog(enabled: Boolean) {
        _uiState.update { it.copy(showDebugLog = enabled) }
        settingsRepository.setShowDebugLog(enabled)
    }

    fun getSettings(): AppSettings {
        val state = _uiState.value
        return AppSettings(
            downloadFolder = state.downloadFolder,
            autoUpdateBinary = state.autoUpdateBinary,
            ytdlpOptions = state.ytdlpOptions,
            ffmpegOptions = state.ffmpegOptions,
            showDebugLog = state.showDebugLog
        )
    }
}
