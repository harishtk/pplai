package com.aiavatar.app.core.data.source.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.aiavatar.app.core.data.source.local.entity.UploadFilesEntity
import com.aiavatar.app.core.data.source.local.entity.UploadFilesTable
import com.aiavatar.app.core.data.source.local.entity.UploadSessionEntity
import com.aiavatar.app.core.data.source.local.entity.UploadSessionTable
import com.aiavatar.app.core.data.source.local.entity.toUploadFile
import com.aiavatar.app.core.data.source.local.entity.toUploadSession
import com.aiavatar.app.core.domain.model.request.UploadSessionWithFiles

data class UploadSessionWithFilesEntity(
    @Embedded val uploadSessionEntity: UploadSessionEntity,
    @Relation(
        parentColumn = UploadSessionTable.Columns.ID,
        entityColumn = UploadFilesTable.Columns.SESSION_ID
    )
    val uploadFilesEntity: List<UploadFilesEntity>
)

fun UploadSessionWithFilesEntity.toUploadSessionWithFiles(): UploadSessionWithFiles {
    return UploadSessionWithFiles(
        uploadSession = uploadSessionEntity.toUploadSession(),
        uploadFiles = uploadFilesEntity.map { it.toUploadFile() }
    )
}