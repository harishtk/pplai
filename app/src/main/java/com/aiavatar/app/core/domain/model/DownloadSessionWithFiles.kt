package com.aiavatar.app.core.domain.model

import android.net.Uri
import com.aiavatar.app.core.data.source.local.entity.DownloadFileStatus
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus


data class DownloadSessionWithFiles(
    val downloadSession: DownloadSession,
    val downloadFiles: List<DownloadFile>
)

data class DownloadSession(
    val id: Long?,
    val createdAt: Long,
    val status: DownloadSessionStatus = DownloadSessionStatus.UNKNOWN,
    val folderName: String,
) {
    var workerId: String? = null
}

data class DownloadFile(
    val id: Long?,
    val sessionId: Long,
    /**
     * Remote file uri
     */
    val fileUri: Uri,
    val localUri: Uri,
    val status: DownloadFileStatus = DownloadFileStatus.UNKNOWN,
    val progress: Int,
    val downloaded: Int,
    val downloadedFileName: String?,
    val downloadedAt: Long?,
    val downloadedSize: Long?
)
