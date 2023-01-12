package com.pepulai.app

object Constant {
    const val PLATFORM = "android"

    /* Extras */
    const val EXTRA_DATA = "com.pepulai.app.extras.DATA"
    const val EXTRA_ENGAGED_TIME = "com.pepulai.app.extras.ENGAGED_TIME"
    const val EXTRA_STREAM_URL = "com.pepulai.app.extras.STREAM_URL"
    const val EXTRA_STREAM_NAME = "com.pepulai.app.extras.STREAM_NAME"
    const val EXTRA_STREAM_ID   = "com.pepulai.app.extras.STREAM_ID"

    const val MIME_TYPE_JPEG = "image/jpeg"
    const val MIME_TYPE_PLAIN_TEXT = "text/plain"

    /* Values */
    const val STREAM_STATE_STARTING = "starting"
    const val STREAM_STATE_STARTED = "started"
    const val STREAM_STATE_PUBLISHING = "publishing"
    const val STREAM_STATE_STOPPED = "stopped"
    const val STREAM_STATE_NEW = "new"

    /* Env */
    const val ENV_DEV = "dev"
    const val ENV_STAGE = "staging"
    const val ENV_PROD = "prod"
    const val ENV_SPECIAL = "sp"
}