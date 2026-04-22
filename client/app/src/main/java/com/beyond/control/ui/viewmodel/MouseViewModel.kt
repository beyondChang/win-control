package com.beyond.control.ui.viewmodel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyond.control.network.ConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

class MouseViewModel(
    private val connectionManager: ConnectionManager
) : ViewModel(), SensorEventListener {

    // 飞鼠模式开关（默认关闭，使用触摸滑动）
    private val _isFlyMouseEnabled = MutableStateFlow(false)
    val isFlyMouseEnabled: StateFlow<Boolean> = _isFlyMouseEnabled.asStateFlow()

    // 传感器相关
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    // 飞鼠参数
    private var lastTimestamp: Long = 0
    private val sensitivity: Float = 0.8f
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
        if (_isFlyMouseEnabled.value) {
            startSensors()
        } else {
            stopSensors()
        }
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

    fun sendMouseMove(dx: Int, dy: Int) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        sendMessage(json.toString())
    }

    fun sendMouseMoveF(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx.toInt())
            put("dy", dy.toInt())
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
        val json = JSONObject().apply {
            put("type", type)
        }
        sendMessage(json.toString())
    }

    fun sendMouseScroll(dy: Int) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("dy", dy)
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
