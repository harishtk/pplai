package com.aiavatar.app.feature.onboard.data.source.remote

import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.dto.AutoLoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.LoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.LogoutRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.SocialLoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.model.AutoLoginResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.LoginResponse
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
    }
}