package com.pepulai.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.home.domain.model.SubscriptionPlan

data class SubscriptionPlanDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("productId")
    val productId: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("variation")
    val variation: Int,
    @SerializedName("style")
    val style: Int,
    @SerializedName("photo")
    val photo: Int
)

fun SubscriptionPlanDto.toSubscriptionPlan(): SubscriptionPlan {
    return SubscriptionPlan(
        id = id,
        productId = productId,
        price = price,
        variation = variation,
        style = style,
        photo = photo
    )
}