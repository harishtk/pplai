package com.aiavatar.app.feature.onboard.di

import com.aiavatar.app.core.di.ApplicationCoroutineScope
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.di.WebService
import com.aiavatar.app.feature.onboard.data.repository.AccountsRepositoryImpl
import com.aiavatar.app.feature.onboard.data.source.remote.AccountsApi
import com.aiavatar.app.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardModule {

    @Provides
    @Singleton
    fun provideAccountRepositoryImpl(
        @ApplicationCoroutineScope
        applicationScope: CoroutineScope,
        @IoDispatcher
        ioDispatcher: CoroutineDispatcher,
        remoteDataSource: AccountsRemoteDataSource
    ): AccountsRepository =
        AccountsRepositoryImpl(
            applicationScope = applicationScope,
            ioDispatcher = ioDispatcher,
            remoteDataSource = remoteDataSource
        )

    @Provides
    @Singleton
    fun provideAccountsApiService(@WebService retrofit: Retrofit): AccountsApi =
        retrofit.create(AccountsApi::class.java)

}