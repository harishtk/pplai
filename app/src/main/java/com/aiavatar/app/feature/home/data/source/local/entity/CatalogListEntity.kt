package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.data.source.local.dao.CatalogListDao
import com.aiavatar.app.feature.home.domain.model.CatalogList
import com.aiavatar.app.feature.home.domain.model.ListAvatar

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

    @ColumnInfo("thumbnail")
    var thumbnail: String? = null
}

fun CatalogListEntity.toCatalogList(): CatalogList {
    return CatalogList(
        catalogName = catalogName,
        imageName = imageName
    ).also {
        it.id = _id
        it.thumbnail = thumbnail
    }
}

fun CatalogList.asEntity(): CatalogListEntity {
    return CatalogListEntity(
        catalogName = catalogName,
        imageName = imageName
    ).also {
        it._id = id
        it.thumbnail = thumbnail
    }
}

object CatalogListTable {
    const val name = AppDatabase.TABLE_CATEGORY_LIST

    object Columns {
        const val ID            = "id"
        const val CATALOG_NAME  = "catalog_name"
        const val IMAGE_NAME    = "image_name"
        const val THUMBNAIL     = "thumbnail"
    }
}
