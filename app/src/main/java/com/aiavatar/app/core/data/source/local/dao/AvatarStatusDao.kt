package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusTable
import com.aiavatar.app.core.data.source.local.model.AvatarStatusWithFilesEntity
import com.aiavatar.app.core.domain.model.ModelStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarStatusDao {

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.AVATAR_STATUS_ID} = :statusId")
    fun getAvatarStatus(statusId: Long): Flow<AvatarStatusWithFilesEntity?>

    @Query("SELECT * FROM ${AvatarStatusTable.name} " +
            "WHERE ${AvatarStatusTable.Columns.MODEL_STATUS} != 'completed'")
    fun getRunningTraining(): Flow<List<AvatarStatusEntity>>?

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.AVATAR_STATUS_ID} = :statusId")
    suspend fun getAvatarStatusSync(statusId: Long): AvatarStatusWithFilesEntity?

    @Query("UPDATE ${AvatarStatusTable.name} SET ${AvatarStatusTable.Columns.MODEL_NAME} = :modelName, " +
            "${AvatarStatusTable.Columns.MODEL_RENAMED} = :renamed " +
            "WHERE ${AvatarStatusTable.Columns.MODEL_ID} = :modelId")
    suspend fun updateModelNameForModelId(modelId: String, modelName: String, renamed: Boolean): Int

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.MODEL_ID} = :modelId")
    fun getAvatarStatusForModelId(modelId: String): Flow<AvatarStatusWithFilesEntity?>

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.MODEL_ID} = :modelId")
    suspend fun getAvatarStatusForModelIdSync(modelId: String): AvatarStatusWithFilesEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAvatarStatus(avatarStatusEntity: AvatarStatusEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(avatarStatusEntity: AvatarStatusEntity)

}