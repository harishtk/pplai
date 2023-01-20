package com.aiavatar.app.feature.home.data.source.remote.model

import com.aiavatar.app.feature.home.data.source.remote.model.dto.CatalogDetailDataDto
import com.google.gson.annotations.SerializedName

data class CatalogDetailResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: CatalogDetailDataDto?
)

