package com.aiavatar.app

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.FloatRange
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

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

fun View.showSnack(
    message: String,
    withBottomNavigation: Boolean = false,
    isLong: Boolean = false,
    autoCancel: Boolean = true,
    actionTitle: String? = null,
    actionCallback: (() -> Unit)? = null
) {
    val length = when {
        actionTitle?.isNotEmpty() == true && !autoCancel -> {
            Snackbar.LENGTH_INDEFINITE
        }
        isLong -> Snackbar.LENGTH_LONG
        else -> Snackbar.LENGTH_SHORT
    }
    val snack = Snackbar.make(this, message, length).apply {
        val snackBarView = view
        if (withBottomNavigation) {
            val params = snackBarView.layoutParams as ViewGroup.MarginLayoutParams

            val marginBottom = resources.getDimensionPixelSize(R.dimen.default_bottom_bar_height)
            params.setMargins(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin + marginBottom
            )
            snackBarView.layoutParams = params
        }
        actionTitle?.let { positiveTitle ->
            setAction(positiveTitle) { actionCallback?.invoke(); dismiss() }
        }
    }.run { show() }
}

fun View.showSnack(
    message: String,
    @FloatRange(from = 0.0, to = 3.0, fromInclusive = true, toInclusive = true)
    bottomMarginMultiplier: Float,
    isLong: Boolean = false,
    actionTitle: String? = null,
    actionCallback: (() -> Unit)? = null
) {
    val length = when {
        actionTitle?.isNotEmpty() == true -> Snackbar.LENGTH_INDEFINITE
        isLong -> Snackbar.LENGTH_LONG
        else -> Snackbar.LENGTH_SHORT
    }
    val snack = Snackbar.make(this, message, length).apply {
        val snackBarView = view
        val params = snackBarView.layoutParams as ViewGroup.MarginLayoutParams

        val marginBottom = resources.getDimensionPixelSize(R.dimen.default_bottom_bar_height)
        params.setMargins(
            params.leftMargin,
            params.topMargin,
            params.rightMargin,
            ((params.bottomMargin + marginBottom) * bottomMarginMultiplier).toInt()
        )
        snackBarView.layoutParams = params
        actionTitle?.let { positiveTitle ->
            setAction(positiveTitle) { actionCallback?.invoke(); dismiss() }
        }
    }.run { show() }
}

fun ImageView.loadWithGlide(url: String) {
    Glide.with(this)
        .load(url)
        .into(this)
}

@Throws(IndexOutOfBoundsException::class)
private fun setClickable(textView: TextView, subString: String, handler: () -> Unit, drawUnderline: Boolean = false) {
    val text = textView.text
    val start = text.indexOf(subString, startIndex = 0)
    val end = start + subString.length

    val span = SpannableString(text)
    span.setSpan(ClickHandler(handler, drawUnderline), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

    textView.linksClickable = true
    textView.isClickable = true
    textView.movementMethod = LinkMovementMethod.getInstance()

    textView.text = span
}

private fun setClickable(span: SpannableString, handler: () -> Unit, drawUnderline: Boolean = false) {

}

private class ClickHandler(
    private val handler: () -> Unit,
    private val drawUnderline: Boolean
) : ClickableSpan() {
    override fun onClick(widget: View) {
        handler()
    }

    override fun updateDrawState(ds: TextPaint) {
        if (drawUnderline) {
            ds?.bgColor = Color.TRANSPARENT
            super.updateDrawState(ds)
        } else {
            ds?.isUnderlineText = false
        }
    }
}

fun TextView.makeLinks(links: List<Pair<String, () -> Unit>>, drawUnderline: Boolean = true) {
    try { links.forEach { setClickable(this, it.first, it.second, drawUnderline) } }
    catch (ignore: Exception) { }
}