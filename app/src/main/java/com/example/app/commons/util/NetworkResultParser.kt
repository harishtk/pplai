package com.example.app.commons.util

import com.example.app.commons.util.net.ApiException
import com.example.app.commons.util.net.NoInternetException
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
}