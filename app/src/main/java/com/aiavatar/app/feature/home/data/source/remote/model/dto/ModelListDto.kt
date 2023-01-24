package com.aiavatar.app.feature.home.data.source.remote.model.dto

import com.aiavatar.app.feature.home.domain.model.ModelList
import com.google.gson.annotations.SerializedName

data class ModelListDto(
    @SerializedName("statusId")
    val statusId: String,
    @SerializedName("data")
    val modelDataDto: ModelDataDto?
)

fun ModelListDto.toModelList(): ModelList {
    return ModelList(
        statusId = statusId,
        modelData = modelDataDto?.toModelData()
    )
}