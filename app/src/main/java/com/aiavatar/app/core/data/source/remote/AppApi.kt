package com.aiavatar.app.core.data.source.remote

import com.aiavatar.app.core.data.source.remote.dto.SendFcmTokenRequestDto
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AppApi {

    suspend fun sendFcmToken(@Body requestDto: SendFcmTokenRequestDto): Response<BaseResponse>

    @POST("customLog.php")
    suspend fun customLog(@Body jsonObject: JsonObject): BaseResponse

}