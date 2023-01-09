package com.example.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.app.feature.onboard.domain.model.request.AutoLoginRequest

data class AutoLoginRequestDto(
    @SerializedName("timestamp")
    val timestamp: Long
)

fun AutoLoginRequest.asDto(): AutoLoginRequestDto {
    return AutoLoginRequestDto(
        timestamp = timestamp
    )
}