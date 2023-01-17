package com.aiavatar.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class UploadService @Inject constructor(
    private val homeRepository: HomeRepository
) : Service() {

    private val localBinder: IBinder = LocalBinder()

    /* Exposed variables */

    override fun onCreate() {
        super.onCreate()

        // TODO: launch foreground service
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBlocking {
            // TODO: perform upload
        }
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    inner class LocalBinder : Binder() {
        fun getService(): UploadService {
            return this@UploadService
        }
    }

}