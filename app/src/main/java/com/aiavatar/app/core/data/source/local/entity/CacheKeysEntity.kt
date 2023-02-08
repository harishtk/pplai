package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.domain.model.CacheKeys
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

@Entity(
    tableName = CacheKeysTable.name,
    indices = [Index(name = "cache_keys_index", value = [CacheKeysTable.Columns.KEY], unique = true)]
)
data class CacheKeysEntity(
    @ColumnInfo("key")
    val key: String,
    @ColumnInfo("created_at")
    val createdAt: Long,
    @ColumnInfo("expires_at")
    val expiresAt: Long
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null

    internal fun expired(): Boolean {
        val delta = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
        Timber.d("Cache keys: current = ${System.currentTimeMillis()} expires = $expiresAt (${Date(expiresAt)}) delta = $delta created = $createdAt")
        return System.currentTimeMillis() >= expiresAt
    }
}

internal fun CacheKeysEntity.modify(
    createdAt: Long? = null,
    expiresAt: Long? = null
): CacheKeysEntity {
    return copy(
        createdAt = createdAt ?: this.createdAt,
        expiresAt = expiresAt ?: this.expiresAt
    )
}

fun CacheKeysEntity.toCacheKeys(): CacheKeys {
    return CacheKeys(
        key = key,
        createdAt = createdAt,
        expiresAt = expiresAt
    ).also {
        it.id = _id
    }
}

fun CacheKeys.asEntity(): CacheKeysEntity {
    return CacheKeysEntity(
        key = key,
        createdAt = createdAt,
        expiresAt = expiresAt
    ).also {
        it._id = id
    }
}

fun cacheKeyForTable(tableName: String): String {
    return when (tableName) {
        AppDatabase.TABLE_AVATAR_CATEGORIES -> "categories"
        AppDatabase.TABLE_CATEGORY_LIST     -> "category_list"
        else -> throw IllegalStateException("Unable to decide a cache key for table $tableName")
    }
}

object CacheKeysTable {
    const val name = AppDatabase.TABLE_CACHE_KEYS

    object Columns {
        const val ID            = "id"
        const val KEY           = "key"
        const val CREATED_AT    = "created_at"
        const val EXPIRES_AT    = "expires_at"
    }
}
