package com.aiavatar.app.core.data.source.local.dao

import android.net.Uri
import androidx.room.*
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesTable

@Dao
interface AvatarFilesDao {

    @Query("SELECT * FROM ${AvatarFilesTable.name} WHERE ${AvatarFilesTable.Columns.AVATAR_STATUS_ID} = :statusId")
    suspend fun getAllAvatarFilesForStatusIdSync(statusId: Long): List<AvatarFilesEntity>

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.PROGRESS} = :progress " +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Int): Int

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.DOWNLOADED} = :downloaded " +
            "${AvatarFilesTable.Columns.DOWNLOADED_AT} = :downloadedAt " +
            "${AvatarFilesTable.Columns.DOWNLOAD_SIZE} = :downloadSize" +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateDownloadStatus(id: Long, downloaded: Int, downloadedAt: Long, downloadSize: Long): Int

    @Query("UPDATE ${AvatarFilesTable.name} SET ${AvatarFilesTable.Columns.LOCAL_URI} = :localUri " +
            "WHERE ${AvatarFilesTable.Columns.ID} = :id")
    suspend fun updateLocalUri(id: Long, localUri: Uri)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(avatarFileEntities: List<AvatarFilesEntity>)

}