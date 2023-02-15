package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.SubscriptionLogRequest
import com.google.gson.annotations.SerializedName

data class SubscriptionLogRequestDto(
    @SerializedName("transactionId")
    val transactionId: String,
    @SerializedName("token")
    val purchaseToken: String,
    @SerializedName("status")
    val paymentStatus: String,
)

fun SubscriptionLogRequest.asDto(): SubscriptionLogRequestDto {
    return SubscriptionLogRequestDto(
        transactionId = transactionId,
        purchaseToken = purchaseToken,
        paymentStatus = paymentStatus
    )
}

