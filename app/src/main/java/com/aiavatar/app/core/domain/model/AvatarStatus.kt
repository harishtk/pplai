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

    internal companion object {
        fun emptyStatus(id: Long): AvatarStatus =
            AvatarStatus("unknown", 0, 0, false, false,
            "", 300)
    }
}