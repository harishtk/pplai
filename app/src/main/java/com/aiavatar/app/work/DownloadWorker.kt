package com.aiavatar.app.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.*
import com.aiavatar.app.Commons
import com.aiavatar.app.Constant
import com.aiavatar.app.MainActivity
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.commons.util.concurrent.ThreadSafeCounter
import com.aiavatar.app.core.data.source.local.AppDatabase
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
        if (!checkStoragePermission(context)) {
            // Nothing to do much.
            return abortWork("No Storage permission granted, aborting photos download.")
        }

        val modelId = workerParameters.inputData.getString(MODEL_ID)
            ?: return abortWork("No model id.")

        val avatarStatusWithFiles = appDatabase.avatarStatusDao().getAvatarStatusForModelIdSync(modelId)
            ?: return abortWork("No download session data found for model $modelId")

        setForegroundAsync(createForegroundInfo(0))
        // TODO: prepare for download
        val relativeDownloadPath = StringBuilder()
            .append(context.getString(R.string.app_name))
            .append(File.separator)
            .append(avatarStatusWithFiles.avatarStatusEntity.modelName)
            .toString()

        var failedCount: Int = 0
        val jobs = avatarStatusWithFiles.avatarFilesEntity
            .filter { entity -> entity.downloaded == 0 }
            .map { avatarFilesEntity ->
            workerScope.launch {
                kotlin.runCatching {
                    val savedUri = StorageUtil.saveFile(
                        context = context,
                        url = avatarFilesEntity.remoteFile,
                        relativePath = relativeDownloadPath,
                        mimeType = Constant.MIME_TYPE_JPEG,
                        displayName = Commons.getFileNameFromUrl(avatarFilesEntity.remoteFile),
                    ) { progress, bytesDownloaded ->
                        workerScope.launch {
                            appDatabase.avatarFilesDao().updateDownloadProgress(
                                id = avatarFilesEntity._id!!,
                                progress
                            )
                            if (progress == 100) {
                                appDatabase.avatarFilesDao().updateDownloadStatus(
                                    id = avatarFilesEntity._id!!,
                                    downloaded = true,
                                    downloadedAt = System.currentTimeMillis(),
                                    downloadSize = bytesDownloaded
                                )
                            }
                        }
                    }

                    appDatabase.avatarFilesDao().apply {
                        if (savedUri != null) {
                            updateLocalUri(avatarFilesEntity._id!!, savedUri.toString())
                        } else {
                            failedCount++
                            updateDownloadStatus(
                                id = avatarFilesEntity._id!!,
                                downloaded = false,
                                downloadedAt = 0L,
                                downloadSize = 0L
                            )
                        }
                    }
                }.onFailure { t ->
                    Timber.e(t)

                }
            }
        }

        totalDownloads = jobs.count()

        jobs.map {
            it.join()
            updateDownloadCounter()
        }

        Timber.d("Download failed for $failedCount files")

        notifyDownloadComplete(context)
        return Result.success()
    }

    private fun updateDownloadCounter() {
        val current = downloadCounter.increment()
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

        // TODO: add pending intent to create avatar
        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

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
        createUploadNotificationChannel(context)

        // Build a notification using bytesRead and contentLength
        val context = applicationContext
        val channelId = context.getString(R.string.download_notification_channel_id)

        // TODO: show notification
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

        const val WORKER_NAME = "download_worker"

        const val STATUS_NOTIFICATION_ID = 101
        private const val ONGOING_NOTIFICATION_ID = 100

        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
    }
}