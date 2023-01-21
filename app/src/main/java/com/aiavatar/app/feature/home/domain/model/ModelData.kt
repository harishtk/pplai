package com.aiavatar.app.feature.home.domain.model

data class ModelData(
    val id: String,
    val name: String,
    val latestImage: String,
    val totalCount: Int,
    val paid: Boolean,
    val renamed: Boolean
)