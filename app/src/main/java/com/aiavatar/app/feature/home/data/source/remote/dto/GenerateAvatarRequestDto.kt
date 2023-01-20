package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.google.gson.annotations.SerializedName

data class GenerateAvatarRequestDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("modelId")
    val modelId: String
)

fun GenerateAvatarRequest.asDto(): GenerateAvatarRequestDto {
    return GenerateAvatarRequestDto(
        id = id,
        modelId = modelId
    )
}