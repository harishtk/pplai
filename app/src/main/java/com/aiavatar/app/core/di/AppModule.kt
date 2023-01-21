package com.aiavatar.app.core.di

import android.content.Context
import com.aiavatar.app.core.data.repository.AppRepositoryImpl
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.remote.AppApi
import com.aiavatar.app.core.data.source.remote.AppRemoteDataSource
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.core.domain.util.JsonParser
import com.aiavatar.app.di.WebService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideAppRepository(
        appRemoteDataSource: AppRemoteDataSource,
        appDatabase: AppDatabase
    ): AppRepository {
        return AppRepositoryImpl(remoteDataSource = appRemoteDataSource, appDatabase)
    }

    @Provides
    fun provideAppApi(@WebService retrofit: Retrofit): AppApi =
        retrofit.create(AppApi::class.java)

    @GsonParser
    @Provides
    fun provideGsonParser(gson: Gson): JsonParser
            = com.aiavatar.app.core.data.util.GsonParser(gson)

}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GsonParser