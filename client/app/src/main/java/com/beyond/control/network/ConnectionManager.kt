package com.beyond.control.network

import com.beyond.control.data.model.Device
import com.beyond.control.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


class ConnectionManager(
    private val webSocket: WebSocketConnection,
    private val deviceRepository: DeviceRepository
) {
    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = webSocket.connectionState
    val errorMessage = webSocket.errorMessage

    fun connectToComputer(ip: String) {
        webSocket.connect(ip)

        kotlinx.coroutines.GlobalScope.launch {
            deviceRepository.insertDevice(Device(ip, "电脑", true))
        }
    }

    suspend fun sendMessage(message: String) {
        webSocket.sendMessage(message)
    }

    fun disconnect() {
        webSocket.disconnect()
        _connectionState.value = "DISCONNECTED"
    }

    val incomingMessages = webSocket.incomingMessages
}
