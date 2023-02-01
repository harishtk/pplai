package com.aiavatar.app.feature.home.data.source.remote.model

import com.aiavatar.app.feature.home.data.source.remote.model.dto.PurchasePlanDataDto
import com.google.gson.annotations.SerializedName

data class PurchasePlanResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val purchasePlanDataDto: PurchasePlanDataDto?
)

