package com.pepulai.app.feature.home.di

import com.pepulai.app.di.IoDispatcher
import com.pepulai.app.di.WebService
import com.pepulai.app.feature.home.data.repository.HomeRepositoryImpl
import com.pepulai.app.feature.home.data.source.remote.HomeApi
import com.pepulai.app.feature.home.data.source.remote.HomeRemoteDataSource
import com.pepulai.app.feature.home.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    fun provideHomeRepository(
        homeRemoteDataSource: HomeRemoteDataSource
    ): HomeRepository {
        return HomeRepositoryImpl(remoteDataSource = homeRemoteDataSource)
    }

    @Provides
    fun provideHomeApi(@WebService retrofit: Retrofit): HomeApi =
        retrofit.create(HomeApi::class.java)
}