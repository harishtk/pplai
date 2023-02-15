package com.aiavatar.app.core.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aiavatar.app.core.data.source.local.dao.*
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.feature.home.data.source.local.dao.*
import com.aiavatar.app.feature.home.data.source.local.entity.*

@Database(
    entities = [UploadSessionEntity::class, UploadFilesEntity::class,
                AvatarStatusEntity::class, AvatarFilesEntity::class,
                CategoryEntity::class, CacheKeysEntity::class,
                CatalogListEntity::class, ModelEntity::class,
                LoginUserEntity::class, ModelAvatarEntity::class,
                DownloadSessionEntity::class, DownloadFilesEntity::class,
                ModelListItemEntity::class, PaymentsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun uploadSessionDao(): UploadSessionDao

    abstract fun uploadFilesDao(): UploadFilesDao

    abstract fun avatarStatusDao(): AvatarStatusDao

    abstract fun avatarFilesDao(): AvatarFilesDao

    abstract fun categoryDao(): CategoryDao

    abstract fun catalogListDao(): CatalogListDao

    abstract fun cacheKeysDao(): CacheKeysDao

    abstract fun modelDao(): ModelDao

    abstract fun loginUserDao(): LoginUserDao

    abstract fun modelAvatarDao(): ModelAvatarDao

    abstract fun modelListItemDao(): ModelListItemDao

    abstract fun downloadSessionDao(): DownloadSessionDao

    abstract fun downloadFilesDao(): DownloadFilesDao

    abstract fun paymentsDao(): PaymentsDao

    class Factory {
        fun createInstance(appContext: Context): AppDatabase =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "AiAvatar.db")
                .fallbackToDestructiveMigration()
                .build()
    }

    companion object {
        private const val DATABASE_NAME = "app_db"

        /* Tables Names */
        const val TABLE_UPLOAD_SESSION      = "upload_session"
        const val TABLE_UPLOAD_FILES        = "upload_files"
        const val TABLE_AVATAR_STATUS       = "avatar_status"
        const val TABLE_AVATAR_FILES        = "avatar_files"
        const val TABLE_AVATAR_CATEGORIES   = "avatar_categories"
        const val TABLE_CATEGORY_LIST       = "category_list"
        const val TABLE_MODEL_LIST_ITEM     = "model_list_item"
        const val TABLE_MODELS              = "models"
        const val TABLE_MODEL_AVATARS       = "model_avatars"
        const val TABLE_LOGIN_USER          = "login_user"
        const val TABLE_DOWNLOAD_SESSION    = "download_session"
        const val TABLE_DOWNLOAD_FILES      = "download_files"
        const val TABLE_PAYMENTS            = "payments"

        const val TABLE_CACHE_KEYS          = "cache_keys"
    }
}