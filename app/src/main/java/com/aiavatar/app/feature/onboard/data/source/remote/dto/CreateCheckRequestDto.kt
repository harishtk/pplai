package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import com.google.gson.annotations.SerializedName

data class CreateCheckRequestDto(
    @SerializedName("timestamp")
    val timestamp: Long
)

fun CreateCheckRequest.asDto(): CreateCheckRequestDto {
    return CreateCheckRequestDto(
        timestamp = timestamp
    )
}