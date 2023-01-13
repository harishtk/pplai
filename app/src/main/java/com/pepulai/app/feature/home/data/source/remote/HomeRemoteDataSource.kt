package com.pepulai.app.feature.home.data.source.remote

import com.pepulai.app.commons.data.source.remote.BaseRemoteDataSource
import com.pepulai.app.commons.util.NetWorkHelper
import com.pepulai.app.commons.util.NetworkResult
import com.pepulai.app.di.IoDispatcher
import com.pepulai.app.feature.home.data.source.remote.model.HomeResponse
import com.pepulai.app.feature.home.data.source.remote.model.HomeResponseOld
import com.pepulai.app.feature.home.data.source.remote.model.SubscriptionPlanResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class HomeRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    private val apiService: HomeApi,
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
) : BaseRemoteDataSource(netWorkHelper) {

    fun getHomeOld(): Flow<NetworkResult<HomeResponseOld>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.homeOld() })
    }.flowOn(dispatcher)

    fun getHome(): Flow<NetworkResult<HomeResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.home() })
    }.flowOn(dispatcher)

    fun getSubscriptionPlans(): Flow<NetworkResult<SubscriptionPlanResponse>> = flow<NetworkResult<SubscriptionPlanResponse>> {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.subscriptionPlans() })
    }.flowOn(dispatcher)

}