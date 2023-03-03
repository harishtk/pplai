package com.aiavatar.app.feature.home.domain.model

import timber.log.Timber

data class ModelAvatar(
    val modelId: String,
    val remoteFile: String,
    @Deprecated("might not be available always")
    val localUri: String,
    val downloaded: Int = 0,
    val progress: Int = 0,
) {
    var _id: Long? = null
    var downloadedAt: Long? = null
    var downloadedSize: Int? = null
    var fileSize: Int? = null
    var thumbnail: String? = null
}

fun ListAvatar.toModelAvatar(modelId: String): ModelAvatar {
    Timber.tag("Parse.Msg").d("ListAvatar#toModelAvatar: thumb = $thumbnail")
    return ModelAvatar(
        modelId = modelId,
        remoteFile = imageName,
        downloaded = 0,
        localUri = "",
        progress = 0,
    ).also {
        it._id = id
        it.fileSize = fileSize
        it.thumbnail = thumbnail
    }
}