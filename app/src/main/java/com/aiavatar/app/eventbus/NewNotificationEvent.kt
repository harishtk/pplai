package com.aiavatar.app.eventbus

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewNotificationEvent(
    val hint: String,
    val timestamp: Long
) : Parcelable