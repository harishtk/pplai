package com.aiavatar.app.analytics

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Size

interface AnalyticsLogger {

    fun logEvent(@Size(min = 1L,max = 40L) name: String, params: Bundle?)

    fun logDebugEvent(@Size(min = 1L, max = 40L) name: String, params: Bundle?)

    fun setUserId(userId: String?)
}