package com.aiavatar.app.feature.home.data.source.remote.model.dto

import com.aiavatar.app.feature.home.domain.model.PurchasePlanData
import com.google.gson.annotations.SerializedName

data class PurchasePlanDataDto(
    @SerializedName("statusId")
    val avatarStatusId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("eta")
    val eta: Int,
    @SerializedName("modelId")
    val modelId: String,
)

fun PurchasePlanDataDto.toPurchasePlanData(): PurchasePlanData {
    return PurchasePlanData(
        avatarStatusId = avatarStatusId,
        userId = userId,
        eta = eta,
        modelId = modelId
    )
}