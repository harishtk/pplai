package com.aiavatar.app

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.core.os.bundleOf
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jakewharton.threetenabp.AndroidThreeTen
import com.aiavatar.app.commons.util.AppForegroundObserver
import com.aiavatar.app.commons.util.AppStartup
import com.aiavatar.app.commons.util.Util
import com.aiavatar.app.commons.util.logging.timber.NoopTree
import com.aiavatar.app.core.Env
import com.aiavatar.app.core.envForConfig
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.di.ApplicationDependencyProvider
import com.aiavatar.app.service.websocket.AppWebSocket
import com.aiavatar.app.service.websocket.WebSocketConnectionState
import dagger.hilt.android.HiltAndroidApp
import io.github.devzwy.nsfw.NSFWHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ApplicationContext : Application(), AppForegroundObserver.Listener, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = Util.getCustomCoroutineScope()

    private var userEngageStartTime: Long = System.currentTimeMillis()

    private var socketKeepAliveJob: Job? = null

    override fun onCreate() {
        AppStartup.getInstance().onApplicationCreate()
        super.onCreate()

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .setClassInstanceLimit(AppWebSocket::class.java, 1)
                .build()
        )

        AndroidThreeTen.init(this)

        ifDebug { NSFWHelper.openDebugLog() }
        NSFWHelper.initHelper(
            context = this,
            isOpenGPU = false
            // modelPath = "nsfw.tflite",
        )

        AppStartup.getInstance()
            .addBlocking("init-logging", this::initializeLogging)
            .addBlocking("app-dependencies", this::initApplicationDependencies)
            .addBlocking("lifecycle-observer") {
                ApplicationDependencies.getAppForegroundObserver().addListener(this)
            }
            // .addBlocking("after-create", this::setupApp)
            .addPostRender(this::setupApp)
            .execute()
    }

    private fun setupApp() {
        ApplicationDependencies.getPersistentStore().getOrCreateDeviceId()
    }

    private fun initApplicationDependencies() {
        ApplicationDependencies.init(this,
            ApplicationDependencyProvider(this)
        )
    }

    private fun initializeLogging() {
        when (envForConfig(BuildConfig.ENV)) {
            Env.DEV -> {
                Timber.plant(Timber.DebugTree())
            }
            else -> {
                Timber.plant(NoopTree())
            }
        }
    }


    private fun initializeSocketIfRequired() {
        ApplicationDependencies.getAppWebSocket().apply {
            if (webSocketState.value == WebSocketConnectionState.DISCONNECTED) {
                forceNewWebSockets()
                connect()

                if (BuildConfig.DEBUG) {
                    webSocketState.onEach { state -> Log.d(TAG, "WebSocketState: $state") }
                        .launchIn(applicationScope)
                }
            }
        }
    }

    override fun onForeground() {
        Timber.d("App is foregrounded")
        socketKeepAliveJob?.cancel(CancellationException("App is now visible."))
        // initializeSocketIfRequired()
        userEngageStartTime = System.currentTimeMillis()
    }

    override fun onBackground() {
        Timber.d("App backgrounded")
        val engagedTime = System.currentTimeMillis() - userEngageStartTime
        Timber.d("JJ: onStop life=$engagedTime")
        val userAwayArgs = bundleOf(
            Constant.EXTRA_ENGAGED_TIME to engagedTime
        )
        /*socketKeepAliveJob = applicationScope.launch {
            delay(SOCKET_KEEP_ALIVE_TIMEOUT)
            ApplicationDependencies.getAppWebSocket().disconnect()
            Log.d(TAG, "Socket is destroyed. Reason: App is in background for so long.")
        }*/
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }

    companion object {
        const val TAG = "AiAvatar"
        const val SOCKET_KEEP_ALIVE_TIMEOUT = 5000L
    }
}