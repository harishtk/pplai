package com.aiavatar.app.feature.home.data.source.remote.model.dto

import android.graphics.ColorSpace.Model
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.google.gson.annotations.SerializedName

data class ModelDataDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("latestImage")
    val latestImage: String,
    @SerializedName("totalCnt")
    val totalCount: Int,
    @SerializedName("paid")
    val paid: Boolean,
    @SerializedName("renamed")
    val renamed: Boolean
)

fun ModelDataDto.toModelData(): ModelData {
    return ModelData(
        id = id,
        name = name,
        latestImage = latestImage,
        totalCount = totalCount,
        paid = paid,
        renamed = renamed
    )
}