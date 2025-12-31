package com.example.ytdlpapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ytdlpapp.data.db.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun get(key: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE key = :key")
    fun getFlow(key: String): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingsEntity>

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun delete(key: String)

    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getValue(key: String): String?
}
