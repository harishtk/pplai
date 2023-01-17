package com.aiavatar.app.core.data.source.remote.dto

import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.google.gson.annotations.SerializedName

data class SendFcmTokenRequestDto(
    @SerializedName("tempUserId")
    val tempUserId: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("platform")
    val platform: String
)

fun SendFcmTokenRequest.asDto(): SendFcmTokenRequestDto {
    return SendFcmTokenRequestDto(
        tempUserId = tempUserId,
        token = token,
        platform = platform
    )
}
