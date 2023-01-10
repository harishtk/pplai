package com.pepulai.app.feature.home.domain.model

import com.google.gson.annotations.SerializedName

data class UserAndCategory(
    val users: List<UserModel>,
    val categories: List<Category>
)

data class Category(
    @SerializedName("title")
    val title: String,
    @SerializedName("preset")
    val preset: List<CategoryPreset>
)

data class CategoryPreset(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("imageUrl")
    val imageUrl: String
)

data class UserModel(
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userImage")
    val userImage: String
)