package com.pepulai.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.onboard.domain.model.LoginData

data class LoginDataDto(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("deviceToken")
    val deviceToken: String
)

fun LoginDataDto.toLoginData(): LoginData {
    return LoginData(
        userId = userId,
        deviceToken = deviceToken
    )
}


