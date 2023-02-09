package com.aiavatar.app

import android.content.Context
import android.text.InputFilter
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.navigation.NavController
import com.aiavatar.app.commons.util.ServiceUtil
import timber.log.Timber

fun <T> List<T>.mapButReplace(targetItem: T, newItem: T) = map {
    if (it == targetItem) {
        newItem
    } else {
        it
    }
}

fun String?.nullAsEmpty(): String {
    return this ?: ""
}

fun String?.nonNullOrEmpty(block: (s: String) -> Unit) {
    if (this?.isNotEmpty() == true) {
        block(this)
    }
}

fun String.asInitials(limit: Int = 2): String {
    val buffer = StringBuffer()
    trim().split(" ").filter {
        it.isNotEmpty()
    }.joinTo(
        buffer = buffer,
        limit = limit,
        separator = "",
        truncated = "",
    ) { s ->
        s.first().uppercase()
    }
    return buffer.toString()
}

fun Boolean.toggle(): Boolean {
    return this.not()
}

fun NavController.isOnBackStack(@IdRes id: Int): Boolean =
    try {
        getBackStackEntry(id); true
    } catch (e: Throwable) {
        false
    }


fun EditText.allowOnlyAlphaNumericCharacters() {
    filters = filters.plus(
        InputFilter { src, start, end, dst, dstart, dend ->
            if (src.toString().matches(Regex("[a-zA-Z0-9]+"))) {
                src
            } else ""
        }
    )
}

fun EditText.allowOnlyCapitalNumericCharacters() {
    filters = filters.plus(
        InputFilter { src, start, end, dst, dstart, dend ->
            if (src.toString().matches(Regex("[A-Z0-9]+"))) {
                src
            } else ""
        }
    )
}

fun EditText.hideKeyboard(): Boolean {
    return ServiceUtil.getInputMethodManager(context)
        .hideSoftInputFromWindow(windowToken, 0)
}

fun EditText.showSoftInputMode(): Boolean {
    return ServiceUtil.getInputMethodManager(context)
        .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.showToast(message: String, isShort: Boolean = true) {
    var toastLength: Int = 0
    toastLength = if (isShort) {
        Toast.LENGTH_SHORT
    } else {
        Toast.LENGTH_LONG
    }
    Toast.makeText(this, message, toastLength).show()
}

fun Context.debugToast(message: String, isShort: Boolean = true) {
    if (BuildConfig.DEBUG) {
        showToast(message, isShort)
    }
}


fun Long.format(): String {
    return String.format("%02d", this)
}

fun Long.pad(): String = format()

object Log {
    @JvmStatic
    fun tag(clazz: Class<*>): String {
        val simpleName = clazz.simpleName
        return if (simpleName.length > 23) {
            simpleName.substring(0, 23)
        } else {
            simpleName
        }
    }
}

inline fun <R> safeCall(block: () -> R): R? {
    return try {
        block()
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            Timber.e(e)
        }
        null
    }
}

inline fun ifDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

/**
 * Calls [block] if [this] is null.
 */
internal inline fun <T> T?.ifNull(block: () -> T): T = this ?: block()

/**
 * Calls [block] if [this] is null. Returns [this] either way.
 */
internal inline fun <T> T?.ifNullAlso(block: () -> Unit): T? = this.also {
    if (it == null) {
        block()
    }
}

fun Boolean.assertBoolean(onTrue: () -> Unit, onFalse: () -> Unit) {
    if (this) onTrue() else onFalse()
}

