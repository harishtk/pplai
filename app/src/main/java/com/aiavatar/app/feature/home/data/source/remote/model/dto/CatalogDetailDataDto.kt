package com.aiavatar.app.feature.home.data.source.remote.model.dto

import com.aiavatar.app.feature.home.data.source.remote.model.ListAvatarDto
import com.aiavatar.app.feature.home.data.source.remote.model.toListAvatar
import com.aiavatar.app.feature.home.domain.model.CatalogDetailData
import com.google.gson.annotations.SerializedName

data class CatalogDetailDataDto(
    @SerializedName("category")
    val category: String,
    @SerializedName("avatars")
    val avatars: List<ListAvatarDto>
)

fun CatalogDetailDataDto.toCatalogDetailData(): CatalogDetailData {
    return CatalogDetailData(
        category = category,
        avatars = avatars.map { it.toListAvatar() }
    )
}