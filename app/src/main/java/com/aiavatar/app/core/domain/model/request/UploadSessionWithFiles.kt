package com.aiavatar.app.core.domain.model.request

import android.net.Uri
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus

data class UploadSessionWithFiles(
    val uploadSession: UploadSession,
    val uploadFiles: List<UploadFile>
)

data class UploadSession(
    val id: Long?,
    val modelId: String?,
    val createdAt: Long,
    val status: UploadSessionStatus = UploadSessionStatus.UNKNOWN,
    val folderName: String,
    val trainingType: String
)

data class UploadFile(
    val id: Long?,
    val sessionId: Long,
    val fileUri: Uri,
    val status: UploadFileStatus = UploadFileStatus.UNKNOWN,
    val progress: Int,
    val uploadedFileName: String?,
    val uploadedAt: Long?
)