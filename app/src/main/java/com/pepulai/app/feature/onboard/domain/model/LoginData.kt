package com.pepulai.app.feature.onboard.domain.model

data class LoginData(
    val loginUser: LoginUser?,
    val deviceToken: String?
)

data class LoginUser(
    val userId: String,
    val username: String,
)