package com.aiavatar.app.feature.home.data.source.remote

import android.net.Network
import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.home.data.source.remote.dto.*
import com.aiavatar.app.feature.home.data.source.remote.model.*
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

    suspend fun getCatalogSync(): NetworkResult<HomeResponse> =
        safeApiCall { apiService.getCatalog() }

    fun getCatalogDetail(catalogDetailRequestDto: CatalogDetailRequestDto): Flow<NetworkResult<CatalogDetailResponse>> =
        flow {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.getCatalogDetail(catalogDetailRequestDto) })
        }.flowOn(dispatcher)

    suspend fun getCatalogDetailSync(catalogDetailRequestDto: CatalogDetailRequestDto): NetworkResult<CatalogDetailResponse> =
        safeApiCall { apiService.getCatalogDetail(catalogDetailRequestDto) }

    fun getSubscriptionPlans(): Flow<NetworkResult<SubscriptionPlanResponse>> =
        flow {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.subscriptionPlans() })
        }.flowOn(dispatcher)

    fun purchasePlan(subscriptionPurchaseRequestDto: SubscriptionPurchaseRequestDto): Flow<NetworkResult<PurchasePlanResponse>> =
        flow {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.purchasePlan(subscriptionPurchaseRequestDto) })
        }.flowOn(dispatcher)

    fun generateAvatar(generateAvatarRequestDto: GenerateAvatarRequestDto): Flow<NetworkResult<GenerateAvatarResponse>> =
        flow {
            emit(NetworkResult.Loading())
            emit(safeApiCall { apiService.generateAvatar(generateAvatarRequestDto) })
        }.flowOn(dispatcher)

    fun getMyModels(): Flow<NetworkResult<MyModelsResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getMyModels() })
    }.flowOn(dispatcher)

    suspend fun getMyModelsSync(): NetworkResult<MyModelsResponse> =
        safeApiCall { apiService.getMyModels() }

    fun getModel(modelId: String): Flow<NetworkResult<MyModelsResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getModel(modelId) })
    }.flowOn(dispatcher)

    suspend fun getModelSync(modelId: String): NetworkResult<MyModelsResponse> =
        safeApiCall { apiService.getModel(modelId) }

    fun getAvatars(getAvatarsRequestDto: GetAvatarsRequestDto): Flow<NetworkResult<GetAvatarsResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getAvatars(getAvatarsRequestDto) })
    }.flowOn(dispatcher)

    suspend fun getAvatarsSync(getAvatarsRequestDto: GetAvatarsRequestDto): NetworkResult<GetAvatarsResponse> =
        safeApiCall { apiService.getAvatars(getAvatarsRequestDto) }

    fun subscriptionLog(subscriptionLogRequestDto: SubscriptionLogRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.subscriptionLog(subscriptionLogRequestDto) })
    }.flowOn(dispatcher)

}