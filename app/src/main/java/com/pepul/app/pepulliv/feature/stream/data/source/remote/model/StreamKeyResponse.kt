package com.pepul.app.pepulliv.feature.stream.data.source.remote.model

import com.google.android.exoplayer2.offline.StreamKey
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class StreamKeyResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: WOWZStreamDto?
)