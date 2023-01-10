package com.pepulai.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.home.data.source.remote.dto.CategoryDto
import com.pepulai.app.feature.home.data.source.remote.dto.UserModelDto

data class HomeResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        val userModel: List<UserModelDto>?,
        val categories: List<CategoryDto>?
    )
}

