package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.AvatarStatus

@Entity(tableName = AvatarStatusTable.name)
data class AvatarStatusEntity(
    @ColumnInfo("model_status")
    val modelStatus: String,
    @ColumnInfo("total_ai_count")
    val totalAiCount: Int,
    @ColumnInfo("generated_ai_count")
    val generatedAiCount: Int,
    @ColumnInfo("model_paid_once")
    val modelPaidOnce: Boolean,
    @ColumnInfo("model_name")
    val userModelName: Boolean,
    @ColumnInfo("model_id")
    val modelId: String,
    @ColumnInfo("eta")
    val eta: Int
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = false)
    var _id: Long? = null
}

fun AvatarStatusEntity.toAvatarStatus(): AvatarStatus {
    return AvatarStatus(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        modelPaidOnce = modelPaidOnce,
        userModelName = userModelName,
        modelId = modelId,
        eta = eta
    ).also {
        it.id = _id
    }
}

fun AvatarStatus.toEntity(): AvatarStatusEntity {
    return AvatarStatusEntity(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        modelPaidOnce = modelPaidOnce,
        userModelName = userModelName,
        modelId = modelId,
        eta = eta
    ).also {
        it._id = id
    }
}

object AvatarStatusTable {
    const val name = AppDatabase.TABLE_AVATAR_STATUS

    object Columns {
        const val ID                    = "id"
        const val MODEL_STATUS          = "model_status"
        const val TOTAL_AI_COUNT        = "total_ai_count"
        const val GENERATED_AI_COUNT    = "generated_ai_count"
        const val MODEL_PAID_ONCE       = "model_paid_once"
        const val MODEL_NAME            = "model_name"
        const val MODEL_ID              = "model_id"
    }
}