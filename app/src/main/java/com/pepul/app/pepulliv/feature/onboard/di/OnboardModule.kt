package com.pepul.app.pepulliv.feature.onboard.di

import com.pepul.app.pepulliv.di.WebService
import com.pepul.app.pepulliv.feature.onboard.data.repository.AccountsRepositoryImpl
import com.pepul.app.pepulliv.feature.onboard.data.source.remote.AccountsApi
import com.pepul.app.pepulliv.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.pepul.app.pepulliv.feature.onboard.domain.repository.AccountsRepository
import com.pepul.app.pepulliv.feature.stream.data.repository.StreamRepositoryImpl
import com.pepul.app.pepulliv.feature.stream.data.source.remote.StreamApi
import com.pepul.app.pepulliv.feature.stream.data.source.remote.StreamRemoteDataSource
import com.pepul.app.pepulliv.feature.stream.domain.repository.StreamRepository
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