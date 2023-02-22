package com.aiavatar.app.feature.home.data.repository

import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.core.data.source.local.CacheLocalDataSource
import com.aiavatar.app.core.data.source.local.entity.CacheKeyProvider
import com.aiavatar.app.core.data.source.local.entity.modify
import com.aiavatar.app.core.di.ApplicationCoroutineScope
import com.aiavatar.app.core.domain.util.BuenoCacheException
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.home.data.model.ModelListWithModelEntity
import com.aiavatar.app.feature.home.data.model.toModelListWithModel
import com.aiavatar.app.feature.home.data.source.local.HomeLocalDataSource
import com.aiavatar.app.feature.home.data.source.local.entity.*
import com.aiavatar.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPlanDto
import com.aiavatar.app.feature.home.data.source.remote.dto.asDto
import com.aiavatar.app.feature.home.data.source.remote.dto.toSubscriptionPlan
import com.aiavatar.app.feature.home.data.source.remote.model.*
import com.aiavatar.app.feature.home.data.source.remote.model.dto.*
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.*
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

val DEFAULT_CACHE_TIME_TO_LIVE: Long = TimeUnit.DAYS.toMillis(1)
val SHORT_CACHE_TIME_TO_LIVE: Long = TimeUnit.HOURS.toMillis(3)

class HomeRepositoryImpl @Inject constructor(
    @ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
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

                        if (categories.isNotEmpty()) {
                            val cacheKeys = cacheLocalDataSource.getOrCreateCacheKeys(
                                CacheKeyProvider.getAvatarCategoriesCacheKey()
                            )

                            val affected = cacheLocalDataSource.updateCacheKey(
                                cacheKeys.modify(
                                    createdAt = System.currentTimeMillis(),
                                    expiresAt = (System.currentTimeMillis() + SHORT_CACHE_TIME_TO_LIVE)
                                ).also {
                                    it._id = cacheKeys._id
                                }
                            )
                            Timber.d("Cache keys: categories updated $affected cacheKeys = $cacheKeys")
                        } else {
                            cacheLocalDataSource.clearCacheKeys(AvatarCategoriesTable.name).let { affected ->
                                Timber.d("Cache keys: categories removed for ${AvatarCategoriesTable.name} $affected")
                            }
                        }
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

                        val cacheKeys = cacheLocalDataSource.getOrCreateCacheKeys(
                            CacheKeyProvider.getCatalogListCacheKey(catalogDetailRequest.category)
                        )

                        if (catalogList.isNotEmpty()) {
                            val affected = cacheLocalDataSource.updateCacheKey(
                                cacheKeys.modify(
                                    createdAt = System.currentTimeMillis(),
                                    expiresAt = (System.currentTimeMillis() + SHORT_CACHE_TIME_TO_LIVE)
                                ).also {
                                    it._id = cacheKeys._id
                                }
                            )
                            Timber.d("Cache keys: categories updated $affected cacheKeys = $cacheKeys")
                        } else {
                            cacheLocalDataSource.clearCacheKeys(AvatarCategoriesTable.name).let { affected ->
                                Timber.d("Cache keys: categories removed for ${AvatarCategoriesTable.name} $affected")
                            }
                        }

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

    override fun getMyModels2(forceRefresh: Boolean): Flow<Result<List<ModelData>>> = flow {
        emit(Result.Loading)
        val cache = observeAllModel().first()

        if (forceRefresh || cache.isEmpty()) {
            when (val remoteResult = refreshAllModelInternal()) {
                is Result.Error -> {
                    throw remoteResult.exception
                }
                else -> {
                    // Noop
                }
            }
        }

        emitAll(
            observeAllModel().map { Result.Success(it) }
        )
    }.catch { t ->
        emit(Result.Error.NonRecoverableError(t as Exception))
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
    }

    // TODO: check cache invalidation
    override fun getCatalog2(forceRefresh: Boolean): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)
        val cache = observeAllCategoryInternal().first()

        var shouldFetch = false
        if (cache.isNotEmpty()) {
            val cacheKeys = cacheLocalDataSource.getCacheKeys(
                CacheKeyProvider.getAvatarCategoriesCacheKey()
            )
            if (cacheKeys != null) {
                if (!forceRefresh && cacheKeys.expired()) {
                    Timber.d("Cache keys: ${cacheKeys.key} expired")
                    localDataSource.deleteAllCategories()
                    shouldFetch = true
                } else if (forceRefresh && !cacheKeys.expired()) {
                    Timber.d("Cache keys: ${cacheKeys.key} valid")
                    val delta = (System.currentTimeMillis() - cacheKeys.createdAt).coerceAtLeast(0)
                    val lastUpdate = TimeUnit.MILLISECONDS.toMinutes(delta)
                    val message = "Can't refresh. Last updated $lastUpdate minutes ago"
                    val t = BuenoCacheException(lastUpdate, message)
                    // Timber.d(t, "Cache keys: $message")
                    emit(Result.Error.NonRecoverableError(t))
                }
            } else {
                shouldFetch = true
            }
        } else {
            shouldFetch = true
        }

        Timber.d("Cache keys: should fetch = $shouldFetch")
        if (shouldFetch) {
            /* Get data from remote */
            when (val result = refreshCategoriesInternal()) {
                is Result.Error -> {
                    throw result.exception
                }
                else -> {
                    /* Noop */
                }
            }
        }

        emitAll(
            observeAllCategoryInternal().map { Result.Success(it) }
        )
    }.catch { t ->
        emit(Result.Error.NonRecoverableError(t as Exception))
    }

    // TODO: check cache invalidation
    override fun getCatalogList2(
        request: CatalogDetailRequest,
        forceRefresh: Boolean
    ): Flow<Result<CatalogDetailData>> = flow {
        emit(Result.Loading)
        val cache = observeAllCatalogListInternal(request.category).first()

        var shouldFetch = false
        if (cache.isNotEmpty()) {
            val cacheKeys = cacheLocalDataSource.getCacheKeys(
                CacheKeyProvider.getCatalogListCacheKey(request.category)
            )
            if (cacheKeys != null) {
                if (!forceRefresh && cacheKeys.expired()) {
                    Timber.d("Cache keys: ${cacheKeys.key} expired")
                    localDataSource.deleteAllCatalogList(request.category)
                    shouldFetch = true
                } else if (forceRefresh && !cacheKeys.expired()) {
                    val delta = (System.currentTimeMillis() - cacheKeys.createdAt).coerceAtLeast(0)
                    val lastUpdate = TimeUnit.MILLISECONDS.toMinutes(delta)
                    val message = "Can't refresh. Last updated $lastUpdate minutes ago"
                    val t = BuenoCacheException(lastUpdate, message)
                    // Timber.d(t, "Cache keys: $message")
                    emit(Result.Error.NonRecoverableError(t))
                }
            } else {
                shouldFetch = true
            }
        } else {
            shouldFetch = true
        }

        Timber.d("Cache keys: should fetch = $shouldFetch")
        if (shouldFetch) {
            /* Get data from remote */
            when (val result = refreshCatalogListInternal(request)) {
                is Result.Error.NonRecoverableError -> {
                    throw result.exception
                }
                else -> {
                    /* Noop */
                }
            }
        }

        emitAll(
            observeAllCatalogListInternal(request.category)
                .map { list ->
                    val catalogDetailData = CatalogDetailData(
                        category = request.category,
                        avatars = list
                    )
                    Result.Success(catalogDetailData)
                }
        )
    }.catch { t ->
        emit(Result.Error.NonRecoverableError(t as Exception))
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

    override fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<PurchasePlanData>> {
        return remoteDataSource.purchasePlan(subscriptionPurchaseRequest.asDto())
            /* Executes the call in the application scope,
                so that it won't get canceled when the calling scope is dead. *//*
            .flowOn(applicationScope.coroutineContext)*/
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
                                Result.Error.NonRecoverableError(ApiException(cause))
                            }
                        } else {
                            badResponse(networkResult)
                        }
                    }
                    else -> parseErrorNetworkResult(networkResult)
                }
            }
            .catch { t ->
                emit(Result.Error.NonRecoverableError(t as Exception))
            }
    }

    fun purchasePlan2(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<PurchasePlanData>> = flow {
        emit(Result.Loading)

        /* Executes the call in the application scope,
        so that it won't get canceled when the calling scope is dead. */
        val networkResult = withContext(applicationScope.coroutineContext) {
            remoteDataSource.purchasePlanSync(
                subscriptionPurchaseRequest.asDto()
            )
        }

        emit(parsePurchasePlanResponse(networkResult))
    }
        .catch { t ->
            emit(Result.Error.NonRecoverableError(t as Exception))
        }
        .flowOn(ioDispatcher)

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

    override fun getMyModels(forceRefresh: Boolean): Flow<Result<List<ModelListWithModel>>> = flow {
        emit(Result.Loading)
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

    override fun subscriptionLog(request: SubscriptionLogRequest): Flow<Result<String>> {
        return remoteDataSource.subscriptionLog(request.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.data.message
                        Result.Success(message)
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    private fun parsePurchasePlanResponse(networkResult: NetworkResult<PurchasePlanResponse>): com.aiavatar.app.commons.util.Result<PurchasePlanData> {
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val data = networkResult.data.purchasePlanDataDto?.toPurchasePlanData()
                    if (data != null) {
                        Result.Success(data)
                    } else {
                        val cause = EmptyResponseException("No status id")
                        Result.Error.NonRecoverableError(ApiException(cause))
                    }
                } else {
                    badResponse(networkResult)
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
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

