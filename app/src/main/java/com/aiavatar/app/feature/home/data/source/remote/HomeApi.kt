package com.aiavatar.app.feature.home.data.source.remote

import com.aiavatar.app.feature.home.data.source.remote.dto.SubscriptionPurchaseRequestDto
import com.aiavatar.app.feature.home.data.source.remote.model.HomeResponse
import com.aiavatar.app.feature.home.data.source.remote.model.HomeResponseOld
import com.aiavatar.app.feature.home.data.source.remote.model.SubscriptionPlanResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HomeApi {

    @POST("ai/home")
    suspend fun homeOld(): Response<HomeResponseOld>

    @POST("ai/home")
    suspend fun home(): Response<HomeResponse>

    @POST("subscription/plans")
    suspend fun subscriptionPlans(): Response<SubscriptionPlanResponse>

    @POST("subscription/purchase")
    suspend fun purchasePlan(@Body subscriptionPurchaseRequestDto: SubscriptionPurchaseRequestDto): Response<BaseResponse>
}