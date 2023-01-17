package com.aiavatar.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.aiavatar.app.feature.home.domain.model.Category

data class CategoryDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("preset")
    val preset: List<CategoryPresetDto>
)

fun CategoryDto.toCategory(): Category {
    return Category(
        title = title,
        preset = preset.map { it.toCategoryPreset() }
    )
}