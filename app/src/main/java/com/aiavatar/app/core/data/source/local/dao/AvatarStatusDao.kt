package com.aiavatar.app.core.data.source.local.dao

import android.app.AppOpsManager.OnOpNotedCallback
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusTable
import com.aiavatar.app.core.data.source.local.model.AvatarStatusWithFilesEntity
import com.aiavatar.app.core.domain.model.AvatarStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarStatusDao {

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.ID} = :id")
    fun getAvatarStatus(id: Long): Flow<AvatarStatusWithFilesEntity?>

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.ID} = :id")
    suspend fun getAvatarStatusSync(id: Long): AvatarStatusWithFilesEntity?

    @Query("UPDATE ${AvatarStatusTable.name} SET ${AvatarStatusTable.Columns.MODEL_NAME} = :modelName " +
            "WHERE ${AvatarStatusTable.Columns.MODEL_ID} = :modelId")
    suspend fun updateModelNameForModelId(modelId: String, modelName: String)

    @Transaction
    @Query("SELECT * FROM ${AvatarStatusTable.name} WHERE ${AvatarStatusTable.Columns.MODEL_ID} = :modelId")
    suspend fun getAvatarStatusForModelIdSync(modelId: String): AvatarStatusWithFilesEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAvatarStatus(avatarStatusEntity: AvatarStatusEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(avatarStatusEntity: AvatarStatusEntity)

}