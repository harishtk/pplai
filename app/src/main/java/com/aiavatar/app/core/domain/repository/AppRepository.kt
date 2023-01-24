package com.aiavatar.app.core.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.core.data.source.remote.dto.RenameModelRequestDto
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.CreateModelData
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
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

    fun createModel(createModelRequest: CreateModelRequest): Flow<Result<CreateModelData>>

    fun avatarStatus(avatarStatusRequest: AvatarStatusRequest): Flow<Result<AvatarStatusWithFiles>>

    fun renameModel(renameModelRequest: RenameModelRequest): Flow<Result<String>>
}