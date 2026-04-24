package com.beyond.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyond.control.network.ConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 音量控制 ViewModel
 * 通过 WebSocket 发送音量指令到服务端
 */
class VolumeViewModel(
    private val connectionManager: ConnectionManager
) : ViewModel() {

    // 当前音量等级 (0.0 ~ 1.0)
    private val _volumeLevel = MutableStateFlow(0.5f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    // 是否静音
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // 是否正在拖拽滑块（避免 UI 闪烁）
    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    // 设置拖拽状态
    fun setDragging(dragging: Boolean) {
        _isDragging.value = dragging
    }

    // 更新本地音量状态（滑块拖拽时预览用）
    fun updateLocalLevel(level: Float) {
        _volumeLevel.value = level.coerceIn(0f, 1f)
    }

    // 设置音量等级
    fun setVolume(level: Float) {
        val clampedLevel = level.coerceIn(0f, 1f)
        val json = JSONObject().apply {
            put("type", "volumeSet")
            put("level", clampedLevel)
        }
        sendMessage(json.toString())
        _volumeLevel.value = clampedLevel
    }

    // 增加音量
    fun volumeUp() {
        val json = JSONObject().apply {
            put("type", "volumeUp")
        }
        sendMessage(json.toString())
    }

    // 减少音量
    fun volumeDown() {
        val json = JSONObject().apply {
            put("type", "volumeDown")
        }
        sendMessage(json.toString())
    }

    // 切换静音
    fun toggleMute() {
        val json = JSONObject().apply {
            put("type", "volumeMute")
        }
        sendMessage(json.toString())
        // 乐观更新
        _isMuted.value = !_isMuted.value
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            connectionManager.sendMessage(message)
        }
    }
}
