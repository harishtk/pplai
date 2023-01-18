package com.aiavatar.app.core.data.source.remote.dto

import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.google.gson.annotations.SerializedName

data class CreateModelRequestDto(
    @SerializedName("foldername")
    val folderName: String,
    @SerializedName("trainingType")
    val trainingType: String,
    @SerializedName("files")
    val files: List<String>,
    @SerializedName("fcm")
    val fcm: String
)

fun CreateModelRequest.asDto(): CreateModelRequestDto {
    return CreateModelRequestDto(
        folderName = folderName,
        trainingType = trainingType,
        files = files,
        fcm = fcm
    )
}
