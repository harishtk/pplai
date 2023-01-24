package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.data.source.remote.dto.LoginUserDto
import com.aiavatar.app.feature.onboard.domain.model.AutoLoginData
import com.google.gson.annotations.SerializedName

data class AutoLoginDataDto(
    @SerializedName("forceUpdate")
    val forceUpdate: Boolean,
    @SerializedName("loginUser")
    val loginUserDto: LoginUserDto
)

fun AutoLoginDataDto.toAutoLoginData(): AutoLoginData {
    return AutoLoginData(
        forceUpdate = forceUpdate,
        loginUser = loginUserDto.toLoginUser()
    )
}