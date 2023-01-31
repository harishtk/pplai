package com.aiavatar.app.feature.home.data.source.local.entity

import android.graphics.ColorSpace.Model
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.google.gson.annotations.SerializedName

@Entity(tableName = ModelEntityTable.name)
data class ModelEntity(
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = false)
    val id: String,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("latest_image")
    val latestImage: String,
    @ColumnInfo("total_count")
    val totalCount: Int,
    @ColumnInfo("paid")
    val paid: Boolean,
    @ColumnInfo("renamed")
    val renamed: Boolean
) {
    @ColumnInfo("status_id")
    var statusId: String? = null
}

fun ModelData.toEntity(): ModelEntity {
    return ModelEntity(
        id = id,
        name = name,
        latestImage = latestImage,
        totalCount = totalCount,
        paid = paid,
        renamed = renamed
    ).also {
        it.statusId = statusId
    }
}

fun ModelEntity.toModelData(): ModelData {
    return ModelData(
        id = id,
        name = name,
        latestImage = latestImage,
        totalCount = totalCount,
        paid = paid,
        renamed = renamed
    ).also {
        it.statusId = statusId
    }
}

object ModelEntityTable {
    const val name: String = AppDatabase.TABLE_MODELS

    object Columns {
        const val ID            = "id"
        const val NAME          = "name"
        const val LATEST_IMAGE  = "latest_image"
        const val TOTAL_COUNT   = "total_count"
        const val PAID          = "paid"
        const val RENAMED       = "renamed"
        const val STATUS_ID     = "status_id"
    }
}
