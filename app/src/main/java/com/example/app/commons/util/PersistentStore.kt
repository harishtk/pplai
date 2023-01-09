package com.example.app.commons.util

import android.content.Context
import android.content.SharedPreferences

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

    fun logout() {
        setUserId("")
        setDeviceToken("")
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

        private const val APP_PREFERENCES_NAME = "pepul_live_preferences"

        object UserPreferenceKeys {
            const val DEVICE_TOKEN: String = "device_token"
            const val USER_ID: String = "user_id"
            const val USERNAME: String = "username"
        }
    }
}