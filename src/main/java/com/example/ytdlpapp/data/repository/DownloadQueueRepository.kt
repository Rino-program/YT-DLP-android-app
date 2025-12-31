package com.example.ytdlpapp.data.repository

import android.content.Context
import com.example.ytdlpapp.data.db.AppDatabase
import com.example.ytdlpapp.data.db.entity.DownloadQueueEntity
import com.example.ytdlpapp.domain.model.QueuedDownload
import com.example.ytdlpapp.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadQueueRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).downloadQueueDao()

    suspend fun addToQueue(download: QueuedDownload): Long {
        return dao.insert(download.toEntity())
    }

    suspend fun updateQueue(download: QueuedDownload) {
        dao.update(download.toEntity())
    }

    suspend fun updateProgress(id: Int, progress: Int, status: QueueStatus = QueueStatus.DOWNLOADING) {
        dao.updateProgress(id, status.name, progress)
    }

    suspend fun getNextInQueue(): QueuedDownload? {
        return dao.getNextInQueue()?.toDomain()
    }

    fun getAllQueued(): Flow<List<QueuedDownload>> {
        return dao.getAllFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getByStatus(status: QueueStatus): Flow<List<QueuedDownload>> {
        return dao.getByStatusFlow(status.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getQueueCount(): Int {
        return dao.getByStatus("QUEUED").size
    }

    fun getQueueCountFlow(): Flow<Int> {
        return dao.getQueueCountFlow()
    }

    suspend fun cancelAllQueued() {
        dao.cancelAllQueued()
    }

    suspend fun deleteCompleted() {
        dao.deleteCompleted()
    }

    suspend fun delete(id: Int) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun QueuedDownload.toEntity() = DownloadQueueEntity(
        id = id,
        url = url,
        title = title,
        format = format,
        outputPath = outputPath,
        ytdlpOptions = ytdlpOptions,
        ffmpegOptions = ffmpegOptions,
        status = status.name,
        priority = priority,
        progress = progress,
        errorMessage = errorMessage,
        addedAt = addedAt,
        isPlaylist = isPlaylist
    )

    private fun DownloadQueueEntity.toDomain() = QueuedDownload(
        id = id,
        url = url,
        title = title,
        format = format,
        outputPath = outputPath,
        ytdlpOptions = ytdlpOptions,
        ffmpegOptions = ffmpegOptions,
        status = QueueStatus.valueOf(status),
        priority = priority,
        progress = progress,
        errorMessage = errorMessage,
        addedAt = addedAt,
        isPlaylist = isPlaylist
    )
}
