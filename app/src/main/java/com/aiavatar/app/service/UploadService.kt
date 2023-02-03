package com.aiavatar.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UploadService : CoroutineService() {

    private val localBinder: IBinder = LocalBinder()

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    /* Exposed variables */

    /* END - Exposed variables */

    override fun onCreate() {
        super.onCreate()

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand: $intent $flags $startId")
        val action = intent?.action
        when (action) {
            ACTION_START_SERVICE -> {
                setupForegroundService(this)
                serviceHandler?.obtainMessage()?.also { msg ->
                    msg.arg1 = startId
                    msg.data = bundleOf(
                        Constant.EXTRA_SESSION_ID to intent.getLongExtra(Constant.EXTRA_SESSION_ID, -1L)
                    )
                    serviceHandler?.sendMessage(msg)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun notifyUploadComplete(context: Context) {
        val channelId = context.getString(R.string.upload_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentTitle("Upload Complete!")
            .setContentText("12 Photos uploaded. Tap here to create your avatar!")
            .build()
        ServiceUtil.getNotificationManager(context)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun setupForegroundService(context: Context, progress: Int = 0) {
        createUploadNotificationChannel(context)

        val channelId = context.getString(R.string.upload_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentTitle("Preparing upload")
            .setProgress(100, progress, true)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    private fun createUploadNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.upload_notification_channel_id)
        val channelName = context.getString(R.string.title_upload_notifications)
        val channelDesc = context.getString(R.string.desc_upload_notifications)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                val sessionId: Long = msg.data.getLong(Constant.EXTRA_SESSION_ID)
                Timber.d("Session id: $sessionId")
                Thread.sleep(15000)
                stopForeground(STOP_FOREGROUND_REMOVE)
                notifyUploadComplete(this@UploadService)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            stopSelf(msg.arg1)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): UploadService {
            return this@UploadService
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "start_service"

        private const val NOTIFICATION_ID = 100
    }

}