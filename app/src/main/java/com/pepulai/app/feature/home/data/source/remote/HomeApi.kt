package com.pepulai.app.feature.home.data.source.remote

import com.pepulai.app.feature.home.data.source.remote.model.HomeResponse
import com.pepulai.app.feature.home.data.source.remote.model.HomeResponseOld
import com.pepulai.app.feature.home.data.source.remote.model.SubscriptionPlanResponse
import retrofit2.Response
import retrofit2.http.POST

interface HomeApi {

    @POST("ai/home")
    suspend fun homeOld(): Response<HomeResponseOld>

    @POST("ai/home")
    suspend fun home(): Response<HomeResponse>

    @POST("subscription/plans")
    suspend fun subscriptionPlans(): Response<SubscriptionPlanResponse>
}