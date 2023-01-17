package com.aiavatar.app.feature.stream.data.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.ParseException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.feature.stream.data.source.remote.StreamRemoteDataSource
import com.aiavatar.app.feature.stream.data.source.remote.dto.StreamItemDto
import com.aiavatar.app.feature.stream.data.source.remote.dto.StreamUserDto
import com.aiavatar.app.feature.stream.data.source.remote.dto.asDto
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamDto
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamStateResponse
import com.aiavatar.app.feature.stream.data.source.remote.model.WOWZStreamDto
import com.aiavatar.app.feature.stream.data.source.remote.model.toStreamState
import com.aiavatar.app.feature.stream.domain.model.StreamState
import com.aiavatar.app.feature.stream.domain.model.request.StreamIdRequest
import com.aiavatar.app.feature.stream.domain.model.request.UploadThumbnailRequest
import com.aiavatar.app.feature.stream.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class StreamRepositoryImpl @Inject constructor(
    private val remoteDataSource: StreamRemoteDataSource
) : StreamRepository, NetworkResultParser {

    override fun getStreams(): Flow<Result<List<StreamDto>>> {
        return remoteDataSource.getStreams().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data
                        if (data != null) {
                            val streams = data.streams ?: emptyList()
                            Result.Success(streams)
                        } else {
                            val cause = BadResponseException("No data")
                            Result.Error(ApiException(cause))
                        }
                    } else {
                        val cause = BadResponseException("No data")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getStreamKey(): Flow<Result<WOWZStreamDto>> {
        return remoteDataSource.getStreamKey().map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val streamKey = networkResult.data?.data
                        if (streamKey != null) {
                            Result.Success(streamKey)
                        } else {
                            val cause = BadResponseException("No stream key")
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

    override fun getStreamInfo(request: StreamIdRequest): Flow<Result<WOWZStreamDto>> {
        return remoteDataSource.getStreamInfo(request.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val streamData = networkResult.data.data
                        if (streamData != null) {
                            Result.Success(streamData)
                        } else {
                            val cause = BadResponseException("No stream data")
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

    override fun startStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>> {
        return remoteDataSource.startStream(streamIdRequest.asDto()).map(this::parseStreamIdState)
    }

    override fun stopStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>> {
        return remoteDataSource.stopStream(streamIdRequest.asDto()).map(this::parseStreamIdState)
    }

    override fun deleteStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>> {
        return remoteDataSource.deleteStream(streamIdRequest.asDto()).map(this::parseStreamIdState)
    }

    override fun getStreamState(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>> {
        return remoteDataSource.getStreamState(streamIdRequest.asDto()).map(this::parseStreamIdState)
    }

    override fun uploadStreamThumbnail(uploadThumbnailRequest: UploadThumbnailRequest): Flow<Result<String>> {
        return remoteDataSource.uploadStreamThumbnail(
            file = uploadThumbnailRequest.file,
            type = uploadThumbnailRequest.type,
            streamName = uploadThumbnailRequest.streamName,
            userId = uploadThumbnailRequest.userId
        ).map(this::parseUploadFileResponse)
    }

    override suspend fun uploadStreamThumbnailSync(uploadThumbnailRequest: UploadThumbnailRequest): Result<String> {
        val networkResult = remoteDataSource.uploadStreamThumbnailSync(
            file = uploadThumbnailRequest.file,
            type = uploadThumbnailRequest.type,
            streamName = uploadThumbnailRequest.streamName,
            userId = uploadThumbnailRequest.userId
        )
        return parseUploadFileResponse(networkResult)
    }

    private fun parseUploadFileResponse(networkResult: NetworkResult<String>): Result<String> {
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.code == HttpsURLConnection.HTTP_OK) {
                    val data = networkResult.data
                    if (data != null) {
                        Result.Success(data)
                    } else {
                        val cause = BadResponseException("No data")
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

    private fun parseStreamIdState(networkResult: NetworkResult<StreamStateResponse>): Result<StreamState> {
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult?.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val data = networkResult.data.data?.liveStreamState
                    if (data != null) {
                        Result.Success(data.toStreamState())
                    } else {
                        val cause = BadResponseException("No stream state")
                        Result.Error(ApiException(cause))
                    }
                } else {
                    val cause = BadResponseException("No data")
                    Result.Error(ApiException(cause))
                }
            }
            else -> parseErrorNetworkResult(networkResult)
        }
    }

    private fun parseLiveStreamsOld(jsonObject: JsonObject): Result<List<StreamItemDto>> {
        return try {
            if (!jsonObject.has("live")) {
                Result.Success(emptyList())
            } else {
                val liveJsonObject = jsonObject.getAsJsonObject("live")
                Timber.d("parsed: ${liveJsonObject.entrySet()}")
                val gson = GsonBuilder().create()
                return Result.Success(
                    liveJsonObject.entrySet().map { map ->
                        val valueJsonObject = map.value.asJsonObject
                        val publisher: StreamUserDto =
                            gson.fromJson(valueJsonObject.get("publisher"),
                                StreamUserDto::class.java)
                        val subscribers: List<StreamUserDto> =
                            if (valueJsonObject.has("subscribers")) {
                                gson.fromJson(valueJsonObject.get("subscribers"),
                                    object : TypeToken<List<StreamUserDto>>() {}.type)
                            } else {
                                emptyList()
                            }
                        StreamItemDto(
                            stream = map.key,
                            publisher = publisher,
                            subscribers = null
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Result.Error(ParseException(e))
        }
    }
}