package com.aiavatar.app.core.data.source.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesTable
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarStatusTable
import com.aiavatar.app.core.data.source.local.entity.toAvatarFile
import com.aiavatar.app.core.data.source.local.entity.toAvatarStatus
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles

data class AvatarStatusWithFilesEntity(
    @Embedded
    val avatarStatusEntity: AvatarStatusEntity,
    @Relation(
        parentColumn = AvatarStatusTable.Columns.ID,
        entityColumn = AvatarFilesTable.Columns.AVATAR_STATUS_ID
    )
    val avatarFilesEntity: List<AvatarFilesEntity>
)

fun AvatarStatusWithFilesEntity.toAvatarStatusWithFiles(): AvatarStatusWithFiles {
    return AvatarStatusWithFiles(
        avatarStatus = avatarStatusEntity.toAvatarStatus(),
        avatarFiles = avatarFilesEntity.map { it.toAvatarFile() }
    )
}