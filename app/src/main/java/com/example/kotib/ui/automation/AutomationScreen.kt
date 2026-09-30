package com.example.kotib.ui.automation

import android.accessibilityservice.AccessibilityService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotib.data.local.entity.AutomationTaskEntity
import com.example.kotib.service.KotibAccessibilityService
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel,
    onNavigateToChatWithCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var shellInput by remember { mutableStateOf("getprop ro.build.version.release") }
    var shellAsRoot by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IndigoSecondary.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SkyBlueLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Root & Avtomatlashtirish",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Real Tizim & UI Boshqaruvi",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = SkyBluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HOLAT PANELI (Root & Accessibility Status Badges)
            item {
                DeviceStatusCard(
                    isRoot = uiState.isRootAvailable,
                    isAccessibility = uiState.isAccessibilityEnabled,
                    onOpenAccessibility = { KotibAccessibilityService.openAccessibilitySettings(context) }
                )
            }

            // 2. REAL OVOZ REJIMLARI VA SLIDERLAR
            item {
                SoundAndVolumeCard(
                    ringerMode = uiState.ringerMode,
                    musicPercent = uiState.musicVolumePercent,
                    ringPercent = uiState.ringVolumePercent,
                    onSetRinger = { viewModel.setRingerMode(it) },
                    onSetVolume = { stream, p -> viewModel.setVolume(stream, p) }
                )
            }

            // 3. GLOBAL UI VA TIZIM SOZLAMALARI TEZKOR TUGMALARI
            item {
                QuickActionsCard(
                    isAccEnabled = uiState.isAccessibilityEnabled,
                    onGlobalAction = { viewModel.performGlobal(it) },
                    onOpenSetting = { viewModel.openSetting(it) },
                    onLaunchApp = { viewModel.launchApp(it) }
                )
            }

            // 4. AVTOMATIK SKRIPTLAR (AlarmManager Scheduled Tasks)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Avtomatik Skriptlar (${tasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = { showAddTaskDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skript qo'shish", fontSize = 11.sp)
                    }
                }
            }

            items(tasks, key = { it.id }) { task ->
                AutomationTaskCard(
                    task = task,
                    onToggle = { enabled -> viewModel.toggleTask(task.id, enabled) },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onRunNow = { onNavigateToChatWithCommand(task.actionCommand) }
                )
            }

            // 5. HAQIQIY SHELL & TERMINAL KONSOLI
            item {
                TerminalConsoleCard(
                    command = shellInput,
                    onCommandChange = { shellInput = it },
                    asRoot = shellAsRoot,
                    onAsRootChange = { shellAsRoot = it },
                    isRunning = uiState.isRunningShell,
                    shellResult = uiState.shellResult,
                    onRun = { viewModel.executeShell(shellInput, shellAsRoot) }
                )
            }
        }
    }

    if (showAddTaskDialog) {
        AddAutomationTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, time, command ->
                viewModel.addTask(title, time, command)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun DeviceStatusCard(
    isRoot: Boolean,
    isAccessibility: Boolean,
    onOpenAccessibility: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tizim Ruxsatlari & Imkoniyatlari",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Root Holati Chipi
                Surface(
                    color = if (isRoot) SuccessGreen.copy(alpha = 0.15f) else AmberAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRoot) Icons.Default.Security else Icons.Default.DeveloperMode,
                                contentDescription = null,
                                tint = if (isRoot) SuccessGreen else AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Root Huquqi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRoot) SuccessGreen else AmberAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRoot) "Mavjud (su faol)" else "Mavjud emas (sh rejimi)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Accessibility Service Chipi
                Surface(
                    color = if (isAccessibility) SuccessGreen.copy(alpha = 0.15f) else SkyBluePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isAccessibility) { onOpenAccessibility() }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccessibility) Icons.Default.CheckCircle else Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isAccessibility) SuccessGreen else SkyBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UI Avtomatlashtirish",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibility) SuccessGreen else SkyBluePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAccessibility) "Faol (Ekranni o'qish/bosish)" else "O'chiq (Yoqish uchun bosing)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoundAndVolumeCard(
    ringerMode: String,
    musicPercent: Int,
    ringPercent: Int,
    onSetRinger: (String) -> Unit,
    onSetVolume: (String, Int) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Haqiqiy Ovoz Rejimlari & Balandligi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ringer Mode tugmalari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ringerMode.contains("Jimjit", ignoreCase = true),
                    onClick = { onSetRinger("silent") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Jimjit", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = ringerMode.contains("Tebranish", ignoreCase = true),
                    onClick = { onSetRinger("vibrate") },
                    leadingIcon = { Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Tebranish", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = ringerMode.contains("Odatiy", ignoreCase = true),
                    onClick = { onSetRinger("normal") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Normal", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Musiqa balandligi slideri
            var localMusic by remember(musicPercent) { mutableFloatStateOf(musicPercent.toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Musiqa / Media: ${localMusic.toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = localMusic,
                onValueChange = { localMusic = it },
                onValueChangeFinished = { onSetVolume("music", localMusic.toInt()) },
                valueRange = 0f..100f
            )

            // Qo'ng'iroq balandligi slideri
            var localRing by remember(ringPercent) { mutableFloatStateOf(ringPercent.toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Qo'ng'iroq (Ringtone): ${localRing.toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = localRing,
                onValueChange = { localRing = it },
                onValueChangeFinished = { onSetVolume("ring", localRing.toInt()) },
                valueRange = 0f..100f
            )
        }
    }
}

@Composable
fun QuickActionsCard(
    isAccEnabled: Boolean,
    onGlobalAction: (Int) -> Unit,
    onOpenSetting: (String) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tizim Sozlamalari & UI Avtomat Amallari",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Global Actions (UI Navigation)
            Text(
                text = "Tugmalarsiz navigatsiya (Accessibility):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bosh ekran", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Orqaga", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bildirishnomalar", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Qulflash", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tezkor sozlamalar
            Text(
                text = "Tezkor sozlamalar oynasi:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onOpenSetting("wifi") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wi-Fi", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { onOpenSetting("bluetooth") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bluetooth", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { onOpenSetting("battery") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batareya tejash", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { onOpenSetting("airplane") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AirplanemodeActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Samolyot", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AutomationTaskCard(
    task: AutomationTaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRunNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isEnabled) IndigoSecondary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.triggerTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = task.actionCommand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRunNow) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI bilan bajarish", fontSize = 11.sp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun TerminalConsoleCard(
    command: String,
    onCommandChange: (String) -> Unit,
    asRoot: Boolean,
    onAsRootChange: (Boolean) -> Unit,
    isRunning: Boolean,
    shellResult: com.example.kotib.domain.automation.ShellResult?,
    onRun: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF0F172A)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Haqiqiy Shell & Terminal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("su (Root)", fontSize = 10.sp, color = if (asRoot) AmberAccent else Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = asRoot,
                        onCheckedChange = onAsRootChange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                label = { Text("Shell buyrug'i", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRun,
                enabled = !isRunning && command.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buyruqni Ijro Etish (Run)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (shellResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Exit Code: ${shellResult.exitCode} (${if (shellResult.isSuccess) "Success" else "Failed"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (shellResult.isSuccess) SuccessGreen else ErrorRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (shellResult.output.isNotBlank()) shellResult.output else shellResult.error).ifBlank { "[Natija bo'sh]" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AddAutomationTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, time: String, command: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("23:00") }
    var command by remember { mutableStateOf("Telefon ovozini o'chir va dam olish rejimiga o't") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi Avtomatik Skript") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Vazifa nomi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Ijro vaqti (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("AI Buyrug'i") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, time, command) },
                enabled = title.isNotBlank() && time.isNotBlank() && command.isNotBlank()
            ) {
                Text("Qo'shish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
