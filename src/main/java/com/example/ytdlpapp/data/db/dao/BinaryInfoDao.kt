package com.example.ytdlpapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ytdlpapp.data.db.entity.BinaryInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BinaryInfoDao {
    @Insert
    suspend fun insert(binaryInfo: BinaryInfoEntity)

    @Update
    suspend fun update(binaryInfo: BinaryInfoEntity)

    @Query("SELECT * FROM binary_info WHERE name = :name")
    suspend fun getByName(name: String): BinaryInfoEntity?

    @Query("SELECT * FROM binary_info WHERE name = :name")
    fun getByNameFlow(name: String): Flow<BinaryInfoEntity?>

    @Query("SELECT * FROM binary_info")
    suspend fun getAll(): List<BinaryInfoEntity>

    @Query("SELECT * FROM binary_info")
    fun getAllFlow(): Flow<List<BinaryInfoEntity>>

    @Query("DELETE FROM binary_info WHERE name = :name")
    suspend fun deleteByName(name: String)
}
