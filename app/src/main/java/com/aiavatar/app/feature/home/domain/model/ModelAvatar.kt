package com.aiavatar.app.feature.home.domain.model

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