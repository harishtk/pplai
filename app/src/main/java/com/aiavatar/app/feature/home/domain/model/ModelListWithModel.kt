package com.aiavatar.app.feature.home.domain.model

import androidx.room.Embedded
import androidx.room.Relation
import com.aiavatar.app.feature.home.data.source.local.entity.ModelEntityTable
import com.aiavatar.app.feature.home.data.source.local.entity.ModelListItemTable
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.aiavatar.app.feature.home.domain.model.ModelListItem

data class ModelListWithModel(
    @Embedded
    val modelListItem: ModelListItem,
    @Relation(
        parentColumn = ModelListItemTable.Columns.MODEL_ID,
        entityColumn = ModelEntityTable.Columns.ID
    )
    val model: ModelData?
)