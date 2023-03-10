package com.aiavatar.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkUtil {

    fun scheduleUploadWorker(applicationContext: Context, sessionId: Long): Operation {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(UploadWorker.KEY_SESSION_ID, sessionId)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInitialDelay(0L, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        val opr = WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            UploadWorker.WORKER_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        return opr
    }

    fun scheduleDownloadWorker(context: Context, downloadSessionId: Long) {
        DownloadWorker.Builder()
            .setDownloadSessionId(downloadSessionId)
            .buildOneTimeRequest().apply {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    DownloadWorker.WORKER_NAME,
                    ExistingWorkPolicy.REPLACE,
                    this
                )
            }
    }

    fun clearScheduledUploadWorker(applicationContext: Context) {
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(UploadWorker.WORKER_NAME)
    }

}