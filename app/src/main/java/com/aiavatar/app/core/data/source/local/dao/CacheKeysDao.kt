package com.aiavatar.app.core.data.source.local.dao

import androidx.room.*
import com.aiavatar.app.core.data.source.local.entity.CacheKeysEntity
import com.aiavatar.app.core.data.source.local.entity.CacheKeysTable

@Dao
interface CacheKeysDao {

    @Query("SELECT * FROM ${CacheKeysTable.name} WHERE ${CacheKeysTable.Columns.KEY} = :key")
    suspend fun getCacheKeysSync(key: String): CacheKeysEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCacheKeys(cacheKeysEntity: CacheKeysEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cacheKeysEntity: CacheKeysEntity): Long

    @Query("DELETE FROM ${CacheKeysTable.name}")
    suspend fun deleteAll()
}