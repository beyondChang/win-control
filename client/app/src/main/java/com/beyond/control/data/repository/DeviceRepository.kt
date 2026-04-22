package com.beyond.control.data.repository

import com.beyond.control.data.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceRepository {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val allDevices: Flow<List<Device>> = _devices.asStateFlow()

    suspend fun insertDevice(device: Device) {
        val currentList = _devices.value.toMutableList()
        currentList.add(device)
        _devices.value = currentList
    }

    suspend fun updateDevice(device: Device) {
        val currentList = _devices.value.toMutableList()
        val index = currentList.indexOfFirst { it.ip == device.ip }
        if (index != -1) {
            currentList[index] = device
            _devices.value = currentList
        }
    }

    suspend fun deleteDevice(device: Device) {
        val currentList = _devices.value.toMutableList()
        currentList.removeAll { it.ip == device.ip }
        _devices.value = currentList
    }

    suspend fun getDeviceByIp(ip: String): Device? {
        return _devices.value.find { it.ip == ip }
    }

    suspend fun clearAllDevices() {
        _devices.value = emptyList()
    }
}
