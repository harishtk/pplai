package com.aiavatar.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.aiavatar.app.Constant
import com.aiavatar.app.MainActivity
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.WakeLockUtil
import com.aiavatar.app.core.di.GsonParser
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.core.domain.util.JsonParser
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.eventbus.NewNotificationEvent
import com.aiavatar.app.feature.home.presentation.create.AvatarResultFragment
import com.aiavatar.app.service.model.SimplePushMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    @GsonParser
    lateinit var jsonParser: JsonParser

    private val WAKE_LOCK_TAG = "aiavtr::fcm-wake-lock"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            wakeLock = WakeLockUtil.acquire(this@MyFirebaseMessagingService,
                PowerManager.PARTIAL_WAKE_LOCK,
                TimeUnit.SECONDS.toMillis(15),
                WAKE_LOCK_TAG)

            createGeneralNotificationChannels(this)

            remoteMessage.data.apply {
                Timber.tag(TAG).d("Remote Message: $this")
                val jsonObject: JSONObject = JSONObject(remoteMessage.data.get("message"))
                val fcmPushMessageDto = jsonParser.fromJson(jsonObject.toString(),
                    FcmPushMessageDto::class.java)
                Timber.tag(TAG).d("Data arr: $fcmPushMessageDto")
                fcmPushMessageDto?.data?.forEach { fcmData ->
                    // TODO: parse and publish notification
                    Timber.tag(TAG).d("onMessageReceived: notification $fcmData")
                    when (fcmData.category) {
                        CATEGORY_AVATAR -> {
                            handleAvatarNotification(fcmData)
                            EventBus.getDefault().post(NewNotificationEvent("avatar_status", System.currentTimeMillis()))
                        }
                        CATEGORY_GENERAL -> {
                            handleGeneralNotification(fcmData)
                        }
                    }
                }
                /*val dataArr: JSONArray = jsonObject.getJSONArray("data")

                if (dataArr.length() > 0) {
                    val dataJson = dataArr.getJSONObject(0)
                    Timber.tag(TAG).d("onMessageReceived: notification $dataJson")
                    EventBus.getDefault().post(NewNotificationEvent("avatar_status", System.currentTimeMillis()))
                    if (ApplicationDependencies.getPersistentStore().notifyMe) {
                        runBlocking { handleNotification(dataJson) }
                    }
                }*/
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e)
        } finally {
            WakeLockUtil.release(wakeLock, WAKE_LOCK_TAG)
        }
    }

    private fun handleGeneralNotification(fcmData: FcmPushMessageDto.Data) {
        val channelId = getString(R.string.general_notifications_channel_id)
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.catalog_list)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notification = defaultNotificationBuilder(channelId)
            .setContentTitle("AI Avatar")
            .setContentText(fcmData.content)
            .setContentIntent(pendingIntent)
            .build()

        ServiceUtil.getNotificationManager(this)
            .notify(generateNewNotificationId(), notification)
    }

    private fun handleAvatarNotification(fcmData: FcmPushMessageDto.Data) {
        val channelId = getString(R.string.general_notifications_channel_id)

        val args = Bundle().apply {
            putString(Constant.ARG_STATUS_ID, fcmData.modelData?.statusId)
        }

        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setArguments(args)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notification = defaultNotificationBuilder(channelId)
            .setContentTitle("AI Avatar")
            .setContentText(fcmData.content)
            .setContentIntent(pendingIntent)
            .build()

        ServiceUtil.getNotificationManager(this)
            .notify(AVATAR_STATUS_NOTIFICATION_ID, notification)

        runBlocking(Dispatchers.IO) {
            fcmData.modelData?.statusId?.let { statusId ->
                AvatarStatusRequest(statusId).apply {
                    appRepository.avatarStatusSync(this)
                }
            }
        }
    }

    private fun handleNotification(json: JSONObject) {
        val messageModel = jsonParser.fromJson(
            json.toString(),
            SimplePushMessage::class.java
        ) ?: return

        val channelId = getString(R.string.general_notifications_channel_id)

        // TODO: add pending intent to create avatar
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notification = defaultNotificationBuilder(channelId)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentText(messageModel.content)
            .setContentIntent(pendingIntent)
            .build()

        ServiceUtil.getNotificationManager(this)
            .notify(AVATAR_STATUS_NOTIFICATION_ID, notification)
    }

    private fun defaultNotificationBuilder(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
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

        const val AVATAR_STATUS_NOTIFICATION_ID = 500

        const val CATEGORY_GENERAL = "general"
        const val CATEGORY_AVATAR = "avatar"

        fun generateNewNotificationId(): Int {
            return abs(Random.nextInt())
        }
    }
}

data class FcmPushMessageDto(
    @SerializedName("data")
    val data: List<Data>?
) {
    data class Data(
        @SerializedName("dateTime")
        val dateTime: String,
        @SerializedName("content")
        val content: String,
        @SerializedName("category")
        val category: String,
        @SerializedName("modelData")
        val modelData: ModelData?
    )

    data class ModelData(
        @SerializedName("id")
        val id: String,
        @SerializedName("statusId")
        val statusId: String,
        @SerializedName("status")
        val status: String,
    )
}