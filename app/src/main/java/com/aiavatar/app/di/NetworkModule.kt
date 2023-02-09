package com.aiavatar.app.di

import android.content.Context
import androidx.lifecycle.asFlow
import com.aiavatar.app.commons.util.net.ConnectivityManagerLiveData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideConnectivityMangerFlow(
        @ApplicationContext context: Context
    ) = ConnectivityManagerLiveData(context).asFlow()


}