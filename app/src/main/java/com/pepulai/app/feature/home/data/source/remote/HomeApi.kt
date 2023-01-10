package com.pepulai.app.feature.home.data.source.remote

import com.pepulai.app.feature.home.data.source.remote.model.HomeResponse
import retrofit2.Response
import retrofit2.http.POST

interface HomeApi {

    @POST("ai/home")
    suspend fun home(): Response<HomeResponse>
}