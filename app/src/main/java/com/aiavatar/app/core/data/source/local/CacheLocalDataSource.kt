package com.aiavatar.app.core.data.source.local

import com.aiavatar.app.core.data.source.local.entity.CacheKeysEntity
import com.aiavatar.app.core.data.source.local.entity.cacheKeyForTable
import com.aiavatar.app.feature.home.data.repository.DEFAULT_CACHE_TIME_TO_LIVE
import com.aiavatar.app.ifNull
import timber.log.Timber
import javax.inject.Inject

class CacheLocalDataSource @Inject constructor(
    private val appDatabase: AppDatabase
) {

    suspend fun getOrCreateCacheKeys(cacheKey: String): CacheKeysEntity {
        return appDatabase.cacheKeysDao().getCacheKeysSync(cacheKey).ifNull {
            CacheKeysEntity(
                key = cacheKey,
                createdAt = System.currentTimeMillis(),
                expiresAt = (System.currentTimeMillis() + DEFAULT_CACHE_TIME_TO_LIVE)
            ).also { entity ->
                appDatabase.cacheKeysDao().insert(entity).also { id -> entity._id = id }
            }
        }
    }

    suspend fun getCacheKeys(cacheKey: String): CacheKeysEntity? {
        return appDatabase.cacheKeysDao().getCacheKeysSync(cacheKey)
    }

    suspend fun insertCacheKey(entity: CacheKeysEntity) {
        appDatabase.cacheKeysDao().insert(entity)
    }

    suspend fun updateCacheKey(entity: CacheKeysEntity): Int {
        Timber.d("Cache keys: Update ${entity.toString()}")
        return appDatabase.cacheKeysDao().updateCacheKeys(entity)
    }

    /*suspend fun getCacheKeys(key: String): CacheKeysEntity? {
        return appDatabase.cacheKeysDao().getCacheKeysSync(key)
    }*/

    suspend fun clearCacheKeys(cacheKey: String): Int {
        return appDatabase.cacheKeysDao().deleteCacheKeys(cacheKey)
    }

    suspend fun clearCacheKeys() {
        appDatabase.cacheKeysDao().deleteAll()
    }
}