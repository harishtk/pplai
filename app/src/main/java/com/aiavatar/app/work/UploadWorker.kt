package com.aiavatar.app.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aiavatar.app.Constant
import com.aiavatar.app.MainActivity
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.net.ProgressRequestBody
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.onboard.domain.model.UploadImageData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import timber.log.Timber
import kotlin.math.abs
import kotlin.random.Random

@HiltWorker
class UploadWorker @AssistedInject constructor(
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun doWork(): Result {
        setForegroundAsync(createForegroundInfo(0))

        val sessionId = workerParameters.inputData.getLong(KEY_SESSION_ID, -1L)
        if (sessionId == -1L) {
            return abortWork("No session id, aborting photo upload.")
        }

        val uploadSessionWithFiles = appDatabase.uploadSessionDao().getUploadSessionSync(sessionId)
            ?: return abortWork("No upload session data found for session $sessionId")

        appDatabase.uploadSessionDao().updateUploadSessionStatus(
            uploadSessionWithFiles.uploadSessionEntity._id!!,
            UploadSessionStatus.PARTIALLY_DONE.status
        )
        val uploadResultList: List<Deferred<com.aiavatar.app.commons.util.Result<UploadImageData>>> =
            uploadSessionWithFiles.uploadFilesEntity.map { uploadFilesEntity ->
                val task = workerScope.async {
                    Timber.d("Preparing upload ${uploadFilesEntity.fileUriString}")
                    val file = Uri.parse(uploadFilesEntity.fileUriString).toFile()
                    val progressRequestBody = ProgressRequestBody(
                        file,
                        Constant.MIME_TYPE_JPEG,
                        object : ProgressRequestBody.ProgressCallback {
                            override fun onProgressUpdate(percentage: Int) {
                                Timber.d("Uploading: ${file.name} PROGRESS $percentage")
                                // progressCallback((percentage / 10f).coerceIn(0.0F, 1.0F))
                                runBlocking {
                                    appDatabase.uploadFilesDao()
                                        .updateFileUploadProgress(uploadFilesEntity._id!!, percentage)
                                }
                            }

                            override fun onError() {

                            }
                        }
                    )
                    val filePart: MultipartBody.Part =
                        MultipartBody.Part.createFormData(
                            "files",
                            file.name,
                            progressRequestBody
                        )
                    val folderNamePart: MultipartBody.Part =
                        MultipartBody.Part.createFormData(
                            "foldername",
                            uploadSessionWithFiles.uploadSessionEntity.folderName
                        )
                    val typePart = MultipartBody.Part.createFormData(
                        "type",
                        "photo_sample"
                    )

                    val result = appRepository.uploadFileSync(folderNamePart, typePart, filePart)
                    when (result) {
                        is com.aiavatar.app.commons.util.Result.Loading -> {
                            appDatabase.uploadFilesDao().updateFileStatus(
                                uploadFilesEntity._id!!,
                                UploadFileStatus.UPLOADING.status
                            )
                        }

                        is com.aiavatar.app.commons.util.Result.Success -> {
                            appDatabase.uploadFilesDao().updateUploadedFileName(
                                uploadFilesEntity._id!!,
                                result.data.imageName,
                                System.currentTimeMillis()
                            )
                            appDatabase.uploadFilesDao().updateFileStatus(
                                uploadFilesEntity._id!!,
                                UploadFileStatus.COMPLETE.status
                            )
                        }

                        is com.aiavatar.app.commons.util.Result.Error -> {
                            appDatabase.uploadFilesDao().updateFileStatus(
                                uploadFilesEntity._id!!,
                                UploadFileStatus.FAILED.status
                            )
                        }
                    }
                    result
                }
                task
            }

        uploadResultList.awaitAll()
        uploadResultList.map { it.getCompleted() }
            .forEachIndexed { index, result ->
                if (result is com.aiavatar.app.commons.util.Result.Success) {
                    Timber.d("Upload result: ${index + 1} ${result.data.imageName} success")
                }
            }
        appDatabase.uploadSessionDao().updateUploadSessionStatus(
            uploadSessionWithFiles.uploadSessionEntity._id!!,
            UploadSessionStatus.UPLOAD_COMPLETE.status
        )

        notifyUploadComplete(context, uploadResultList.size)
        return Result.success()
    }

    private fun abortWork(message: String): Result {
        val outputData: Data = Data.Builder()
            .putString(
                EXTRA_ERROR_MESSAGE,
                message
            )
            .build()
        return Result.failure(outputData)
    }

    private fun notifyUploadComplete(context: Context, photosCount: Int) {
        val channelId = context.getString(R.string.upload_notification_channel_id)

        // TODO: add pending intent to create avatar
        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentTitle("Upload Complete!")
            .setContentText("$photosCount Photos uploaded. Tap here to create your avatar!")
            .setContentIntent(contentIntent)
            .build()
        ServiceUtil.getNotificationManager(context)
            .notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        createUploadNotificationChannel(context)

        // Build a notification using bytesRead and contentLength
        val context = applicationContext
        // This PendingIntent can be used to cancel the worker
        val intent: PendingIntent = WorkManager.getInstance(context)
            .createCancelPendingIntent(getId())
        val channelId = context.getString(R.string.upload_notification_channel_id)

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        // TODO: show notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentTitle("Preparing upload")
            .setProgress(100, progress, true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
        return ForegroundInfo(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createUploadNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.upload_notification_channel_id)
        val channelName = context.getString(R.string.title_upload_notifications)
        val channelDesc = context.getString(R.string.desc_upload_notifications)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }


    companion object {
        const val WORKER_NAME = "upload_worker"
        const val STATUS_NOTIFICATION_ID = 101
        private const val ONGOING_NOTIFICATION_ID = 100

        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
        const val KEY_SESSION_ID = "com.aiavatar.app.keys.SESSION_ID"

        fun generateNewNotificationId(): Int {
            return abs(Random.nextInt())
        }
    }
}