package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiavatar.app.core.data.source.local.entity.LoginUserEntity
import com.aiavatar.app.core.data.source.local.entity.LoginUserTable
import com.aiavatar.app.feature.onboard.domain.model.LoginUser
import kotlinx.coroutines.flow.Flow

@Dao
interface LoginUserDao {

    @Query("SELECT * FROM ${LoginUserTable.name} LIMIT 1")
    fun getLoginUser(): Flow<LoginUserEntity?>

    @Query("SELECT * FROM ${LoginUserTable.name} LIMIT 1")
    suspend fun getLoginUserSync(): LoginUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loginUserEntity: LoginUserEntity)

    @Query("DELETE FROM ${LoginUserTable.name}")
    suspend fun deleteAll()
}