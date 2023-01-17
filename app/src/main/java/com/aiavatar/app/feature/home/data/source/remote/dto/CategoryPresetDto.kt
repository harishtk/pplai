package com.aiavatar.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.home.domain.model.CategoryPreset

data class CategoryPresetDto(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("imageUrl")
    val imageUrl: String
)

fun CategoryPresetDto.toCategoryPreset(): CategoryPreset {
    return CategoryPreset(
        prompt = prompt,
        imageUrl = imageUrl
    )
}