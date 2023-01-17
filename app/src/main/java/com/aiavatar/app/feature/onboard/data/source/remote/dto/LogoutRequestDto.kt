package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.onboard.domain.model.request.LogoutRequest

data class LogoutRequestDto(
    @SerializedName("userId")
    val userId: String
)

fun LogoutRequest.asDto(): LogoutRequestDto {
    return LogoutRequestDto(
        userId = userId
    )
}