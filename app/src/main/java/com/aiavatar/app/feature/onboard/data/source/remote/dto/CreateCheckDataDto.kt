package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.google.gson.annotations.SerializedName

data class CreateCheckDataDto(
    @SerializedName("allowModelCreate")
    val allowModelCreate: Boolean,
    @SerializedName("siteDown")
    val siteDown: Boolean,
    @SerializedName("modelPay")
    val pendingModelPayment: Boolean
)

fun CreateCheckDataDto.toCreateCheckData(): CreateCheckData {
    return CreateCheckData(
        allowModelCreate = allowModelCreate,
        siteDown = siteDown,
        pendingModelPayment = pendingModelPayment
    )
}