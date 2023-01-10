package com.pepulai.app.feature.stream.domain.repository

import com.google.gson.JsonObject
import com.pepulai.app.commons.util.Result
import com.pepulai.app.feature.stream.data.source.remote.dto.StreamItemDto
import com.pepulai.app.feature.stream.data.source.remote.model.StreamDto
import com.pepulai.app.feature.stream.data.source.remote.model.StreamStateDto
import com.pepulai.app.feature.stream.data.source.remote.model.WOWZStreamDto
import com.pepulai.app.feature.stream.domain.model.StreamState
import com.pepulai.app.feature.stream.domain.model.request.StreamIdRequest
import com.pepulai.app.feature.stream.domain.model.request.UploadThumbnailRequest
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

interface StreamRepository {

    fun getStreams(): Flow<Result<List<StreamDto>>>

    fun getStreamKey(): Flow<Result<WOWZStreamDto>>

    fun getStreamInfo(streamIdRequest: StreamIdRequest): Flow<Result<WOWZStreamDto>>

    fun startStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>>

    fun stopStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>>

    fun deleteStream(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>>

    fun getStreamState(streamIdRequest: StreamIdRequest): Flow<Result<StreamState>>

    fun uploadStreamThumbnail(uploadThumbnailRequest: UploadThumbnailRequest): Flow<Result<String>>

    suspend fun uploadStreamThumbnailSync(uploadThumbnailRequest: UploadThumbnailRequest): Result<String>

}