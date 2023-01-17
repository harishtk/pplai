package com.aiavatar.app.feature.stream.domain.model

data class CommentItem(
    val userId: String,
    val content: String,
    val postedAt: Long
)
