package com.pepulai.app.feature.onboard.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class BaseResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
)