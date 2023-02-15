package com.aiavatar.app.feature.home.domain.model.request

data class SubscriptionLogRequest(
    val transactionId: String,
    val purchaseToken: String,
    val paymentStatus: String
)