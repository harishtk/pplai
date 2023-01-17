package com.aiavatar.app.core.data.repository

import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.core.data.source.remote.AppRemoteDataSource
import com.aiavatar.app.core.data.source.remote.dto.asDto
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class AppRepositoryImpl @Inject constructor(
    private val remoteDataSource: AppRemoteDataSource
) : AppRepository, NetworkResultParser {

    override fun sendFcmToken(request: SendFcmTokenRequest): Flow<Result<String>> {
        return remoteDataSource.sendFcmTokenToServer(request.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.message ?: "Success."
                        Result.Success(message)
                    } else {
                        val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override suspend fun sendFcmTokenSync(request: SendFcmTokenRequest): Result<String> {
        return when (val networkResult = remoteDataSource.sendFcmTokenToServer(request.asDto()).last()) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val message = networkResult.message ?: "Success."
                    Result.Success(message)
                } else {
                    val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                    Result.Error(ApiException(cause))
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }
}