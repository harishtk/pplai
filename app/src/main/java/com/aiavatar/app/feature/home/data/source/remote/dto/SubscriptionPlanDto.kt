package com.aiavatar.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan

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
    val photo: Int,
    @SerializedName("currencySymbol")
    val currencySymbol: String,
    @SerializedName("currencyCode")
    val currencyCode: String,
    @SerializedName("bestSeller")
    val bestSeller: Boolean
)

fun SubscriptionPlanDto.toSubscriptionPlan(): SubscriptionPlan {
    return SubscriptionPlan(
        id = id,
        productId = productId,
        price = price,
        variation = variation,
        style = style,
        photo = photo,
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        bestSeller = bestSeller
    )
}