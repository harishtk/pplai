package com.aiavatar.app.core.domain.model

data class LoginUser(
    val deviceToken: String,
    val userId: String,
    val email: String,
    val socialImage: String?,
)

/*
*
* const val DEVICE_TOKEN: String = "device_token"
            const val USER_ID: String = "user_id"
            const val USERNAME: String = "username"
            const val EMAIL: String = "email"
            const val SOCIAL_IMAGE: String = "social_image"
            const val NOTIFY_UPON_COMPLETION = "notify_upon_completion"
            const val GUEST_USER_ID = "guest_user_id"
            const val PROCESSING_MODEL = "processing_model"
            const val UPLOADING_PHOTOS = "uploading_photos"
            const val CURRENT_AVATAR_STATUS_ID = "current_avatar_status_id"
            const val UPLOAD_STEP_SKIPPED: String = "upload_step_skipped"*/
