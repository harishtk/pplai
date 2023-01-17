package com.aiavatar.app.core.data.source.remote

import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.core.data.source.remote.dto.SendFcmTokenRequestDto
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AppRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    private val apiService: AppApi,
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
) : BaseRemoteDataSource(netWorkHelper) {

    fun sendFcmTokenToServer(requestDto: SendFcmTokenRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.sendFcmToken(requestDto) })
    }.flowOn(dispatcher)

}