package com.aiavatar.app.core.domain.model.request

data class CreateModelRequest(
    val folderName: String,
    val trainingType: String,
    val files: List<String>,
    val fcm: String
)


