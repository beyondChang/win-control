package com.beyond.control.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beyond.control.ui.theme.*
import com.beyond.control.ui.viewmodel.TouchpadViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.pm.ActivityInfo

@Composable
fun TouchpadScreen(
    viewModel: TouchpadViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左列：修饰键
            Column(
                modifier = Modifier.fillMaxHeight().width(80.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                ModifierButton(
                    icon = Icons.Default.Search,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFA8E6CF), Color(0xFF88D8B0))),
                    onClick = { viewModel.sendKeyPress("search") }
                )
                ModifierButton(
                    text = "Alt",
                    gradient = Brush.verticalGradient(listOf(Color(0xFFFFB7B2), Color(0xFFFFDAC1))),
                    onClick = { viewModel.sendKeyPress("alt") }
                )
                ModifierButton(
                    text = "Shift",
                    gradient = Brush.verticalGradient(listOf(Color(0xFFB5EAD7), Color(0xFFC7CEEA))),
                    onClick = { viewModel.sendKeyPress("shift") }
                )
                ModifierButton(
                    text = "Ctrl",
                    gradient = Brush.verticalGradient(listOf(Color(0xFFE2F0CB), Color(0xFFB5EAD7))),
                    onClick = { viewModel.sendKeyPress("ctrl") }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中央触摸板
            TouchpadArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(CardBackground)
                    .padding(24.dp),
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右列：功能键
            Column(
                modifier = Modifier.fillMaxHeight().width(80.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                ModifierButton(
                    icon = Icons.Default.ArrowBack,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFC7CEEA), Color(0xFFB5B9E0))),
                    onClick = onBack
                )
                ModifierButton(
                    icon = Icons.Default.DesktopWindows,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFE2F0CB), Color(0xFFD4E7C5))),
                    onClick = { viewModel.sendKeyPress("desktop") }
                )
                ModifierButton(
                    icon = Icons.Default.Layers,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFFFDAC1), Color(0xFFFFB7B2))),
                    onClick = { viewModel.sendKeyPress("layers") }
                )
                ModifierButton(
                    icon = Icons.Default.Keyboard,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFA8E6CF), Color(0xFF87CEEB))),
                    onClick = { viewModel.sendKeyPress("keyboard") }
                )
            }
        }
    }
}

@Composable
fun ModifierButton(
    text: String? = null,
    icon: ImageVector? = null,
    gradient: Brush,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun TouchpadArea(
    modifier: Modifier = Modifier,
    viewModel: TouchpadViewModel
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var lastPosition = down.position
                        var pointerCount = 1

                        val dragDetectionJob = launch {
                            delay(500)
                            if (pointerCount == 1) {
                                isDragging = true
                                viewModel.sendMouseClick("left_down")
                            }
                        }

                        var event: PointerEvent
                        do {
                            event = awaitPointerEvent()
                            pointerCount = event.changes.size

                            if (pointerCount > 1) {
                                dragDetectionJob.cancel()
                                if (isDragging) {
                                    isDragging = false
                                    viewModel.sendMouseClick("left_up")
                                }
                            }

                            when (pointerCount) {
                                1 -> {
                                    val change = event.changes.first()
                                    if (change.pressed) {
                                        val currentPosition = change.position
                                        val dx = currentPosition.x - lastPosition.x
                                        val dy = currentPosition.y - lastPosition.y

                                        if (abs(dx) > 0.1f || abs(dy) > 0.1f) {
                                            viewModel.sendMouseMove(dx, dy)
                                            lastPosition = currentPosition
                                        }
                                        change.consume()
                                    }
                                }
                                2 -> {
                                    val change1 = event.changes[0]
                                    val change2 = event.changes[1]
                                    if (change1.pressed && change2.pressed) {
                                        val dy = (change1.position.y - change1.previousPosition.y +
                                                  change2.position.y - change2.previousPosition.y) / 2
                                        if (abs(dy) > 1f) {
                                            viewModel.sendMouseScroll(0f, dy)
                                        }
                                    }
                                }
                                3 -> {
                                    val change = event.changes.first()
                                    val dx = change.position.x - change.previousPosition.x
                                    if (abs(dx) > 20f) {
                                        if (dx > 0) viewModel.sendKeyPress("next_window")
                                        else viewModel.sendKeyPress("prev_window")
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        dragDetectionJob.cancel()
                        val upTime = System.currentTimeMillis()

                        if (isDragging) {
                            isDragging = false
                            viewModel.sendMouseClick("left_up")
                        } else if (upTime - downTime < 300) {
                            if (pointerCount == 1) {
                                viewModel.sendMouseClick("left")
                            } else if (pointerCount == 2) {
                                viewModel.sendMouseClick("right")
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = FreshGreen.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("触摸板", fontSize = 28.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(32.dp))
            InstructionList()
        }
    }
}

@Composable
fun InstructionList() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InstructionItem("单指轻点 = 左键单击")
        InstructionItem("双指轻点 = 右键点击")
        InstructionItem("单指长按0.5s = 拖拽")
        InstructionItem("双指上下滑动 = 滚轮滚动")
        InstructionItem("三指左右滑动 = 切换窗口")
    }
}

@Composable
fun InstructionItem(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
