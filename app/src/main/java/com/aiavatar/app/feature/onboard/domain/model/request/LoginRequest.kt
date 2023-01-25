package com.aiavatar.app.feature.onboard.domain.model.request

data class LoginRequest(
    val email: String,
    val guestUserId: Long,
    val callFor: String,
    val platform: String,
    val fcm: String,
) {
    var otp: String? = null
}

