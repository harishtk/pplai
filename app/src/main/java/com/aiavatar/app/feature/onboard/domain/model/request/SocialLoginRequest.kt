package com.aiavatar.app.feature.onboard.domain.model.request

data class SocialLoginRequest(
    val accountType: String,
    val accountId: String,
    val email: String,
    val guestUserId: Long,
    val platform: String,
    val fcm: String
)
