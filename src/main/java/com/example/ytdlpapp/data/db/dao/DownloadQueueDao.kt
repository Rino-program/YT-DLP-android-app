package com.example.ytdlpapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ytdlpapp.data.db.entity.DownloadQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadQueueDao {
    @Insert
    suspend fun insert(item: DownloadQueueEntity): Long

    @Update
    suspend fun update(item: DownloadQueueEntity)

    @Delete
    suspend fun delete(item: DownloadQueueEntity)

    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getById(id: Int): DownloadQueueEntity?

    @Query("SELECT * FROM download_queue ORDER BY priority DESC, addedAt ASC")
    fun getAllFlow(): Flow<List<DownloadQueueEntity>>

    @Query("SELECT * FROM download_queue WHERE status = 'QUEUED' ORDER BY priority DESC, addedAt ASC LIMIT 1")
    suspend fun getNextInQueue(): DownloadQueueEntity?

    @Query("SELECT * FROM download_queue WHERE status = :status")
    suspend fun getByStatus(status: String): List<DownloadQueueEntity>

    @Query("SELECT * FROM download_queue WHERE status = :status")
    fun getByStatusFlow(status: String): Flow<List<DownloadQueueEntity>>

    @Query("UPDATE download_queue SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Int, status: String, progress: Int)

    @Query("UPDATE download_queue SET status = 'CANCELLED' WHERE status = 'QUEUED'")
    suspend fun cancelAllQueued()

    @Query("DELETE FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM download_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'QUEUED'")
    fun getQueueCountFlow(): Flow<Int>
}
