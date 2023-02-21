package com.aiavatar.app.feature.home.di

import com.aiavatar.app.feature.home.data.repository.HomeRepositoryImpl
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository
}