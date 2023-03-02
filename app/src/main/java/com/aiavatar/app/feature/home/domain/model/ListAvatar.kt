package com.aiavatar.app.feature.home.domain.model

import com.aiavatar.app.core.domain.model.AvatarFile

data class ListAvatar(
    val id: Long,
    val categoryName: String?,
    val imageName: String,
    val fileSize: Int = 0,
    val thumbnail: String? = null,
)

fun ListAvatar.toAvatarFile(modelId: String): AvatarFile {
    return AvatarFile(
        modelId = modelId,
        remoteFile = imageName,
        localUri = "",
    )
}