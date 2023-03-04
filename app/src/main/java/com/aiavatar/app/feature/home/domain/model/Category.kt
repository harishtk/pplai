package com.aiavatar.app.feature.home.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class Category(
    val categoryName: String,
    val imageName: String
) {
    var id: Long? = null
    var thumbnail: String? = null
}