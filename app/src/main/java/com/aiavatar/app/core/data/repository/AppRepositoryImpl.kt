package com.aiavatar.app.core.data.repository

import androidx.room.withTransaction
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.data.source.remote.AppRemoteDataSource
import com.aiavatar.app.core.data.source.remote.dto.asDto
import com.aiavatar.app.core.data.source.remote.model.toAvatarStatusWithFiles
import com.aiavatar.app.core.data.source.remote.model.toCreateModelData
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.CreateModelData
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.onboard.data.source.remote.dto.asUploadImageData
import com.aiavatar.app.feature.onboard.data.source.remote.model.UploaderResponse
import com.aiavatar.app.feature.onboard.domain.model.UploadImageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class AppRepositoryImpl @Inject constructor(
    private val remoteDataSource: AppRemoteDataSource,
    @Deprecated("move to local data source")
    private val appDatabase: AppDatabase
) : AppRepository, NetworkResultParser {

    override fun sendFcmToken(request: SendFcmTokenRequest): Flow<Result<String>> {
        return remoteDataSource.sendFcmTokenToServer(request.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.message ?: "Success."
                        Result.Success(message)
                    } else {
                        val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override suspend fun sendFcmTokenSync(request: SendFcmTokenRequest): Result<String> {
        return when (val networkResult = remoteDataSource.sendFcmTokenToServer(request.asDto()).last()) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val message = networkResult.message ?: "Success."
                    Result.Success(message)
                } else {
                    badResponse(networkResult)
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    override suspend fun uploadFileSync(
        folderName: MultipartBody.Part,
        type: MultipartBody.Part,
        fileName: MultipartBody.Part,
        files: MultipartBody.Part
    ): Result<UploadImageData> {
        val networkResult = remoteDataSource.uploadFileSync(
            folderName = folderName,
            type = type,
            fileName = fileName,
            files = files
        )
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> parseUploadResponse(networkResult)
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    override fun createModel(createModelRequest: CreateModelRequest): Flow<Result<CreateModelData>> {
        return remoteDataSource.createModel(createModelRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data
                        if (data != null) {
                            Result.Success(data.toCreateModelData())
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun avatarStatus(avatarStatusRequest: AvatarStatusRequest): Flow<Result<AvatarStatusWithFiles>> {
        return remoteDataSource.avatarStatus(avatarStatusRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data
                        if (data != null) {
                            val avatarStatusWithFiles = networkResult.data.data.toAvatarStatusWithFiles()
                            appDatabase.withTransaction {
                                appDatabase.avatarStatusDao().insert(avatarStatusWithFiles.avatarStatus.toEntity())
                                val files = avatarStatusWithFiles.avatarFiles.map { it.toEntity() }
                                appDatabase.avatarFilesDao().insertAll(files)
                            }
                            Result.Success(avatarStatusWithFiles)
                        } else {
                            val cause = EmptyResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun renameModel(renameModelRequest: RenameModelRequest): Flow<Result<String>> {
        return remoteDataSource.renameModel(renameModelRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.data?.message ?: "Success. No message"
                        Result.Success(message)
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    private fun parseUploadResponse(networkResult: NetworkResult.Success<UploaderResponse>): Result<UploadImageData> {
        return if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
            val data = networkResult.data.data?.asUploadImageData()
            if (data != null) {
                Result.Success(data)
            } else {
                val cause = EmptyResponseException("No data")
                Result.Error(ApiException(cause))
            }
        } else {
            val cause = BadResponseException("Unexpected response code ${networkResult.code}")
            Result.Error(ApiException(cause))
        }
    }
}