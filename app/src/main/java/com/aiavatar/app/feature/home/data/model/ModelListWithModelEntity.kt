package com.aiavatar.app.feature.home.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.aiavatar.app.feature.home.data.source.local.entity.*
import com.aiavatar.app.feature.home.domain.model.ModelListWithModel

data class ModelListWithModelEntity(
    @Embedded
    val modelListItemEntity: ModelListItemEntity,
    @Relation(
        parentColumn = ModelListItemTable.Columns.MODEL_ID,
        entityColumn = ModelEntityTable.Columns.ID
    )
    val modelEntity: ModelEntity?
)

fun ModelListWithModelEntity.toModelListWithModel(): ModelListWithModel {
    return ModelListWithModel(
        modelListItem = modelListItemEntity.toModelListItem(),
        model = modelEntity?.toModelData()
    )
}