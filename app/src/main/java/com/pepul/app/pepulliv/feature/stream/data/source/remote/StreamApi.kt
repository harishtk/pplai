package com.pepul.app.pepulliv.feature.stream.data.source.remote

import com.pepul.app.pepulliv.feature.stream.data.source.remote.dto.StreamIdRequestDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamInfoResponse
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamsResponse
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamKeyResponse
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamStateResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface StreamApi {

    /*@GET("streams/live")
    suspend fun getStreams(): Response<JSONObject>*/

    @POST("stream/allStream")
    suspend fun getStreams(): StreamsResponse

    @POST("stream/live")
    suspend fun getStreamKey(): StreamKeyResponse

    @POST("stream/info")
    suspend fun getStreamInfo(@Body streamIdRequestDto: StreamIdRequestDto): Response<StreamInfoResponse>

    @POST("stream/state")
    suspend fun getStreamState(@Body streamIdRequestDto: StreamIdRequestDto): StreamStateResponse

    @POST("upload/thumbnail")
    @Multipart
    suspend fun uploadStreamThumbnail(
        @Part type: MultipartBody.Part,
        @Part streamName: MultipartBody.Part,
        @Part userId: MultipartBody.Part,
        @Part file: MultipartBody.Part,
    ): Response<String>

    /* Stream Controls */
    @POST("stream/start")
    suspend fun startStreamPublish(@Body streamInfoRequest: StreamIdRequestDto): StreamStateResponse

    @POST("stream/stop")
    suspend fun stopStreamPublish(@Body streamInfoRequest: StreamIdRequestDto): StreamStateResponse

    @POST("stream/delete")
    suspend fun deleteStream(@Body streamIdRequestDto: StreamIdRequestDto): StreamStateResponse
}