package com.aiavatar.app.feature.stream.data.source.remote

import com.aiavatar.app.commons.data.source.remote.BaseRemoteDataSource
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.stream.data.source.remote.dto.StreamIdRequestDto
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamInfoResponse
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamKeyResponse
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamStateResponse
import com.aiavatar.app.feature.stream.data.source.remote.model.StreamsResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import retrofit2.http.Part
import javax.inject.Inject

class StreamRemoteDataSource @Inject constructor(
    netWorkHelper: NetWorkHelper,
    val apiService: StreamApi,
    @IoDispatcher
    val dispatcher: CoroutineDispatcher,
) : BaseRemoteDataSource(netWorkHelper) {

    fun getStreams(): Flow<NetworkResult<StreamsResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.getStreams() })
    }.flowOn(dispatcher)

    fun getStreamKey(): Flow<NetworkResult<StreamKeyResponse>> = flow {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.getStreamKey() })
    }.flowOn(dispatcher)

    fun startStream(requestDto: StreamIdRequestDto): Flow<NetworkResult<StreamStateResponse>> = flow<NetworkResult<StreamStateResponse>> {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.startStreamPublish(requestDto) })
    }.flowOn(dispatcher)

    fun stopStream(requestDto: StreamIdRequestDto): Flow<NetworkResult<StreamStateResponse>> = flow<NetworkResult<StreamStateResponse>> {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.stopStreamPublish(requestDto) })
    }.flowOn(dispatcher)

    fun deleteStream(requestDto: StreamIdRequestDto): Flow<NetworkResult<StreamStateResponse>> = flow<NetworkResult<StreamStateResponse>> {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.deleteStream(requestDto) })
    }.flowOn(dispatcher)

    fun getStreamState(requestDto: StreamIdRequestDto): Flow<NetworkResult<StreamStateResponse>> = flow<NetworkResult<StreamStateResponse>> {
        emit(NetworkResult.Loading())
        emit(synchronizedCall { apiService.getStreamState(requestDto) })
    }.flowOn(dispatcher)

    fun getStreamInfo(requestDto: StreamIdRequestDto): Flow<NetworkResult<StreamInfoResponse>> = flow<NetworkResult<StreamInfoResponse>> {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.getStreamInfo(requestDto) })
    }.flowOn(dispatcher)

    fun uploadStreamThumbnail(
        @Part file: MultipartBody.Part,
        @Part type: MultipartBody.Part,
        @Part streamName: MultipartBody.Part,
        @Part userId: MultipartBody.Part,
    ): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        emit(safeApiCall { apiService.uploadStreamThumbnail(
            type = type,
            streamName = streamName,
            userId = userId,
            file = file) })
    }.flowOn(dispatcher)

    suspend fun uploadStreamThumbnailSync(
        @Part file: MultipartBody.Part,
        @Part type: MultipartBody.Part,
        @Part streamName: MultipartBody.Part,
        @Part userId: MultipartBody.Part,
    ): NetworkResult<String> {
        return safeApiCall { apiService.uploadStreamThumbnail(
            type = type,
            streamName = streamName,
            userId = userId,
            file = file) }
    }

    /*fun getStreamsWithInfo(): Flow<NetworkResult<JSONObject>> = getStreams()
        .map { networkResult1 ->
            when (networkResult1) {
                is NetworkResult.Loading -> NetworkResult.Loading()
                is NetworkResult.Error -> networkResult1
                is NetworkResult.Success -> {
                    if (networkResult1.code == HttpsURLConnection.HTTP_OK) {
                        Timber.d("Network Result: $networkResult1")
                        try {
                            val live = networkResult1.data?.getJSONObject("live")
                            if (live != null) {
                                safeApiCall { apiService.getStreamInfo(streams = live.toString()) }
                            } else {
                                NetworkResult.Error("No data")
                            }
                        } catch (e: JSONException) {
                            NetworkResult.Error(e.message!!)
                        }
                    } else {
                        NetworkResult.Error("No data")
                    }
                }
                else -> networkResult1
            }
        }*/

}