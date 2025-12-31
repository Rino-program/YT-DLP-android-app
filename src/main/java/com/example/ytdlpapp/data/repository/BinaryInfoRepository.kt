package com.example.ytdlpapp.data.repository

import android.content.Context
import com.example.ytdlpapp.data.db.AppDatabase
import com.example.ytdlpapp.data.db.entity.BinaryInfoEntity
import com.example.ytdlpapp.domain.model.BinaryInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BinaryInfoRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).binaryInfoDao()

    suspend fun upsert(binaryInfo: BinaryInfo) {
        dao.insert(binaryInfo.toEntity())
    }

    suspend fun update(binaryInfo: BinaryInfo) {
        dao.update(binaryInfo.toEntity())
    }

    suspend fun getByName(name: String): BinaryInfo? {
        return dao.getByName(name)?.toDomain()
    }

    fun getByNameFlow(name: String): Flow<BinaryInfo?> {
        return dao.getByNameFlow(name).map { it?.toDomain() }
    }

    suspend fun getAll(): List<BinaryInfo> {
        return dao.getAll().map { it.toDomain() }
    }

    fun getAllFlow(): Flow<List<BinaryInfo>> {
        return dao.getAllFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun delete(name: String) {
        dao.deleteByName(name)
    }

    private fun BinaryInfo.toEntity() = BinaryInfoEntity(
        name = name,
        version = version,
        installPath = installPath,
        isInstalled = isInstalled,
        lastUpdateCheck = lastUpdateCheck,
        fileSize = fileSize
    )

    private fun BinaryInfoEntity.toDomain() = BinaryInfo(
        name = name,
        version = version,
        installPath = installPath,
        isInstalled = isInstalled,
        lastUpdateCheck = lastUpdateCheck,
        fileSize = fileSize
    )
}
