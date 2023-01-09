package com.example.app.commons.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

@SuppressLint("ObsoleteSdkInt")
object HapticUtil {

    fun createOneShot(context: Context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            marshmelloVibrateEffect(context, SHORT_HAPTIC_FEEDBACK_PATTERN)
        } else {
            lollipopVibrateEffect(context, SHORT_HAPTIC_FEEDBACK_PATTERN)
        }
    }

    fun createError(context: Context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            marshmelloVibrateEffect(context, ERROR_HAPTIC_FEEDBACK_PATTERN)
        } else {
            lollipopVibrateEffect(context, ERROR_HAPTIC_FEEDBACK_PATTERN)
        }
    }

    @Suppress("DEPRECATION")
    private fun lollipopVibrateEffect(context: Context, pattern: LongArray, repeat: Int = -1) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let { vibrator ->
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun marshmelloVibrateEffect(context: Context, pattern: LongArray, repeat: Int = -1) {
        context.getSystemService(Vibrator::class.java)?.let { vibrator ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        pattern,
                        ERROR_HAPTIC_FEEDBACK_AMPLITUDE,
                        repeat)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
        }
    }

    private const val SHORT_HAPTIC_FEEDBACK_DURATION = 40L
    private const val LONG_HAPTIC_FEEDBACK_DURATION = 300L

    private val SHORT_HAPTIC_FEEDBACK_PATTERN = longArrayOf(
        0, SHORT_HAPTIC_FEEDBACK_DURATION
    )

    private val ERROR_HAPTIC_FEEDBACK_PATTERN = longArrayOf(
        0,
        LONG_HAPTIC_FEEDBACK_DURATION
    )
    private val ERROR_HAPTIC_FEEDBACK_AMPLITUDE = intArrayOf(
        0,
        VibrationEffect.DEFAULT_AMPLITUDE,
    )
}