package com.aiavatar.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName
import retrofit2.SkipCallbackExecutor

data class PurchasePlanResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {

    data class Data(
        @SerializedName("statusId")
        val avatarStatusId: String
    )
}
