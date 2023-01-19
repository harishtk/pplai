package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiavatar.app.core.data.source.local.entity.UploadSessionEntity
import com.aiavatar.app.core.data.source.local.entity.UploadSessionTable
import com.aiavatar.app.core.data.source.local.model.UploadSessionWithFilesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadSessionDao {

    @Query("UPDATE ${UploadSessionTable.name} " +
            "SET ${UploadSessionTable.Columns.STATUS} = :status " +
            "WHERE ${UploadSessionTable.Columns.ID} = :id")
    suspend fun updateUploadSessionStatus(id: Long, status: Int)

    @Query("UPDATE ${UploadSessionTable.name} " +
            "SET ${UploadSessionTable.Columns.TRAINING_TYPE} = :type " +
            "WHERE ${UploadSessionTable.Columns.ID} = :id")
    suspend fun updateUploadSessionTrainingType(id: Long, type: String)

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} " +
            "WHERE ${UploadSessionTable.Columns.STATUS} > 1 " +
            "ORDER BY ${UploadSessionTable.Columns.CREATED_AT} DESC " +
            "LIMIT 1")
    suspend fun getCurrentUploadSessionSync(): List<UploadSessionWithFilesEntity>

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} " +
            "WHERE ${UploadSessionTable.Columns.STATUS} > 1 " +
            "ORDER BY ${UploadSessionTable.Columns.CREATED_AT} DESC " +
            "LIMIT 1")
    fun getCurrentUploadSession(): Flow<List<UploadSessionWithFilesEntity>>

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} WHERE ${UploadSessionTable.Columns.ID} = :id")
    fun getUploadSession(id: Long): Flow<UploadSessionWithFilesEntity?>

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} WHERE ${UploadSessionTable.Columns.ID} = :id")
    suspend fun getUploadSessionSync(id: Long): UploadSessionWithFilesEntity?

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} ORDER BY ${UploadSessionTable.Columns.CREATED_AT} DESC")
    fun getAllUploadSessions(): Flow<List<UploadSessionWithFilesEntity>>

    @Transaction
    @Query("SELECT * FROM ${UploadSessionTable.name} ORDER BY ${UploadSessionTable.Columns.CREATED_AT} DESC")
    suspend fun getAllUploadSessionsSync(): List<UploadSessionWithFilesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uploadSessionEntity: UploadSessionEntity): Long

    @Query("DELETE FROM ${UploadSessionTable.name}")
    suspend fun deleteAllUploadSessions()

}