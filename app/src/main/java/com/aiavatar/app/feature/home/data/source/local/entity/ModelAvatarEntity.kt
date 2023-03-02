package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.feature.home.domain.model.ModelAvatar

@Entity(tableName = ModelAvatarTable.name)
data class ModelAvatarEntity(
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

    @ColumnInfo("file_size")
    var fileSize: Int? = null

    @ColumnInfo("thumbnail")
    var thumbnail: String? = null
}

fun ModelAvatarEntity.toModelAvatar(): ModelAvatar {
    return ModelAvatar(
        modelId = modelId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress,
    ).also {
        it._id = _id
        it.downloadedSize = downloadedSize
        it.downloadedAt = downloadedAt
        it.fileSize = fileSize
        it.thumbnail = thumbnail
    }
}

fun ModelAvatar.toEntity(): ModelAvatarEntity {
    return ModelAvatarEntity(
        modelId = modelId,
        remoteFile = remoteFile,
        localUri = localUri,
        downloaded = downloaded,
        progress = progress
    ).also {
        it._id = _id
        it.downloadedSize = downloadedSize
        it.downloadedAt = downloadedAt
        it.fileSize = fileSize
        it.thumbnail = thumbnail
    }
}

object ModelAvatarTable {
    const val name: String = AppDatabase.TABLE_MODEL_AVATARS

    object Columns {
        const val ID                = "id"
        const val MODEL_ID          = "model_id"
        const val REMOTE_FILE       = "remote_file"
        const val LOCAL_URI         = "local_uri"
        const val DOWNLOADED        = "downloaded"
        const val PROGRESS          = "progress"
        const val DOWNLOADED_AT     = "downloaded_at"
        const val DOWNLOADED_SIZE   = "downloaded_size"
        const val FILE_SIZE         = "file_size"
        const val THUMBNAIL         = "thumbnail"
    }
}
