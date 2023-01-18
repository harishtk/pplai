package com.aiavatar.app.core.data.source.remote.model

import com.aiavatar.app.core.domain.model.AvatarStatus
import com.google.gson.annotations.SerializedName

data class AvatarStatusResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: AvatarStatusDto?
)

data class AvatarStatusDto(
    @SerializedName("modelStatus")
    val modelStatus: String,
    @SerializedName("totalAiCount")
    val totalAiCount: Int,
    @SerializedName("generatedAiCount")
    val generatedAiCount: Int,
    @SerializedName("generatedImages")
    val generatedImages: List<String>,
    @SerializedName("modelPay")
    val modelPay: Boolean,
    @SerializedName("modelname")
    val modelName: Boolean,
    @SerializedName("modelId")
    val modelId: String
)

fun AvatarStatusDto.toAvatarStatus(): AvatarStatus {
    return AvatarStatus(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        generatedImages = generatedImages,
        modelPay = modelPay,
        modelName = modelName,
        modelId = modelId
    )
}