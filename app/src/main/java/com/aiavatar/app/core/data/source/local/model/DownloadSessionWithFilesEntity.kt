package com.aiavatar.app.core.data.source.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.core.domain.model.DownloadSessionWithFiles
import com.aiavatar.app.core.domain.model.UploadSessionWithFiles

data class DownloadSessionWithFilesEntity(
    @Embedded val downloadSessionEntity: DownloadSessionEntity,
    @Relation(
        parentColumn = DownloadSessionTable.Columns.ID,
        entityColumn = DownloadFilesTable.Columns.SESSION_ID
    )
    val downloadFilesEntity: List<DownloadFilesEntity>
)

fun DownloadSessionWithFilesEntity.toDownloadSessionWithFiles(): DownloadSessionWithFiles {
    return DownloadSessionWithFiles(
        downloadSession = downloadSessionEntity.toDownloadSession(),
        downloadFiles = downloadFilesEntity.map { it.toDownloadFile() }
    )
}
