package com.aiavatar.app

import android.os.Build

object VersionCompat

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

val VersionCompat.isAtLeastT: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

inline fun VersionCompat.sdkAtLeastT(block: () -> Unit): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        block()
        return true
    }
    return false
}