package com.aiavatar.app.feature.onboard.domain.model.request

import com.google.gson.annotations.SerializedName

data class SocialLoginRequest(
    val accountType: String,
    val accountId: String,
    val email: String,
    val guestUserId: String,
    val platform: String,
    val fcm: String
)
