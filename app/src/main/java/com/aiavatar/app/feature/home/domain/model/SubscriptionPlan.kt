package com.aiavatar.app.feature.home.domain.model

data class SubscriptionPlan(
    val id: Int,
    val productId: String,
    val price: String,
    val variation: Int,
    val style: Int,
    val photo: Int
)