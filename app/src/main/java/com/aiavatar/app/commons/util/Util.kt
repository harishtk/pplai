package com.aiavatar.app.commons.util

import android.content.Context
import android.util.DisplayMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("unused")
object Util {
    const val APPLICATION_COROUTINE_NAME = "AiAvatar::ApplicationCoroutine"

    private const val DEFAULT_COROUTINE_NAME = "AiAvatar::DefaultCoroutine"

    private val defaultCoroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }

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

    fun buildCoroutineScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        job: Job = SupervisorJob(),
        exceptionHandler: CoroutineExceptionHandler = defaultCoroutineExceptionHandler,
        coroutineName: String = DEFAULT_COROUTINE_NAME
    ): CoroutineScope {
        val context = dispatcher + job + exceptionHandler
        return CoroutineScope(context = context)
    }

    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    @JvmStatic
    fun isEmpty(value: String?): Boolean {
        return value == null || value.length == 0
    }

    fun hasItems(collection: Collection<*>?): Boolean {
        return collection != null && !collection.isEmpty()
    }

    @JvmStatic
    fun getFirstNonEmpty(vararg values: String?): String? {
        for (value in values) {
            if (!isEmpty(value)) {
                return value
            }
        }
        return ""
    }

    private const val PRE_DOMAIN_PATTERN = "http(?:s)?://"
    fun urlMatches(s1: String, s2: String): Boolean {
        return s1.replace(PRE_DOMAIN_PATTERN.toRegex(), "")
            .equals(s2.replace(PRE_DOMAIN_PATTERN.toRegex(), ""), ignoreCase = true)
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float, context: Context): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}