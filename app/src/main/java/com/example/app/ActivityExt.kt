package com.example.app

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowInsets

@Suppress("DEPRECATION")
fun Activity.getDisplaySize(): Size {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        val width = windowMetrics.bounds.width() - insets.left - insets.right
        val height = windowMetrics.bounds.height() - insets.top - insets.bottom
        Size(width, height)
    } else {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Size(metrics.widthPixels, metrics.heightPixels)
    }
}