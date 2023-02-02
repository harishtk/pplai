package com.aiavatar.app.core.data.source.local.entity

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.UploadFile

@Entity(
    tableName = UploadFilesTable.name,
    indices = [Index(name = "upload_session_index", value = [UploadFilesTable.Columns.SESSION_ID])],
    foreignKeys = [
        ForeignKey(
            entity = UploadSessionEntity::class,
            parentColumns = [UploadSessionTable.Columns.ID],
            childColumns = [UploadFilesTable.Columns.SESSION_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UploadFilesEntity(
    @ColumnInfo("session_id")
    val sessionId: Long,
    @ColumnInfo("file_uri_string")
    val fileUriString: String,
    @ColumnInfo("local_uri_string")
    val localUriString: String,
    @ColumnInfo("status")
    val status: Int,
    @ColumnInfo("progress")
    val progress: Int,
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("uploaded_filename")
    var uploadedFileName: String? = null

    @ColumnInfo("uploaded_at")
    var uploadedAt: Long? = null

}

object UploadFilesTable {
    const val name: String = AppDatabase.TABLE_UPLOAD_FILES

    object Columns {
        const val ID                = "id"
        const val SESSION_ID        = "session_id"
        const val FILE_URI_STRING   = "file_uri_string"
        const val LOCAL_URI_STRING  = "local_uri_string"
        const val STATUS            = "status"
        const val PROGRESS          = "progress"
        const val UPLOADED_FILENAME = "uploaded_filename"
        const val UPLOADED_AT       = "uploaded_at"
    }
}

fun UploadFilesEntity.toUploadFile(): UploadFile {
    return UploadFile(
        id = _id,
        sessionId = sessionId,
        fileUri = fileUriString.toUri(),
        localUri = localUriString.toUri(),
        status = UploadFileStatus.fromRawValue(status),
        progress = progress,
        uploadedFileName = uploadedFileName,
        uploadedAt = uploadedAt
    )
}

enum class UploadFileStatus(val status: Int) {
    NOT_STARTED(0), UPLOADING(1), COMPLETE(2), FAILED(3), UNKNOWN(-1);

    companion object  {
        fun fromRawValue(rawValue: Int): UploadFileStatus {
            return when (rawValue) {
                0 -> NOT_STARTED
                1 -> UPLOADING
                2 -> COMPLETE
                3 -> FAILED
                else -> UNKNOWN
            }
        }
    }
}