package com.aiavatar.app.core.data.source.remote.model

import com.aiavatar.app.core.domain.model.CreateModelData
import com.google.gson.annotations.SerializedName

data class CreateModelResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: CreateModelDataDto?
)

data class CreateModelDataDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("userId")
    val guestUserId: String,
    @SerializedName("eta")
    val eta: Long
)

fun CreateModelDataDto.toCreateModelData(): CreateModelData {
    return CreateModelData(
        statusId = id,
        guestUserId = guestUserId,
        eta = eta
    )
}