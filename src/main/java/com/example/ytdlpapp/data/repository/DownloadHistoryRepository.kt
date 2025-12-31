package com.example.ytdlpapp.data.repository

import android.content.Context
import com.example.ytdlpapp.data.db.AppDatabase
import com.example.ytdlpapp.data.db.entity.DownloadHistoryEntity
import com.example.ytdlpapp.domain.model.DownloadInfo
import com.example.ytdlpapp.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadHistoryRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).downloadHistoryDao()

    suspend fun insertDownload(downloadInfo: DownloadInfo): Long {
        return dao.insert(downloadInfo.toEntity())
    }

    suspend fun updateDownload(downloadInfo: DownloadInfo) {
        dao.update(downloadInfo.toEntity())
    }

    suspend fun deleteDownload(id: Int) {
        dao.deleteById(id)
    }

    fun getAll(): Flow<List<DownloadInfo>> {
        return dao.getAllFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getById(id: Int): DownloadInfo? {
        return dao.getById(id)?.toDomain()
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun DownloadInfo.toEntity() = DownloadHistoryEntity(
        id = id,
        url = url,
        title = title,
        format = format,
        outputPath = outputPath,
        status = status.name,
        progress = progress,
        errorMessage = errorMessage,
        timestamp = timestamp
    )

    private fun DownloadHistoryEntity.toDomain() = DownloadInfo(
        id = id,
        url = url,
        title = title,
        format = format,
        outputPath = outputPath,
        status = DownloadStatus.valueOf(status),
        progress = progress,
        errorMessage = errorMessage,
        timestamp = timestamp
    )
}
