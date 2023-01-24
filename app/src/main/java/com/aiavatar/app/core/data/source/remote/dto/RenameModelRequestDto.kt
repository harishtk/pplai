package com.aiavatar.app.core.data.source.remote.dto

import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.google.gson.annotations.SerializedName

data class RenameModelRequestDto(
    @SerializedName("modelId")
    val modelId: String,
    @SerializedName("modelName")
    val modelName: String
)

fun RenameModelRequest.asDto(): RenameModelRequestDto {
    return RenameModelRequestDto(
        modelId = modelId,
        modelName = modelName
    )
}
