package com.aiavatar.app.feature.home.presentation.util

import com.aiavatar.app.BuildConfig
import com.aiavatar.app.feature.home.presentation.create.UploadStep2Fragment

object UploadUtil {

    fun getMinUploadImageCount(): Int {
        return if (BuildConfig.DEBUG) {
            1
        } else {
            UploadStep2Fragment.MIN_IMAGES
        }
    }
}