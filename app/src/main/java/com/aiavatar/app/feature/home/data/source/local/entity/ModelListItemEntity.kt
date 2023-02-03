package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.domain.model.ModelListItem

@Entity(
    tableName = ModelListItemTable.name
)
data class ModelListItemEntity(
    @ColumnInfo("status_id")
    val statusId: String,
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("model_id")
    var modelId: String? = null
}

fun ModelListItemEntity.toModelListItem(): ModelListItem {
    return ModelListItem(
        statusId = statusId
    ).also {
        it.id = _id
        it.modelId = modelId
    }
}

fun ModelListItem.toEntity(): ModelListItemEntity {
    return ModelListItemEntity(
        statusId = statusId
    ).also {
        it._id = id
        it.modelId = modelId
    }
}

object ModelListItemTable {
    const val name: String = AppDatabase.TABLE_MODEL_LIST_ITEM

    object Columns {
        const val ID                = "id"
        const val STATUS_ID         = "status_id"
        const val MODEL_ID          = "model_id"
    }
}
