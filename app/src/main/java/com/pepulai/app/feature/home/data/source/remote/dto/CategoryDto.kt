package com.pepulai.app.feature.home.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.pepulai.app.feature.home.domain.model.Category
import com.pepulai.app.feature.home.domain.model.CategoryPreset

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