package com.aiavatar.app.feature.home.domain.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class UserAndCategory(
    val users: List<UserModel>,
    val categories: List<Category>
)

@Parcelize
data class Category(
    @SerializedName("title")
    val title: String,
    @SerializedName("preset")
    val preset: List<CategoryPreset>
) : Parcelable

@Parcelize
data class CategoryPreset(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("imageUrl")
    val imageUrl: String
) : Parcelable

data class UserModel(
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userImage")
    val userImage: String
)