package com.aiavatar.app.feature.home.data.repository

import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.feature.home.data.source.local.HomeLocalDataSource
import com.aiavatar.app.feature.home.data.source.local.entity.CategoryEntity
import com.aiavatar.app.feature.home.data.source.local.entity.asEntity
import com.aiavatar.app.feature.home.data.source.local.entity.toCategory
import com.aiavatar.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPlanDto
import com.aiavatar.app.feature.home.data.source.remote.dto.asDto
import com.aiavatar.app.feature.home.data.source.remote.dto.toSubscriptionPlan
import com.aiavatar.app.feature.home.data.source.remote.model.dto.toCatalogDetailData
import com.aiavatar.app.feature.home.data.source.remote.model.dto.toModelList
import com.aiavatar.app.feature.home.data.source.remote.model.toCategory
import com.aiavatar.app.feature.home.data.source.remote.model.toListAvatar
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val localDataSource: HomeLocalDataSource,
) : HomeRepository, NetworkResultParser {

    private fun observeAllCategoryInternal(): Flow<List<Category>> {
        return localDataSource.observeAllCategories()
            .map { list ->
                list.map(CategoryEntity::toCategory)
            }
    }

    private suspend fun refreshCategoriesInternal(): Result<List<Category>> {
        val networkCatalogResult = remoteDataSource.getCatalogSync()
        return when (networkCatalogResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkCatalogResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val categoryDtoList = networkCatalogResult?.data?.data?.avatars?.map { it.toCategory() }
                    if (categoryDtoList != null) {
                        val categories = categoryDtoList.map(Category::asEntity)
                        localDataSource.deleteAllCategories()
                        localDataSource.insertAllCategories(categories)
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

    override fun getCatalog2(forceRefresh: Boolean): Flow<Result<List<Category>>> {
        return observeAllCategoryInternal()
            .onStart {
                if (forceRefresh) {
                    localDataSource.deleteAllCategories()
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
                Result.Error(t as Exception)
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

    override fun getCatalogDetail(request: CatalogDetailRequest): Flow<Result<CatalogDetailData>> {
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

    override fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<String>> {
        return remoteDataSource.purchasePlan(subscriptionPurchaseRequest.asDto())
            .map { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> Result.Loading
                    is NetworkResult.Success -> {
                        if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                            val statusId = networkResult.data.data?.avatarStatusId
                            if (statusId != null) {
                                Result.Success(statusId)
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

    override fun getMyModels(): Flow<Result<List<ModelList>>> {
        return remoteDataSource.getMyModels().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val modelList = networkResult.data.data?.models?.map { it.toModelList() }
                        if (modelList != null) {
                            Result.Success(modelList)
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause = BadResponseException("Unexpected response code ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getAvatars(getAvatarsRequest: GetAvatarsRequest): Flow<Result<List<ListAvatar>>> {
        return remoteDataSource.getAvatars(getAvatarsRequest.asDto()).map { networkResult ->
            when (networkResult) {
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
                        val cause = BadResponseException("Unexpected response code ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }
}

