package com.pepulai.app.feature.home.presentation.util

import androidx.annotation.DrawableRes

data class SettingsItem(
    val settingsListType: SettingsListType,
    val id: Int,
    val title: String,
    @DrawableRes val icon: Int?,
    val description: String?,
)

enum class SettingsListType {
    SIMPLE
}