package com.aiavatar.app.core.domain.util

class BuenoCacheException constructor(
    val minutesAgo: Long, message: String?) : Exception(message)