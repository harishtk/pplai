package com.aiavatar.app.core.domain.model

data class AvatarStatus(
    val modelStatus: String,
    val totalAiCount: Int,
    val generatedAiCount: Int,
    val paid: Boolean,
    val modelRenamedByUser: Boolean,
    val modelId: String,
    val eta: Int,
) {
    var id: Long? = null
    var modelName: String? = null
    var avatarStatusId: Long? = null

    internal companion object {
        fun emptyStatus(modelId: String): AvatarStatus =
            AvatarStatus(
                modelStatus = "unknown",
                totalAiCount = 0,
                generatedAiCount = 0,
                paid = false,
                modelRenamedByUser = false,
                modelId = modelId,
                eta = 300
            )
    }
}