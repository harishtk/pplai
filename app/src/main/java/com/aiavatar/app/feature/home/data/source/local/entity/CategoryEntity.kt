package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.domain.model.Category
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = AvatarCategoriesTable.name,
    indices = [Index(name = "catalog_name_index", value = [AvatarCategoriesTable.Columns.NAME], unique = true)]
)
data class CategoryEntity(
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("imageName")
    val imageName: String
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    @ColumnInfo("thumbnail")
    var thumbnail: String? = null
}

fun CategoryEntity.toCategory(): Category {
    return Category(
        categoryName = name,
        imageName = imageName
    ).also {
        it.id = _id
        it.thumbnail = thumbnail
    }
}

fun Category.asEntity(): CategoryEntity {
    return CategoryEntity(
        name = categoryName,
        imageName = imageName
    ).also {
        it._id = id
        it.thumbnail = thumbnail
    }
}

object AvatarCategoriesTable {
    const val name = AppDatabase.TABLE_AVATAR_CATEGORIES

    object Columns {
        const val ID            = "_id"
        const val NAME          = "name"
        const val IMAGE_NAME    = "imageName"
        const val THUMBNAIL     = "thumbnail"
    }
}
