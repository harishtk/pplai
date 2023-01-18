package com.aiavatar.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.WakeLockUtil
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.ApplicationDependencies
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var appRepository: AppRepository

    private val WAKE_LOCK_TAG = "aiavtr::fcm-wake-lock"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createGeneralNotificationChannels(this)
        }
        try {
            wakeLock = WakeLockUtil.acquire(this@MyFirebaseMessagingService,
                PowerManager.PARTIAL_WAKE_LOCK,
                TimeUnit.SECONDS.toMillis(15),
                WAKE_LOCK_TAG)

            remoteMessage.data.apply {
                Timber.tag(TAG).d("Remote Message: $this")
                val jsonObject: JSONObject = JSONObject(remoteMessage.data.get("message"))
                val dataArr: JSONArray = jsonObject.getJSONArray("data")

                if (dataArr.length() > 0) {
                    val dataJson = dataArr.getJSONObject(0)
                    Timber.tag(TAG).d("onMessageReceived: notification $dataJson")
                }
            }
            // TODO: process the message
        } catch (e: Exception) {
            Timber.tag(TAG).e(e)
        } finally {
            WakeLockUtil.release(wakeLock, WAKE_LOCK_TAG)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("onNewToken(token=$token)")
        ApplicationDependencies.getPersistentStore()
            .setFcmToken(token)
            .setFcmTokenSynced(false)
            .setLastTokenSyncTime(0L)
    }

    private suspend fun syncTokenWithServer() {
        try {
            wakeLock = WakeLockUtil.acquire(this@MyFirebaseMessagingService,
                PowerManager.PARTIAL_WAKE_LOCK,
                TimeUnit.SECONDS.toMillis(15),
                WAKE_LOCK_TAG)

            val tokenRequest = SendFcmTokenRequest(
                tempUserId = ApplicationDependencies.getPersistentStore().deviceId,
                token = ApplicationDependencies.getPersistentStore().deviceToken,
                platform = Constant.PLATFORM
            )
            val result = appRepository.sendFcmTokenSync(tokenRequest)
            when (result) {
                is Result.Success -> {
                    ApplicationDependencies.getPersistentStore().apply {
                        setFcmTokenSynced()
                        setLastTokenSyncTime(System.currentTimeMillis())
                    }
                }
                else -> {
                    // Noop
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e)
        } finally {
            WakeLockUtil.release(wakeLock, WAKE_LOCK_TAG)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGeneralNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.general_notifications_channel_id)
        val channelName = context.getString(R.string.title_general_notifications)
        val channelDesc = context.getString(R.string.desc_general_notifications)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val TAG = "FCM.Msg"
    }

}