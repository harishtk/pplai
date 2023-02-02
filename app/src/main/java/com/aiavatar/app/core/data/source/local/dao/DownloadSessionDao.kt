package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionEntity
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionTable
import com.aiavatar.app.core.data.source.local.model.DownloadSessionWithFilesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadSessionDao {

    @Query("UPDATE ${DownloadSessionTable.name} " +
            "SET ${DownloadSessionTable.Columns.STATUS} = :status " +
            "WHERE ${DownloadSessionTable.Columns.ID} = :id")
    suspend fun updateDownloadSessionStatus(id: Long, status: Int)

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} " +
            "WHERE ${DownloadSessionTable.Columns.STATUS} > 1 " +
            "ORDER BY ${DownloadSessionTable.Columns.CREATED_AT} DESC " +
            "LIMIT 1")
    suspend fun getCurrentDownloadSessionSync(): List<DownloadSessionWithFilesEntity>

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} " +
            "WHERE ${DownloadSessionTable.Columns.STATUS} > 1 " +
            "ORDER BY ${DownloadSessionTable.Columns.CREATED_AT} DESC " +
            "LIMIT 1")
    fun getCurrentDownloadSession(): Flow<List<DownloadSessionWithFilesEntity>>

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} WHERE ${DownloadSessionTable.Columns.ID} = :id")
    fun getDownloadSession(id: Long): Flow<DownloadSessionWithFilesEntity?>

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} WHERE ${DownloadSessionTable.Columns.ID} = :id")
    suspend fun getDownloadSessionSync(id: Long): DownloadSessionWithFilesEntity?

    @Query("UPDATE ${DownloadSessionTable.name} " +
            "SET ${DownloadSessionTable.Columns.WORKER_ID} = :workerId " +
            "WHERE ${DownloadSessionTable.Columns.ID} = :sessionId")
    suspend fun updateDownloadWorkerId(sessionId: Long, workerId: String?)

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} ORDER BY ${DownloadSessionTable.Columns.CREATED_AT} DESC")
    fun getAllDownloadSessions(): Flow<List<DownloadSessionWithFilesEntity>>

    @Transaction
    @Query("SELECT * FROM ${DownloadSessionTable.name} ORDER BY ${DownloadSessionTable.Columns.CREATED_AT} DESC")
    suspend fun getAllDownloadSessionsSync(): List<DownloadSessionWithFilesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadSessionEntity: DownloadSessionEntity): Long?

    @Query("DELETE FROM ${DownloadSessionTable.name}")
    suspend fun deleteAllDownloadSessions()

}