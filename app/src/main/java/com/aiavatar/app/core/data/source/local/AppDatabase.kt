package com.aiavatar.app.core.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aiavatar.app.core.data.source.local.dao.*
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.feature.home.data.source.local.dao.CatalogListDao
import com.aiavatar.app.feature.home.data.source.local.dao.CategoryDao
import com.aiavatar.app.feature.home.data.source.local.dao.ModelDao
import com.aiavatar.app.feature.home.data.source.local.entity.CatalogListEntity
import com.aiavatar.app.feature.home.data.source.local.entity.CategoryEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelEntity

@Database(
    entities = [UploadSessionEntity::class, UploadFilesEntity::class,
                AvatarStatusEntity::class, AvatarFilesEntity::class,
                CategoryEntity::class, CacheKeysEntity::class,
                CatalogListEntity::class, ModelEntity::class, LoginUserEntity::class],
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

    companion object {
        @Volatile
        var INSTANCE: AppDatabase? = null

        @Synchronized
        fun getInstance(appContext: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(appContext).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "app_db")
                .fallbackToDestructiveMigration()
                .build()

        /* Tables Names */
        const val TABLE_UPLOAD_SESSION      = "upload_session"
        const val TABLE_UPLOAD_FILES        = "upload_files"
        const val TABLE_AVATAR_STATUS       = "avatar_status"
        const val TABLE_AVATAR_FILES        = "avatar_files"
        const val TABLE_AVATAR_CATEGORIES   = "avatar_categories"
        const val TABLE_CATEGORY_LIST       = "category_list"
        const val TABLE_MODELS              = "models"
        const val TABLE_MODEL_AVATARS       = "model_avatars"
        const val TABLE_LOGIN_USER          = "login_user"

        const val TABLE_CACHE_KEYS          = "cache_keys"
    }
}