package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.*
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modelEntities: List<ModelEntity>)

    @Update
    suspend fun updateModel(modelEntity: ModelEntity): Int

    @Query("DELETE FROM ${ModelEntityTable.name} WHERE ${ModelEntityTable.Columns.ID} = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ${ModelEntityTable.name}")
    suspend fun deleteAll()
}