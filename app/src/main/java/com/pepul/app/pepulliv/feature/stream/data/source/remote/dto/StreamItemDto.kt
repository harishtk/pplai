package com.pepul.app.pepulliv.feature.stream.data.source.remote.dto

import com.google.gson.annotations.SerializedName

data class StreamItemDto(
    @SerializedName("stream")
    val stream: String,
    @SerializedName("publisher")
    val publisher: StreamUserDto?,
    @SerializedName("subscribers")
    val subscribers: List<StreamUserDto>?
)

data class VideoDto(
    @SerializedName("codec")
    val codec: String,
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int,
    @SerializedName("profile")
    val profile: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("fps")
    val fps: Int
)

data class AudioDto(
    @SerializedName("codec")
    val codec: String,
    @SerializedName("profile")
    val profile: String,
    @SerializedName("samplerate")
    val sampleRate: Int,
    @SerializedName("channels")
    val channels: Int
)

data class StreamUserDto(
    @SerializedName("app")
    val app: String,
    @SerializedName("stream")
    val stream: String,
    @SerializedName("clientId")
    val clientId: String,
    @SerializedName("connectCreated")
    val connectCreated: String?,
    @SerializedName("bytes")
    val bytes: Long,
    @SerializedName("ip")
    val ipAddr: String?,
    @SerializedName("protocol")
    val protocol: String?,
    @SerializedName("video")
    val video: VideoDto?,
    @SerializedName("audio")
    val audio: AudioDto?
)
