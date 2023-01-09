package com.example.app.commons.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

object StorageUtil {

    fun cleanUp(context: Context) {
        cleanThumbnails(context)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveThumbnail(context: Context, bitmap: Bitmap, streamName: String, fileName: String): File? {
        val targetDir = File(context.filesDir, "$DIR_THUMBNAILS/$streamName")
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                val t = IllegalStateException("Failed to create thumbnail dir")
                Log.w("$TAG#saveThumbnail", t)
                return null
            }
        }

        val compressed = Glide.with(context.applicationContext)
            .asBitmap()
            .load(bitmap)
            .override(MAX_THUMB_WIDTH, MAX_THUMB_HEIGHT)
            .submit().get()

        val file = File(targetDir, "$fileName$EXTENSION_JPEG")
        runBlocking {
            FileOutputStream(file).use { outputStream ->
                compressed.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, outputStream)
                outputStream.flush()
            }
        }
        rotateThumbnails(targetDir, 5)
        return file
    }

    @Suppress("SameParameterValue")
    private fun rotateThumbnails(dir: File, maxKeep: Int = 5) {
        val files: Array<File> = dir.listFiles { _, name ->
            name != FIRST_THUMBNAIL_FILENAME
        } ?: return
        if (files.size > maxKeep) {
            files.sortedBy { it.lastModified() }
                .take((files.size - maxKeep).coerceAtLeast(0))
                .forEach { it.delete() }
            Log.d(TAG, "rotateThumbnails: ${files.map { it.name }}")
        }
    }

    private fun cleanThumbnails(context: Context) {
        val dir = File(context.filesDir, DIR_THUMBNAILS)
        dir.deleteRecursively()
        Log.d(TAG, "cleanThumbnails: success")
    }

    const val THUMB_PREFIX = "thumbnail_"
    private const val EXTENSION_JPEG = ".jpg"

    const val FIRST_THUMBNAIL_FILENAME = "${THUMB_PREFIX}00$EXTENSION_JPEG"

    private const val MAX_THUMB_WIDTH = 720
    private const val MAX_THUMB_HEIGHT = 1280

    private const val THUMBNAIL_JPEG_QUALITY = 70

    private val TAG = StorageUtil::class.java.simpleName
    private const val DIR_THUMBNAILS = "thumbnails"
}