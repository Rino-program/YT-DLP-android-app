package com.example.ytdlpapp.data.repository

import android.content.Context
import com.example.ytdlpapp.data.db.AppDatabase
import com.example.ytdlpapp.data.db.entity.DownloadStatisticsEntity
import com.example.ytdlpapp.data.db.entity.ProxySettingsEntity
import com.example.ytdlpapp.domain.model.DownloadStatistics
import com.example.ytdlpapp.domain.model.ProxySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProxyRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).proxySettingsDao()

    suspend fun saveProxySettings(proxySettings: ProxySettings) {
        dao.insert(proxySettings.toEntity())
    }

    suspend fun getProxySettings(): ProxySettings? {
        return dao.getProxySettings()?.toDomain()
    }

    fun getProxySettingsFlow(): Flow<ProxySettings?> {
        return dao.getProxySettingsFlow().map { it?.toDomain() }
    }

    private fun ProxySettings.toEntity() = ProxySettingsEntity(
        enabled = enabled,
        protocol = protocol,
        host = host,
        port = port,
        username = username,
        password = password
    )

    private fun ProxySettingsEntity.toDomain() = ProxySettings(
        enabled = enabled,
        protocol = protocol,
        host = host,
        port = port,
        username = username,
        password = password
    )
}

class StatisticsRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).downloadStatisticsDao()

    suspend fun getStatistics(): DownloadStatistics? {
        return dao.getStatistics()?.toDomain()
    }

    fun getStatisticsFlow(): Flow<DownloadStatistics?> {
        return dao.getStatisticsFlow().map { it?.toDomain() }
    }

    suspend fun recordSuccess(bytes: Long) {
        // 既に存在しるなら更新、なければ作成
        val current = dao.getStatistics()
        if (current != null) {
            dao.recordSuccess(bytes)
        } else {
            dao.insert(
                DownloadStatisticsEntity(
                    totalDownloads = 1,
                    successfulDownloads = 1,
                    totalBytesDownloaded = bytes
                )
            )
        }
    }

    suspend fun recordFailure() {
        val current = dao.getStatistics()
        if (current != null) {
            dao.recordFailure()
        } else {
            dao.insert(
                DownloadStatisticsEntity(
                    totalDownloads = 1,
                    failedDownloads = 1
                )
            )
        }
    }

    suspend fun reset() {
        dao.reset()
    }

    private fun DownloadStatisticsEntity.toDomain() = DownloadStatistics(
        totalDownloads = totalDownloads,
        successfulDownloads = successfulDownloads,
        failedDownloads = failedDownloads,
        totalBytesDownloaded = totalBytesDownloaded,
        totalDuration = totalDuration,
        averageSpeed = averageSpeed
    )
}
