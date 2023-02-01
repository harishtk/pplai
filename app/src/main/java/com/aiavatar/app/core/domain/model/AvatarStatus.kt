package com.aiavatar.app.core.domain.model

data class AvatarStatus(
    val modelStatus: ModelStatus,
    val totalAiCount: Int,
    val generatedAiCount: Int,
    val paid: Boolean,
    val modelRenamedByUser: Boolean,
    val modelId: String,
    val eta: Int,
) {
    var id: Long? = null
    var modelName: String? = null
    var avatarStatusId: String? = null

    internal companion object {
        fun emptyStatus(modelId: String): AvatarStatus =
            AvatarStatus(
                modelStatus = ModelStatus.Default,
                totalAiCount = 0,
                generatedAiCount = 0,
                paid = false,
                modelRenamedByUser = false,
                modelId = modelId,
                eta = 300
            )
    }
}

enum class ModelStatus(val statusString: String) {
    TRAINING_PROCESSING("training_processing"),
    TRAINING_FAILED("training_failed"),
    AVATAR_PROCESSING("avatar_processing"),
    COMPLETED("completed"),
    UNKNOWN("unknown");

    companion object {
        internal val Default = UNKNOWN

        fun fromRawValue(rawValue: String): ModelStatus {
            return when (rawValue) {
                "training_processing" -> TRAINING_PROCESSING
                "training_failed" -> TRAINING_FAILED
                "avatar_processing" -> AVATAR_PROCESSING
                "completed" -> COMPLETED
                else -> UNKNOWN
            }
        }
    }
}