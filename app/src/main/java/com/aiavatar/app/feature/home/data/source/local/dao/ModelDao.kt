package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.*
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusTable
import com.aiavatar.app.feature.home.data.source.local.entity.ModelEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelEntityTable
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {

    @Query("SELECT * FROM ${ModelEntityTable.name}")
    fun getAllModel(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM ${ModelEntityTable.name} WHERE ${ModelEntityTable.Columns.ID} = :id")
    fun getModel(id: String): Flow<ModelEntity?>

    @Query("SELECT * FROM ${ModelEntityTable.name} WHERE ${ModelEntityTable.Columns.ID} = :id")
    suspend fun getModelSync(id: String): ModelEntity?

    @Query("UPDATE ${ModelEntityTable.name} SET ${ModelEntityTable.Columns.NAME} = :modelName, " +
            "${ModelEntityTable.Columns.RENAMED} = :renamed " +
            "WHERE ${ModelEntityTable.Columns.ID} = :modelId")
    suspend fun updateModelNameForModelId(modelId: String, modelName: String, renamed: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modelEntities: List<ModelEntity>)

    @Update
    suspend fun updateModel(modelEntity: ModelEntity): Int

    @Query("DELETE FROM ${ModelEntityTable.name} WHERE ${ModelEntityTable.Columns.ID} = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ${ModelEntityTable.name}")
    suspend fun deleteAll()
}