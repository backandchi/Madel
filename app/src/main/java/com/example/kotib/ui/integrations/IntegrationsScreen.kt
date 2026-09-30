package com.example.kotib.ui.integrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    viewModel: IntegrationsViewModel,
    onExecutePromptInChat: (String) -> Unit
) {
    val configs by viewModel.configs.collectAsState()
    val testState by viewModel.testState.collectAsState()

    var configToEdit by remember { mutableStateOf<IntegrationConfigEntity?>(null) }

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
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = SkyBlueLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Integratsiyalar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI bilan ilovalarni boshqarish",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO BANNER: Integratsiyalar haqida tushuntirish
            item {
                IntegrationHeroBanner()
            }

            // Test natijasi bildirishnomasi
            if (testState.serviceId != null) {
                item {
                    TestResultBanner(
                        state = testState,
                        onDismiss = { viewModel.clearTestState() }
                    )
                }
            }

            // INTEGRATSIYA KARTALARI
            items(configs, key = { it.serviceId }) { config ->
                IntegrationCard(
                    config = config,
                    isTesting = testState.serviceId == config.serviceId && testState.isTesting,
                    onToggle = { enabled -> viewModel.toggleService(config.serviceId, enabled) },
                    onEdit = { configToEdit = config },
                    onTest = { viewModel.testService(config.serviceId) },
                    onSelectExamplePrompt = onExecutePromptInChat
                )
            }
        }
    }

    if (configToEdit != null) {
        EditIntegrationDialog(
            config = configToEdit!!,
            onDismiss = { configToEdit = null },
            onSave = { updated ->
                viewModel.saveConfig(updated)
                configToEdit = null
            }
        )
    }
}

@Composable
fun IntegrationHeroBanner() {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B),
            Color(0xFF0F172A)
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SkyBluePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SkyBlueLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ilovalarni Kotib AI orqali boshqaring",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Xizmatlarni yoqing va tokenlarni kiriting. Kotib suhbatda yoki ovozli qo'ng'iroqda buyruqlaringizni avtomatik bajaradi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun TestResultBanner(
    state: IntegrationTestState,
    onDismiss: () -> Unit
) {
    val bgColor = if (state.isSuccess) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
    val contentColor = if (state.isSuccess) SuccessGreen else ErrorRed
    val icon = if (state.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.message.ifBlank { "Sinov yakunlandi" },
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(onClick = onDismiss) {
                Text("Yopish", fontSize = 11.sp, color = contentColor)
            }
        }
    }
}

@Composable
fun IntegrationCard(
    config: IntegrationConfigEntity,
    isTesting: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onSelectExamplePrompt: (String) -> Unit
) {
    val (brandColor, brandIcon, promptExamples) = when (config.serviceId) {
        IntegrationConfigEntity.SERVICE_TELEGRAM -> Triple(
            Color(0xFF2AABEE),
            Icons.AutoMirrored.Filled.Send,
            listOf("Telegram botimga bugungi hisobotni jo'nat", "Telegram'ga 'Uchrashuv soat 15:00 da' deb xabar yoz")
        )
        IntegrationConfigEntity.SERVICE_GITHUB -> Triple(
            Color(0xFF94A3B8),
            Icons.Default.Code,
            listOf("GitHub'da 'Login oynasida xatolik' deb issue och", "GitHub'dagi ochiq issue'larni ko'rsat")
        )
        IntegrationConfigEntity.SERVICE_TODOIST -> Triple(
            Color(0xFFE44332),
            Icons.Default.CheckCircle,
            listOf("Todoist'ga 'Ertaga hisobot tayyorlash' vazifasini qo'sh", "Todoist'dagi bugungi vazifalarni ko'rsat")
        )
        IntegrationConfigEntity.SERVICE_NOTION -> Triple(
            Color(0xFF6366F1),
            Icons.AutoMirrored.Filled.MenuBook,
            listOf("Notion'ga yangi g'oyani yoz: AI Agent arxitekturasi")
        )
        IntegrationConfigEntity.SERVICE_OBSIDIAN -> Triple(
            Color(0xFF8B5CF6),
            Icons.Default.Description,
            listOf("Obsidian vault'ga 'Rejalar.md' qaydini saqla", "Obsidian qaydlarim ro'yxatini ko'rsat")
        )
        else -> Triple(SkyBluePrimary, Icons.Default.Bolt, emptyList())
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("integration_card_${config.serviceId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Yuqori qator: Ikonka, Nomi va Yoqish switchi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(brandColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = brandIcon,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = config.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = config.lastStatusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (config.isEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_${config.serviceId}_switch")
                )
            }

            // Kengaytirilgan sozlamalar bloki
            AnimatedVisibility(
                visible = config.isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Kalit kiritilgan/kiritilmagan holati
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val maskedToken = if (config.apiKeyOrToken.isNotBlank()) {
                                    "••••••••" + config.apiKeyOrToken.takeLast(4)
                                } else if (config.serviceId == IntegrationConfigEntity.SERVICE_OBSIDIAN) {
                                    "Papka: ${config.extraParam1.ifBlank { "KotibVault" }}"
                                } else {
                                    "Token kiritilmagan"
                                }

                                Text(
                                    text = maskedToken,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (config.apiKeyOrToken.isNotBlank() || config.serviceId == IntegrationConfigEntity.SERVICE_OBSIDIAN) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        AmberAccent
                                    }
                                )
                                if (config.extraParam1.isNotBlank() && config.serviceId != IntegrationConfigEntity.SERVICE_OBSIDIAN) {
                                    Text(
                                        text = "Parametr: ${config.extraParam1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                FilledTonalButton(
                                    onClick = onEdit,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sozlash", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                OutlinedButton(
                                    onClick = onTest,
                                    enabled = !isTesting,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tekshirish", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // AI buyruq namunalari (Tezkor sinash)
                    if (promptExamples.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AI Buyruq namunalari (Chatda sinash):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            promptExamples.forEach { prompt ->
                                Surface(
                                    color = brandColor.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectExamplePrompt(prompt) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "\"$prompt\"",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = brandColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditIntegrationDialog(
    config: IntegrationConfigEntity,
    onDismiss: () -> Unit,
    onSave: (IntegrationConfigEntity) -> Unit
) {
    var tokenText by remember { mutableStateOf(config.apiKeyOrToken) }
    var param1Text by remember { mutableStateOf(config.extraParam1) }
    var param2Text by remember { mutableStateOf(config.extraParam2) }
    var showPassword by remember { mutableStateOf(false) }

    val (tokenLabel, param1Label) = when (config.serviceId) {
        IntegrationConfigEntity.SERVICE_TELEGRAM -> Pair("Bot Token (BotFather'dan olingan)", "Chat ID (masalan: 12345678)")
        IntegrationConfigEntity.SERVICE_GITHUB -> Pair("GitHub Personal Access Token", "Repozitoriy (masalan: owner/repo)")
        IntegrationConfigEntity.SERVICE_TODOIST -> Pair("Todoist API Token", "")
        IntegrationConfigEntity.SERVICE_NOTION -> Pair("Notion Internal Integration Token", "Database ID")
        IntegrationConfigEntity.SERVICE_OBSIDIAN -> Pair("Tavsif / Izoh", "Vault Papkasi nomi (masalan: KotibVault)")
        else -> Pair("API Token", "Qo'shimcha parametr")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${config.displayName} Sozlamalari") },
        text = {
            Column {
                if (config.serviceId != IntegrationConfigEntity.SERVICE_OBSIDIAN) {
                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        label = { Text(tokenLabel) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (param1Label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = param1Text,
                        onValueChange = { param1Text = it },
                        label = { Text(param1Label) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        config.copy(
                            apiKeyOrToken = tokenText.trim(),
                            extraParam1 = param1Text.trim(),
                            extraParam2 = param2Text.trim()
                        )
                    )
                }
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
