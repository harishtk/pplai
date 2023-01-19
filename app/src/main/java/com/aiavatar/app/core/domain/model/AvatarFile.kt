package com.aiavatar.app.core.domain.model

data class AvatarFile(
    val avatarStatusId: Long,
    val remoteFile: String,
    val localUri: String,
    val downloaded: Int = 0,
    val progress: Int = 0,
) {
    var id: Long? = null
    var downloadedAt: Long? = null
    var downloadedSize: Int? = null
}