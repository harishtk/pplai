package com.pepulai.app.feature.home.data.source.remote.model

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.home.data.source.remote.dto.CategoryDto
import com.pepulai.app.feature.home.data.source.remote.dto.UserModelDto
import com.pepulai.app.feature.home.domain.model.Avatar

data class HomeResponseOld(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("avatars")
        val userModel: List<UserModelDto>?,
        @SerializedName("categories")
        val categories: List<CategoryDto>?
    )
}

data class HomeResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("avatars")
        val avatars: List<AvatarDto>?
    )
}

data class AvatarDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("category")
    val categoryName: String,
    @SerializedName("imageUrl")
    val imageUrl: String,
)

fun AvatarDto.toAvatar(): Avatar {
    return Avatar(
        id = id,
        categoryName = categoryName,
        imageUrl = imageUrl
    )
}

