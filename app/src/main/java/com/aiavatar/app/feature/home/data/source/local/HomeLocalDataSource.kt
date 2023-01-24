package com.aiavatar.app.feature.home.data.source.local

import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.data.source.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeLocalDataSource @Inject constructor(
    private val database: AppDatabase
) {

    fun observeAllCategories(): Flow<List<CategoryEntity>> =
        database.categoryDao().getAllCategories()

    suspend fun insertCategory(categoryEntity: CategoryEntity) {
        insertAllCategories(listOf(categoryEntity))
    }

    suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        database.categoryDao().insertAll(categories)
    }

    suspend fun deleteAllCategories() {
        database.categoryDao().deleteAll()
    }

}