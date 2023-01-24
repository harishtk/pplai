package com.aiavatar.app.commons.util

import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.google.android.gms.common.api.Api
import timber.log.Timber

interface NetworkResultParser {

    /**
     * Abstracts the boilerplate to parse error results
     */
    fun <T, R> parseErrorNetworkResult(networkResult: NetworkResult<T>): Result<R> {
        return when (networkResult) {
            is NetworkResult.Loading -> {
                Timber.w("Not an error result")
                Result.Loading
            }
            is NetworkResult.Success -> {
                Timber.w("Not an error result")
                val cause = IllegalStateException("Parsing non error result!")
                Result.Error(ApiException(cause))
            }
            is NetworkResult.Error -> {
                val cause = IllegalStateException(networkResult.message ?: "Something went wrong")
                Result.Error(ApiException(networkResult.message, cause))
            }
            is NetworkResult.NoInternet -> {
                val cause = NoInternetException("Please check internet connection")
                Result.Error(cause)
            }
            is NetworkResult.UnAuthorized -> {
                val cause = IllegalStateException("Something went wrong")
                Result.Error(ApiException(networkResult.message, cause))
            }
        }
    }

    /**
     * Abstracts the boilerplate to ack. bad response or unexpected response code
     */
    fun badResponse(networkResult: NetworkResult<*>): Result.Error {
        val cause = BadResponseException("Unexpected response code ${networkResult.code}")
        return Result.Error(ApiException(cause))
    }

    /**
     * Abstracts the boilerplate to ack. empty response i.e. data is 'null'
     */
    fun emptyResponse(networkResult: NetworkResult<*>): Result.Error {
        val cause = EmptyResponseException("No data")
        return Result.Error(ApiException(cause))
    }
}