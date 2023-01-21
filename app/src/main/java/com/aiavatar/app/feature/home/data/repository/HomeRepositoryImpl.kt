package com.aiavatar.app.feature.home.data.repository

import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPlanDto
import com.aiavatar.app.feature.home.data.source.remote.dto.asDto
import com.aiavatar.app.feature.home.data.source.remote.dto.toSubscriptionPlan
import com.aiavatar.app.feature.home.data.source.remote.model.dto.toCatalogDetailData
import com.aiavatar.app.feature.home.data.source.remote.model.dto.toModelList
import com.aiavatar.app.feature.home.data.source.remote.model.toCategory
import com.aiavatar.app.feature.home.domain.model.CatalogDetailData
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.model.ModelList
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.home.presentation.create.AvatarResultUiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
) : HomeRepository, NetworkResultParser {

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
}

