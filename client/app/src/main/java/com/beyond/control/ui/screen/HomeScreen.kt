package com.beyond.control.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beyond.control.ui.theme.*
import com.beyond.control.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val connectedIp by viewModel.connectedIp.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 启动时自动连接（如果 IP 不为空）
    var autoConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (connectedIp.isNotEmpty() && !autoConnected) {
            autoConnected = true
            viewModel.connectToComputer(connectedIp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("遥控器", style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连接卡片
            RemoteCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 标题行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MintGreen.copy(alpha = 0.3f)
                            ) {
                                Icon(
                                    Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = FreshGreen,
                                    modifier = Modifier.padding(10.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("设备连接", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }

                        IconButton(onClick = {}) {
                            Icon(Icons.Default.QrCode, contentDescription = "扫码连接", tint = FreshBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // IP 输入框
                    OutlinedTextField(
                        value = connectedIp,
                        onValueChange = { viewModel.setConnectedIp(it) },
                        label = { Text("设备 IP 地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FreshGreen,
                            unfocusedBorderColor = DividerLight,
                            focusedLabelColor = FreshGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 连接状态指示 + 错误详情
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (connectionState) {
                                    "CONNECTED" -> FreshGreen
                                    "CONNECTING" -> FreshOrange
                                    "ERROR" -> Color(0xFFEF5350)
                                    else -> TextHint
                                }
                            ) {
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (connectionState) {
                                    "CONNECTED" -> "已连接"
                                    "CONNECTING" -> "正在连接..."
                                    "ERROR" -> "连接失败"
                                    else -> "连接需要在同一局域网内"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (connectionState) {
                                    "CONNECTED" -> FreshGreen
                                    "CONNECTING" -> FreshOrange
                                    "ERROR" -> Color(0xFFEF5350)
                                    else -> TextSecondary
                                }
                            )
                        }

                        // 错误详情
                        if (connectionState == "ERROR" && errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF5350).copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = errorMessage.toString(),
                                    fontSize = 12.sp,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 连接按钮
                    Button(
                        onClick = {
                            if (connectionState == "CONNECTED") {
                                viewModel.disconnect()
                            } else if (connectionState != "CONNECTING") {
                                viewModel.connectToComputer(connectedIp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (connectionState) {
                                "CONNECTED" -> Color(0xFFEF5350)
                                "CONNECTING" -> FreshOrange
                                "ERROR" -> FreshGreen
                                else -> FreshGreen
                            }
                        ),
                        enabled = connectionState != "CONNECTING"
                    ) {
                        if (connectionState == "CONNECTING") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = when (connectionState) {
                                "CONNECTED" -> "断开连接"
                                "CONNECTING" -> "连接中..."
                                else -> "连接电脑"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 功能网格
            Text(
                text = "功能模块",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MouseCard(
                    isConnected = connectionState == "CONNECTED",
                    onClick = {
                        if (connectionState == "CONNECTED") {
                            onNavigate("mouse")
                        }
                    }
                )
                TVTimeCard(
                    isConnected = connectionState == "CONNECTED",
                    onClick = {
                        if (connectionState == "CONNECTED") {
                            onNavigate("tv")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TouchPadCard(
                    isConnected = connectionState == "CONNECTED",
                    onClick = {
                        if (connectionState == "CONNECTED") {
                            onNavigate("touchpad")
                        }
                    }
                )
                VolumeCard(
                    isConnected = connectionState == "CONNECTED",
                    onClick = {
                        if (connectionState == "CONNECTED") {
                            onNavigate("volume")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RemoteCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBackground,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) { content() }
    }
}

@Composable
fun RowScope.MouseCard(isConnected: Boolean, onClick: () -> Unit) {
    NavigationCard(
        iconVector = Icons.Default.Mouse,
        iconColor = FreshBlue,
        backgroundColor = SoftSky.copy(alpha = 0.4f),
        title = "鼠标",
        subtitle = "飞鼠 / 触控板",
        isEnabled = isConnected,
        onClick = onClick
    )
}

@Composable
fun RowScope.TVTimeCard(isConnected: Boolean, onClick: () -> Unit) {
    NavigationCard(
        iconVector = Icons.Default.Tv,
        iconColor = FreshOrange,
        backgroundColor = SoftPeach.copy(alpha = 0.5f),
        title = "电视时光",
        subtitle = "视频搜索 / 换台",
        isEnabled = isConnected,
        onClick = onClick
    )
}

@Composable
fun RowScope.TouchPadCard(isConnected: Boolean, onClick: () -> Unit) {
    NavigationCard(
        iconVector = Icons.Default.TouchApp,
        iconColor = FreshGreen,
        backgroundColor = MintGreen.copy(alpha = 0.4f),
        title = "触控板",
        subtitle = "手势控制",
        isEnabled = isConnected,
        onClick = onClick
    )
}

@Composable
fun RowScope.VolumeCard(isConnected: Boolean, onClick: () -> Unit) {
    NavigationCard(
        iconVector = Icons.Default.VolumeUp,
        iconColor = FreshPink,
        backgroundColor = Lavender.copy(alpha = 0.6f),
        title = "音量",
        subtitle = "音量 / 静音",
        isEnabled = isConnected,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.NavigationCard(
    iconVector: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    title: String,
    subtitle: String = "",
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) CardBackground else CardBackground.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isEnabled) backgroundColor else DividerLight,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = title,
                        tint = if (isEnabled) iconColor else TextHint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isEnabled) TextPrimary else TextHint
            )

            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) TextSecondary else TextHint
                )
            }

            if (!isEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "请先连接设备",
                    fontSize = 11.sp,
                    color = FreshOrange,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
