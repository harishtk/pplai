package com.aiavatar.app.core.data.source.remote.model

import com.aiavatar.app.core.domain.model.AvatarFile
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
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
    val generatedImages: List<String>,
    @SerializedName("modelPaidOnce")
    val modelPaidOnce: Boolean,
    @SerializedName("userModelname")
    val userModelName: Boolean,
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
        modelPaidOnce = modelPaidOnce,
        userModelName = userModelName,
        modelId = modelId,
        eta = eta
    ).also {
        it.id = id
    }
}

fun AvatarStatusDto.toAvatarFiles(): List<AvatarFile> {
    return generatedImages.map { remoteImage ->
        AvatarFile(
            avatarStatusId = id,
            remoteFile = remoteImage,
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