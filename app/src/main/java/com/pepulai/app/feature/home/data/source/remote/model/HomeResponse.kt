package com.pepulai.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName

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

data class CategoryDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("preset")
    val preset: List<CategoryPresetDto>
)

data class CategoryPresetDto(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("imageUrl")
    val imageUrl: String
)

data class UserModelDto(
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userImage")
    val userImage: String
)