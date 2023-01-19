package com.aiavatar.app.core.domain.model

data class AvatarStatusWithFiles(
    val avatarStatus: AvatarStatus,
    val avatarFiles: List<AvatarFile>
)

