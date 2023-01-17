package com.aiavatar.app.core.data.source.remote

import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApi {

    @POST("updateCount.php")
    suspend fun updateCount(@Body params: JsonObject): Response<BaseResponse>

}