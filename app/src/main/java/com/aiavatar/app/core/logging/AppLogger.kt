package com.aiavatar.app.core.logging

import com.aiavatar.app.core.data.repository.LoggingRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class AppLogger @Inject constructor(
    val repository: LoggingRepository
) {
    private val exceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }

    private val coroutineContext: CoroutineContext =
        Dispatchers.IO + SupervisorJob() + exceptionHandler

    private val myCoroutineScope: CoroutineScope = CoroutineScope(
        coroutineContext
    )

    fun log(tag: String, message: String) = myCoroutineScope.launch {
        val params = JsonObject().apply {
            addProperty("tag", tag)
            addProperty("message", message)
        }
        repository.customLog(params).collectLatest {
            Timber.tag("AppLogger.Msg").d(it.toString())
        }
    }
}