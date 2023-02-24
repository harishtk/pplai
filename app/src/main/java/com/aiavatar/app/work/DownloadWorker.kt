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
import android.util.Log
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val appRepository: AppRepository,
    @Deprecated("migrate to repo")
    private val appDatabase: AppDatabase,
) : CoroutineWorker(context, workerParameters) {

    private val NUM_THREADS: Int = Runtime.getRuntime().availableProcessors() * 2

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, t ->
            Timber.e(t)
        }

    private val backgroundDispatcher
        = newFixedThreadPoolContext(NUM_THREADS, "Download photos pool")

    private val workerContext =
        backgroundDispatcher.limitedParallelism(8) + SupervisorJob() + coroutineExceptionHandler

    private val workerScope = CoroutineScope(context = workerContext)

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var downloadCounter: ThreadSafeCounter = ThreadSafeCounter()
    private var totalDownloads: Int = 0

    override suspend fun doWork(): Result {
        Timber.d("NUM_CORES: $NUM_THREADS")
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

        withContext(Dispatchers.IO) {
            appDatabase.downloadSessionDao().apply {
                updateDownloadWorkerId(sessionWithFilesEntity.downloadSessionEntity._id!!, workerId = id.toString())
            }
            setForeground(createForegroundInfo(0))
        }

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
                        Timber.tag("DownloadSeq.Msg").d("Initiating download ${downloadFilesEntity.fileUriString}")
                        var lastProgress = -1
                        var statusUpdated = false
                        val savedUri = StorageUtil.saveFile(
                            context = context,
                            url = downloadFilesEntity.fileUriString,
                            relativePath = relativeDownloadPath,
                            mimeType = mimeType,
                            displayName = Commons.getFileNameFromUrl(downloadFilesEntity.fileUriString),
                        ) { progress, bytesDownloaded ->
                            /* To avoid raining updates to the database. */
                            val shouldUpdateProgress = lastProgress == -1 || abs(progress - lastProgress) > UPDATE_PROGRESS_DELTA
                                    || progress == 100

                            runBlocking(Dispatchers.IO) {
                                appDatabase.downloadFilesDao().apply {
                                    if (!statusUpdated) {
                                        updateFileStatus(
                                            id = downloadFilesEntity._id!!,
                                            DownloadFileStatus.DOWNLOADING.status
                                        )
                                        statusUpdated = true
                                    }

                                    if (shouldUpdateProgress) {
                                        lastProgress = progress
                                        Timber.d("Update progress: progress = $progress bytes = $bytesDownloaded")
                                        updateFileDownloadProgress(
                                            id = downloadFilesEntity._id!!,
                                            progress
                                        )

                                        if (progress == 100) {
                                            updateDownloadStatus(
                                                id = downloadFilesEntity._id!!,
                                                downloaded = true,
                                                downloadedAt = System.currentTimeMillis(),
                                                downloadSize = bytesDownloaded
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Timber.tag("DownloadSeq.Msg").d("Updating database ${downloadFilesEntity.fileUriString}")
                        withContext(Dispatchers.IO) {
                            appDatabase.downloadFilesDao().apply {
                                if (savedUri != null) {
                                    updateDownloadedFileName(downloadFilesEntity._id!!, savedUri.toString())
                                    updateFileStatus(
                                        id = downloadFilesEntity._id!!,
                                        DownloadFileStatus.COMPLETE.status
                                    )
                                    updateDownloadCounter()
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
                                    updateDownloadCounter()
                                }
                                Timber.tag("DownloadSeq.Msg").d("Download Finished. Waiting to join.. ${downloadFilesEntity.fileUriString}")
                            }
                        }
                    }.onFailure { t ->
                        if (BuildConfig.DEBUG) {
                            Timber.e(t)
                        }
                        appDatabase.downloadFilesDao().updateFileStatus(
                            id = downloadFilesEntity._id!!,
                            DownloadFileStatus.FAILED.status
                        )
                        updateDownloadCounter()
                    }
                }
            }

        totalDownloads = jobs.count()
        Timber.d("Downloading $totalDownloads images")

        /*withContext(Dispatchers.IO) {
            Timber.d("Waiting for $totalDownloads to complete")
            downloadCountLatch.await(15, TimeUnit.MINUTES)
        }*/
        Timber.d("Waiting for $totalDownloads to complete")

        /* CAUTION: [Job#join] completes only when all of the children are complete. */
        jobs.map {
            Timber.tag("Thread.Msg").d("parent: $it children = ${it.children.count()}")
            it.join()
        }

        appDatabase.downloadSessionDao().updateDownloadSessionStatus(
            sessionWithFilesEntity.downloadSessionEntity._id!!, DownloadSessionStatus.COMPLETE.status)
        appDatabase.downloadSessionDao().updateDownloadWorkerId(
            sessionWithFilesEntity.downloadSessionEntity._id!!, null
        )

        Timber.d("Download: complete. $failedCount failed")

        notifyDownloadComplete(context)
        return Result.success()
    }

    private fun updateDownloadCounter() {
        val current = downloadCounter.increment()
        Log.d("Counter", "Download counter: $current")
        val progress = if (totalDownloads != 0) {
            (current * 100) / totalDownloads
        } else {
            100
        }
        setForegroundAsync(createForegroundInfo(progress, current, totalDownloads))
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

    private fun createForegroundInfo(progress: Int, current: Int = -1, total: Int = -1): ForegroundInfo {
        Timber.d("createForegroundInfo: $progress")
        createDownloadNotificationChannel(context)

        // Build a notification using bytesRead and contentLength
        val context = applicationContext
        val channelId = context.getString(R.string.download_notification_channel_id)

        val contentMessage = if (current == -1 || totalDownloads == -1) {
            "Preparing to download"
        } else {
            "Downloading photos $current of $total"
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setSilent(true)
            .setContentTitle(contentMessage)
            .setOngoing(true)

        if (progress > 0) {
            notificationBuilder.setProgress(100, progress, false)
        } else {
            notificationBuilder.setProgress(100, 0, true)
        }
        return ForegroundInfo(ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createDownloadNotificationChannel(context: Context) {
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

        private val UPDATE_PROGRESS_DELTA: Int = 15

        const val STATUS_NOTIFICATION_ID = 102
        private const val ONGOING_NOTIFICATION_ID = 103

        private const val OPEN_GALLERY_REQUEST_CODE = 100

        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
    }
}