package com.aiavatar.app.feature.home.data.source.remote

import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.home.data.source.remote.dto.CatalogDetailRequestDto
import com.aiavatar.app.feature.home.data.source.remote.dto.GenerateAvatarRequestDto
import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPurchaseRequestDto
import com.aiavatar.app.feature.home.data.source.remote.model.CatalogDetailResponse
import com.aiavatar.app.feature.home.data.source.remote.model.GenerateAvatarResponse
import com.aiavatar.app.feature.home.data.source.remote.model.HomeResponse
import com.aiavatar.app.feature.home.data.source.remote.model.SubscriptionPlanResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class HomeRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    private val apiService: HomeApi,
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher,
) : BaseRemoteDataSource(netWorkHelper) {

    fun getCatalog(): Flow<NetworkResult<HomeResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getCatalog() })
    }.flowOn(dispatcher)

    fun getCatalogDetail(catalogDetailRequestDto: CatalogDetailRequestDto): Flow<NetworkResult<CatalogDetailResponse>> =
        flow {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.getCatalogDetail(catalogDetailRequestDto) })
        }.flowOn(dispatcher)

    fun getSubscriptionPlans(): Flow<NetworkResult<SubscriptionPlanResponse>> =
        flow<NetworkResult<SubscriptionPlanResponse>> {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.subscriptionPlans() })
        }.flowOn(dispatcher)

    fun purchasePlan(subscriptionPurchaseRequestDto: SubscriptionPurchaseRequestDto): Flow<NetworkResult<BaseResponse>> =
        flow<NetworkResult<BaseResponse>> {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.purchasePlan(subscriptionPurchaseRequestDto) })
        }.flowOn(dispatcher)

    fun generateAvatar(generateAvatarRequestDto: GenerateAvatarRequestDto): Flow<NetworkResult<GenerateAvatarResponse>> =
        flow<NetworkResult<GenerateAvatarResponse>> {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.generateAvatar(generateAvatarRequestDto) })
        }.flowOn(dispatcher)

}