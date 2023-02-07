package com.aiavatar.app

import android.os.Build

/**
 * Runs the block for devices below [Build.VERSION_CODES.Q]
 */
inline fun sdkBelowQ(block: () -> Unit): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        block()
        return true
    }
    return false
}

inline fun Boolean.doElse(block: () -> Unit) {
    if (this) {
        block()
    }
}