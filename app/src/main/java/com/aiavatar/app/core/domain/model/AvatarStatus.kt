package com.aiavatar.app.core.domain.model

data class AvatarStatus(
    val modelStatus: String,
    val totalAiCount: Int,
    val generatedAiCount: Int,
    val modelPaidOnce: Boolean,
    val userModelName: Boolean,
    val modelId: String,
    val eta: Int,
) {
    var id: Long? = null

    internal companion object {
        fun emptyStatus(id: Long): AvatarStatus =
            AvatarStatus("unknown", 0, 0, false, false,
            "", 300)
    }
}