package com.pepulai.app.commons.util

import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation

object AnimationUtil {

    fun View.shakeNow() {
        val shake = TranslateAnimation(0F, 15F, 0F, 0F)
        shake.duration = SHAKE_ANIMATION_DURATION.toLong()
        shake.interpolator = CycleInterpolator(4F)
        // shake.setAnimationListener(animationListener)
        startAnimation(shake)
    }

    private const val SHAKE_ANIMATION_DURATION = 500
}