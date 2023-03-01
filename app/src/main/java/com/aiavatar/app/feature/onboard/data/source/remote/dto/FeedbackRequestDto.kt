package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.request.FeedbackRequest
import com.google.gson.annotations.SerializedName

data class FeedbackRequestDto(
    @SerializedName("rating")
    val rating: String,
    @SerializedName("tags")
    val tags: String,
    @SerializedName("comment")
    val comment: String
)

fun FeedbackRequest.asDto(): FeedbackRequestDto {
    return FeedbackRequestDto(
        rating = rating,
        tags = tags,
        comment = comment
    )
}