package com.aiavatar.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class MyModelsResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    /*@SerializedName("data")
    val data: Data?*/
) {
    /*data class Data(
        @SerializedName("models")
        val models: List<ModelDataDto>
    )*/
}