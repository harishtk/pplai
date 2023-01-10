package com.pepulai.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.onboard.domain.model.request.LoginRequest

data class LoginRequestDto(
    @SerializedName("userName")
    val userName: String
)

fun LoginRequest.asDto(): LoginRequestDto {
    return LoginRequestDto(userName = userName)
}