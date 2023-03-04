package com.aiavatar.app.feature.onboard.data.source.remote

import android.net.Network
import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.dto.*
import com.aiavatar.app.feature.onboard.data.source.remote.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AccountsRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    private val apiService: AccountsApi,
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
) : BaseRemoteDataSource(netWorkHelper) {

    fun login(loginRequestDto: LoginRequestDto): Flow<NetworkResult<LoginResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.login(loginRequestDto) })
    }.flowOn(dispatcher)

    fun socialLogin(socialLoginRequestDto: SocialLoginRequestDto): Flow<NetworkResult<LoginResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.socialLogin(socialLoginRequestDto) })
    }.flowOn(dispatcher)

    fun autoLogin(autoLoginRequestDto: AutoLoginRequestDto): Flow<NetworkResult<AutoLoginResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.autoLogin(autoLoginRequestDto) })
    }.flowOn(dispatcher)

    fun logout(logoutRequestDto: LogoutRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.logout(logoutRequestDto) })
    }.flowOn(dispatcher)

    fun getShareLink(getShareLinkRequestDto: GetShareLinkRequestDto): Flow<NetworkResult<GetShareLinkResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getShareLink(getShareLinkRequestDto) })
    }.flowOn(dispatcher)

    fun feedback(feedbackRequestDto: FeedbackRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.feedback(feedbackRequestDto) })
    }.flowOn(dispatcher)

    fun createCheck(createCheckRequestDto: CreateCheckRequestDto): Flow<NetworkResult<CreateCheckResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.createCheck(createCheckRequestDto) })
    }.flowOn(dispatcher)
}