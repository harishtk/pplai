package com.example.app.service.websocket

import android.util.Log
import com.example.app.BuildConfig
import com.example.app.core.Env
import com.example.app.core.envForConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.lang.AssertionError

class AppWebSocket constructor(
    private val webSocketFactory: WebSocketFactory
) : WebSocketConnection.SocketEventListener {

    private var webSocket: WebSocketConnection? = null

    private val _webSocketState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val webSocketState = _webSocketState.asStateFlow()

    private var canConnect: Boolean = false

    val messageBroker = object : MessageBroker {
        override fun publishStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_PUBLISH_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_PUBLISH_STREAM")
            }
        }

        override fun stopStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_STOP_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_STOP_STREAM")
            }
        }

        override fun joinStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_JOIN_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_JOIN_STREAM")
            }
        }

        override fun leaveStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_LEAVE_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_LEAVE_STREAM")
            }
        }

        override fun likeStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_LIKE_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_LIKE_STREAM")
            }
        }

        override fun commentStream(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_COMMENT_STREAM, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_COMMENT_STREAM")
            }
        }

        override fun sendKeepAlive(payload: String) {
            if (webSocket?.isConnected() == true) {
                webSocket!!.send(EVENT_KEEP_ALIVE, payload)
            } else {
                Log.e(TAG, "WebSocket is not connected, ignoring $EVENT_KEEP_ALIVE")
            }
        }
    }

    private val compositeListener = CompositeWebSocketEventListener()

    fun registerListener(listener: WebSocketEventListener) {
        compositeListener.registerListener(listener)
    }

    fun unregisterListener(listener: WebSocketEventListener) {
        compositeListener.unregisterListener(listener)
    }

    /**
     * Indicate that WebSocketConnections can now be made and attempt to connect it.
     */
    @Synchronized fun connect() {
        canConnect = true
        try {
            getWebSocket()
        } catch (e: WebSocketUnavailableException) {
            throw AssertionError(e)
        }
    }

    /**
     * Indicate that WebSocketConnections can no longer be made and disconnect it.
     */
    @Synchronized fun disconnect() {
        canConnect = false
        disconnectSocket()
    }

    @Synchronized fun forceNewWebSockets() {
        Log.i("$TAG#forceNewWebSockets", "Forcing new WebSockets" +
            " socket " + webSocket?.name ?: "[null]" +
            " canConnect: " + canConnect)
        disconnect()
    }

    private fun disconnectSocket() {
        if (webSocket != null) {
            webSocket!!.disconnect()
            webSocket = null

            _webSocketState.update { WebSocketConnectionState.DISCONNECTED }
        }
    }

    @Throws(WebSocketUnavailableException::class)
    @Synchronized private fun getWebSocket(): WebSocketConnection {
        if (!canConnect) {
            throw WebSocketUnavailableException()
        }

        if (webSocket == null || webSocket!!.isDead()) {
            webSocket = webSocketFactory.createWebSocket()
            webSocket!!.socketEventListener = this

            webSocket!!.connect()
                .onEach { _webSocketState.update { it } }
        }
        return webSocket!!
    }

    override fun onEvent(event: String, args: Array<out Any>?) {
        log("$event: ${args?.firstOrNull()}")
        when (event) {
            EVENT_SUBSCRIBER_JOINED -> {
                (args?.get(0) as? JSONObject)?.let { payload ->
                    compositeListener.onSubscriberJoined(payload.toString())
                }
            }
            EVENT_SUBSCRIBER_LEFT -> {
                (args?.get(0) as? JSONObject)?.let { payload ->
                    compositeListener.onSubscriberLeft(payload.toString())
                }
            }
            EVENT_STREAM_STATUS -> {
                (args?.get(0) as? JSONObject)?.let { payload ->
                    compositeListener.onStreamStatus(payload.toString())
                }
            }
            EVENT_STREAM_LIKED -> {
                (args?.get(0) as? JSONObject)?.let { payload ->
                    compositeListener.onStreamLiked(payload.toString())
                }
            }
            EVENT_STREAM_COMMENT -> {
                (args?.get(0) as? JSONObject)?.let { payload ->
                    compositeListener.onStreamComment(payload.toString())
                }
            }
        }
    }

    private class CompositeWebSocketEventListener : WebSocketEventListener {
        private val registeredListeners: MutableSet<WebSocketEventListener> = hashSetOf()

        fun registerListener(listener: WebSocketEventListener) {
            registeredListeners.add(listener)
        }

        fun unregisterListener(listener: WebSocketEventListener) {
            registeredListeners.remove(listener)
        }

        override fun onStreamStatus(payload: String) {
            registeredListeners.forEach { it.onStreamStatus(payload) }
        }

        override fun onSubscriberJoined(payload: String) {
            registeredListeners.forEach { it.onSubscriberJoined(payload) }
        }

        override fun onSubscriberLeft(payload: String) {
            registeredListeners.forEach { it.onSubscriberLeft(payload) }
        }

        override fun onStreamLiked(payload: String) {
            registeredListeners.forEach { it.onStreamLiked(payload) }
        }

        override fun onStreamComment(payload: String) {
            registeredListeners.forEach { it.onStreamComment(payload) }
        }
    }

    interface MessageBroker {
        fun publishStream(payload: String)
        fun stopStream(payload: String)
        fun joinStream(payload: String)
        fun leaveStream(payload: String)
        fun likeStream(payload: String)
        fun commentStream(payload: String)
        fun sendKeepAlive(payload: String)
    }

    interface WebSocketEventListener {
        fun onStreamStatus(payload: String)
        fun onSubscriberJoined(payload: String)
        fun onSubscriberLeft(payload: String)
        fun onStreamLiked(payload: String)
        fun onStreamComment(payload: String)
    }

    companion object {
        val TAG = AppWebSocket::class.java.simpleName

        @Volatile
        var visibleStreamId: String = ""
            private set

        @JvmName("setVisibleChatThreadId1")
        @Synchronized
        fun setVisibleStreamId(threadId: String) {
            synchronized(visibleStreamId) {
                visibleStreamId = threadId
            }
        }

        @Synchronized
        fun clearVisibleStreamId() {
            synchronized(visibleStreamId) {
                visibleStreamId = ""
            }
        }

        /* Socket outgoing events */
        const val EVENT_PUBLISH_STREAM  = "_publishStream"
        const val EVENT_STOP_STREAM     = "_stopStream"
        const val EVENT_JOIN_STREAM     = "_joinStream"
        const val EVENT_LEAVE_STREAM    = "_leaveStream"
        const val EVENT_LIKE_STREAM     = "_likeStream"
        const val EVENT_COMMENT_STREAM  = "_commentStream"
        const val EVENT_KEEP_ALIVE      = "_keepAlive"
        /* END - Socket outgoing events */

        /* Socket incoming events */
        const val EVENT_STREAM_STATUS       = "_streamStatus"
        const val EVENT_SUBSCRIBER_JOINED   = "_subscriberJoined"
        const val EVENT_SUBSCRIBER_LEFT     = "_subscriberLeft"
        const val EVENT_STREAM_LIKED        = "_streamLiked"
        const val EVENT_STREAM_COMMENT      = "_streamComment"
        /* END - Socket incoming events */

        /* Log utils */
        private fun log(msg: String, t: Throwable? = null) {
            /*val id = AppWebSocketThread.getInstance().id
            val socketId = AppWebSocketThread.getInstance().mSocket?.id()*/
            if (t != null) {
                Log.e(
                    WebSocketConnection.TAG,
                    String.format("Socket: %s", msg),
                    t
                )
            } else {
                Log.d(
                    WebSocketConnection.TAG,
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
        /* END - Log utils */

    }
}

interface WebSocketFactory {
    fun createWebSocket(): WebSocketConnection
}