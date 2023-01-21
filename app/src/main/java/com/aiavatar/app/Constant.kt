package com.aiavatar.app

object Constant {
    const val PLATFORM = "android"

    /* Extras */
    const val EXTRA_DATA = "com.aiavatar.app.extras.DATA"
    const val EXTRA_ENGAGED_TIME = "com.aiavatar.app.extras.ENGAGED_TIME"
    const val EXTRA_STREAM_URL = "com.aiavatar.app.extras.STREAM_URL"
    const val EXTRA_STREAM_NAME = "com.aiavatar.app.extras.STREAM_NAME"
    const val EXTRA_STREAM_ID   = "com.aiavatar.app.extras.STREAM_ID"
    const val EXTRA_SESSION_ID = "com.aiavatar.app.extras.SESSION_ID"
    const val EXTRA_FROM = "com.aiavatar.app.extras.FROM"

    /* Args */
    const val ARG_PLAN_ID = "com.aiavatar.app.args.PLAN_ID"

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

    /** Permission **/
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val PERMISSION_PERMANENTLY_DENIED = "PERMISSION_PERMANENTLY_DENIED"
}