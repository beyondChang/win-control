package com.beyond.control.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketConnection {
    val connectionState: StateFlow<String>
    val errorMessage: StateFlow<String>
    val incomingMessages: Flow<String>

    fun connect(ip: String)
    suspend fun sendMessage(message: String)
    fun disconnect()
}
