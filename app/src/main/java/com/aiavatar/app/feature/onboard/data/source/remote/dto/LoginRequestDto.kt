package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.onboard.domain.model.request.LoginRequest
import com.aiavatar.app.nullAsEmpty

data class LoginRequestDto(
    @SerializedName("emailAddress")
    val email: String,
    @SerializedName("guestUserId")
    val guestUserId: Long,
    @SerializedName("callFor")
    val callFor: String,
    @SerializedName("otp")
    val otp: String,
    @SerializedName("device")
    val platform: String,
    @SerializedName("fcm")
    val fcm: String
)

fun LoginRequest.asDto(): LoginRequestDto {
    return LoginRequestDto(
        email = email,
        callFor = callFor,
        guestUserId = guestUserId,
        otp = otp.nullAsEmpty(),
        platform = platform,
        fcm = fcm
    )
}