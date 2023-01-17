package com.aiavatar.app.core.domain.model.request

data class SendFcmTokenRequest(
    val tempUserId: String,
    val token: String,
    val platform: String
)
