package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.google.gson.annotations.SerializedName

data class GetAvatarsRequestDto(
    @SerializedName("modelId")
    val modelId: String
)

fun GetAvatarsRequest.asDto(): GetAvatarsRequestDto {
    return GetAvatarsRequestDto(
        modelId = modelId
    )
}