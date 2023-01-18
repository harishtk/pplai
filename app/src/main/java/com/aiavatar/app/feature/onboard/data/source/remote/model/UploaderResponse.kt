package com.aiavatar.app.feature.onboard.data.source.remote.model

import com.aiavatar.app.feature.onboard.data.source.remote.dto.UploadImageDataDto
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UploaderResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: UploadImageDataDto?
) : Serializable
