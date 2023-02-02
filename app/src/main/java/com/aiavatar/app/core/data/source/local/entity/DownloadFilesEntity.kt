package com.aiavatar.app.core.data.source.local.entity

import androidx.core.net.toUri
import androidx.room.*
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.DownloadFile

@Entity(
    tableName = DownloadFilesTable.name,
    indices = [Index(name = "download_session_index", value = [DownloadFilesTable.Columns.SESSION_ID])],
    foreignKeys = [
        ForeignKey(
            entity = DownloadSessionEntity::class,
            parentColumns = [DownloadSessionTable.Columns.ID],
            childColumns = [DownloadFilesTable.Columns.SESSION_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadFilesEntity(
    @ColumnInfo("session_id")
    val sessionId: Long,
    @ColumnInfo("file_uri_string")
    val fileUriString: String,
    @ColumnInfo("status")
    val status: Int,
    @ColumnInfo("downloaded")
    val downloaded: Int = 0,
    @ColumnInfo("progress")
    val progress: Int,
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("downloaded_filename")
    var downloadedFileName: String? = null

    @ColumnInfo("downloaded_at")
    var downloadedAt: Long? = null

    @ColumnInfo("downloaded_size")
    var downloadedSize: Long? = null
}

fun DownloadFilesEntity.toDownloadFile(): DownloadFile {
    return DownloadFile(
        sessionId = sessionId,
        fileUri = fileUriString.toUri(),
        status = DownloadFileStatus.fromRawValue(status),
        progress = progress,
        downloaded = downloaded,
    ).also {
        it.id = _id
        it.downloadedFileName = downloadedFileName
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

fun DownloadFile.toEntity(): DownloadFilesEntity {
    return DownloadFilesEntity(
        sessionId = sessionId,
        fileUriString = fileUri.toString(),
        status = status.status,
        progress = progress,
        downloaded = downloaded
    ).also {
        it._id = id
        it.downloadedFileName = downloadedFileName
        it.downloadedAt = downloadedAt
        it.downloadedSize = downloadedSize
    }
}

object DownloadFilesTable {
    const val name: String = AppDatabase.TABLE_DOWNLOAD_FILES

    object Columns {
        const val ID                    = "id"
        const val SESSION_ID            = "session_id"
        const val FILE_URI_STRING       = "file_uri_string"
        const val STATUS                = "status"
        const val PROGRESS              = "progress"
        const val DOWNLOADED            = "downloaded"
        const val DOWNLOADED_FILENAME   = "downloaded_filename"
        const val DOWNLOADED_AT         = "downloaded_at"
        const val DOWNLOADED_SIZE       = "downloaded_size"
    }
}

enum class DownloadFileStatus(val status: Int) {
    NOT_STARTED(0), DOWNLOADING(1), COMPLETE(2), FAILED(3), UNKNOWN(-1);

    companion object  {
        fun fromRawValue(rawValue: Int): DownloadFileStatus {
            return when (rawValue) {
                0 -> NOT_STARTED
                1 -> DOWNLOADING
                2 -> COMPLETE
                3 -> FAILED
                else -> UNKNOWN
            }
        }
    }
}