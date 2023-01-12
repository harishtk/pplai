package com.pepulai.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.onboard.domain.model.request.LoginRequest
import com.pepulai.app.nullAsEmpty

data class LoginRequestDto(
    @SerializedName("emailAddress")
    val email: String,
    @SerializedName("callFor")
    val callFor: String,
    @SerializedName("otp")
    val otp: String,
    @SerializedName("device")
    val platform: String
)

fun LoginRequest.asDto(): LoginRequestDto {
    return LoginRequestDto(
        email = email,
        callFor = callFor,
        otp = otp.nullAsEmpty(),
        platform = platform,
    )
}