package com.pepulai.app.service.websocket

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    AUTHENTICATION_FAILED,
    FAILED;

    val isFailure: Boolean
        get() = this == AUTHENTICATION_FAILED || this == FAILED
}