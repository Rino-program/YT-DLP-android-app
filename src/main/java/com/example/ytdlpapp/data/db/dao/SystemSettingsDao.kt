package com.example.ytdlpapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ytdlpapp.data.db.entity.DownloadStatisticsEntity
import com.example.ytdlpapp.data.db.entity.ProxySettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(proxySettings: ProxySettingsEntity)

    @Update
    suspend fun update(proxySettings: ProxySettingsEntity)

    @Query("SELECT * FROM proxy_settings WHERE id = 'default'")
    suspend fun getProxySettings(): ProxySettingsEntity?

    @Query("SELECT * FROM proxy_settings WHERE id = 'default'")
    fun getProxySettingsFlow(): Flow<ProxySettingsEntity?>

    @Query("DELETE FROM proxy_settings")
    suspend fun deleteAll()
}

@Dao
interface DownloadStatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: DownloadStatisticsEntity)

    @Update
    suspend fun update(statistics: DownloadStatisticsEntity)

    @Query("SELECT * FROM download_statistics WHERE id = 'global'")
    suspend fun getStatistics(): DownloadStatisticsEntity?

    @Query("SELECT * FROM download_statistics WHERE id = 'global'")
    fun getStatisticsFlow(): Flow<DownloadStatisticsEntity?>

    @Query("UPDATE download_statistics SET totalDownloads = totalDownloads + 1 WHERE id = 'global'")
    suspend fun incrementTotal()

    @Query("UPDATE download_statistics SET successfulDownloads = successfulDownloads + 1, totalBytesDownloaded = totalBytesDownloaded + :bytes WHERE id = 'global'")
    suspend fun recordSuccess(bytes: Long)

    @Query("UPDATE download_statistics SET failedDownloads = failedDownloads + 1 WHERE id = 'global'")
    suspend fun recordFailure()

    @Query("DELETE FROM download_statistics")
    suspend fun reset()
}
