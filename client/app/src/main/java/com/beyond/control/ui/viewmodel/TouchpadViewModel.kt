package com.beyond.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyond.control.network.ConnectionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class TouchpadViewModel(
    private val connectionManager: ConnectionManager
) : ViewModel() {

    fun sendMouseMove(dx: Float, dy: Float) {
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
            else -> button // Pass through for other types
        }
        val json = JSONObject().apply {
            put("type", type)
        }
        sendMessage(json.toString())
    }

    fun sendMouseScroll(dx: Float, dy: Float) {
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

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            connectionManager.sendMessage(message)
        }
    }
}
