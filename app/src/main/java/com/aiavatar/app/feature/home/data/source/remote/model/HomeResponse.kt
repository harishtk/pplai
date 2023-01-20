package com.aiavatar.app.feature.home.data.source.remote.model

import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.model.ListAvatar
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
        @SerializedName("avatars")
        val avatars: List<ListAvatarDto>?
    )
}

data class ListAvatarDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("category")
    val categoryName: String?,
    @SerializedName("imageName")
    val imageName: String,
)

fun ListAvatarDto.toCategory(): Category {
    return Category(
        id = id,
        categoryName = categoryName,
        imageName = imageName
    )
}

fun ListAvatarDto.toListAvatar(): ListAvatar {
    return ListAvatar(
        id = id,
        categoryName = categoryName,
        imageName = imageName
    )
}


