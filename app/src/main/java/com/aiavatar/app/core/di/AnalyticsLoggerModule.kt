package com.aiavatar.app.core.di

import com.aiavatar.app.analytics.AnalyticsLoggerImpl
import com.pepulnow.app.analytics.AnalyticsLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsLoggerModule {
    @Binds
    abstract fun bindAnalyticsLogger(analyticsLoggerImpl: AnalyticsLoggerImpl): AnalyticsLogger
    
}