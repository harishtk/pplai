package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesEntity
import com.aiavatar.app.core.data.source.local.entity.AvatarFilesTable
import com.aiavatar.app.core.domain.model.AvatarFile

@Dao
interface AvatarFilesDao {

    @Query("SELECT * FROM ${AvatarFilesTable.name} WHERE ${AvatarFilesTable.Columns.AVATAR_STATUS_ID} = :statusId")
    suspend fun getAllAvatarFilesForStatusIdSync(statusId: Long): List<AvatarFilesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(avatarFileEntities: List<AvatarFilesEntity>)

}