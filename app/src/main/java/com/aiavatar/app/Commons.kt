package com.aiavatar.app

import java.util.Random
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Size
import android.view.View
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavOptions
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.set
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

typealias Continuation = () -> Unit

private const val EXPORT_DIR = "Export"
private const val TEMP_DIR = "Temp"

private const val FILENAME_REAR = "rearImage.jpg"
private const val FILENAME_FRONT = "frontImage.jpg"
private const val FILENAME_EXPORTED_POST = "exported.jpg"
private const val FILENAME_ACHIEVEMENT_TOKEN = "token.jpg"

public fun parseViews(viewCount: Int): String = when {
    viewCount < 50 -> "$viewCount"
    viewCount < 100 -> "50+"
    viewCount < 1000 -> "${(viewCount / 100).toInt() * 100}+"
    else -> {
        val exp = (ln(viewCount.toDouble()) / ln(1000.0)).toInt()
        val format = DecimalFormat("##.#")
        String.format(
            "${format.format(viewCount / 1000.0.pow(exp.toDouble()))}%c",
            "kMGTPE"[exp - 1]
        )
    }
}

fun defaultNavOptsBuilder(): NavOptions.Builder {
    return NavOptions.Builder()
        .setEnterAnim(R.anim.fade_scale_in)
        .setExitAnim(R.anim.fade_scale_out)
}

/**
 *
 */
private fun saveImageInCache(
    context: Context,
    outFile: File,
    uri: Uri
): String {
    val sizeHd = Size(720, 1280)
    val bmp = Glide.with(context)
        .asBitmap()
        .load(uri)
        .override(sizeHd.width, sizeHd.height)
        .submit()
        .get()
    FileOutputStream(outFile).use {
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return outFile.absolutePath
}

fun saveTempImage(
    context: Context,
    bmp: Bitmap
) : File? {
    runCatching {
        val cacheDir = File(context.cacheDir, TEMP_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs().also {
                if (!it) {
                    Timber.tag("Export.Msg").w("Cannot create cache dir!")
                }
            }
        }
        val outFile = getNewFile(File(cacheDir, FILENAME_ACHIEVEMENT_TOKEN))
        FileOutputStream(outFile).use {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        outFile
    }.fold(
        onSuccess = { return it },
        onFailure = { return null }
    )
}

private fun galleryAddPic(context: Context, imagePath: String) {
    MediaScannerConnection.scanFile(
        context,
        arrayOf(imagePath),
        null,
        null,
    )
}

fun getRoundedCornerBitmap(bitmap: Bitmap, @Px cornerSize: Float): Bitmap? {
    val output = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawRoundRect(rectF, cornerSize, cornerSize, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}

private fun getNewFile(target: File): File {
    if (!target.exists()) target.createNewFile()
    else {
        target.delete()
        target.createNewFile()
    }
    return target
}

fun getRoundedDrawable(@Px radius: Int, bgColor: Int): GradientDrawable {
    return GradientDrawable().apply {
        setColor(bgColor)
        cornerRadii = floatArrayOf(
            radius.toFloat(), radius.toFloat(), // top left
            radius.toFloat(), radius.toFloat(), // top right
            radius.toFloat(), radius.toFloat(), // bottom right
            radius.toFloat(), radius.toFloat()  // bottom left
        )
    }
}

fun makeColoredWeightedSubstring(
    context: Context,
    originalString: String,
    subString: String,
    onClick: () -> Unit = {}
) : SpannableString {
    val spannableString = SpannableString(originalString)
    try {
        val start = originalString.indexOf(subString)
        val end = start + subString.length
        val color = ResourcesCompat.getColor(
            context.resources,
            R.color.yellow_200,
            null
        )
        val clickHandler = object : ClickableSpan() {
            override fun onClick(p0: View) {
                onClick()
            }
        }
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(clickHandler, start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(color), start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    } catch (ignore: Exception) { }
    return spannableString
}

fun makeWeightedSubstring(
    context: Context,
    originalString: String,
    subString: String
) : SpannableString {
    val spannableString = SpannableString(originalString)
    try {
        val start = originalString.indexOf(subString)
        val end = start + subString.length
        val color = ResourcesCompat.getColor(
            context.resources,
            R.color.yellow_200,
            null
        )
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    } catch (ignore: Exception) { }
    return spannableString
}

fun makeColoredSubstring(
    context: Context,
    originalString: String,
    subStrings: List<String>
) : SpannableString {
    val spannableString = SpannableString(originalString)
    try {
        for (subString in subStrings) {
            val start = originalString.indexOf(subString)
            val end = start + subString.length
            val color = ResourcesCompat.getColor(
                context.resources,
                R.color.yellow_200,
                null
            )
            spannableString.setSpan(ForegroundColorSpan(color), start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    } catch (ignore: Exception) { }
    return spannableString
}

fun tag(clazz: Class<*>): String {
    val simpleName = clazz.simpleName
    return simpleName.substring(0, simpleName.length.coerceAtMost(23))
}

fun parseUtmParameters(utmString: String): Map<String, String> {
    // utm_campaign=GTVm5tFUsXvj&utm_medium=invite&utm_source=pepulnow
    try {
        if (utmString.isBlank()) {
            return mapOf()
        }
        val mapping: MutableMap<String, String> = mutableMapOf()
        utmString.split("&").forEach {
            val key = it.split("=")[0]
            val value = it.split("=")[1]
            if (key.isNotBlank()) {
                mapping[key] = value
            }
        }
        return mapping
    } catch (e: IndexOutOfBoundsException) {
        Timber.d(e, "Failed to parse utm parameters")
        return mapOf()
    }
}

class PhoneNumberFormatException(override val message: String): Exception()

fun isContactPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalTime::class)
fun countDownFlow(
    start: kotlin.time.Duration,
    step: kotlin.time.Duration = 1.seconds,
    initialDelay: kotlin.time.Duration = kotlin.time.Duration.ZERO,
): Flow<Long> = flow {
    var counter: Long = start.toLong(DurationUnit.MILLISECONDS)
    delay(initialDelay.toLong(DurationUnit.MILLISECONDS))
    while (counter >= 0) {
        emit(counter)
        if (counter != 0L) {
            delay(step.toLong(DurationUnit.MILLISECONDS))
            counter -= step.toLong(DurationUnit.MILLISECONDS)
        } else {
            break
        }
    }
}

fun getRandomHexCode(): String {
    val random = Random()
    val int = random.nextInt(0xffffff + 1)
    return String.format("#%06x", int)
}
