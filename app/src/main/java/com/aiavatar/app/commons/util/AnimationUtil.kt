package com.aiavatar.app.commons.util

import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.CycleInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation

object AnimationUtil {

    fun View.shakeNow(onAnimationEnd: () -> Unit) {
        val shake = TranslateAnimation(0F, 15F, 0F, 0F)
        shake.duration = SHAKE_ANIMATION_DURATION.toLong()
        shake.interpolator = CycleInterpolator(4F)
        shake.animationListener(
            onAnimationEnd = onAnimationEnd
        )
        startAnimation(shake)
    }

    fun View.shakeNow() {
        val shake = TranslateAnimation(0F, 15F, 0F, 0F)
        shake.duration = SHAKE_ANIMATION_DURATION.toLong()
        shake.interpolator = CycleInterpolator(4F)
        startAnimation(shake)
    }

    fun View.touchInteractFeedback(scaleMultiplier: Float = DEFAULT_SCALE_MULTIPLIER) {
        val scale = ScaleAnimation(scaleMultiplier, 1.0f, scaleMultiplier, 1.0f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        scale.duration = QUICK_ANIMATION_DURATION
        scale.interpolator = LinearInterpolator()
        startAnimation(scale)
    }

    private inline fun animationListener(
        crossinline onAnimationStart: () -> Unit = {},
        crossinline onAnimationEnd: () -> Unit = {},
        crossinline onAnimationRepeat: () -> Unit = {}
    ) = object : AnimationListener {
        override fun onAnimationStart(animation: Animation?) { onAnimationStart() }
        override fun onAnimationEnd(animation: Animation?) { onAnimationEnd() }
        override fun onAnimationRepeat(animation: Animation?) { onAnimationRepeat() }
    }

    private inline fun Animation.animationListener(
        crossinline onAnimationStart: () -> Unit = {},
        crossinline onAnimationEnd: () -> Unit = {},
        crossinline onAnimationRepeat: () -> Unit = {}
    ) = setAnimationListener( object : AnimationListener {
        override fun onAnimationStart(animation: Animation?) { onAnimationStart() }
        override fun onAnimationEnd(animation: Animation?) { onAnimationEnd() }
        override fun onAnimationRepeat(animation: Animation?) { onAnimationRepeat() }
    })

    private const val SHAKE_ANIMATION_DURATION = 500
    private const val DEFAULT_ANIMATION_DURATION = 500L
    private const val QUICK_ANIMATION_DURATION = 200L

    private const val DEFAULT_SCALE_MULTIPLIER = 1.2F
}