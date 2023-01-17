package com.aiavatar.app.commons.util

import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import com.dd.CircularProgressButton

object CircularProgressButtonUtil {

    @JvmStatic
    fun setSpinning(button: CircularProgressButton?) {
        button?.apply {
            isClickable = false
            isIndeterminateProgressMode = true
            progress = 50
        }
    }

    @JvmStatic
    fun cancelSpinning(button: CircularProgressButton?) {
        button?.apply {
            progress = 0
            isIndeterminateProgressMode = false
            isClickable = true
        }
    }

    fun startShakeAnimation(button: CircularProgressButton?) {
        button?.apply {
            val shake = TranslateAnimation(0F, 15F, 0F, 0F)
            shake.duration = SHAKE_ANIMATION_DURATION.toLong()
            shake.interpolator = CycleInterpolator(4F)
            // shake.setAnimationListener(animationListener)
            startAnimation(shake)
        }
    }

    const val SHAKE_ANIMATION_DURATION = 500
}

fun CircularProgressButton.setSpinning() {
    isClickable = false
    isIndeterminateProgressMode = true
    progress = 50
}

fun CircularProgressButton.cancelSpinning() {
    progress = 0
    isIndeterminateProgressMode = false
    isClickable = true
}

fun CircularProgressButton.shakeNow() {
    val shake = TranslateAnimation(0F, 15F, 0F, 0F)
    shake.duration = CircularProgressButtonUtil.SHAKE_ANIMATION_DURATION.toLong()
    shake.interpolator = CycleInterpolator(4F)
    // shake.setAnimationListener(animationListener)
    startAnimation(shake)
}

fun CircularProgressButton.setEnabledWithAlpha(enabled: Boolean = true) {
    if (enabled) {
        this.isEnabled = true
        this.alpha = 1.0f
    } else {
        this.isEnabled = false
        this.alpha = 0.5f
    }
}

fun CircularProgressButton.fakeEnable(enabled: Boolean = true) {
    if (enabled) {
        this.alpha = 1.0f
    } else {
        this.alpha = 0.5f
    }
}