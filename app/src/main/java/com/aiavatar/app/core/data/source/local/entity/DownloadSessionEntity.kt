package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.DownloadSession

@Entity(tableName = DownloadSessionTable.name)
data class DownloadSessionEntity(
    @ColumnInfo("created_at")
    val createdAt: Long,
    @ColumnInfo("status")
    val status: Int,
    @ColumnInfo("folder_name")
    val folderName: String,
) {
    @ColumnInfo("id")
    var _id: Long? = null

    @ColumnInfo("worker_id")
    var workerId: String? = null
}

fun DownloadSessionEntity.toDownloadSession(): DownloadSession {
    return DownloadSession(
        id = _id ?: -1L,
        createdAt = createdAt,
        status = DownloadSessionStatus.fromRawValue(status),
        folderName = folderName
    ).also {
        it.workerId = workerId
    }
}

object DownloadSessionTable {
    const val name: String = AppDatabase.TABLE_DOWNLOAD_SESSION

    object Columns {
        const val CREATED_AT            = "created_at"
        const val STATUS                = "status"
        const val ID                    = "id"
        const val FOLDER_NAME           = "folder_name"
        const val WORKER_ID             = "worker_id"
    }
}

enum class DownloadSessionStatus(val status: Int) {
    NOT_STARTED(0), PARTIALLY_DONE(1), COMPLETE(2),
    FAILED(5), UNKNOWN(-1);

    companion object {
        fun fromRawValue(rawValue: Int): DownloadSessionStatus {
            return when (rawValue) {
                0 -> NOT_STARTED
                1 -> PARTIALLY_DONE
                2 -> COMPLETE
                5 -> FAILED
                else -> UNKNOWN
            }
        }
    }
}
