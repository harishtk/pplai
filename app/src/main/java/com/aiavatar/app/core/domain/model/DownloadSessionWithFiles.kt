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
    val createdAt: Long,
    val status: DownloadSessionStatus = DownloadSessionStatus.UNKNOWN,
    val folderName: String,
) {
    var id:         Long?   = null
    var workerId:   String? = null
}

data class DownloadFile(
    val sessionId: Long,
    /**
     * Remote file uri
     */
    val fileUri: Uri,
    val localUri: Uri,
    val status: DownloadFileStatus = DownloadFileStatus.UNKNOWN,
    val progress: Int = 0,
    val downloaded: Int = 0,
) {
    var id:                 Long?   = null
    var downloadedFileName: String? = null
    var downloadedAt:       Long?   = null
    var downloadedSize:     Long?   = null
}
