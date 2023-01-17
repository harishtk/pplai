package com.aiavatar.app.feature.stream.domain.model.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreamIdRequest(
    val id: String
) : Parcelable
