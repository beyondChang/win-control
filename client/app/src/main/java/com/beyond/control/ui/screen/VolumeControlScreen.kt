package com.beyond.control.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beyond.control.ui.theme.*
import com.beyond.control.ui.viewmodel.VolumeViewModel

/**
 * 音量控制屏幕
 * 提供音量滑块、增减按钮和静音切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeControlScreen(
    viewModel: VolumeViewModel,
    onBack: () -> Unit
) {
    val volumeLevel by viewModel.volumeLevel.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音量控制", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = FreshBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 音量可视化圆环
            VolumeCircle(
                level = volumeLevel,
                isMuted = isMuted,
                modifier = Modifier.padding(vertical = 32.dp)
            )

            // 音量百分比
            Text(
                text = if (isMuted) "已静音" else "${(volumeLevel * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) TextHint else TextPrimary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 音量滑块
            VolumeSlider(
                level = volumeLevel,
                isMuted = isMuted,
                onLevelChange = { viewModel.updateLocalLevel(it) },
                onLevelChangeFinished = { viewModel.setVolume(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 控制按钮组
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 音量减
                VolumeActionButton(
                    icon = Icons.Default.VolumeDown,
                    label = "减",
                    enabled = !isMuted,
                    onClick = { viewModel.volumeDown() }
                )

                // 静音切换
                VolumeActionButton(
                    icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    label = if (isMuted) "取消静音" else "静音",
                    enabled = true,
                    isMuteButton = true,
                    isMuted = isMuted,
                    onClick = { viewModel.toggleMute() }
                )

                // 音量加
                VolumeActionButton(
                    icon = Icons.Default.VolumeUp,
                    label = "加",
                    enabled = !isMuted,
                    onClick = { viewModel.volumeUp() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 快捷键提示
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = FreshBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "操作说明",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 拖动滑块精确调节音量", fontSize = 13.sp, color = TextSecondary)
                    Text("• 点击加减按钮步进调节（5%）", fontSize = 13.sp, color = TextSecondary)
                    Text("• 静音后仍可通过滑块调节", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
    }
}

/**
 * 音量可视化圆环
 */
@Composable
private fun VolumeCircle(
    level: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆环
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.size(200.dp),
            color = DividerLight,
            strokeWidth = 12.dp,
            trackColor = DividerLight
        )
        // 音量进度圆环
        if (!isMuted && level > 0f) {
            CircularProgressIndicator(
                progress = level,
                modifier = Modifier.size(200.dp),
                color = FreshGreen,
                strokeWidth = 12.dp,
                trackColor = Color.Transparent
            )
        }
        // 中央图标
        Surface(
            modifier = Modifier.size(120.dp).clip(CircleShape),
            color = CardBackground,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isMuted -> Icons.Default.VolumeOff
                        level < 0.3f -> Icons.Default.VolumeDown
                        level < 0.7f -> Icons.Default.VolumeUp
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = null,
                    tint = when {
                        isMuted -> TextHint
                        level < 0.3f -> FreshOrange
                        else -> FreshGreen
                    },
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

/**
 * 音量滑块
 */
@Composable
private fun VolumeSlider(
    level: Float,
    isMuted: Boolean,
    onLevelChange: (Float) -> Unit,
    onLevelChangeFinished: (Float) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.VolumeDown,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = level,
            onValueChange = {
                dragging = true
                onLevelChange(it)
            },
            onValueChangeFinished = {
                dragging = false
                onLevelChangeFinished(level)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isMuted,
            colors = SliderDefaults.colors(
                thumbColor = if (isMuted) TextHint else FreshGreen,
                activeTrackColor = if (isMuted) TextHint else FreshGreen,
                inactiveTrackColor = DividerLight,
                disabledThumbColor = TextHint,
                disabledActiveTrackColor = TextHint,
                disabledInactiveTrackColor = DividerLight
            )
        )
    }
}

/**
 * 音量操作按钮
 */
@Composable
private fun VolumeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    isMuteButton: Boolean = false,
    isMuted: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(72.dp).clip(CircleShape),
            color = when {
                isMuteButton && isMuted -> Color(0xFFEF5350).copy(alpha = 0.15f)
                enabled -> MintGreen.copy(alpha = 0.4f)
                else -> DividerLight
            },
            enabled = enabled || isMuteButton
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        isMuteButton && isMuted -> Color(0xFFEF5350)
                        enabled -> FreshGreen
                        else -> TextHint
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (enabled || isMuteButton) TextSecondary else TextHint
        )
    }
}
