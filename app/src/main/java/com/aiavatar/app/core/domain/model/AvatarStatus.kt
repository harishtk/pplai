package com.aiavatar.app.core.domain.model

data class AvatarStatus(
    val modelStatus: String,
    val totalAiCount: Int,
    val generatedAiCount: Int,
    val generatedImages: List<String>,
    val modelPay: Boolean,
    val modelName: Boolean,
    val modelId: String
)