package com.pepulai.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.home.data.source.remote.dto.SubscriptionPlanDto

data class SubscriptionPlanResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("plans")
        val plans: List<SubscriptionPlanDto>
    )
}

