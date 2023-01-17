package com.aiavatar.app.analytics

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Size
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.core.Env
import com.aiavatar.app.core.envForConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.pepulnow.app.analytics.AnalyticsLogger
import javax.inject.Inject

class AnalyticsLoggerImpl @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    // private val facebookAnalytics: AppEventsLogger
) : AnalyticsLogger {

    private val environment: Env = envForConfig(BuildConfig.ENV)

    override fun logEvent(@NonNull @Size(min = 1L,max = 40L) name: String, @Nullable params: Bundle?) {
        if (environment == Env.PROD || environment == Env.SPECIAL) {
            firebaseAnalytics.logEvent(name, params)
            // facebookAnalytics.logEvent(name, params)
        }

        /*val adjustToken = ACTIVE_ADJUST_EVENT_TOKEN_MAP[name]
        adjustToken?.let {
            val adjustEvent = AdjustEvent(adjustToken)
            Adjust.trackEvent(adjustEvent)
        }*/
    }

    override fun logDebugEvent(name: String, params: Bundle?) {
        if (environment == Env.DEV) {
            // No op.
        }
    }

    override fun setUserId(@Nullable userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
}