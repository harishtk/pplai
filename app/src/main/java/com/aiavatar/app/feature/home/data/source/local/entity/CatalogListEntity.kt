package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase

@Entity(
    tableName = CatalogListTable.name,
)
data class CatalogListEntity(
    @ColumnInfo("catalog_name")
    val catalogName: String,
    @ColumnInfo("image_name")
    val imageName: String
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null
}

object CatalogListTable {
    const val name = AppDatabase.TABLE_CATEGORY_LIST

    object Columns {
        const val ID            = "id"
        const val CATALOG_NAME  = "catalog_name"
        const val IMAGE_NAME    = "image_name"
    }
}
