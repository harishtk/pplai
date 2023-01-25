package com.aiavatar.app.feature.home.di

import com.aiavatar.app.core.data.source.local.CacheLocalDataSource
import com.aiavatar.app.di.WebService
import com.aiavatar.app.feature.home.data.repository.HomeRepositoryImpl
import com.aiavatar.app.feature.home.data.source.local.HomeLocalDataSource
import com.aiavatar.app.feature.home.data.source.remote.HomeApi
import com.aiavatar.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    fun provideHomeRepository(
        homeRemoteDataSource: HomeRemoteDataSource,
        homeLocalDataSource: HomeLocalDataSource,
        cacheLocalDataSource: CacheLocalDataSource,
    ): HomeRepository {
        return HomeRepositoryImpl(
            remoteDataSource = homeRemoteDataSource,
            localDataSource = homeLocalDataSource,
            cacheLocalDataSource = cacheLocalDataSource
        )
    }

    @Provides
    fun provideHomeApi(@WebService retrofit: Retrofit): HomeApi =
        retrofit.create(HomeApi::class.java)
}