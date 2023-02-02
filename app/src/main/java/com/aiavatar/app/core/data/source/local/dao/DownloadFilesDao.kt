package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiavatar.app.core.data.source.local.entity.DownloadFilesEntity
import com.aiavatar.app.core.data.source.local.entity.DownloadFilesTable
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadFilesDao {

    @Query("SELECT * FROM ${DownloadFilesTable.name} WHERE ${DownloadFilesTable.Columns.SESSION_ID} = :sessionId")
    fun getAllDownloadFiles(sessionId: Long): Flow<List<DownloadFilesEntity>>

    @Query("SELECT * FROM ${DownloadFilesTable.name} WHERE ${DownloadFilesTable.Columns.SESSION_ID} = :sessionId")
    suspend fun getAllDownloadFilesSync(sessionId: Long): List<DownloadFilesEntity>

    @Query("SELECT * FROM ${DownloadFilesTable.name} " +
            "WHERE ${DownloadFilesTable.Columns.SESSION_ID} = :sessionId AND " +
            "${DownloadFilesTable.Columns.LOCAL_URI_STRING} = :uriString")
    suspend fun getDeviceFileForUriSync(sessionId: Long, uriString: String): DownloadFilesEntity?

    @Query("UPDATE ${DownloadFilesTable.name} " +
            "SET ${DownloadFilesTable.Columns.STATUS} = :status " +
            "WHERE ${DownloadFilesTable.Columns.ID} = :id")
    suspend fun updateFileStatus(id: Long, status: Int)

    @Query("UPDATE ${DownloadFilesTable.name} SET ${DownloadFilesTable.Columns.DOWNLOADED} = :downloaded, " +
            "${DownloadFilesTable.Columns.DOWNLOADED_AT} = :downloadedAt, " +
            "${DownloadFilesTable.Columns.DOWNLOADED_SIZE} = :downloadSize " +
            "WHERE ${DownloadFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadStatus(id: Long, downloaded: Boolean, downloadedAt: Long, downloadSize: Long): Int

    @Query("UPDATE ${DownloadFilesTable.name} " +
            "SET ${DownloadFilesTable.Columns.PROGRESS} = :progress " +
            "WHERE ${DownloadFilesTable.Columns.ID} = :id")
    suspend fun updateFileDownloadProgress(id: Long, progress: Int)

    @Query("UPDATE ${DownloadFilesTable.name} " +
            "SET ${DownloadFilesTable.Columns.DOWNLOADED_FILENAME} = :downloadedFileUriString " +
            "WHERE ${DownloadFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadedFileName(id: Long, downloadedFileUriString: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadFilesEntity: DownloadFilesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloadFilesEntities: List<DownloadFilesEntity>)

    @Query("DELETE FROM ${DownloadFilesTable.name} WHERE ${DownloadFilesTable.Columns.ID} = :id")
    suspend fun deleteFile(id: Long)

}