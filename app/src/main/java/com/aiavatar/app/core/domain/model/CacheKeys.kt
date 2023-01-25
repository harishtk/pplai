package com.aiavatar.app.core.domain.model

data class CacheKeys(
    val key: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    var id: Long? = null
}