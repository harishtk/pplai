package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesTable
import com.aiavatar.app.feature.home.data.source.local.entity.ModelAvatarEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelAvatarTable
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelAvatarDao {

    @Query("SELECT * FROM ${ModelAvatarTable.name} " +
            "WHERE ${ModelAvatarTable.Columns.MODEL_ID} = :modelId")
    fun getModelAvatars(modelId: String): Flow<List<ModelAvatarEntity>>

    @Query("UPDATE ${ModelAvatarTable.name} SET ${ModelAvatarTable.Columns.PROGRESS} = :progress " +
            "WHERE ${ModelAvatarTable.Columns.ID} = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Int): Int

    @Query("UPDATE ${ModelAvatarTable.name} SET ${ModelAvatarTable.Columns.DOWNLOADED} = :downloaded, " +
            "${ModelAvatarTable.Columns.DOWNLOADED_AT} = :downloadedAt, " +
            "${ModelAvatarTable.Columns.DOWNLOADED_SIZE} = :downloadSize " +
            "WHERE ${ModelAvatarTable.Columns.ID} = :id")
    suspend fun updateDownloadStatus(id: Long, downloaded: Boolean, downloadedAt: Long, downloadSize: Long): Int

    @Query("UPDATE ${ModelAvatarTable.name} SET ${ModelAvatarTable.Columns.LOCAL_URI} = :localUriString " +
            "WHERE ${ModelAvatarTable.Columns.ID} = :id")
    suspend fun updateLocalUri(id: Long, localUriString: String)

    @Query("SELECT * FROM ${ModelAvatarTable.name} " +
            "WHERE ${ModelAvatarTable.Columns.MODEL_ID} = :modelId")
    suspend fun getModelAvatarsSync(modelId: String): List<ModelAvatarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modelAvatarEntities: List<ModelAvatarEntity>)

    @Query("DELETE FROM ${ModelAvatarTable.name} " +
            "WHERE ${ModelAvatarTable.Columns.MODEL_ID} = :modelId")
    suspend fun deleteAll(modelId: String)
}