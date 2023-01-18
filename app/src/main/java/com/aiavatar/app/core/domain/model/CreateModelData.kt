package com.aiavatar.app.core.domain.model

data class CreateModelData(
    val statusId: Int,
    val guestUserId: String,
    val eta: Long
)