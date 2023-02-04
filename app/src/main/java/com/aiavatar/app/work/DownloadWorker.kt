package com.aiavatar.app.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.Commons
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.commons.util.concurrent.ThreadSafeCounter
import com.aiavatar.app.commons.util.getMimeType
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.DownloadFileStatus
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.core.data.source.local.model.DownloadSessionWithFilesEntity
import com.aiavatar.app.core.domain.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val appRepository: AppRepository,
    @Deprecated("migrate to repo")
    private val appDatabase: AppDatabase,
) : CoroutineWorker(context, workerParameters) {

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, t ->
            Timber.e(t)
        }
    private val workerContext =
        Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler

    private val workerScope = CoroutineScope(context = workerContext)

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var downloadCounter: ThreadSafeCounter = ThreadSafeCounter()
    private var totalDownloads: Int = 0

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!checkStoragePermission(context)) {
                // Nothing to do much.
                return abortWork("No Storage permission granted, aborting photos download.")
            }
        }

        Timber.d("input data ${workerParameters.inputData.keyValueMap.values.first().javaClass.simpleName}")

        val sessionId: Long = workerParameters.inputData.getLong(SESSION_ID, -1)

        val sessionWithFilesEntity: DownloadSessionWithFilesEntity = appDatabase.downloadSessionDao()
            .getDownloadSessionSync(sessionId)
            ?: return abortWork("No download session data found for session id $sessionId")

        appDatabase.downloadSessionDao().apply {
            updateDownloadWorkerId(sessionWithFilesEntity.downloadSessionEntity._id!!, workerId = id.toString())
        }

        setForegroundAsync(createForegroundInfo(0))

        val relativeDownloadPath = StringBuilder()
            .append(context.getString(R.string.app_name))
            .append(File.separator)
            .append(sessionWithFilesEntity.downloadSessionEntity.folderName)
            .toString()

        /*val viewUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StringBuilder()
                .append(Environment.DIRECTORY_PICTURES)
                .append(File.separator)
                .append(relativeDownloadPath)
                .toString()
        } else {

        }*/

        appDatabase.downloadSessionDao().updateDownloadSessionStatus(
            sessionWithFilesEntity.downloadSessionEntity._id!!, DownloadSessionStatus.PARTIALLY_DONE.status)

        var failedCount = 0
        val jobs = sessionWithFilesEntity.downloadFilesEntity
            .filter { entity -> entity.downloaded == 0 }
            .map { downloadFilesEntity ->
                workerScope.launch {
                    val mimeType = getMimeType(context, downloadFilesEntity.fileUriString.toUri())
                        ?: Constant.MIME_TYPE_JPEG
                    kotlin.runCatching {
                        val savedUri = StorageUtil.saveFile(
                            context = context,
                            url = downloadFilesEntity.fileUriString,
                            relativePath = relativeDownloadPath,
                            mimeType = mimeType,
                            displayName = Commons.getFileNameFromUrl(downloadFilesEntity.fileUriString),
                        ) { progress, bytesDownloaded ->
                            workerScope.launch {
                                appDatabase.downloadFilesDao().apply {
                                    updateFileDownloadProgress(
                                        id = downloadFilesEntity._id!!,
                                        progress
                                    )
                                    updateFileStatus(
                                        id = downloadFilesEntity._id!!,
                                        DownloadFileStatus.DOWNLOADING.status
                                    )
                                }
                                if (progress == 100) {
                                    appDatabase.downloadFilesDao().updateDownloadStatus(
                                        id = downloadFilesEntity._id!!,
                                        downloaded = true,
                                        downloadedAt = System.currentTimeMillis(),
                                        downloadSize = bytesDownloaded
                                    )
                                }
                            }
                        }

                        appDatabase.downloadFilesDao().apply {
                            if (savedUri != null) {
                                updateDownloadedFileName(downloadFilesEntity._id!!, savedUri.toString())
                                updateFileStatus(
                                    id = downloadFilesEntity._id!!,
                                    DownloadFileStatus.COMPLETE.status
                                )
                            } else {
                                failedCount++
                                updateDownloadStatus(
                                    id = downloadFilesEntity._id!!,
                                    downloaded = false,
                                    downloadedAt = 0L,
                                    downloadSize = 0L
                                )
                                updateFileStatus(
                                    id = downloadFilesEntity._id!!,
                                    DownloadFileStatus.FAILED.status
                                )
                            }
                        }
                    }.onFailure { t ->
                        if (BuildConfig.DEBUG) {
                            Timber.e(t)
                        }
                    }
                }
            }

        totalDownloads = jobs.count()
        Timber.d("Downloading $totalDownloads images")

        jobs.map {
            it.join()
            updateDownloadCounter()
        }

        appDatabase.downloadSessionDao().updateDownloadSessionStatus(
            sessionWithFilesEntity.downloadSessionEntity._id!!, DownloadSessionStatus.COMPLETE.status)
        appDatabase.downloadSessionDao().updateDownloadWorkerId(
            sessionWithFilesEntity.downloadSessionEntity._id!!, null
        )

        Timber.d("Download failed for $failedCount files")

        notifyDownloadComplete(context)
        return Result.success()
    }

    private fun updateDownloadCounter() {
        val current = downloadCounter.increment()
        Timber.d("Download counter: $current")
        val progress = if (totalDownloads != 0) {
            (current * 100) / totalDownloads
        } else {
            100
        }
        createForegroundInfo(progress)
    }

    private fun abortWork(message: String): Result {
        val outputData: Data = Data.Builder()
            .putString(
                UploadWorker.EXTRA_ERROR_MESSAGE,
                message
            )
            .build()
        return Result.failure(outputData)
    }

    private fun checkStoragePermission(context: Context): Boolean {
        return storagePermissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun notifyDownloadComplete(context: Context) {

        val channelId = context.getString(R.string.download_notification_channel_id)

        val galleryIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            type = Constant.MIME_TYPE_IMAGE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentIntent = PendingIntentCompat.getActivity(
            context,
            OPEN_GALLERY_REQUEST_CODE,
            galleryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentTitle("Download Complete!")
            .setContentText("Tap here to view your downloaded avatars")
            .setContentIntent(contentIntent)
            .build()
        ServiceUtil.getNotificationManager(context)
            .notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        Timber.d("createForegroundInfo: $progress")
        createUploadNotificationChannel(context)

        // Build a notification using bytesRead and contentLength
        val context = applicationContext
        val channelId = context.getString(R.string.download_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentTitle("Downloading photos")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        return ForegroundInfo(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createUploadNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.download_notification_channel_id)
        val channelName = context.getString(R.string.title_download_notifications)
        val channelDesc = context.getString(R.string.desc_download_notifications)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }

    class Builder {
        private val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        private val inputDataBuilder: Data.Builder = Data.Builder()

        fun setModelId(modelId: String): Builder {
            inputDataBuilder.putString(MODEL_ID, modelId)
            return this
        }

        fun setStatusId(statusId: String): Builder {
            inputDataBuilder.putString(STATUS_ID, statusId)
            return this
        }

        fun setDownloadSessionId(sessionId: Long): Builder {
            inputDataBuilder.putLong(SESSION_ID, sessionId)
            return this
        }

        fun buildOneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setInputData(inputDataBuilder.build())
                .build()
        }
    }

    companion object {
        private const val MODEL_ID = "model_id"
        private const val STATUS_ID = "status_id"
        private const val SESSION_ID = "session_id"

        const val WORKER_NAME = "download_worker"

        const val STATUS_NOTIFICATION_ID = 102
        private const val ONGOING_NOTIFICATION_ID = 103

        private const val OPEN_GALLERY_REQUEST_CODE = 100

        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
    }
}