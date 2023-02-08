package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.request.GetShareLinkRequest
import com.google.gson.annotations.SerializedName

data class GetShareLinkRequestDto(
    @SerializedName("modelId")
    val modelId: String
)

fun GetShareLinkRequest.asDto(): GetShareLinkRequestDto {
    return GetShareLinkRequestDto(
        modelId = modelId
    )
}