package com.aiavatar.app.feature.home.data.source.local.dao

import androidx.room.*
import com.aiavatar.app.feature.home.data.source.local.entity.AvatarCategoriesTable
import com.aiavatar.app.feature.home.data.source.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM ${AvatarCategoriesTable.name}")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(newCategoryEntities: List<CategoryEntity>)

    @Query("DELETE FROM ${AvatarCategoriesTable.name}")
    suspend fun deleteAll()

}