package com.pepulai.app.feature.home.data.repository

import com.pepulai.app.commons.util.NetworkResult
import com.pepulai.app.commons.util.NetworkResultParser
import com.pepulai.app.commons.util.Result
import com.pepulai.app.commons.util.net.ApiException
import com.pepulai.app.commons.util.net.BadResponseException
import com.pepulai.app.commons.util.net.EmptyResponseException
import com.pepulai.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.pepulai.app.feature.home.data.source.remote.dto.toCategory
import com.pepulai.app.feature.home.data.source.remote.dto.toUserModel
import com.pepulai.app.feature.home.domain.model.UserAndCategory
import com.pepulai.app.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
) : HomeRepository, NetworkResultParser {

    override fun getCatalog(): Flow<Result<UserAndCategory>> {
        return remoteDataSource.getHome().map { networkResult ->
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
}