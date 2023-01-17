package com.aiavatar.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.home.domain.model.UserModel

data class UserModelDto(
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userImage")
    val userImage: String
)

fun UserModelDto.toUserModel(): UserModel {
    return UserModel(
        userName = userName,
        userImage = userImage
    )
}