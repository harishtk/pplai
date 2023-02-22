package com.aiavatar.app.core.data.repository

import com.aiavatar.app.core.data.source.remote.LoggingRemoteDataSource
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.aiavatar.app.commons.util.Result
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject

class LoggingRepository @Inject constructor(
    private val remoteDataSource: LoggingRemoteDataSource,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher
) {

    fun customLog(params: JsonObject): Flow<Result<BaseResponse>> = flow {
        emit(Result.Loading)
        kotlin.runCatching {
            val response = remoteDataSource.customLog(jsonObject = params)
            if (response.statusCode == HttpURLConnection.HTTP_ACCEPTED) {
                response
            } else {
                throw IllegalStateException("Bad response: ${response.statusCode}")
            }
        }
            .fold(
                onSuccess = { emit(Result.Success(it)) },
                onFailure = { t ->
                    when (t) {
                        is HttpException -> emit(Result.Error.NonRecoverableError(t))
                        is IOException -> emit(Result.Error.NonRecoverableError(t))
                        else -> emit(Result.Error.NonRecoverableError(t as Exception))
                    }
                }
            )
    }.flowOn(ioDispatcher)
}