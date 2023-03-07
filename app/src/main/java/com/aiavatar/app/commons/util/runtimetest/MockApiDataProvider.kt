package com.aiavatar.app.commons.util.runtimetest

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.core.di.GsonParser
import com.aiavatar.app.core.domain.model.CreateModelData
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.util.JsonParser
import com.aiavatar.app.feature.home.presentation.create.util.BadPhotoSamplesException
import com.aiavatar.app.feature.onboard.data.source.remote.dto.toCreateCheckData
import com.aiavatar.app.feature.onboard.data.source.remote.model.CreateCheckResponse
import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@ViewModelScoped
class MockApiDataProvider @Inject constructor(
    @GsonParser
    private val jsonParser: JsonParser
) {

    fun createModel_Fail(
        createModelRequest: CreateModelRequest,
    ): Flow<Result<CreateModelData>> = flow {
        emit(Result.Loading)
        delay(NETWORK_DELAY_LONG)
        val cause = BadPhotoSamplesException()
        emit(Result.Error.NonRecoverableError(ApiException(cause)))
    }

    fun createModel_Succeed(
        createModelRequest: CreateModelRequest
    ): Flow<com.aiavatar.app.commons.util.Result<CreateModelData>> = flow {
        emit(Result.Loading)
        delay(DEFAULT_NETWORK_DELAY)
        TODO("Not implemented")
    }

    fun createCheck(
        createCheckRequest: CreateCheckRequest,
        shouldFail: Boolean = false
    ): Flow<Result<CreateCheckData>> = flow {
        emit(Result.Loading)
        val request = CreateCheckRequest(
            timestamp = System.currentTimeMillis()
        )
        delay(DEFAULT_NETWORK_DELAY)
        if (shouldFail) {
            val cause = IllegalStateException("Something went wrong")
            emit(Result.Error.NonRecoverableError(ApiException(cause)))
        } else {
            val response = jsonParser.fromJson(createCheckResponseString, CreateCheckResponse::class.java)
            emit(Result.Success(response?.data?.toCreateCheckData()!!))
        }
    }

    companion object {
        const val DEFAULT_NETWORK_DELAY: Long = 2000
        const val NETWORK_DELAY_LONG: Long = 10000
        const val NETWORK_DELAY_NONE: Long = 0

        val createCheckResponseString = """
            {
                "statusCode": 200,
                "message": "success",
                "data": {
                    "allowModelCreate": false,
                    "siteDown": true,
                    "modelPay": false
                }
            }
        """.trimIndent()
    }
}