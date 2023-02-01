package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.ModelStatus

@Entity(
    tableName = AvatarStatusTable.name,
    indices = [Index(name = "avatar_status_model_id", value = [AvatarStatusTable.Columns.MODEL_ID], unique = true)]
)
data class AvatarStatusEntity(
    @ColumnInfo("model_id")
    var modelId: String,
    @ColumnInfo("model_status")
    val modelStatus: String,
    @ColumnInfo("total_ai_count")
    val totalAiCount: Int,
    @ColumnInfo("generated_ai_count")
    val generatedAiCount: Int,
    @ColumnInfo("paid")
    val paid: Boolean,
    @ColumnInfo("renamed")
    val modelRenamed: Boolean,
    @ColumnInfo("eta")
    val eta: Int
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("model_name")
    var modelName: String? = null

    @ColumnInfo("avatar_status_id")
    var avatarStatusId: String? = null
}

fun AvatarStatusEntity.toAvatarStatus(): AvatarStatus {
    return AvatarStatus(
        modelId = modelId,
        modelStatus = ModelStatus.fromRawValue(modelStatus),
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        paid = paid,
        modelRenamedByUser = modelRenamed,
        eta = eta
    ).also {
        it.id = _id
        it.modelName = modelName
        it.avatarStatusId = avatarStatusId
    }
}

fun AvatarStatus.toEntity(): AvatarStatusEntity {
    return AvatarStatusEntity(
        modelStatus = modelStatus.statusString,
        totalAiCount = totalAiCount,
        generatedAiCount = generatedAiCount,
        paid = paid,
        modelRenamed = modelRenamedByUser,
        modelId = modelId,
        eta = eta
    ).also {
        it._id = id
        it.modelName = modelName
        it.avatarStatusId = avatarStatusId
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
        const val MODEL_RENAMED         = "renamed"
        const val MODEL_ID              = "model_id"
        const val AVATAR_STATUS_ID      = "avatar_status_id"
    }
}