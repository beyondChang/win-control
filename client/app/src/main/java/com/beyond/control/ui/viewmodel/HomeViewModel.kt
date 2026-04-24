package com.beyond.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyond.control.data.model.Device
import com.beyond.control.data.repository.DeviceRepository
import com.beyond.control.network.ConnectionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val deviceRepository: DeviceRepository,
    private val connectionManager: ConnectionManager
) : ViewModel() {
    val allDevices = deviceRepository.allDevices

    private val _connectedIp = MutableStateFlow("10.24.102.28")
    val connectedIp: StateFlow<String> = _connectedIp.asStateFlow()

    // 连接错误提示消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val connectionState = connectionManager.connectionState

    fun setConnectedIp(ip: String) {
        _connectedIp.value = ip
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun connectToComputer(ip: String) {
        if (ip.isNotEmpty()) {
            _errorMessage.value = null
            connectionManager.connectToComputer(ip)
            viewModelScope.launch {
                deviceRepository.insertDevice(Device(ip = ip, name = "电脑", isConnected = true))

                // 监听 WebSocket 错误消息
                var errorShown = false
                while (connectionState.value == "CONNECTING") {
                    delay(300)
                    val wsError = connectionManager.errorMessage.value
                    if (wsError.isNotEmpty()) {
                        _errorMessage.value = wsError
                        errorShown = true
                        break
                    }
                }
                // 超时兜底
                if (!errorShown && connectionState.value == "ERROR") {
                    val wsError = connectionManager.errorMessage.value
                    _errorMessage.value = wsError.ifEmpty { "连接失败，请检查 IP 地址和网络是否连通" }
                }
            }
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
        _errorMessage.value = null
    }
}
