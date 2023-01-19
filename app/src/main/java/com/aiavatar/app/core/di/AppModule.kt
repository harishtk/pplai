package com.aiavatar.app.core.di

import android.content.Context
import com.aiavatar.app.core.data.repository.AppRepositoryImpl
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.remote.AppApi
import com.aiavatar.app.core.data.source.remote.AppRemoteDataSource
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.WebService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

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
}