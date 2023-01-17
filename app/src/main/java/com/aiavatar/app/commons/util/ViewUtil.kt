package com.aiavatar.app.commons.util

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.Animation
import androidx.annotation.Px

object ViewUtil {

    fun getRoundedDrawable(@Px radius: Int, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(
                radius.toFloat(), radius.toFloat(), // top left
                radius.toFloat(), radius.toFloat(), // top right
                radius.toFloat(), radius.toFloat(), // bottom right
                radius.toFloat(), radius.toFloat()  // bottom left
            )
        }
    }

    fun getRoundedDrawable(
        @Px topLeft: Int,
        @Px topRight: Int,
        @Px bottomRight: Int,
        @Px bottomLeft: Int,
        color: Int,
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(
                topLeft.toFloat(), topLeft.toFloat(),           // top left
                topRight.toFloat(), topRight.toFloat(),         // top right
                bottomRight.toFloat(), bottomRight.toFloat(),   // bottom right
                bottomLeft.toFloat(), bottomLeft.toFloat()      // bottom left
            )
        }
    }

    fun animateIn(view: View, animation: Animation) {
        if (view.visibility == View.VISIBLE) return
        view.clearAnimation()
        animation.reset()
        animation.startTime = 0
        view.visibility = View.VISIBLE
        view.startAnimation(animation)
    }

    fun animateOut(
        view: View,
        animation: Animation,
        visibility: Int,
        future: (Boolean?) -> Unit = { _ -> }
    ) {
        if (view.visibility == visibility) {
            future(true)
        } else {
            view.clearAnimation()
            animation.reset()
            animation.startTime = 0
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    view.visibility = visibility
                    future(true)
                }
            })
            view.startAnimation(animation)
        }
        return future(true)
    }
}