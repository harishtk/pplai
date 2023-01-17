package com.aiavatar.app.commons.util

import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

public fun parseViews(viewCount: Int): String = when {
    viewCount < 1000 -> "$viewCount"
    else -> {
        val exp = (ln(viewCount.toDouble()) / ln(1000.0)).toInt()
        val format = DecimalFormat("##.#")
        String.format(
            "${format.format(viewCount / 1000.0.pow(exp.toDouble()))}%c",
            "kMGTPE"[exp - 1]
        )
    }
}
