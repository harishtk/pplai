package com.aiavatar.app.service

import com.aiavatar.app.di.ApplicationDependencies
import com.google.firebase.messaging.FirebaseMessagingService
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("onNewToken(token=$token)")
        ApplicationDependencies.getPersistentStore()
            .setFcmToken(token)
            .setFcmTokenSynced(false)
            .setLastTokenSyncTime(0L)
    }

    companion object {
        const val TAG = "FCM.Msg"
    }

}