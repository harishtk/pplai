package com.pepul.app.pepulliv.commons.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("unused")
object Util {

    fun countDownFlow(
        start: kotlin.time.Duration,
        step: kotlin.time.Duration = 1.seconds,
        initialDelay: kotlin.time.Duration = kotlin.time.Duration.ZERO,
    ): Flow<Long> = flow {
        var counter: Long = start.toLong(DurationUnit.MILLISECONDS)
        delay(initialDelay.toLong(DurationUnit.MILLISECONDS))
        while (counter >= 0) {
            emit(counter)
            if (counter != 0L) {
                delay(step.toLong(DurationUnit.MILLISECONDS))
                counter -= step.toLong(DurationUnit.MILLISECONDS)
            } else {
                break
            }
        }
    }

    fun getCustomCoroutineScope(): CoroutineScope {
        val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, t ->
                Timber.e(t)
            }
        val context =
            Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler

        return CoroutineScope(context = context)
    }
}