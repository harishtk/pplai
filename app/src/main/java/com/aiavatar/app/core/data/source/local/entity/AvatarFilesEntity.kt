package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.AvatarFile
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.saveTempImage

@Entity(
    tableName = AvatarFilesTable.name,
    indices = [
        Index(name = "avatar_files_model_id", value = [AvatarFilesTable.Columns.MODEL_ID]),
        Index(
            name = "remote_uri_index",
            value = [AvatarFilesTable.Columns.REMOTE_FILE],
            unique = true
        )
    ],
    foreignKeys = [
        ForeignKey(
            entity = AvatarStatusEntity::class,
            parentColumns = [AvatarStatusTable.Columns.MODEL_ID],
            childColumns = [AvatarFilesTable.Columns.MODEL_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AvatarFilesEntity(
    @ColumnInfo("model_id")
    val modelId: String,
    @ColumnInfo("remote_file")
    val remoteFile: String,
    @ColumnInfo("local_uri")
    val localUri: String,
    @ColumnInfo("downloaded")
    val downloaded: Int = 0,
    @ColumnInfo("progress")
    val progress: Int = 0
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
        modelId = modelId,
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

fun AvatarFilesEntity.toModelAvatar(): ModelAvatar {
    return ModelAvatar(
        modelId = modelId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress
    ).also {
        it._id = _id
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

fun AvatarFile.toModelAvatar(): ModelAvatar {
    return ModelAvatar(
        modelId = modelId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress
    ).also {
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

fun AvatarFile.toEntity(): AvatarFilesEntity {
    return AvatarFilesEntity(
        modelId = modelId,
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

fun AvatarFile.toListAvatar(): ListAvatar {
    return ListAvatar(
        id = id!!,
        categoryName = null,
        imageName = remoteFile
    )
}

object AvatarFilesTable {
    const val name = AppDatabase.TABLE_AVATAR_FILES

    object Columns {
        const val ID = "id"
        const val MODEL_ID = "model_id"
        const val REMOTE_FILE = "remote_file"
        const val LOCAL_URI = "local_uri"
        const val DOWNLOADED = "downloaded"
        const val PROGRESS = "progress"
        const val DOWNLOADED_AT = "downloaded_at"
        const val DOWNLOADED_SIZE = "downloaded_size"
    }

}
