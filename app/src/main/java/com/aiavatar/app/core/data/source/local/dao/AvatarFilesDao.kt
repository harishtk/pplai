package com.aiavatar.app.core.data.source.local.dao

import android.net.Uri
import androidx.room.*
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesTable

@Dao
interface AvatarFilesDao {

    @Query("SELECT * FROM ${AvatarFilesTable.name} WHERE ${AvatarFilesTable.Columns.MODEL_ID} = :modelId")
    suspend fun getAllAvatarFilesForModelIdSync(modelId: Long): List<AvatarFilesEntity>

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.PROGRESS} = :progress " +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Int): Int

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.DOWNLOADED} = :downloaded, " +
            "${AvatarFilesTable.Columns.DOWNLOADED_AT} = :downloadedAt, " +
            "${AvatarFilesTable.Columns.DOWNLOADED_SIZE} = :downloadSize " +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadStatus(id: Long, downloaded: Boolean, downloadedAt: Long, downloadSize: Long): Int

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.LOCAL_URI} = :localUriString " +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateLocalUri(id: Long, localUriString: String)

    @Query("SELECT ${AvatarFilesTable.Columns.ID} FROM ${AvatarFilesTable.name} " +
            "WHERE ${AvatarFilesTable.Columns.MODEL_ID} = :modelId AND " +
            "${AvatarFilesTable.Columns.REMOTE_FILE} = :remoteUriString " +
            "LIMIT 1")
    suspend fun checkIfRemoteUrlExists(modelId: String, remoteUriString: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(avatarFileEntities: List<AvatarFilesEntity>)

}