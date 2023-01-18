package com.aiavatar.app.service

import android.app.Service
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

abstract class CoroutineService : Service() {

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }

    private val context: CoroutineContext = Dispatchers.Default +
            SupervisorJob() + exceptionHandler
    val serviceScope = CoroutineScope(context)


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel(CancellationException("Service is dead."))
    }
}
