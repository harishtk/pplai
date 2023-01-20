package com.aiavatar.app.feature.home.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: Long,
    val categoryName: String?,
    val imageName: String
) : Parcelable