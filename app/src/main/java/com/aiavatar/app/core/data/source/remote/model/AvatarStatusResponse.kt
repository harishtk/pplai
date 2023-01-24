package com.aiavatar.app.core.data.source.remote.model

import com.aiavatar.app.core.domain.model.AvatarFile
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.aiavatar.app.feature.home.data.source.remote.model.ListAvatarDto
import com.google.gson.annotations.SerializedName

data class AvatarStatusResponse(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: AvatarStatusDto?
)

data class AvatarStatusDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("modelStatus")
    val modelStatus: String,
    @SerializedName("totalAiCount")
    val totalAiCount: Int,
    @SerializedName("generatedAiCount")
    val generatedAiCount: Int,
    @SerializedName("generatedImages")
    val generatedImages: List<ListAvatarDto>,
    @SerializedName("paid")
    val paid: Boolean,
    @SerializedName("renamed")
    val modelRenamedByUser: Boolean,
    @SerializedName("modelId")
    val modelId: String,
    @SerializedName("eta")
    val eta: Int,
)

fun AvatarStatusDto.toAvatarStatus(): AvatarStatus {
    return AvatarStatus(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        paid = paid,
        modelRenamedByUser = modelRenamedByUser,
        modelId = modelId,
        eta = eta
    ).also {
        it.avatarStatusId = id
    }
}

fun AvatarStatusDto.toAvatarFiles(): List<AvatarFile> {
    return generatedImages.map { listAvatar ->
        AvatarFile(
            modelId = modelId,
            remoteFile = listAvatar.imageName,
            localUri = "",
        )
    }
}

fun AvatarStatusDto.toAvatarStatusWithFiles(): AvatarStatusWithFiles {
    return AvatarStatusWithFiles(
        avatarStatus = toAvatarStatus(),
        avatarFiles = toAvatarFiles()
    )
}