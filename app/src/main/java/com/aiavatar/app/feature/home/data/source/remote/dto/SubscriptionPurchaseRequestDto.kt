package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.google.gson.annotations.SerializedName

data class SubscriptionPurchaseRequestDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("modelId")
    val modelId: String,
    @SerializedName("token")
    val purchaseToken: String,
)

fun SubscriptionPurchaseRequest.asDto(): SubscriptionPurchaseRequestDto {
    return SubscriptionPurchaseRequestDto(
        id = id,
        modelId = modelId,
        purchaseToken = purchaseToken
    )
}