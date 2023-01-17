package com.aiavatar.app.feature.stream.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class StreamKeyResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: WOWZStreamDto?
)