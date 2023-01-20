package com.aiavatar.app.feature.home.presentation.util

import androidx.annotation.DrawableRes

data class SettingsItem(
    val settingsListType: SettingsListType,
    val id: Int,
    val title: String,
    @DrawableRes val icon: Int?,
    val description: String?,
    val hasMore: Boolean = false
)

enum class SettingsListType {
    SIMPLE
}