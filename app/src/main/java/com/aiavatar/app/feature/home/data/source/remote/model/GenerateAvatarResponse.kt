package com.aiavatar.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class GenerateAvatarResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("id")
        val id: Long
    )
}
