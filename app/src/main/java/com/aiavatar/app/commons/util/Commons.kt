package com.aiavatar.app.commons.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.text.DecimalFormat
import java.util.*
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

fun getMimeType(context: Context, uri: Uri): String? {
    var mimeType: String? = null
    mimeType = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        val cr: ContentResolver = context.contentResolver
        cr.getType(uri)
    } else {
        val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(
            uri
                .toString()
        )
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.lowercase(Locale.getDefault())
        )
    }
    return mimeType
}
