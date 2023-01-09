package com.example.app.feature.onboard.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.example.app.feature.onboard.data.source.remote.dto.LoginDataDto
import com.example.app.feature.onboard.domain.model.LoginData

data class LoginResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val loginDataDto: LoginDataDto?
)

