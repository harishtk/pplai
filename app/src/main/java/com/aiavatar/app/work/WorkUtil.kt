package com.aiavatar.app.work

import android.content.Context
import android.provider.ContactsContract
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.aiavatar.app.Constant
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

    fun clearScheduledUploadWorker(applicationContext: Context) {
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(UploadWorker.WORKER_NAME)
    }

}