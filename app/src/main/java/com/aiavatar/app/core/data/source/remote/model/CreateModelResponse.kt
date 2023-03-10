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
    @SerializedName("statusId")
    val id: String,
    @SerializedName("userId")
    val guestUserId: Long?,
    @SerializedName("eta")
    val eta: Long,
    @SerializedName("modelId")
    val modelId: String,
)

fun CreateModelDataDto.toCreateModelData(): CreateModelData {
    return CreateModelData(
        statusId = id,
        modelId = modelId,
        guestUserId = guestUserId,
        eta = eta
    )
}