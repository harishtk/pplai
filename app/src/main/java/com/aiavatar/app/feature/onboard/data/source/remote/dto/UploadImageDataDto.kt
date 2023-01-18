package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.UploadImageData
import com.aiavatar.app.nullAsEmpty
import com.google.gson.annotations.SerializedName

data class UploadImageDataDto(
    @SerializedName("imageName")
    val imageName: String?
)

fun UploadImageDataDto.asUploadImageData(): UploadImageData =
    UploadImageData(
        imageName = imageName.nullAsEmpty()
    )
