package com.beyond.control.ui.screen

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.beyond.control.ui.theme.*
import com.beyond.control.ui.viewmodel.ControlViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MainScreen(viewModel: ControlViewModel) {
    val context = LocalContext.current
    val connectedIp by viewModel.connectedIp.collectAsState()
    val isFlyMouse by viewModel.isFlyMouseEnabled.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showVolumeSheet by remember { mutableStateOf(false) }
    var showDirectionSheet by remember { mutableStateOf(false) }

    // QR 扫码
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            val ip = parseIpFromQrContent(result.contents)
            if (ip != null) {
                viewModel.setConnectedIp(ip)
                viewModel.connectToComputer(ip)
            }
        }
    }

    // 锁定竖屏
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 注册传感器
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.setSensorManager(sensorManager)
        onDispose {
            viewModel.setSensorManager(null)
        }
    }

    // 启动时自动连接
    var autoConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (connectedIp.isNotEmpty() && !autoConnected) {
            autoConnected = true
            viewModel.connectToComputer(connectedIp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // === 顶部：连接控制卡片 ===
        ConnectionHeader(
            viewModel = viewModel,
            onScanClick = {
                val options = ScanOptions()
                options.setPrompt("扫描二维码连接电脑")
                options.setBeepEnabled(false)
                options.setOrientationLocked(true)
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                scanLauncher.launch(options)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // === 中间：触控板 / 飞鼠 ===
        if (isFlyMouse) {
            FlyMouseCenterArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                viewModel = viewModel
            )
        } else {
            UnifiedTouchpadArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground),
                viewModel = viewModel
            )
        }

        // === 底部：功能按钮 ===
        FunctionButtonRow(
            onVolumeClick = { showVolumeSheet = true },
            onDirectionClick = { showDirectionSheet = true },
            onSearchClick = { viewModel.sendKeyPress("search") },
            onDesktopClick = { viewModel.sendKeyPress("desktop") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 音量弹窗
    if (showVolumeSheet) {
        VolumeBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVolumeSheet = false }
        )
    }

    // 方向控制弹窗
    if (showDirectionSheet) {
        DirectionPadBottomSheet(
            viewModel = viewModel,
            onDismiss = { showDirectionSheet = false }
        )
    }
}

// ============================================================
// 从二维码内容解析 IP
// ============================================================

private fun parseIpFromQrContent(content: String): String? {
    // 支持格式: ws://IP:1800/ws/control 或纯 IP 地址
    val wsPattern = Regex("""ws://(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""")
    wsPattern.find(content)?.groupValues?.get(1)?.let { return it }

    val ipPattern = Regex("""^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})$""")
    ipPattern.find(content.trim())?.groupValues?.get(1)?.let { return it }

    return null
}

// ============================================================
// 顶部连接控制卡片
// ============================================================

@Composable
private fun ConnectionHeader(
    viewModel: ControlViewModel,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedIp by viewModel.connectedIp.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isFlyMouse by viewModel.isFlyMouseEnabled.collectAsState()

    val isConnected = connectionState == "CONNECTED"
    val isConnecting = connectionState == "CONNECTING"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        tonalElevation = 1.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：状态信息 + 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态指示点
                Surface(
                    shape = CircleShape,
                    color = when (connectionState) {
                        "CONNECTED" -> FreshGreen
                        "CONNECTING" -> FreshOrange
                        "ERROR" -> Color(0xFFEF5350)
                        else -> TextHint
                    },
                    modifier = Modifier.size(10.dp)
                ) {}

                Spacer(modifier = Modifier.width(10.dp))

                // 状态文本 / IP
                Text(
                    text = when (connectionState) {
                        "CONNECTED" -> connectedIp
                        "CONNECTING" -> "正在连接..."
                        "ERROR" -> "连接失败"
                        else -> "未连接"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = when (connectionState) {
                        "CONNECTED" -> FreshGreen
                        "CONNECTING" -> FreshOrange
                        "ERROR" -> Color(0xFFEF5350)
                        else -> TextSecondary
                    },
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 飞鼠模式切换
                HeaderIconButton(
                    icon = if (isFlyMouse) Icons.Default.ScreenRotation else Icons.Default.PanTool,
                    contentDescription = "飞鼠模式",
                    tint = if (isFlyMouse) FreshGreen else TextSecondary,
                    bgColor = if (isFlyMouse) MintGreen.copy(alpha = 0.3f) else DividerLight,
                    onClick = { viewModel.toggleFlyMouse() }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 扫码按钮
                HeaderIconButton(
                    icon = Icons.Default.QrCodeScanner,
                    contentDescription = "扫码连接",
                    tint = FreshBlue,
                    bgColor = SoftSky.copy(alpha = 0.4f),
                    onClick = onScanClick
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 连接/断开按钮
                if (isConnected) {
                    // 已连接：红色断开按钮
                    Button(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("断开", fontSize = 13.sp, color = Color.White)
                    }
                } else if (isConnecting) {
                    // 连接中：橙色按钮
                    Button(
                        onClick = {},
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshOrange),
                        enabled = false,
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("连接中", fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            // 第二行：IP 输入 + 连接按钮（未连接时显示）
            if (!isConnected && !isConnecting) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = connectedIp,
                        onValueChange = { viewModel.setConnectedIp(it) },
                        placeholder = {
                            Text("输入 IP 地址", fontSize = 13.sp, color = TextHint)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FreshGreen,
                            unfocusedBorderColor = DividerLight,
                            cursorColor = FreshGreen
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { viewModel.connectToComputer(connectedIp) },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("连接", fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            // 错误信息
            if (connectionState == "ERROR" && errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF5350).copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp)),
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================
// 触控板区域（合并版）
// ============================================================

@Composable
private fun UnifiedTouchpadArea(
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel
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
                        var maxPointerCount = 1

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
                            if (pointerCount > maxPointerCount) {
                                maxPointerCount = pointerCount
                            }

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
                                            viewModel.sendMouseScroll(dy)
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
                            if (maxPointerCount == 1) viewModel.sendMouseClick("left")
                            else if (maxPointerCount >= 2) viewModel.sendMouseClick("right")
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
            Text("三指左右 = 切换窗口", fontSize = 13.sp, color = TextHint)
            Text("长按0.5s = 拖拽", fontSize = 13.sp, color = TextHint)
        }
    }
}

// ============================================================
// 飞鼠模式中心区域
// ============================================================

@Composable
private fun FlyMouseCenterArea(
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel
) {
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    var maxPointerCount = 1
                    var moved = false

                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent()
                        val pointerCount = event.changes.size
                        if (pointerCount > maxPointerCount) {
                            maxPointerCount = pointerCount
                        }

                        when (pointerCount) {
                            2 -> {
                                val change1 = event.changes[0]
                                val change2 = event.changes[1]
                                if (change1.pressed && change2.pressed) {
                                    val dy = (change1.position.y - change1.previousPosition.y +
                                              change2.position.y - change2.previousPosition.y) / 2
                                    if (abs(dy) > 1f) {
                                        moved = true
                                        viewModel.sendMouseScroll(dy)
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (moved) return@awaitEachGesture

                    val upTime = System.currentTimeMillis()
                    val duration = upTime - downTime

                    when {
                        maxPointerCount >= 2 && duration < 300 -> {
                            viewModel.sendMouseClick("right")
                        }
                        maxPointerCount == 1 && duration >= 500 -> {
                            viewModel.sendMouseClick("right")
                        }
                        maxPointerCount == 1 && duration < 300 -> {
                            if (upTime - lastTapTime < 300) {
                                viewModel.sendMouseClick("left")
                                viewModel.sendMouseClick("left")
                                lastTapTime = 0L
                            } else {
                                lastTapTime = upTime
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),
                color = MintGreen.copy(alpha = 0.2f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        color = CardBackground,
                        tonalElevation = 6.dp,
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = null,
                                tint = FreshGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "摇动手机控制鼠标",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("操作提示", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("单击 → 左键", fontSize = 13.sp, color = TextSecondary)
                Text("双击 → 双击左键", fontSize = 13.sp, color = TextSecondary)
                Text("长按 → 右键", fontSize = 13.sp, color = TextSecondary)
                Text("双指上下 = 滚轮", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

// ============================================================
// 底部功能按钮行
// ============================================================

@Composable
private fun FunctionButtonRow(
    onVolumeClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDesktopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FunctionButton(
                icon = Icons.Default.VolumeUp,
                label = "音量",
                iconColor = FreshPink,
                bgColor = Lavender.copy(alpha = 0.5f),
                onClick = onVolumeClick
            )
            FunctionButton(
                icon = Icons.Default.Navigation,
                label = "方向",
                iconColor = FreshBlue,
                bgColor = SoftSky.copy(alpha = 0.4f),
                onClick = onDirectionClick
            )
            FunctionButton(
                icon = Icons.Default.Search,
                label = "搜索",
                iconColor = FreshOrange,
                bgColor = SoftPeach.copy(alpha = 0.5f),
                onClick = onSearchClick
            )
            FunctionButton(
                icon = Icons.Default.DesktopWindows,
                label = "桌面",
                iconColor = FreshGreen,
                bgColor = MintGreen.copy(alpha = 0.4f),
                onClick = onDesktopClick
            )
        }
    }
}

@Composable
private fun FunctionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = bgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

// ============================================================
// 音量控制弹窗
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeBottomSheet(
    viewModel: ControlViewModel,
    onDismiss: () -> Unit
) {
    val volumeLevel by viewModel.volumeLevel.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VolumeCircle(
                level = volumeLevel,
                isMuted = isMuted,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isMuted) "已静音" else "${(volumeLevel * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) TextHint else TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            VolumeSlider(
                level = volumeLevel,
                isMuted = isMuted,
                onLevelChange = { viewModel.updateLocalLevel(it) },
                onLevelChangeFinished = { viewModel.setVolume(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VolumeActionButton(
                    icon = Icons.Default.VolumeDown,
                    label = "减",
                    enabled = !isMuted,
                    onClick = { viewModel.volumeDown() }
                )

                VolumeActionButton(
                    icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    label = if (isMuted) "取消静音" else "静音",
                    enabled = true,
                    isMuteButton = true,
                    isMuted = isMuted,
                    onClick = { viewModel.toggleMute() }
                )

                VolumeActionButton(
                    icon = Icons.Default.VolumeUp,
                    label = "加",
                    enabled = !isMuted,
                    onClick = { viewModel.volumeUp() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VolumeCircle(
    level: Float,
    isMuted: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.size(size),
            color = DividerLight,
            strokeWidth = 8.dp,
            trackColor = DividerLight
        )
        if (!isMuted && level > 0f) {
            CircularProgressIndicator(
                progress = level,
                modifier = Modifier.size(size),
                color = FreshGreen,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )
        }
        val innerSize = size * 0.6f
        Surface(
            modifier = Modifier.size(innerSize).clip(CircleShape),
            color = CardBackground,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isMuted -> Icons.Default.VolumeOff
                        level < 0.3f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = null,
                    tint = when {
                        isMuted -> TextHint
                        level < 0.3f -> FreshOrange
                        else -> FreshGreen
                    },
                    modifier = Modifier.size(innerSize * 0.43f)
                )
            }
        }
    }
}

@Composable
private fun VolumeSlider(
    level: Float,
    isMuted: Boolean,
    onLevelChange: (Float) -> Unit,
    onLevelChangeFinished: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextHint, modifier = Modifier.size(20.dp))
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextHint, modifier = Modifier.size(20.dp))
        }

        Slider(
            value = level,
            onValueChange = { onLevelChange(it) },
            onValueChangeFinished = { onLevelChangeFinished(level) },
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

@Composable
private fun VolumeActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    isMuteButton: Boolean = false,
    isMuted: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(60.dp).clip(CircleShape),
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
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (enabled || isMuteButton) TextSecondary else TextHint
        )
    }
}

// ============================================================
// 方向控制弹窗
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionPadBottomSheet(
    viewModel: ControlViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("方向控制", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Spacer(modifier = Modifier.size(80.dp))
                    DirectionPadButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "上",
                        onClick = { viewModel.sendKeyPress("up") }
                    )
                    Spacer(modifier = Modifier.size(80.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DirectionPadButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        label = "左",
                        onClick = { viewModel.sendKeyPress("left") }
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    DirectionPadButton(
                        icon = Icons.Default.Done,
                        label = "确认",
                        onClick = { viewModel.sendKeyPress("enter") },
                        isCenter = true
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    DirectionPadButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        label = "右",
                        onClick = { viewModel.sendKeyPress("right") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row {
                    Spacer(modifier = Modifier.size(80.dp))
                    DirectionPadButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        label = "下",
                        onClick = { viewModel.sendKeyPress("down") }
                    )
                    Spacer(modifier = Modifier.size(80.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DirectionPadButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isCenter: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = if (isCenter) FreshGreen.copy(alpha = 0.2f) else SoftSky.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isCenter) FreshGreen else FreshBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}
