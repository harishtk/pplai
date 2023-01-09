package com.example.app.service.websocket

import io.socket.client.Socket

interface WebSocketListener {

    fun onOpen(webSocket: Socket)

    fun onDisconnect(webSocket: Socket)

    fun onError(webSocket: Socket)

    fun onMessage(webSocket: Socket)

    fun onClosed()
}