package com.aiavatar.app.feature.onboard.data.source.remote.model

import com.aiavatar.app.feature.onboard.data.source.remote.dto.CreateCheckDataDto
import com.google.gson.annotations.SerializedName

data class CreateCheckResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: CreateCheckDataDto?
)
