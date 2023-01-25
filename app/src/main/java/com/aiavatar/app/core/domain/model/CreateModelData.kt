package com.aiavatar.app.core.domain.model

data class CreateModelData(
    val statusId: Long,
    val modelId: String,
    val guestUserId: Long?,
    val eta: Long
)