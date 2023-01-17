package com.aiavatar.app.core.di

import com.aiavatar.app.core.data.repository.AnalyticsRepositoryImpl
import com.aiavatar.app.core.data.source.remote.AnalyticsApi
import com.aiavatar.app.core.data.source.remote.AnalyticsRemoteDataSource
import com.aiavatar.app.core.domain.repository.AnalyticsRepository
import com.aiavatar.app.di.WebService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Singleton
    @Provides
    fun provideAnalyticsApi(@WebService retrofit: Retrofit): AnalyticsApi =
        retrofit.create(AnalyticsApi::class.java)

    @Singleton
    @Provides
    fun provideAnalyticsRepository(
        remoteDataSource: AnalyticsRemoteDataSource,
    ): AnalyticsRepository =
        AnalyticsRepositoryImpl(remoteDataSource)

}