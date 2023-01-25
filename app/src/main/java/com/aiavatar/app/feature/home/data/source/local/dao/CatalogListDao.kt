package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.*
import com.aiavatar.app.feature.home.data.source.local.entity.CatalogListEntity
import com.aiavatar.app.feature.home.data.source.local.entity.CatalogListTable
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogListDao {

    @Query("SELECT * FROM ${CatalogListTable.name} WHERE ${CatalogListTable.Columns.CATALOG_NAME} = :catalogName")
    fun getAllCatalogList(catalogName: String): Flow<List<CatalogListEntity>>

    @Query("SELECT * FROM ${CatalogListTable.name} WHERE ${CatalogListTable.Columns.CATALOG_NAME} = :catalogName")
    suspend fun getAllCatalogListSync(catalogName: String): List<CatalogListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CatalogListEntity): Long

    @Delete
    suspend fun deleteAll()
}