package com.example.ytdlpapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ytdlpapp.data.db.entity.DownloadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Insert
    suspend fun insert(history: DownloadHistoryEntity): Long

    @Update
    suspend fun update(history: DownloadHistoryEntity)

    @Delete
    suspend fun delete(history: DownloadHistoryEntity)

    @Query("SELECT * FROM download_history WHERE id = :id")
    suspend fun getById(id: Int): DownloadHistoryEntity?

    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<DownloadHistoryEntity>

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM download_history")
    suspend fun deleteAll()
}
