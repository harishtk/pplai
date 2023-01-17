package com.aiavatar.app.commons.util

import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation

object AnimationUtil {

    fun View.shakeNow() {
        val shake = TranslateAnimation(0F, 15F, 0F, 0F)
        shake.duration = SHAKE_ANIMATION_DURATION.toLong()
        shake.interpolator = CycleInterpolator(4F)
        // shake.setAnimationListener(animationListener)
        startAnimation(shake)
    }

    fun View.touchInteractFeedback() {
        val scale = ScaleAnimation(1.2f, 1.0f, 1.2f, 1.0f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        scale.duration = QUICK_ANIMATION_DURATION
        scale.interpolator = LinearInterpolator()
        startAnimation(scale)
    }

    private const val SHAKE_ANIMATION_DURATION = 500
    private const val DEFAULT_ANIMATION_DURATION = 500L
    private const val QUICK_ANIMATION_DURATION = 200L
}