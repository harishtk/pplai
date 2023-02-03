package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.*
import com.aiavatar.app.feature.home.data.model.ModelListWithModelEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelListItemEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelListItemTable
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelListItemDao {

    @Transaction
    @Query("SELECT * FROM ${ModelListItemTable.name}")
    fun getAllModelList(): Flow<List<ModelListWithModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modelListItemEntities: List<ModelListItemEntity>)

    @Query("DELETE FROM ${ModelListItemTable.name}")
    suspend fun deleteAll()
}