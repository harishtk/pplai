package com.aiavatar.app.feature.home.data.source.local

import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.data.model.ModelListWithModelEntity
import com.aiavatar.app.feature.home.data.source.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeLocalDataSource @Inject constructor(
    private val database: AppDatabase
) {

    fun observeAllModelListItem(): Flow<List<ModelListWithModelEntity>> {
        return database.modelListItemDao().getAllModelList()
    }

    suspend fun insertAllModelListItem(modelListItemEntities: List<ModelListItemEntity>) {
        database.modelListItemDao().insertAll(modelListItemEntities)
    }

    suspend fun deleteAllModelListItem() {
        database.modelListItemDao().deleteAll()
    }

    fun observeModelAvatars(modelId: String): Flow<List<ModelAvatarEntity>> =
        database.modelAvatarDao().getModelAvatars(modelId)

    suspend fun insertAllModelAvatars(modelAvatars: List<ModelAvatarEntity>) {
        database.modelAvatarDao().insertAll(modelAvatars)
    }

    suspend fun deleteAllModelAvatars(modelId: String) {
        database.modelAvatarDao().deleteAll(modelId)
    }

    fun observeAllModels(): Flow<List<ModelEntity>> =
        database.modelDao().getAllModel()

    fun observeModel(modelId: String): Flow<ModelEntity?> =
        database.modelDao().getModel(id = modelId)

    suspend fun insertModel(entity: ModelEntity) {
        insertAllModel(listOf(entity))
    }

    suspend fun insertAllModel(models: List<ModelEntity>) {
        database.modelDao().insertAll(models)
    }

    suspend fun deleteModel(id: String) {
        database.modelDao().delete(id)
    }

    suspend fun deleteAllModels() {
        database.modelDao().deleteAll()
    }

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

    fun observeAllCatalogList(catalogName: String): Flow<List<CatalogListEntity>> =
        database.catalogListDao().getAllCatalogList(catalogName)

    suspend fun insertCatalogList(catalogList: CatalogListEntity) {
        insertAllCatalogList(listOf(catalogList))
    }

    suspend fun insertAllCatalogList(catalogLists: List<CatalogListEntity>) {
        database.catalogListDao().insertAll(catalogLists)
    }

    suspend fun deleteAllCatalogList(catalogName: String) {
        database.catalogListDao().deleteAll(catalogName)
    }

}