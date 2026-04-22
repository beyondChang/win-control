package com.beyond.control.ui.screen

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beyond.control.ui.theme.*
import com.beyond.control.ui.viewmodel.MouseViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MouseControlScreen(
    viewModel: MouseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isFlyMouse by viewModel.isFlyMouseEnabled.collectAsState()

    // 锁定竖屏
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 注册 SensorManager
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.setSensorManager(sensorManager)
        onDispose {
            viewModel.setSensorManager(null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (isFlyMouse) {
            // 飞鼠模式：不拦截触摸，全屏显示
            FlyMouseContent(viewModel, onBack, isFlyMouse)
        } else {
            // 触摸滑动模式：整页可滑动
            TouchpadContent(viewModel, onBack)
        }
    }
}

@Composable
private fun FlyMouseContent(
    viewModel: MouseViewModel,
    onBack: () -> Unit,
    isFlyMouse: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
                color = CardBackground.copy(alpha = 0.9f),
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = FreshBlue)
                }
            }

            Text("飞鼠模式", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

            FlyMouseToggle(isEnabled = isFlyMouse, onToggle = { viewModel.toggleFlyMouse() })
        }

        // 中央状态区 - 垂直水平居中
        FlyMouseCenterArea(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun TouchpadContent(
    viewModel: MouseViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
                color = CardBackground.copy(alpha = 0.9f),
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = FreshBlue)
                }
            }

            Text("滑动控制", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

            FlyMouseToggle(isEnabled = false, onToggle = { viewModel.toggleFlyMouse() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 触摸滑动区域
        TouchpadArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(16.dp),
            viewModel = viewModel
        )
    }
}

@Composable
fun FlyMouseToggle(isEnabled: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) FreshGreen else TextSecondary,
        label = "toggleBg"
    )

    Surface(
        modifier = Modifier.size(44.dp).clip(CircleShape),
        color = backgroundColor,
        onClick = onToggle
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.ScreenRotation else Icons.Default.PanTool,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FlyMouseCenterArea(modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(targetValue = 1.08f, label = "centerScale")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(180.dp).scale(scale).clip(CircleShape),
            color = MintGreen.copy(alpha = 0.2f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(130.dp).clip(CircleShape),
                    color = CardBackground,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = FreshGreen,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "摇动手机控制鼠标",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("操作提示", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("单击 → 左键", fontSize = 13.sp, color = TextSecondary)
            Text("双击 → 双击左键", fontSize = 13.sp, color = TextSecondary)
            Text("长按 → 右键", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
fun TouchpadArea(
    modifier: Modifier = Modifier,
    viewModel: MouseViewModel
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    var lastPosition = down.position
                    var pointerCount = 1
                    var isLongPress = false

                    val dragDetectJob = launch {
                        delay(500)
                        if (pointerCount == 1) {
                            isLongPress = true
                            isDragging = true
                            viewModel.sendMouseClick("left_down")
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        pointerCount = event.changes.size
                        if (pointerCount > 1) {
                            dragDetectJob.cancel()
                            if (isDragging) {
                                isDragging = false
                                viewModel.sendMouseClick("left_up")
                            }
                        }

                        when (pointerCount) {
                            1 -> {
                                val change = event.changes.first()
                                if (change.pressed) {
                                    val current = change.position
                                    val dx = current.x - lastPosition.x
                                    val dy = current.y - lastPosition.y
                                    if (abs(dx) > 0.1f || abs(dy) > 0.1f) {
                                        viewModel.sendMouseMoveF(dx, dy)
                                        lastPosition = current
                                    }
                                    change.consume()
                                }
                            }
                            2 -> {
                                val c1 = event.changes[0]
                                val c2 = event.changes[1]
                                if (c1.pressed && c2.pressed) {
                                    val dy = (c1.position.y - c1.previousPosition.y +
                                              c2.position.y - c2.previousPosition.y) / 2
                                    if (abs(dy) > 1f) {
                                        viewModel.sendMouseScroll(dy.toInt())
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    dragDetectJob.cancel()
                    val upTime = System.currentTimeMillis()

                    if (isDragging) {
                        isDragging = false
                        viewModel.sendMouseClick("left_up")
                    } else if (upTime - downTime < 300 && !isLongPress) {
                        if (pointerCount == 1) viewModel.sendMouseClick("left")
                        else if (pointerCount == 2) viewModel.sendMouseClick("right")
                    }
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = FreshGreen.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("滑动控制鼠标", fontSize = 20.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("单指滑动 = 移动", fontSize = 13.sp, color = TextHint)
            Text("单指轻点 = 左键", fontSize = 13.sp, color = TextHint)
            Text("双指轻点 = 右键", fontSize = 13.sp, color = TextHint)
            Text("双指上下 = 滚轮", fontSize = 13.sp, color = TextHint)
            Text("长按0.5s = 拖拽", fontSize = 13.sp, color = TextHint)
        }
    }
}
