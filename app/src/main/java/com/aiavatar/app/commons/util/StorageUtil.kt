package com.aiavatar.app.commons.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Size
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object StorageUtil {

    fun cleanUp(context: Context) {
        cleanThumbnails(context)
        cleanUploadDirs(context)
    }

    @WorkerThread
    fun saveFilesToFolder(context: Context, folderName: String = getTempFolderName(), uris: List<Uri>): Pair<String?, List<File>> {
        val targetDir = File(context.filesDir, "$DIR_UPLOADS/$folderName")
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                val t = IllegalStateException("Failed to create upload dir")
                Log.w(TAG, "saveFilesToFolder", t)
                return null to emptyList()
            }
        }

        val cr = context.contentResolver
        val savedFiles = uris.mapNotNull { uri ->
            try {
                val outFile = File(targetDir, getNewPhotoFileName())
                Timber.d("Output file: ${outFile.absolutePath}")

                val reqSize = Size(UPLOAD_MAX_WIDTH, UPLOAD_MAX_HEIGHT)
                runBlocking(Dispatchers.IO) {
                    val compressed = Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(uri)
                        .override(reqSize.width, reqSize.height)
                        .submit().get()

                    BufferedOutputStream(FileOutputStream(outFile)).use { outputStream ->
                        compressed.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, outputStream)
                        outputStream.flush()
                    }
                }
                return@mapNotNull outFile
                /*cr.openFileDescriptor(uri, "r").use { pfd ->
                    if (pfd != null) {

                    } else {
                        return@mapNotNull null
                    }
                }*/
            } catch (e: IOException) {
                Timber.e(e)
                null
            }
        }

        Timber.d("Saved Files: $savedFiles")

        return targetDir.name to savedFiles
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

    private fun cleanUploadDirs(context: Context) {
        val dir = File(context.filesDir, DIR_UPLOADS)
        dir.deleteRecursively()
        Log.d(TAG, "cleanUploadDirs: success")
    }

    fun getTempFolderName(): String {
        return UPLOAD_DIR_PREFIX + System.currentTimeMillis()
    }

    private fun getNewPhotoFileName(): String {
        return PHOTO_FILE_PREFIX + System.currentTimeMillis() + EXTENSION_JPEG
    }

    @WorkerThread
    fun createScaledBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }
        if (maxWidth <= 0 || maxHeight <= 0) {
            return bitmap
        }
        var newWidth = maxWidth
        var newHeight = maxHeight
        val widthRatio = bitmap.width / maxWidth.toFloat()
        val heightRatio = bitmap.height / maxHeight.toFloat()
        if (widthRatio > heightRatio) {
            newHeight = (bitmap.height / widthRatio).toInt()
        } else {
            newWidth = (bitmap.width / heightRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (width: Int, height: Int) = options.run { outWidth to outHeight }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    const val THUMB_PREFIX = "thumbnail_"
    private const val EXTENSION_JPEG = ".jpg"

    const val FIRST_THUMBNAIL_FILENAME = "${THUMB_PREFIX}00$EXTENSION_JPEG"

    private const val MAX_THUMB_WIDTH = 720
    private const val MAX_THUMB_HEIGHT = 1280

    private const val UPLOAD_MAX_SIZE = 512
    private const val UPLOAD_MAX_WIDTH = 512
    private const val UPLOAD_MAX_HEIGHT = 512

    private const val THUMBNAIL_JPEG_QUALITY = 70
    private const val UPLOAD_JPEG_QUALITY = 80

    private const val DEFAULT_BUFF_SIZE = 1024

    private val TAG = StorageUtil::class.java.simpleName
    private const val DIR_THUMBNAILS = "thumbnails"
    private const val DIR_UPLOADS    = "uploads"

    private const val UPLOAD_DIR_PREFIX = "AI Avatar_"
    private const val PHOTO_FILE_PREFIX = "photo_"
}