package com.aiavatar.app.feature.onboard.domain.model.request

import com.google.gson.annotations.SerializedName

data class GetShareLinkRequest(
    val modelId: String,
    val avatarId: String,
    val folderName: String,
    val fileName: String
)