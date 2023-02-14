package com.aiavatar.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.aiavatar.app.core.data.repository.AppRepositoryImpl
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.remote.AppApi
import com.aiavatar.app.core.data.source.remote.AppRemoteDataSource
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.core.domain.util.JsonParser
import com.aiavatar.app.di.WebService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.Factory().createInstance(context)
    }

    @Provides
    fun provideAppRepository(
        appRemoteDataSource: AppRemoteDataSource,
        appDatabase: AppDatabase
    ): AppRepository {
        return AppRepositoryImpl(remoteDataSource = appRemoteDataSource, appDatabase)
    }

    @Provides
    fun provideAppApi(@WebService retrofit: Retrofit): AppApi =
        retrofit.create(AppApi::class.java)

    @GsonParser
    @Provides
    fun provideGsonParser(gson: Gson): JsonParser
            = com.aiavatar.app.core.data.util.GsonParser(gson)

    @Singleton
    @Provides
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        preferencesDataStore(name = "data-store").getValue(context, String::javaClass)

    /* Analytics */
    @Singleton
    @Provides
    fun provideFirebaseAnalytics(@ApplicationContext app: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(app)

    /* END - Analytics */

}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GsonParser