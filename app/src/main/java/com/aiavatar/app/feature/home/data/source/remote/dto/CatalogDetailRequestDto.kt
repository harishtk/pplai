package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.google.gson.annotations.SerializedName

data class CatalogDetailRequestDto(
    @SerializedName("category")
    val category: String
)

fun CatalogDetailRequest.asDto(): CatalogDetailRequestDto {
    return CatalogDetailRequestDto(
        category = category
    )
}
