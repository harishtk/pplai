package com.example.app.feature.onboard.data.source.remote

import com.example.app.commons.data.source.remote.BaseRemoteDataSource
import com.example.app.commons.util.NetWorkHelper
import com.example.app.commons.util.NetworkResult
import com.example.app.di.IoDispatcher
import com.example.app.feature.onboard.data.source.remote.dto.AutoLoginRequestDto
import com.example.app.feature.onboard.data.source.remote.dto.LoginRequestDto
import com.example.app.feature.onboard.data.source.remote.model.AutoLoginResponse
import com.example.app.feature.onboard.data.source.remote.model.LoginResponse
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

    fun autoLogin(autoLoginRequestDto: AutoLoginRequestDto): Flow<NetworkResult<AutoLoginResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.autoLogin(autoLoginRequestDto) })
    }.flowOn(dispatcher)
}