package com.aiavatar.app.feature.home.domain.model.request

data class SubscriptionPurchaseRequest(
    val id: String,
    val modelId: String,
    val purchaseToken: String? = null,
    val couponCode: String? = null
)