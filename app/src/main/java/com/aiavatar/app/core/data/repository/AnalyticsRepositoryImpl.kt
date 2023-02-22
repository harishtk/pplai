package com.aiavatar.app.core.data.repository

import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.remote.AnalyticsRemoteDataSource
import com.aiavatar.app.core.domain.repository.AnalyticsRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.net.ssl.HttpsURLConnection

class AnalyticsRepositoryImpl constructor(
    private val remoteDataSource: AnalyticsRemoteDataSource,
) : AnalyticsRepository {

    override fun updateCount(params: JsonObject): Flow<Result<String>> =
        remoteDataSource.updateCount(params).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> { Result.Loading }
                is NetworkResult.Error -> {
                    Result.Error.NonRecoverableError(IllegalStateException("Something went wrong"))
                }
                is NetworkResult.NoInternet -> {
                    Result.Error.NonRecoverableError(NoInternetException())
                }
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        Result.Success(networkResult.data?.message ?: "Success")
                    } else {
                        Result.Error.NonRecoverableError(BadResponseException("Unexpected response"))
                    }
                }
                else -> {
                    Result.Error.NonRecoverableError(IllegalStateException("Unauthorized"))
                }
            }
        }

}