package com.example.app.feature.stream.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.app.core.envForConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import com.example.app.BuildConfig
import com.example.app.core.Env
import com.example.app.di.WebService
import com.example.app.feature.stream.data.repository.StreamRepositoryImpl
import com.example.app.feature.stream.data.source.remote.StreamApi
import com.example.app.feature.stream.data.source.remote.StreamRemoteDataSource
import com.example.app.feature.stream.domain.repository.StreamRepository
import retrofit2.create

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