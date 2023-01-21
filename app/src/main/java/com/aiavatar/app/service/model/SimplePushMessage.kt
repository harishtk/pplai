package com.aiavatar.app.service.model

import com.google.gson.annotations.SerializedName

data class SimplePushMessage(
    @SerializedName("dateTime")
    val dateTimeUtc: String,
    @SerializedName("content")
    val content: String
)
