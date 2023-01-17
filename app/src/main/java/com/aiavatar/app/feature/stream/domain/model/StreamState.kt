package com.aiavatar.app.feature.stream.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreamState(
    val state: String
) : Parcelable
