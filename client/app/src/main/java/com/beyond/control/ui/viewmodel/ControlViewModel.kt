package com.beyond.control.ui.viewmodel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyond.control.data.model.Device
import com.beyond.control.data.repository.DeviceRepository
import com.beyond.control.network.ConnectionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import kotlin.math.abs

class ControlViewModel(
    private val deviceRepository: DeviceRepository,
    private val connectionManager: ConnectionManager
) : ViewModel(), SensorEventListener {

    // === 连接管理 (from HomeViewModel) ===

    val allDevices = deviceRepository.allDevices

    private val _connectedIp = MutableStateFlow("")
    val connectedIp: StateFlow<String> = _connectedIp.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val connectionState = connectionManager.connectionState

    init {
        val savedIp = deviceRepository.getLastIp()
        if (savedIp.isNotEmpty()) {
            _connectedIp.value = savedIp
        }
    }

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
                if (!errorShown && connectionState.value == "ERROR") {
                    val wsError = connectionManager.errorMessage.value
                    _errorMessage.value = wsError.ifEmpty { "连接失败，请检查 IP 地址和网络是否连通" }
                } else if (!errorShown && connectionState.value == "CONNECTED") {
                    deviceRepository.saveLastIp(ip)
                    fetchVolumeInfo(ip)
                }
            }
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
        _errorMessage.value = null
    }

    // === 飞鼠模式 (from MouseViewModel) ===

    private val _isFlyMouseEnabled = MutableStateFlow(false)
    val isFlyMouseEnabled: StateFlow<Boolean> = _isFlyMouseEnabled.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var lastTimestamp: Long = 0
    private val sensitivity: Float = 1.2f
    private val deadZone: Float = 0.02f
    private var filteredGyroX: Float = 0f
    private var filteredGyroY: Float = 0f
    private val alpha: Float = 0.7f

    fun setSensorManager(sensorManager: SensorManager?) {
        this.sensorManager = sensorManager
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    }

    fun toggleFlyMouse() {
        _isFlyMouseEnabled.value = !_isFlyMouseEnabled.value
        if (_isFlyMouseEnabled.value) startSensors() else stopSensors()
    }

    private fun startSensors() {
        sensorManager?.let { manager ->
            gyroscopeSensor?.let { sensor ->
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
            accelerometerSensor?.let { sensor ->
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    private fun stopSensors() {
        sensorManager?.unregisterListener(this)
        filteredGyroX = 0f
        filteredGyroY = 0f
        lastTimestamp = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!_isFlyMouseEnabled.value) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val now = event.timestamp
                if (lastTimestamp == 0L) {
                    lastTimestamp = now
                    return
                }
                val dt = (now - lastTimestamp) / 1_000_000_000f
                lastTimestamp = now

                if (dt <= 0) return

                filteredGyroX = alpha * filteredGyroX + (1 - alpha) * event.values[0]
                filteredGyroY = alpha * filteredGyroY + (1 - alpha) * event.values[1]

                val gyroX = if (abs(filteredGyroX) < deadZone) 0f else filteredGyroX
                val gyroY = if (abs(filteredGyroY) < deadZone) 0f else filteredGyroY

                val dx = (gyroY * dt * sensitivity * 1000).toInt()
                val dy = (-gyroX * dt * sensitivity * 1000).toInt()

                if (dx != 0 || dy != 0) {
                    sendMouseMove(dx, dy)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {}
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // === 鼠标/触控板指令 (unified from MouseViewModel + TouchpadViewModel) ===

    fun sendMouseMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx.toInt())
            put("dy", dy.toInt())
        }
        sendMessage(json.toString())
    }

    fun sendMouseMove(dx: Int, dy: Int) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        sendMessage(json.toString())
    }

    fun sendMouseClick(button: String = "left") {
        val type = when (button) {
            "right" -> "rightClick"
            "left" -> "click"
            "left_down" -> "leftDown"
            "left_up" -> "leftUp"
            else -> button
        }
        val json = JSONObject().apply { put("type", type) }
        sendMessage(json.toString())
    }

    fun sendMouseScroll(dy: Float) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("dy", dy.toInt())
        }
        sendMessage(json.toString())
    }

    fun sendKeyPress(key: String) {
        val json = JSONObject().apply {
            put("type", "key")
            put("key", key)
        }
        sendMessage(json.toString())
    }

    // === 音量控制 (from VolumeViewModel) ===

    private val _volumeLevel = MutableStateFlow(0.5f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private fun fetchVolumeInfo(ip: String) {
        viewModelScope.launch {
            try {
                val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    URL("http://$ip:1800/api/control/volume").readText()
                }
                val obj = JSONObject(json)
                _volumeLevel.value = obj.optDouble("level", 0.5).toFloat().coerceIn(0f, 1f)
                _isMuted.value = obj.optBoolean("muted", false)
            } catch (e: Exception) {
                android.util.Log.w("ControlViewModel", "Failed to fetch volume info", e)
            }
        }
    }

    fun updateLocalLevel(level: Float) {
        _volumeLevel.value = level.coerceIn(0f, 1f)
    }

    fun setVolume(level: Float) {
        val clampedLevel = level.coerceIn(0f, 1f)
        val json = JSONObject().apply {
            put("type", "volumeSet")
            put("level", clampedLevel)
        }
        sendMessage(json.toString())
        _volumeLevel.value = clampedLevel
    }

    fun volumeUp() {
        val json = JSONObject().apply { put("type", "volumeUp") }
        sendMessage(json.toString())
    }

    fun volumeDown() {
        val json = JSONObject().apply { put("type", "volumeDown") }
        sendMessage(json.toString())
    }

    fun toggleMute() {
        val json = JSONObject().apply { put("type", "volumeMute") }
        sendMessage(json.toString())
        _isMuted.value = !_isMuted.value
    }

    // === 共享 ===

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            connectionManager.sendMessage(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}
