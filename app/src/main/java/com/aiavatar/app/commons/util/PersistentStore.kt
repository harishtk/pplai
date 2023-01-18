package com.aiavatar.app.commons.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PersistentStore private constructor(
    private val appPreferences: SharedPreferences,
) {

    val isLogged: Boolean
        get() = getAppPreferences().getString(UserPreferenceKeys.DEVICE_TOKEN, "")?.isNotBlank() == true
    val deviceToken: String
        get() = getAppPreferences().getString(UserPreferenceKeys.DEVICE_TOKEN, "") ?: ""
    val userId: String
        get() = getAppPreferences().getString(UserPreferenceKeys.USER_ID, "") ?: ""
    val username: String
        get() = getAppPreferences().getString(UserPreferenceKeys.USERNAME, "") ?: ""

    val email: String
        get() = getAppPreferences().getString(UserPreferenceKeys.EMAIL, "") ?: ""

    val deviceId: String
        get() = getAppPreferences().getString(AppEssentialKeys.DEVICE_INSTANCE_ID, "") ?: ""

    val fcmToken: String
        get() = getAppPreferences().getString(AppEssentialKeys.FCM_TOKEN, "") ?: ""

    val notifyMe: Boolean
        get() = getAppPreferences().getBoolean(UserPreferenceKeys.NOTIFY_UPON_COMPLETION, true)

    val guestUserId: String
        get() = getAppPreferences().getString(UserPreferenceKeys.GUEST_USER_ID, "") ?: ""

    val isProcessingModel: Boolean
        get() = getAppPreferences().getBoolean(UserPreferenceKeys.PROCESSING_MODEL, false)

    fun setDeviceToken(newToken: String): PersistentStore {
        getAppPreferences().edit().putString(UserPreferenceKeys.DEVICE_TOKEN, newToken).apply()
        return this
    }

    fun setUserId(userId: String): PersistentStore {
        getAppPreferences().edit().putString(UserPreferenceKeys.USER_ID, userId).apply()
        return this
    }

    fun setUsername(username: String): PersistentStore {
        getAppPreferences().edit().putString(UserPreferenceKeys.USERNAME, username).apply()
        return this
    }

    fun setEmail(email: String): PersistentStore {
        getAppPreferences().edit().putString(UserPreferenceKeys.EMAIL, email).apply()
        return this
    }

    fun setDeviceId(newDeviceId: String): PersistentStore {
        getAppPreferences().edit().putString(AppEssentialKeys.DEVICE_INSTANCE_ID, newDeviceId).apply()
        return this
    }

    fun setFcmToken(newToken: String): PersistentStore {
        getAppPreferences().edit().putString(AppEssentialKeys.FCM_TOKEN, newToken).apply()
        return this
    }

    fun setOnboardPresented(isPresented: Boolean = true): PersistentStore {
        getAppPreferences().edit().putBoolean(AppEssentialKeys.ONBOARD_PRESENTED, isPresented).apply()
        return this
    }

    fun setLastTokenSyncTime(timestamp: Long = System.currentTimeMillis()): PersistentStore {
        getAppPreferences().edit().putLong(AppEssentialKeys.LAST_FCM_TOKEN_SYNC_TIME, timestamp).apply()
        return this
    }

    fun setFcmTokenSynced(isSynced: Boolean = false): PersistentStore {
        getAppPreferences().edit().putBoolean(AppEssentialKeys.FCM_TOKEN_SYNCED, isSynced).apply()
        return this
    }

    fun setGuestUserId(userId: String): PersistentStore {
        getAppPreferences().edit().putString(UserPreferenceKeys.GUEST_USER_ID, userId).apply()
        return this
    }

    fun setProcessingModel(isProcessing: Boolean = true): PersistentStore {
        getAppPreferences().edit().putBoolean(UserPreferenceKeys.PROCESSING_MODEL, isProcessing).apply()
        return this
    }

    fun logout() {
        setUserId("")
        setDeviceToken("")
        setUsername("")
        setEmail("")
        setGuestUserId("")
        setProcessingModel(false)
    }

    fun getOrCreateDeviceId(): String {
        val id = deviceId.ifBlank {
            UUID.randomUUID().toString().also { newId ->
                appPreferences.edit().putString(AppEssentialKeys.DEVICE_INSTANCE_ID, newId).apply()
            }
        }
        return id
    }

    fun setNotifyUponCompletion(notify: Boolean = true): PersistentStore {
        appPreferences.edit().putBoolean(UserPreferenceKeys.NOTIFY_UPON_COMPLETION, notify).apply()
        return this
    }

    private fun getAppPreferences(): SharedPreferences {
        return appPreferences
    }

    companion object {
        @Volatile private var INSTANCE: PersistentStore? = null

        @Synchronized
        fun getInstance(application: Context): PersistentStore =
            INSTANCE ?: synchronized(this) { INSTANCE ?: createInstance(application) }

        private fun createInstance(application: Context): PersistentStore {
            return PersistentStore(application.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE))
                .also { INSTANCE = it }
        }

        private const val APP_PREFERENCES_NAME = "ai_avatar_preferences"

        object UserPreferenceKeys {
            const val DEVICE_TOKEN: String = "device_token"
            const val USER_ID: String = "user_id"
            const val USERNAME: String = "username"
            const val EMAIL: String = "email"
            const val NOTIFY_UPON_COMPLETION = "notify_upon_completion"
            const val GUEST_USER_ID = "guest_user_id"
            const val PROCESSING_MODEL = "processing_model"
        }

        object AppEssentialKeys {
            const val ONBOARD_PRESENTED: String = "onboard_presented"
            const val DEVICE_INSTANCE_ID: String = "device_instance_id"
            const val FCM_TOKEN: String = "fcm_token"
            const val LAST_FCM_TOKEN_SYNC_TIME: String = "last_fcm_token_sync_time"
            const val FCM_TOKEN_SYNCED: String = "fcm_token_synced"
        }
    }
}