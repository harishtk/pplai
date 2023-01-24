package com.aiavatar.app.feature.home.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.home.domain.model.Category
import com.google.gson.annotations.SerializedName

@Entity(tableName = AppDatabase.TABLE_AVATAR_CATEGORIES)
data class CategoryEntity(
    @SerializedName("name")
    val name: String,
    @SerializedName("imageName")
    val imageName: String
) {
    @SerializedName("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null
}

fun CategoryEntity.toCategory(): Category {
    return Category(
        categoryName = name,
        imageName = imageName
    ).also {
        it.id = _id
    }
}

fun Category.asEntity(): CategoryEntity {
    return CategoryEntity(
        name = categoryName,
        imageName = imageName
    ).also {
        it._id = id
    }
}

object AvatarCategoriesTable {
    const val name = AppDatabase.TABLE_AVATAR_CATEGORIES

    object Columns {
        const val ID            = "_id"
        const val NAME          = "name"
        const val IMAGE_NAME    = "imageName"
    }
}
