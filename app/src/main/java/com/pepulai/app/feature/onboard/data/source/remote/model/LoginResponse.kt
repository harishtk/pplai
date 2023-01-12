package com.pepulai.app.feature.onboard.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.onboard.data.source.remote.dto.LoginDataDto

data class LoginResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val loginDataDto: LoginDataDto?
)

