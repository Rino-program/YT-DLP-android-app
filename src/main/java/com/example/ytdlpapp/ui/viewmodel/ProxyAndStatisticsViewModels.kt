package com.example.ytdlpapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpapp.data.repository.ProxyRepository
import com.example.ytdlpapp.data.repository.StatisticsRepository
import com.example.ytdlpapp.domain.model.DownloadStatistics
import com.example.ytdlpapp.domain.model.ProxySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProxySettingsUiState(
    val enabled: Boolean = false,
    val protocol: String = "http",
    val host: String = "",
    val port: Int = 0,
    val username: String? = null,
    val password: String? = null
)

class ProxySettingsViewModel(
    private val proxyRepository: ProxyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxySettingsUiState())
    val uiState: StateFlow<ProxySettingsUiState> = _uiState.asStateFlow()

    init {
        loadProxySettings()
    }

    private fun loadProxySettings() {
        viewModelScope.launch {
            proxyRepository.getProxySettingsFlow().collect { settings ->
                if (settings != null) {
                    _uiState.value = ProxySettingsUiState(
                        enabled = settings.enabled,
                        protocol = settings.protocol,
                        host = settings.host,
                        port = settings.port,
                        username = settings.username,
                        password = settings.password
                    )
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    fun setProtocol(protocol: String) {
        _uiState.update { it.copy(protocol = protocol) }
    }

    fun setHost(host: String) {
        _uiState.update { it.copy(host = host) }
    }

    fun setPort(port: Int) {
        _uiState.update { it.copy(port = port) }
    }

    fun setUsername(username: String?) {
        _uiState.update { it.copy(username = username) }
    }

    fun setPassword(password: String?) {
        _uiState.update { it.copy(password = password) }
    }

    fun saveProxySettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = ProxySettings(
                enabled = state.enabled,
                protocol = state.protocol,
                host = state.host,
                port = state.port,
                username = state.username,
                password = state.password
            )
            proxyRepository.saveProxySettings(settings)
        }
    }
}

data class StatisticsUiState(
    val totalDownloads: Int = 0,
    val successfulDownloads: Int = 0,
    val failedDownloads: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val totalDuration: Long = 0L,
    val averageSpeed: Float = 0f,
    val successRate: Float = 0f
)

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            statisticsRepository.getStatisticsFlow().collect { stats ->
                if (stats != null) {
                    _uiState.value = StatisticsUiState(
                        totalDownloads = stats.totalDownloads,
                        successfulDownloads = stats.successfulDownloads,
                        failedDownloads = stats.failedDownloads,
                        totalBytesDownloaded = stats.totalBytesDownloaded,
                        totalDuration = stats.totalDuration,
                        averageSpeed = stats.averageSpeed,
                        successRate = stats.successRate
                    )
                }
            }
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            statisticsRepository.reset()
            _uiState.value = StatisticsUiState()
        }
    }
}
