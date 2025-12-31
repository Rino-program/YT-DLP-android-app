package com.example.ytdlpapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ytdlpapp.data.db.dao.AppSettingsDao
import com.example.ytdlpapp.data.db.dao.BinaryInfoDao
import com.example.ytdlpapp.data.db.dao.DownloadHistoryDao
import com.example.ytdlpapp.data.db.dao.DownloadQueueDao
import com.example.ytdlpapp.data.db.dao.DownloadStatisticsDao
import com.example.ytdlpapp.data.db.dao.ProxySettingsDao
import com.example.ytdlpapp.data.db.entity.AppSettingsEntity
import com.example.ytdlpapp.data.db.entity.BinaryInfoEntity
import com.example.ytdlpapp.data.db.entity.DownloadHistoryEntity
import com.example.ytdlpapp.data.db.entity.DownloadQueueEntity
import com.example.ytdlpapp.data.db.entity.DownloadStatisticsEntity
import com.example.ytdlpapp.data.db.entity.ProxySettingsEntity

@Database(
    entities = [
        DownloadHistoryEntity::class,
        BinaryInfoEntity::class,
        AppSettingsEntity::class,
        DownloadQueueEntity::class,
        ProxySettingsEntity::class,
        DownloadStatisticsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao
    abstract fun binaryInfoDao(): BinaryInfoDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun downloadQueueDao(): DownloadQueueDao
    abstract fun proxySettingsDao(): ProxySettingsDao
    abstract fun downloadStatisticsDao(): DownloadStatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ytdlp_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
