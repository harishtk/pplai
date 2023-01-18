package com.aiavatar.app.core.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.feature.onboard.domain.model.UploadImageData
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import retrofit2.http.Part

interface AppRepository {

    fun sendFcmToken(request: SendFcmTokenRequest): Flow<Result<String>>

    suspend fun sendFcmTokenSync(request: SendFcmTokenRequest): Result<String>

    suspend fun uploadFileSync(
        @Part folderName: MultipartBody.Part,
        @Part type: MultipartBody.Part,
        @Part files: MultipartBody.Part,
    ): Result<UploadImageData>
}