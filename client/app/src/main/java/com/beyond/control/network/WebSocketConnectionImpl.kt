package com.beyond.control.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketConnectionImpl : WebSocketConnection {
    private val TAG = "WebSocketConnection"

    private val stateFlow = MutableStateFlow("DISCONNECTED")
    override val connectionState = stateFlow.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    override val errorMessage = _errorMessage.asStateFlow()

    private val messageChannel = Channel<String>()
    override val incomingMessages = messageChannel.receiveAsFlow()

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null

    override fun connect(ip: String) {
        Log.d(TAG, "Connecting to $ip")

        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://$ip:1800/ws")
            .build()

        stateFlow.value = "CONNECTING"
        _errorMessage.value = ""

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                stateFlow.value = "CONNECTED"
                this@WebSocketConnectionImpl.webSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                messageChannel.trySend(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                stateFlow.value = "DISCONNECTED"
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val responseCode = response?.code
                val responseBody = response?.body?.string()
                val detail = buildString {
                    append(t.message ?: "未知错误")
                    if (responseCode != null) append(" ($responseCode)")
                    if (!responseBody.isNullOrBlank()) append(": $responseBody")
                }
                Log.e(TAG, "WebSocket error: $detail", t)
                _errorMessage.value = detail
                stateFlow.value = "ERROR"
            }
        }

        client?.newWebSocket(request, listener)
    }

    override suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            try {
                webSocket?.send(text = message)
                Log.d(TAG, "Sent: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Send message error", e)
                stateFlow.value = "ERROR"
            }
        }
    }

    override fun disconnect() {
        Log.d(TAG, "Disconnecting")
        webSocket?.close(1000, "Client disconnect")
        client?.dispatcher?.executorService?.shutdown()
        stateFlow.value = "DISCONNECTED"
    }
}
