package com.beyond.control.data.model

data class Device(
    val ip: String,
    val name: String = "设备",
    val isConnected: Boolean = false
)
