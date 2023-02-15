package com.aiavatar.app.feature.home.data.source.remote

import com.aiavatar.app.feature.home.data.source.remote.dto.*
import com.aiavatar.app.feature.home.data.source.remote.model.*
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface HomeApi {

    @POST("ai/home")
    suspend fun getCatalog(): Response<HomeResponse>

    @POST("ai/list")
    suspend fun getCatalogDetail(@Body catalogDetailRequestDto: CatalogDetailRequestDto): Response<CatalogDetailResponse>

    @POST("ai/generateAvatar")
    suspend fun generateAvatar(@Body requestDto: GenerateAvatarRequestDto): Response<GenerateAvatarResponse>

    @POST("user/models")
    suspend fun getMyModels(): Response<MyModelsResponse>

    @POST("user/models/{modelId}")
    suspend fun getModel(@Path("modelId") modelId: String): Response<MyModelsResponse>

    @POST("user/avatars")
    suspend fun getAvatars(@Body requestDto: GetAvatarsRequestDto): Response<GetAvatarsResponse>

    @POST("subscription/plans")
    suspend fun subscriptionPlans(): Response<SubscriptionPlanResponse>

    @POST("subscription/purchase")
    suspend fun purchasePlan(@Body subscriptionPurchaseRequestDto: SubscriptionPurchaseRequestDto): Response<PurchasePlanResponse>

    @POST("subscription/log")
    suspend fun subscriptionLog(@Body subscriptionLogRequestDto: SubscriptionLogRequestDto): Response<BaseResponse>
}