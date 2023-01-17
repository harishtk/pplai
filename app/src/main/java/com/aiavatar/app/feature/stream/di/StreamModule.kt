package com.aiavatar.app.feature.stream.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

import com.aiavatar.app.di.WebService
import com.aiavatar.app.feature.stream.data.repository.StreamRepositoryImpl
import com.aiavatar.app.feature.stream.data.source.remote.StreamApi
import com.aiavatar.app.feature.stream.data.source.remote.StreamRemoteDataSource
import com.aiavatar.app.feature.stream.domain.repository.StreamRepository

@Module
@InstallIn(SingletonComponent::class)
object StreamModule {

    @Provides
    @Singleton
    fun provideStreamRepositoryImpl(
        remoteDataSource: StreamRemoteDataSource
    ): StreamRepository =
        StreamRepositoryImpl(
            remoteDataSource
        )

    @Provides
    @Singleton
    fun provideStreamApiService(@WebService retrofit: Retrofit): StreamApi =
        retrofit.create(StreamApi::class.java)

}