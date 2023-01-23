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
    @ColumnInfo("paid")
    val paid: Boolean,
    @ColumnInfo("model_renamed")
    val modelRenamed: Boolean,
    @ColumnInfo("model_id")
    val modelId: String,
    @ColumnInfo("eta")
    val eta: Int
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = false)
    var _id: Long? = null

    @ColumnInfo("model_name")
    var modelName: String? = null
}

fun AvatarStatusEntity.toAvatarStatus(): AvatarStatus {
    return AvatarStatus(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        paid = paid,
        modelRenamedByUser = modelRenamed,
        modelId = modelId,
        eta = eta
    ).also {
        it.id = _id
        it.modelName = modelName
    }
}

fun AvatarStatus.toEntity(): AvatarStatusEntity {
    return AvatarStatusEntity(
        modelStatus = modelStatus,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        paid = paid,
        modelRenamed = modelRenamedByUser,
        modelId = modelId,
        eta = eta
    ).also {
        it._id = id
        it.modelName = modelName
    }
}

object AvatarStatusTable {
    const val name = AppDatabase.TABLE_AVATAR_STATUS

    object Columns {
        const val ID                    = "id"
        const val MODEL_STATUS          = "model_status"
        const val MODEL_NAME            = "model_name"
        const val TOTAL_AI_COUNT        = "total_ai_count"
        const val GENERATED_AI_COUNT    = "generated_ai_count"
        const val PAID                  = "paid"
        const val MODEL_RENAMED         = "model_renamed"
        const val MODEL_ID              = "model_id"
    }
}