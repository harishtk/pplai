package com.aiavatar.app.core.data.source.remote

import com.aiavatar.app.core.data.source.remote.dto.AvatarStatusRequestDto
import com.aiavatar.app.core.data.source.remote.dto.CreateModelRequestDto
import com.aiavatar.app.core.data.source.remote.dto.RenameModelRequestDto
import com.aiavatar.app.core.data.source.remote.dto.SendFcmTokenRequestDto
import com.aiavatar.app.core.data.source.remote.model.AvatarStatusResponse
import com.aiavatar.app.core.data.source.remote.model.CreateModelResponse
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.UploaderResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AppApi {

    suspend fun sendFcmToken(@Body requestDto: SendFcmTokenRequestDto): Response<BaseResponse>

    @POST("customLog")
    suspend fun customLog(@Body jsonObject: JsonObject): BaseResponse

    @POST("ai/upload")
    @Multipart
    suspend fun uploadFile(
        @Part folderName: MultipartBody.Part,
        @Part type: MultipartBody.Part,
        @Part fileName: MultipartBody.Part,
        @Part file: MultipartBody.Part,
    ): Response<UploaderResponse>

    @POST("ai/createModel")
    suspend fun createModel(@Body createModelRequestDto: CreateModelRequestDto): Response<CreateModelResponse>

    @POST("ai/status")
    suspend fun avatarStatus(@Body avatarStatusRequestDto: AvatarStatusRequestDto): Response<AvatarStatusResponse>

    @POST("ai/rename")
    suspend fun renameModel(@Body renameModelRequestDto: RenameModelRequestDto): Response<BaseResponse>
}