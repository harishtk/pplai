package com.aiavatar.app.eventbus

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnAuthorizedEvent(
    val timestamp: Long
) : Parcelable
