package com.pepul.app.pepulliv.feature.stream.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.pepul.app.pepulliv.feature.stream.domain.model.StreamState

data class StreamStateResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("live_stream")
        val liveStreamState: StreamStateDto?
    )
}

data class StreamStateDto(
    @SerializedName("state")
    val state: String
)

fun StreamStateDto.toStreamState(): StreamState {
    return StreamState(
        state = state
    )
}
