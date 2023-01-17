package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.onboard.domain.model.LoginData
import com.aiavatar.app.feature.onboard.domain.model.LoginUser

data class LoginDataDto(
    @SerializedName("deviceToken")
    val deviceToken: String,
    @SerializedName("loginUser")
    val loginUserDto: LoginUserDto?
)

fun LoginDataDto.toLoginData(): LoginData {
    return LoginData(
        loginUser = loginUserDto?.toLoginUser(),
        deviceToken = deviceToken
    )
}

data class LoginUserDto(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("username")
    val username: String
)

fun LoginUserDto.toLoginUser(): LoginUser {
    return LoginUser(
        userId = userId,
        username = username
    )
}


