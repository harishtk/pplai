package com.aiavatar.app.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.NetWorkHelper
import com.aiavatar.app.commons.util.net.AndroidHeaderInterceptor
import com.aiavatar.app.commons.util.net.JwtInterceptor
import com.aiavatar.app.commons.util.net.UserAgentInterceptor
import com.aiavatar.app.core.Env
import com.aiavatar.app.core.envForConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkHelper(@ApplicationContext context: Context): NetWorkHelper =
        NetWorkHelper(context)

    @Provides
    @Singleton
    @WebService
    fun provideRetrofit(gson: Gson): Retrofit {
        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.MINUTES)

        okHttpClientBuilder.addInterceptor(UserAgentInterceptor())
        okHttpClientBuilder.addInterceptor(AndroidHeaderInterceptor())
        okHttpClientBuilder.addInterceptor(JwtInterceptor())

        if (envForConfig(BuildConfig.ENV) == Env.DEV || BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            okHttpClientBuilder.addInterceptor(httpLoggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    fun provideGson(): Gson = GsonBuilder().create()

}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebService