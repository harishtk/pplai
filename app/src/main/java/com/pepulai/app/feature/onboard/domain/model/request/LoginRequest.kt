package com.pepulai.app.feature.onboard.domain.model.request

data class LoginRequest(
    val email: String,
    val callFor: String,
    val platform: String,
) {
    var otp: String? = null
}

