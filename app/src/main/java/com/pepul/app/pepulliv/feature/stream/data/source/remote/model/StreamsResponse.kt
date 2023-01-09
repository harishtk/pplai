package com.pepul.app.pepulliv.feature.stream.data.source.remote.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.pepul.app.pepulliv.feature.stream.data.source.remote.dto.StreamItemDto
import org.json.JSONObject

data class StreamsResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("allStream")
        val streams: List<StreamDto>?
    )
}

/**
 *
 *"id": 1,
"userId": 95243888,
"streamId": "k6qb0jyb",
"streamName": "zfkz9PFlM",
"createdAt": "2022-11-07T16:23:47.000Z",
"updatedAt": "2022-11-07T16:23:53.000Z",
"state": "stopped"
 */

data class StreamDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("streamId")
    val streamId: String,
    @SerializedName("streamName")
    val streamName: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("state")
    val state: String,
)

data class WOWZStreamDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("broadcast_location")
    val broadcastLocation: String?,
    @SerializedName("aspect_ratio_width")
    val aspectRatioWidth: Int?,
    @SerializedName("aspect_ratio_height")
    val aspectRatioHeight: Int?,
    @SerializedName("source_connection_information")
    val sourceConnectionInfo: SourceConnectionInfoDto?,
    @SerializedName("player_hls_playback_url")
    val hlsPlaybackUrl: String?
)

data class SourceConnectionInfoDto(
    @SerializedName("primary_server")
    val primaryServer: String,
    @SerializedName("host_port")
    val hostPost: Int,
    @SerializedName("stream_name")
    val streamName: String,
    @SerializedName("disable_authentication")
    val disableAuthentication: Boolean,
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)
