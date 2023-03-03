package com.aiavatar.app.feature.home.data.source.remote.model.dto

import android.graphics.ColorSpace.Model
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.aiavatar.app.nullAsEmpty
import com.google.gson.annotations.SerializedName

data class ModelDataDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("latestImage")
    val latestImage: String?,
    @SerializedName("totalCnt")
    val totalCount: Int,
    @SerializedName("paid")
    val paid: Boolean,
    @SerializedName("renamed")
    val renamed: Boolean,
    @SerializedName("updatedAt")
    val updatedAt: String
) {
    @SerializedName("thumbnail")
    var thumbnail: String? = null
}

fun ModelDataDto.toModelData(statusId: String): ModelData {
    return ModelData(
        id = id,
        name = name,
        latestImage = latestImage.nullAsEmpty(),
        totalCount = totalCount,
        paid = paid,
        renamed = renamed,
        updatedAt = updatedAt
    ).also {
        it.statusId = statusId
        it.thumbnail = thumbnail
    }
}