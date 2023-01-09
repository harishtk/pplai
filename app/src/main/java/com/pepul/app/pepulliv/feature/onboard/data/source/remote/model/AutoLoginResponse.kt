package com.pepul.app.pepulliv.feature.onboard.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class AutoLoginResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String
)