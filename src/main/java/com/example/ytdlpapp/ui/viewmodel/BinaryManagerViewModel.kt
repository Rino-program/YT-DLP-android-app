package com.example.ytdlpapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpapp.domain.model.BinaryInfo
import com.example.ytdlpapp.domain.usecase.BinaryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BinaryManagerUiState(
    val ytdlp: BinaryInfo = BinaryInfo(name = "yt-dlp"),
    val ffmpeg: BinaryInfo = BinaryInfo(name = "ffmpeg"),
    val isInstalling: Boolean = false,
    val isUpdating: Boolean = false,
    val progress: Int = 0,
    val statusMessage: String = "",
    val errorMessage: String? = null
)

class BinaryManagerViewModel(
    private val binaryManager: BinaryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BinaryManagerUiState())
    val uiState: StateFlow<BinaryManagerUiState> = _uiState.asStateFlow()

    init {
        checkBinaryStatus()
    }

    private fun checkBinaryStatus() {
        viewModelScope.launch {
            val ytdlpInstalled = binaryManager.isBinaryInstalled("yt-dlp")
            val ffmpegInstalled = binaryManager.isBinaryInstalled("ffmpeg")

            _uiState.update { state ->
                state.copy(
                    ytdlp = state.ytdlp.copy(
                        isInstalled = ytdlpInstalled,
                        installPath = binaryManager.getBinaryPath("yt-dlp") ?: ""
                    ),
                    ffmpeg = state.ffmpeg.copy(
                        isInstalled = ffmpegInstalled,
                        installPath = binaryManager.getBinaryPath("ffmpeg") ?: ""
                    )
                )
            }
        }
    }

    fun installYtdlp() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInstalling = true,
                    statusMessage = "yt-dlpをダウンロード中...",
                    errorMessage = null
                )
            }

            binaryManager.downloadAndInstallYtdlp().onSuccess { path ->
                _uiState.update { state ->
                    state.copy(
                        isInstalling = false,
                        ytdlp = state.ytdlp.copy(
                            isInstalled = true,
                            installPath = path
                        ),
                        statusMessage = "✓ yt-dlpをインストールしました",
                        progress = 100
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isInstalling = false,
                        errorMessage = "インストール失敗: ${error.message}",
                        statusMessage = ""
                    )
                }
            }
        }
    }

    fun installFfmpeg() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInstalling = true,
                    statusMessage = "ffmpegをダウンロード中...",
                    errorMessage = null
                )
            }

            binaryManager.downloadAndInstallFfmpeg().onSuccess { path ->
                _uiState.update { state ->
                    state.copy(
                        isInstalling = false,
                        ffmpeg = state.ffmpeg.copy(
                            isInstalled = true,
                            installPath = path
                        ),
                        statusMessage = "✓ ffmpegをインストールしました",
                        progress = 100
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isInstalling = false,
                        errorMessage = "インストール失敗: ${error.message}",
                        statusMessage = ""
                    )
                }
            }
        }
    }

    fun updateYtdlp() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    statusMessage = "yt-dlpをアップデート中...",
                    errorMessage = null
                )
            }

            binaryManager.updateYtdlp().onSuccess { message ->
                _uiState.update { state ->
                    state.copy(
                        isUpdating = false,
                        statusMessage = "✓ アップデート完了",
                        progress = 100
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = "アップデート失敗: ${error.message}",
                        statusMessage = ""
                    )
                }
            }
        }
    }

    fun removeYtdlp() {
        if (binaryManager.removeBinary("yt-dlp")) {
            _uiState.update { state ->
                state.copy(
                    ytdlp = state.ytdlp.copy(
                        isInstalled = false,
                        installPath = ""
                    ),
                    statusMessage = "✓ yt-dlpを削除しました"
                )
            }
        }
    }

    fun removeFfmpeg() {
        if (binaryManager.removeBinary("ffmpeg")) {
            _uiState.update { state ->
                state.copy(
                    ffmpeg = state.ffmpeg.copy(
                        isInstalled = false,
                        installPath = ""
                    ),
                    statusMessage = "✓ ffmpegを削除しました"
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = "") }
    }
}
