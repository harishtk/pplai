package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.AvatarFile

@Entity(
    tableName = AvatarFilesTable.name,
    indices = [
        Index(name = "status_id_index", value = [AvatarFilesTable.Columns.AVATAR_STATUS_ID]),
        Index(
            name = "remote_uri_index",
            value = [AvatarFilesTable.Columns.REMOTE_FILE],
            unique = true
        )
    ],
    foreignKeys = [
        ForeignKey(
            entity = AvatarStatusEntity::class,
            parentColumns = [AvatarStatusTable.Columns.ID],
            childColumns = [AvatarFilesTable.Columns.AVATAR_STATUS_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AvatarFilesEntity(
    @ColumnInfo("avatar_status_id")
    val avatarStatusId: Long,
    @ColumnInfo("remote_file")
    val remoteFile: String,
    @ColumnInfo("local_uri")
    val localUri: String,
    @ColumnInfo("downloaded")
    val downloaded: Int = 0,
    @ColumnInfo("progress")
    val progress: Int = 0,
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("downloaded_at")
    var downloadedAt: Long? = null

    @ColumnInfo("downloaded_size")
    var downloadedSize: Int? = null
}

fun AvatarFilesEntity.toAvatarFile(): AvatarFile {
    return AvatarFile(
        avatarStatusId = avatarStatusId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress
    ).also {
        it.id = _id
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

fun AvatarFile.toEntity(): AvatarFilesEntity {
    return AvatarFilesEntity(
        avatarStatusId = avatarStatusId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress
    ).also {
        it._id = id
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

object AvatarFilesTable {
    const val name = AppDatabase.TABLE_AVATAR_FILES

    object Columns {
        const val ID = "id"
        const val AVATAR_STATUS_ID = "avatar_status_id"
        const val REMOTE_FILE = "remote_file"
        const val LOCAL_URI = "local_uri"
        const val DOWNLOADED = "downloaded"
        const val PROGRESS = "progress"
        const val DOWNLOADED_AT = "downloaded_at"
        const val DOWNLOAD_SIZE = "download_size"
    }

}
