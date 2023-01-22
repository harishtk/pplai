package com.aiavatar.app

import android.view.View

private const val DEFAULT_DOUBLE_CLICK_DELAY = 1000L

fun View.setOnSingleClickListener(
    delay: Long = DEFAULT_DOUBLE_CLICK_DELAY,
    onClickListener: View.OnClickListener
) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        val delta = (currentTime - lastClickTime)
            .coerceAtLeast(0)
        lastClickTime = currentTime
        if (delta <= delay) {
            return@setOnClickListener
        }
        onClickListener.onClick(view)

    }
}