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
import com.aiavatar.app.feature.home.data.source.remote.dto.toCategory
import com.aiavatar.app.feature.home.data.source.remote.dto.toSubscriptionPlan
import com.aiavatar.app.feature.home.data.source.remote.dto.toUserModel
import com.aiavatar.app.feature.home.data.source.remote.model.toAvatar
import com.aiavatar.app.feature.home.domain.model.Avatar
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.UserAndCategory
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
) : HomeRepository, NetworkResultParser {

    override fun getAvatars(): Flow<Result<List<Avatar>>> {
        return remoteDataSource.getHome().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data
                        if (data != null) {
                            val avatars: List<Avatar> = data?.data?.avatars?.map { it.toAvatar() }
                                ?: listOf<Avatar>()
                            Result.Success(avatars)
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

    override fun getCatalog(): Flow<Result<UserAndCategory>> {
        return remoteDataSource.getHomeOld().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data
                        if (data != null) {
                            val users = data.data?.userModel?.map { it.toUserModel() }
                                ?: emptyList()
                            val categories = data?.data?.categories?.map { it.toCategory() }
                                ?: emptyList()
                            val userAndCategory = UserAndCategory(users, categories)
                            Result.Success(userAndCategory)
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

    override fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>> {
        return remoteDataSource.getSubscriptionPlans().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data
                        if (data != null) {
                            val plans = data.data?.plans?.map(SubscriptionPlanDto::toSubscriptionPlan)
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
        return remoteDataSource.purchasePlan(subscriptionPurchaseRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.message ?: "Success. No message"
                        Result.Success(message)
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
}