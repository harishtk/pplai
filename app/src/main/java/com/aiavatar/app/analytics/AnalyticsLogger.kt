package com.pepulnow.app.analytics

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Size

interface AnalyticsLogger {

    fun logEvent(@NonNull @Size(min = 1L,max = 40L) name: String, @Nullable params: Bundle?)

    fun logDebugEvent(@NonNull @Size(min = 1L, max = 40L) name: String, @Nullable params: Bundle?)

    fun setUserId(userId: String?)
}