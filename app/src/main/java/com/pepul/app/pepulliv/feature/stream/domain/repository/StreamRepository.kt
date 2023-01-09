package com.pepul.app.pepulliv.feature.stream.domain.repository

import com.google.gson.JsonObject
import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.feature.stream.data.source.remote.dto.StreamItemDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamStateDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.WOWZStreamDto
import com.pepul.app.pepulliv.feature.stream.domain.model.StreamState
import com.pepul.app.pepulliv.feature.stream.domain.model.request.StreamIdRequest
import com.pepul.app.pepulliv.feature.stream.domain.model.request.UploadThumbnailRequest
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