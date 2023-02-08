package com.aiavatar.app.core.di

import android.content.Context
import com.aiavatar.app.analytics.AnalyticsLoggerImpl
import com.aiavatar.app.analytics.AnalyticsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsLoggerModule {
    @Binds
    abstract fun bindAnalyticsLogger(analyticsLoggerImpl: AnalyticsLoggerImpl): AnalyticsLogger
    
}