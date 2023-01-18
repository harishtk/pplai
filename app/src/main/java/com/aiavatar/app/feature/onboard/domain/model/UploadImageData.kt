package com.aiavatar.app.feature.onboard.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UploadImageData(
    val imageName: String
) : Parcelable
