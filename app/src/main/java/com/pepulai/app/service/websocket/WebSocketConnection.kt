package com.pepulai.app.service.websocket

import android.util.Log
import com.pepulai.app.BuildConfig
import com.pepulai.app.core.Env
import com.pepulai.app.core.envForConfig
import com.pepulai.app.di.ApplicationDependencies
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class WebSocketConnection constructor(
    name: String
) {
    val name: String = "[$name:${System.identityHashCode(this)}]"

    private val _webSocketConnectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val webSocketConnectionState = _webSocketConnectionState.asStateFlow()

    var client: Socket? = null

    val wsUri: String = BuildConfig.SOCKET_URL

    private val exceptionHandler =
        CoroutineExceptionHandler { _, t -> Timber.e(t) }

    private val coroutineContext: CoroutineContext =
        Dispatchers.IO + SupervisorJob() + exceptionHandler

    val webSocketScope = CoroutineScope(coroutineContext)

    var socketEventListener: SocketEventListener? = null

    @Throws(IllegalStateException::class)
    fun send(event: String, payload: String?) {
        if (client == null || !client!!.connected()) {
            throw IllegalStateException("Socket is not connected")
        }

        log("$event: $payload")
        client!!.emit(event, payload)
    }

    @Synchronized fun connect(): StateFlow<WebSocketConnectionState> {
        log("connect()")

        if (client == null) {
            _webSocketConnectionState.update { WebSocketConnectionState.CONNECTING }
            val username = ApplicationDependencies.getPersistentStore().username
            /*val opts = IO.Options().apply {
                // this.forceNew = true
                this.query = "username=$username"
                this.secure = false
            }*/
            this.client = IO.socket(wsUri/*, opts*/)
                .connect()
            setupEvents()
        }
        return webSocketConnectionState
    }

    @Synchronized fun disconnect() {
        log("disconnect()")

        if (client != null) {
            client!!.close()
            client = null
            _webSocketConnectionState.update { WebSocketConnectionState.DISCONNECTING }
            webSocketScope.cancel(CancellationException("WebSocket is dead."))
        }
    }

    @Synchronized fun isDead(): Boolean { return client == null }

    @Synchronized fun isConnected(): Boolean { return client != null && client!!.connected() }

    private fun setupEvents() {
        client?.on(Socket.EVENT_CONNECT, connectHandler)
        client?.on(Socket.EVENT_DISCONNECT, disconnectHandler)
        client?.on(Socket.EVENT_CONNECT_ERROR, errorHandler)

        // TODO('haris'): Do this in any other method?
        client?.on(AppWebSocket.EVENT_SUBSCRIBER_JOINED) {
            socketEventListener?.onEvent(AppWebSocket.EVENT_SUBSCRIBER_JOINED, it)
        }
        client?.on(AppWebSocket.EVENT_SUBSCRIBER_LEFT) {
            socketEventListener?.onEvent(AppWebSocket.EVENT_SUBSCRIBER_LEFT, it)
        }
        client?.on(AppWebSocket.EVENT_STREAM_LIKED) {
            socketEventListener?.onEvent(AppWebSocket.EVENT_STREAM_LIKED, it)
        }
        client?.on(AppWebSocket.EVENT_STREAM_STATUS) {
            socketEventListener?.onEvent(AppWebSocket.EVENT_STREAM_STATUS, it)
        }
        client?.on(AppWebSocket.EVENT_STREAM_COMMENT) {
            socketEventListener?.onEvent(AppWebSocket.EVENT_STREAM_COMMENT, it)
        }
    }

    private val connectHandler = Emitter.Listener { args: Array<out Any>? ->
        debugLog("ON_CONNECT")
        _webSocketConnectionState.update { WebSocketConnectionState.CONNECTED }
    }

    private val disconnectHandler = Emitter.Listener { args: Array<out Any>? ->
        debugLog("ON_DISCONNECT")
        if (client != null) {
            _webSocketConnectionState.update { WebSocketConnectionState.RECONNECTING }
        } else {
            _webSocketConnectionState.update { WebSocketConnectionState.DISCONNECTED }
        }
    }

    private val errorHandler = Emitter.Listener { args: Array<out Any>? ->
        log("CONNECT_ERROR")
        _webSocketConnectionState.update { WebSocketConnectionState.RECONNECTING }
    }

    private fun log(msg: String, t: Throwable? = null) {
        /*val id = AppWebSocketThread.getInstance().id
        val socketId = AppWebSocketThread.getInstance().mSocket?.id()*/
        if (t != null) {
            Log.e(
                TAG,
                String.format("Socket: %s", msg),
                t
            )
        } else {
            Log.d(
                TAG,
                String.format("Socket: %s", msg)
            )
        }
    }

    private fun debugLog(msg: String) {
        if (envForConfig(BuildConfig.ENV) == Env.DEV || BuildConfig.DEBUG) {
            log(msg)
        }
    }

    private fun debugErrorLog(msg: String, exception: Throwable? = null) {
        if (envForConfig(BuildConfig.ENV) == Env.DEV || BuildConfig.DEBUG) {
            log(msg, exception)
        }
    }

    interface SocketEventListener {
        fun onEvent(event: String, args: Array<out Any>?)
    }

    companion object {
        val TAG = WebSocketConnection::class.java.simpleName
    }

}

