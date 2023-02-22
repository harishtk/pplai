package com.aiavatar.app.commons.util

import android.R
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.util.Size
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.Commons
import com.aiavatar.app.Constant
import com.aiavatar.app.commons.util.io.FileUtils
import com.aiavatar.app.commons.util.storage.SavedFileResult
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.StringBuilder


object StorageUtil {

    fun cleanUp(context: Context) {
        cleanThumbnails(context)
        cleanUploadDirs(context)
    }

    @WorkerThread
    fun saveFilesToFolder(context: Context, folderName: String = getTempFolderName(), uris: List<Uri>): SavedFileResult? {
        val targetDir = File(context.filesDir, "$DIR_UPLOADS/$folderName")
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                val t = IllegalStateException("Failed to create upload dir")
                Log.w(TAG, "saveFilesToFolder", t)
                return null
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
                        .centerCrop()
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

        return SavedFileResult(
            folderName = targetDir.name,
            savedFiles = savedFiles,
            originalFiles = uris
        )
    }

    fun checkIfFolderExists(context: Context, name: String): Boolean {
        return true
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

    @Throws(IOException::class)
    @WorkerThread
    public suspend fun saveFile(
        context: Context,
        url: String,
        relativePath: String,
        mimeType: String,
        displayName: String,
        downloadProgress: (progress: Int, bytes: Long) -> Unit,
    ): Uri? {
        val outFile = getTempDownloadFile(context) ?: return null
        ImageDownloader(url, outFile, downloadProgress).download()
        Timber.tag("DownloadSeq.Msg").d("Download complete ${outFile.name}")
        val tempUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", outFile, displayName)
        Timber.tag("DownloadSeq.Msg").d("Saving to Gallery")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cr = context.contentResolver

            val relativePathForUri = java.lang.StringBuilder()
                .append(Environment.DIRECTORY_PICTURES)
                .append(File.separator)
                .append(relativePath)
                .toString()

            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathForUri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                values.put(MediaStore.MediaColumns.IS_PENDING, true)
            }

            var savedUri: Uri? = null
            try {
                val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                savedUri = cr.insert(contentUri, values) ?: throw IOException("Failed to create media store record")

                cr.openOutputStream(savedUri)?.use { outStream ->
                    cr.openInputStream(tempUri).use { inputStream ->
                        FileUtils.copy(inputStream, outStream)
                    }
                } ?: throw IOException("Failed to open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, false)
                    cr.update(savedUri, values, null)
                }
                Timber.tag("DownloadSeq.Msg").d("Image saved $savedUri")
                return savedUri
            } catch (e: IOException) {
                savedUri?.let { orphan -> cr.delete(orphan, null, null) }
                return null
            }
        } else {
            val targetDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), relativePath)
            if (!targetDir.exists()) {
                if (!targetDir.mkdirs()) {
                    val t = IOException("Failed to create download directory")
                    Timber.e(t)
                    return null
                }
            }
            val targetFile = File(targetDir, Commons.getFileNameFromUrl(url))
            FileUtils.copyFile(outFile, targetFile)
            Timber.tag("DownloadSeq.Msg").d("Image saved ${targetFile.name}")
            return getImageContentUri(context, targetFile)
        }
    }

    @SuppressLint("Range")
    fun getImageContentUri(context: Context, imageFile: File): Uri? {
        val filePath = imageFile.absolutePath
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=? ", arrayOf(filePath), null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id: Int = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            cursor.close()
            Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id)
        } else {
            if (imageFile.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
            } else {
                null
            }
        }
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

    fun getTempDownloadFile(context: Context): File? {
        val dir = File(context.cacheDir, DIR_DOWNLOAD_CACHE)
        return try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Timber.w("Download cache dir already exists")
                }
            }
            File.createTempFile(TEMP_FILE_PREFIX, System.currentTimeMillis().toString(), dir)
        } catch (e: IOException) {
            Timber.e(e, "Failed to create temp file")
            null
        }
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
    private const val DIR_DOWNLOAD_CACHE = "download_cache"

    private const val UPLOAD_DIR_PREFIX = "Avatar_"
    private const val PHOTO_FILE_PREFIX = "photo_"
    private const val TEMP_FILE_PREFIX = "Temp_"

    private const val PARENT_EXPORT_DIR_NAME = "AI Avatar"
}