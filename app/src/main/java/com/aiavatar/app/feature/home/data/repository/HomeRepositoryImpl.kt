package com.aiavatar.app.feature.home.data.repository

import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.core.data.source.local.CacheLocalDataSource
import com.aiavatar.app.core.data.source.local.entity.modify
import com.aiavatar.app.core.domain.util.BuenoCacheException
import com.aiavatar.app.feature.home.data.model.ModelListWithModelEntity
import com.aiavatar.app.feature.home.data.model.toModelListWithModel
import com.aiavatar.app.feature.home.data.source.local.HomeLocalDataSource
import com.aiavatar.app.feature.home.data.source.local.entity.*
import com.aiavatar.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPlanDto
import com.aiavatar.app.feature.home.data.source.remote.dto.asDto
import com.aiavatar.app.feature.home.data.source.remote.dto.toSubscriptionPlan
import com.aiavatar.app.feature.home.data.source.remote.model.GetAvatarsResponse
import com.aiavatar.app.feature.home.data.source.remote.model.dto.*
import com.aiavatar.app.feature.home.data.source.remote.model.toCatalogList
import com.aiavatar.app.feature.home.data.source.remote.model.toCategory
import com.aiavatar.app.feature.home.data.source.remote.model.toListAvatar
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

val DEFAULT_CACHE_TIME_TO_LIVE: Long = TimeUnit.DAYS.toMillis(1)

class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val localDataSource: HomeLocalDataSource,
    private val cacheLocalDataSource: CacheLocalDataSource,
) : HomeRepository, NetworkResultParser {

    private fun observeModelAvatarsInternal(modelId: String): Flow<List<ModelAvatar>> {
        return localDataSource.observeModelAvatars(modelId)
            .map { list ->
                list.map(ModelAvatarEntity::toModelAvatar)
            }
    }

    private fun observeAllCategoryInternal(): Flow<List<Category>> {
        return localDataSource.observeAllCategories()
            .map { list ->
                list.map(CategoryEntity::toCategory)
            }
    }

    private fun observeAllCatalogListInternal(catalogName: String): Flow<List<CatalogList>> {
        return localDataSource.observeAllCatalogList(catalogName)
            .map { list ->
                list.map(CatalogListEntity::toCatalogList)
            }
    }

    private fun observeAllModelListItem(): Flow<List<ModelListWithModel>> {
        return localDataSource.observeAllModelListItem()
            .map { list ->
                list.map(ModelListWithModelEntity::toModelListWithModel)
            }
    }

    private fun observeAllModel(): Flow<List<ModelData>> {
        return localDataSource.observeAllModels()
            .map { list ->
                list.map(ModelEntity::toModelData)
            }
    }

    private fun observeModel(modelId: String): Flow<ModelData?> {
        return localDataSource.observeModel(modelId)
            .map { it?.toModelData() }
    }

    private suspend fun refreshCategoriesInternal(): Result<List<Category>> {
        val networkCatalogResult = remoteDataSource.getCatalogSync()
        return when (networkCatalogResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkCatalogResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val categoryDtoList =
                        networkCatalogResult.data.data?.avatars?.map { it.toCategory() }
                    if (categoryDtoList != null) {
                        val categories = categoryDtoList.map(Category::asEntity)
                        localDataSource.deleteAllCategories()
                        localDataSource.insertAllCategories(categories)

                        val cacheKeys = cacheLocalDataSource.getOrCreate(AvatarCategoriesTable.name)

                        val affected = cacheLocalDataSource.updateCacheKey(
                            cacheKeys.modify(
                                createdAt = System.currentTimeMillis(),
                                expiresAt = (System.currentTimeMillis() + DEFAULT_CACHE_TIME_TO_LIVE)
                            ).also {
                                it._id = cacheKeys._id
                            }
                        )
                        Timber.d("Cache keys: categories updated $affected cacheKeys = $cacheKeys")
                        Result.Success(categories.map(CategoryEntity::toCategory))
                    } else {
                        emptyResponse(networkCatalogResult)
                    }
                } else {
                    badResponse(networkCatalogResult)
                }
            }
            else -> parseErrorNetworkResult(networkCatalogResult)
        }
    }

    private suspend fun refreshCatalogListInternal(catalogDetailRequest: CatalogDetailRequest): Result<List<CatalogList>> {
        val networkResult = remoteDataSource.getCatalogDetailSync(catalogDetailRequest.asDto())
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val catalogList = networkResult.data.data?.avatars?.map { it.toCatalogList(catalogDetailRequest.category) }
                    if (catalogList != null) {
                        localDataSource.deleteAllCatalogList(catalogDetailRequest.category)
                        localDataSource.insertAllCatalogList(catalogList.map(CatalogList::asEntity))

                        val cacheKeys = cacheLocalDataSource.getOrCreate(CatalogListTable.name)

                        val affected = cacheLocalDataSource.updateCacheKey(
                            cacheKeys.modify(
                                createdAt = System.currentTimeMillis(),
                                expiresAt = (System.currentTimeMillis() + DEFAULT_CACHE_TIME_TO_LIVE)
                            ).also {
                                it._id = cacheKeys._id
                            }
                        )
                        Timber.d("Cache keys: categories updated $affected cacheKeys = $cacheKeys")

                        Result.Success(catalogList)
                    } else {
                        emptyResponse(networkResult)
                    }
                } else {
                    badResponse(networkResult)
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    private suspend fun refreshAllModelInternal(): Result<List<ModelListItem>> {
        val networkResult = remoteDataSource.getMyModelsSync()
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val modelItemListDto = networkResult.data.data?.models

                    if (modelItemListDto != null) {
                        val modelItemList = modelItemListDto.map(ModelListDto::toModelListItem)
                        localDataSource.deleteAllModelListItem()
                        localDataSource.insertAllModelListItem(
                            modelItemList.map(ModelListItem::toEntity)
                        )

                        val modelEntities = modelItemListDto.mapNotNull { it.modelDataDto?.toModelData(it.statusId) }
                            .map(ModelData::toEntity)
                        Timber.d("Models: $modelEntities")
                        localDataSource.insertAllModel(modelEntities)

                        Result.Success(modelItemList)
                    } else {
                        emptyResponse(networkResult)
                    }
                } else {
                    badResponse(networkResult)
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    private suspend fun refreshModelInternal(modelId: String): Result<ModelData> {
        return when (val networkResult = remoteDataSource.getModelSync(modelId)) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val data = networkResult.data.data?.models?.firstOrNull()?.let {
                        it.modelDataDto?.toModelData(it.statusId)
                    }
                    if (data != null) {
                        localDataSource.insertModel(data.toEntity())
                        Result.Success(data)
                    } else {
                        emptyResponse(networkResult)
                    }
                } else {
                    badResponse(networkResult)
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    override fun getMyModels2(forceRefresh: Boolean): Flow<Result<List<ModelData>>> {
        return observeAllModel()
            .onStart {
                /*if (forceRefresh) {
                    localDataSource.deleteAllModels()
                }*/
            }
            .onEach { list ->
                if (list.isEmpty()) {
                    val remoteResult = refreshAllModelInternal()
                    if (remoteResult is Result.Error) {
                        throw remoteResult.exception
                    }
                }
            }
            .map { list ->
                if (list.isEmpty()) {
                    Result.Loading
                } else {
                    Result.Success(list)
                }
            }
            .catch { t ->
                emit(Result.Error(t as Exception))
            }
    }

    override fun getModel(modelId: String): Flow<Result<ModelData>> {
        return remoteDataSource.getModel(modelId).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data?.models?.firstOrNull()?.let {
                            it.modelDataDto?.toModelData(it.statusId)
                        }
                        if (data != null) {
                            Result.Success(data)
                        } else {
                            emptyResponse(networkResult)
                        }
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getModel2(modelId: String): Flow<Result<ModelData>> = flow {
        val cache = observeModel(modelId).first()
        if (cache != null) {
            emit(Result.Success(cache))
        } else {
            emit(Result.Loading)
        }

        if (cache == null) {
            emit(Result.Loading)
            when (val result = refreshModelInternal(modelId)) {
                is Result.Success -> {
                    localDataSource.apply {
                        insertModel(result.data.toEntity())
                    }
                }
                is Result.Error -> {
                    emit(result)
                }
                is Result.Loading -> {
                    /* Noop */
                }
            }
        }

        emitAll(
            observeModel(modelId)
                .map { Result.Success(it!!) }
        )
        /*observeModel(modelId)
            .onEach { modelData ->
                if (modelData == null) {
                    // Model data not available in cache, get from remote
                    val remoteResult = refreshModelInternal(modelId)
                    if (remoteResult is Result.Error) {
                        throw remoteResult.exception
                    }
                }
            }
            .map { modelData ->
                if (modelData == null) {
                    Result.Loading
                } else {
                    Result.Success(modelData)
                }
            }
            .catch { t ->
                emit(Result.Error(t as Exception))
            }*/
    }

    // TODO: [forceRefresh] will not work as expected
    override fun getCatalog2(forceRefresh: Boolean): Flow<Result<List<Category>>> {
        return observeAllCategoryInternal()
            .onStart {
                if (forceRefresh) {
                    val cacheKeys = cacheLocalDataSource.getOrCreate(AvatarCategoriesTable.name)
                    if (cacheKeys.expired()) {
                        Timber.d("Cache keys: ${cacheKeys.key} expired")
                        localDataSource.deleteAllCategories()
                    } else {
                        val delta = (System.currentTimeMillis() - cacheKeys.createdAt).coerceAtLeast(0)
                        val lastUpdate = TimeUnit.MILLISECONDS.toMinutes(delta)
                        val message = "Can't refresh. Last updated $lastUpdate minutes ago"
                        val t = BuenoCacheException(lastUpdate, message)
                        // Timber.d(t, "Cache keys: $message")
                        throw t
                    }
                }
            }
            .onEach { list ->
                if (list.isEmpty()) {
                    val remoteResult = refreshCategoriesInternal()
                    if (remoteResult is Result.Error) {
                        throw remoteResult.exception
                    }
                }
            }
            .map { list ->
                if (list.isEmpty()) {
                    Result.Loading
                } else {
                    Result.Success(list)
                }
            }
            .catch { t ->
                emit(Result.Error(t as Exception))
            }
    }

    // TODO: fix [forceRefresh] will not work as expected
    // TODO: fix [BuenoCacheException] is thrown for empty data!
    override fun getCatalogList2(
        request: CatalogDetailRequest,
        forceRefresh: Boolean
    ): Flow<Result<CatalogDetailData>> {
        return observeAllCatalogListInternal(request.category)
            .onStart {
                if (forceRefresh) {
                    val cacheKeys = cacheLocalDataSource.getOrCreate(CatalogListTable.name)
                    if (cacheKeys.expired()) {
                        Timber.d("Cache keys: ${cacheKeys.key} expired")
                        localDataSource.deleteAllCatalogList(request.category)
                    } else {
                        val delta = (System.currentTimeMillis() - cacheKeys.createdAt).coerceAtLeast(0)
                        val lastUpdate = TimeUnit.MILLISECONDS.toMinutes(delta)
                        val message = "Can't refresh. Last updated $lastUpdate minutes ago"
                        val t = BuenoCacheException(lastUpdate, message)
                        throw t
                    }
                }
            }
            .onEach { list ->
                if (list.isEmpty()) {
                    val remoteResult = refreshCatalogListInternal(request)
                    if (remoteResult is Result.Error) {
                        throw remoteResult.exception
                    }
                } else {
                    Result.Success(list)
                }
            }
            .map { list ->
                if (list.isEmpty()) {
                    Result.Loading
                } else {
                    val catalogDetailData = CatalogDetailData(
                        category = request.category,
                        avatars = list
                    )
                    Result.Success(catalogDetailData)
                }
            }
            .catch { t ->
                emit(Result.Error(t as Exception))
            }
    }

    override fun getCatalog(): Flow<Result<List<Category>>> {
        return remoteDataSource.getCatalog().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data
                        if (data != null) {
                            val categories: List<Category> =
                                data.data?.avatars?.map { it.toCategory() }
                                    ?: listOf<Category>()
                            Result.Success(categories)
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause =
                            BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getCatalogList(request: CatalogDetailRequest): Flow<Result<CatalogDetailData>> {
        return remoteDataSource.getCatalogDetail(request.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data
                        if (data != null) {
                            Result.Success(data.toCatalogDetailData())
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause = BadResponseException("Unexpected response code")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>> {
        return remoteDataSource.getSubscriptionPlans().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data
                        if (data != null) {
                            val plans =
                                data.data?.plans?.map(SubscriptionPlanDto::toSubscriptionPlan)
                                    ?: emptyList<SubscriptionPlan>()
                            Result.Success(plans)
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause =
                            BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<PurchasePlanData>> {
        return remoteDataSource.purchasePlan(subscriptionPurchaseRequest.asDto())
            .map { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> Result.Loading
                    is NetworkResult.Success -> {
                        if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                            val data = networkResult.data.purchasePlanDataDto?.toPurchasePlanData()
                            if (data != null) {
                                Result.Success(data)
                            } else {
                                val cause = EmptyResponseException("No status id")
                                Result.Error(ApiException(cause))
                            }
                        } else {
                            val cause =
                                BadResponseException("Unexpected response code: ${networkResult.code}")
                            Result.Error(ApiException(cause))
                        }
                    }
                    else -> parseErrorNetworkResult(networkResult)
                }
            }
    }

    override fun generateAvatar(generateAvatarRequest: GenerateAvatarRequest): Flow<Result<Long>> {
        return remoteDataSource.generateAvatar(generateAvatarRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val avatarStatusId = networkResult.data.data?.id
                        if (avatarStatusId != null) {
                            Result.Success(avatarStatusId)
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause =
                            BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getMyModels(forceRefresh: Boolean): Flow<Result<List<ModelListWithModel>>> = flow {
        val cache = observeAllModelListItem().first()
        if (cache.isNotEmpty()) {
            emit(Result.Success(cache))
        } else {
            emit(Result.Loading)
        }
        // emit(Result.Loading)

        if (forceRefresh) {
            if (cache.isEmpty()) {
                emit(Result.Loading)
            }
            when (val result = refreshAllModelInternal()) {
                is Result.Error -> {
                    emit(result)
                }
                else -> {
                    /* Noop */
                }
            }
        }

        emitAll(
            observeAllModelListItem().map { Result.Success(it) }
        )
    }

    override fun getAvatars(getAvatarsRequest: GetAvatarsRequest): Flow<Result<List<ListAvatar>>> {
        return remoteDataSource.getAvatars(getAvatarsRequest.asDto()).map(this::parseGetAvatarsResponse)
    }

    override fun getAvatars2(getAvatarsRequest: GetAvatarsRequest, forceRefresh: Boolean): Flow<Result<List<ModelAvatar>>> = flow<Result<List<ModelAvatar>>> {
        val cache = observeModelAvatarsInternal(getAvatarsRequest.modelId).first()
        if (cache.isNotEmpty()) {
            emit(Result.Success(cache))
        } else {
            emit(Result.Loading)
        }

        if (forceRefresh) {
            if (cache.isEmpty()) {
                emit(Result.Loading)
            }
            val result = parseGetAvatarsResponse(
                remoteDataSource.getAvatarsSync(getAvatarsRequest.asDto())
            )
            when (result) {
                is Result.Success -> {
                    val modelAvatarList = result.data.map { listAvatar ->
                        ModelAvatar(
                            modelId = getAvatarsRequest.modelId,
                            remoteFile = listAvatar.imageName,
                            downloaded = 0,
                            localUri = "",
                            progress = 0
                        ).also {
                            it._id = listAvatar.id
                        }
                    }
                    localDataSource.apply {
                        deleteAllModelAvatars(getAvatarsRequest.modelId)
                        insertAllModelAvatars(modelAvatarList.map(ModelAvatar::toEntity))
                    }
                }
                is Result.Error -> {
                    emit(result)
                }
                is Result.Loading -> {
                    /* Noop */
                }
            }
        }

        emitAll(
            observeModelAvatarsInternal(getAvatarsRequest.modelId)
                .map { Result.Success(it) }
        )
    }

    override suspend fun getAvatars2Sync(getAvatarsRequest: GetAvatarsRequest): Result<List<ModelAvatar>> {
        return Result.Loading
    }

    private fun parseGetAvatarsResponse(networkResult: NetworkResult<GetAvatarsResponse>): Result<List<ListAvatar>> {
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val avatarList = networkResult.data.data?.avatars?.map { it.toListAvatar() }
                    if (avatarList != null) {
                        Result.Success(avatarList)
                    } else {
                        val cause = EmptyResponseException("No data")
                        Result.Error(ApiException(cause))
                    }
                } else {
                    val cause =
                        BadResponseException("Unexpected response code ${networkResult.code}")
                    Result.Error(ApiException(cause))
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }
}

