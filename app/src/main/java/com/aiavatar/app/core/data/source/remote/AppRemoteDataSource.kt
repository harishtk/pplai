package com.aiavatar.app.core.data.source.remote

import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.core.data.source.remote.dto.AvatarStatusRequestDto
import com.aiavatar.app.core.data.source.remote.dto.CreateModelRequestDto
import com.aiavatar.app.core.data.source.remote.dto.RenameModelRequestDto
import com.aiavatar.app.core.data.source.remote.dto.SendFcmTokenRequestDto
import com.aiavatar.app.core.data.source.remote.model.AvatarStatusDto
import com.aiavatar.app.core.data.source.remote.model.AvatarStatusResponse
import com.aiavatar.app.core.data.source.remote.model.CreateModelResponse
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.UploaderResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import retrofit2.http.Part
import javax.inject.Inject

class AppRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    private val apiService: AppApi,
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
) : BaseRemoteDataSource(netWorkHelper) {

    fun sendFcmTokenToServer(requestDto: SendFcmTokenRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.sendFcmToken(requestDto) })
    }.flowOn(dispatcher)

    suspend fun uploadFileSync(
        @Part folderName: MultipartBody.Part,
        @Part type: MultipartBody.Part,
        @Part fileName: MultipartBody.Part,
        @Part files: MultipartBody.Part,
    ): NetworkResult<UploaderResponse> = safeApiCall { apiService.uploadFile(
        folderName = folderName,
        type = type,
        fileName = fileName,
        file = files
    ) }

    fun createModel(createModelRequestDto: CreateModelRequestDto): Flow<NetworkResult<CreateModelResponse>> = flow<NetworkResult<CreateModelResponse>> {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.createModel(createModelRequestDto) })
    }.flowOn(dispatcher)

    suspend fun createModelSync(createModelRequestDto: CreateModelRequestDto): NetworkResult<CreateModelResponse> =
        safeApiCall { apiService.createModel(createModelRequestDto) }

    fun avatarStatus(avatarStatusRequestDto: AvatarStatusRequestDto): Flow<NetworkResult<AvatarStatusResponse>> = flow<NetworkResult<AvatarStatusResponse>> {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.avatarStatus(avatarStatusRequestDto) })
    }.flowOn(dispatcher)

    suspend fun avatarStatusSync(avatarStatusRequestDto: AvatarStatusRequestDto): NetworkResult<AvatarStatusResponse> =
        safeApiCall { apiService.avatarStatus(avatarStatusRequestDto) }

    fun renameModel(renameModelRequestDto: RenameModelRequestDto): Flow<NetworkResult<BaseResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.renameModel(renameModelRequestDto) })
    }.flowOn(dispatcher)

}