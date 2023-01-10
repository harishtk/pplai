package com.pepulai.app.feature.onboard.di

import com.pepulai.app.di.WebService
import com.pepulai.app.feature.onboard.data.repository.AccountsRepositoryImpl
import com.pepulai.app.feature.onboard.data.source.remote.AccountsApi
import com.pepulai.app.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.pepulai.app.feature.onboard.domain.repository.AccountsRepository
import com.pepulai.app.feature.stream.data.repository.StreamRepositoryImpl
import com.pepulai.app.feature.stream.data.source.remote.StreamApi
import com.pepulai.app.feature.stream.data.source.remote.StreamRemoteDataSource
import com.pepulai.app.feature.stream.domain.repository.StreamRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardModule {

    @Provides
    @Singleton
    fun provideAccountRepositoryImpl(
        remoteDataSource: AccountsRemoteDataSource
    ): AccountsRepository =
        AccountsRepositoryImpl(
            remoteDataSource
        )

    @Provides
    @Singleton
    fun provideStreamApiService(@WebService retrofit: Retrofit): AccountsApi =
        retrofit.create(AccountsApi::class.java)

}