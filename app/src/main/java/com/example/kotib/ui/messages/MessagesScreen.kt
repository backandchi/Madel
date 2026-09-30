package com.example.kotib.ui.messages

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotib.data.local.entity.NotificationMessageEntity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onOpenChatWithQuery: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSimulateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IndigoSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = IndigoLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Xabarlar Markazi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI Saralash & Bildirishnomalar",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Sinov xabari yuborish tugmasi
                    FilledTonalButton(
                        onClick = { showSimulateDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("simulate_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sinov Xabari", fontSize = 11.sp)
                    }

                    if (state.messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllMessages() },
                            modifier = Modifier.testTag("clear_all_messages_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Tozalash",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. STATISTIKA PLITKALARI (Expressive Stat Cards)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Bugun",
                        count = state.todayCount,
                        icon = Icons.Default.MarkEmailRead,
                        color = SkyBluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatMetricCard(
                        title = "Muhim (VIP)",
                        count = state.todayImportantCount,
                        icon = Icons.Default.Star,
                        color = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )

                    val urgentCount = state.messages.count { it.urgencyLevel >= 4 }
                    StatMetricCard(
                        title = "Shoshilinch",
                        count = urgentCount,
                        icon = Icons.Default.PriorityHigh,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. HERO CARD: "BUGUN KIM NIMA YOZDI?" (One-tap Executive Briefing)
            item {
                TodayReportHeroCard(
                    isGenerating = state.isGeneratingReport,
                    reportText = state.todayReportText,
                    onGenerateReport = { viewModel.generateTodayReport() },
                    onDismissReport = { viewModel.clearReport() },
                    onAskKotib = {
                        onOpenChatWithQuery("Bugun kim nima yozdi? To'liq hisobot ber.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 3. FILTRLAR (Filter Chips with vibrant icons)
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = SkyBlueLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kategoriya bo'yicha saralash:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            MessageFilterChip(
                                label = "Barchasi (${state.messages.size})",
                                isSelected = state.selectedFilter == "ALL",
                                icon = Icons.Default.Inbox,
                                onClick = { viewModel.setFilter("ALL") }
                            )
                        }
                        item {
                            MessageFilterChip(
                                label = "Telegram",
                                isSelected = state.selectedFilter == "TELEGRAM",
                                icon = Icons.AutoMirrored.Filled.Send,
                                activeColor = Color(0xFF2AABEE),
                                onClick = { viewModel.setFilter("TELEGRAM") }
                            )
                        }
                        item {
                            MessageFilterChip(
                                label = "WhatsApp",
                                isSelected = state.selectedFilter == "WHATSAPP",
                                icon = Icons.Default.ChatBubble,
                                activeColor = Color(0xFF25D366),
                                onClick = { viewModel.setFilter("WHATSAPP") }
                            )
                        }
                        item {
                            MessageFilterChip(
                                label = "SMS",
                                isSelected = state.selectedFilter == "SMS",
                                icon = Icons.Default.Sms,
                                activeColor = IndigoLight,
                                onClick = { viewModel.setFilter("SMS") }
                            )
                        }
                        item {
                            MessageFilterChip(
                                label = "Muhim (4-5★)",
                                isSelected = state.selectedFilter == "IMPORTANT",
                                icon = Icons.Default.Star,
                                activeColor = AmberAccent,
                                onClick = { viewModel.setFilter("IMPORTANT") }
                            )
                        }
                    }
                }
            }

            // 4. XABARLAR RO'YXATI
            if (state.filteredMessages.isEmpty()) {
                item {
                    EmptyMessagesView(onSimulate = { showSimulateDialog = true })
                }
            } else {
                items(state.filteredMessages, key = { it.id }) { message ->
                    NotificationMessageCard(
                        message = message,
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onAskKotib = {
                            onOpenChatWithQuery("${message.sender}ning quyidagi xabariga qanday javob yozishni maslahat berasan: \"${message.cleanContent}\"")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    if (showSimulateDialog) {
        SimulateMessageDialog(
            onDismiss = { showSimulateDialog = false },
            onSend = { app, sender, text ->
                viewModel.simulateMessage(app, sender, text)
                showSimulateDialog = false
            }
        )
    }
}

@Composable
fun StatMetricCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TodayReportHeroCard(
    isGenerating: Boolean,
    reportText: String?,
    onGenerateReport: () -> Unit,
    onDismissReport: () -> Unit,
    onAskKotib: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            IndigoSecondary.copy(alpha = 0.85f),
            SkyBluePrimary.copy(alpha = 0.85f)
        )
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column {
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
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bugun kim nima yozdi?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onGenerateReport,
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = IndigoSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = IndigoSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Hisobot olish", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Barcha kiruvchi xabarlar va SMS'larni Gemini orqali tahlil qilib, 1 daqiqada to'liq xulosa beradi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                // Agar hisobot tayyor bo'lsa
                AnimatedVisibility(
                    visible = !reportText.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kotib AI Xulosasi:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SkyBlueLight
                            )
                            TextButton(onClick = onDismissReport) {
                                Text("Yopish", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        Text(
                            text = reportText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = onAskKotib,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kotib bilan muhokama qilish", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageFilterChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color = SkyBluePrimary,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.5.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else activeColor,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun NotificationMessageCard(
    message: NotificationMessageEntity,
    onDelete: () -> Unit,
    onAskKotib: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    val (appColor, appIcon) = when {
        message.appName.contains("telegram", ignoreCase = true) -> Pair(Color(0xFF2AABEE), Icons.AutoMirrored.Filled.Send)
        message.appName.contains("whatsapp", ignoreCase = true) -> Pair(Color(0xFF25D366), Icons.Default.ChatBubble)
        message.messageType == NotificationMessageEntity.TYPE_SMS || message.appName.contains("sms", ignoreCase = true) -> Pair(IndigoLight, Icons.Default.Sms)
        else -> Pair(SkyBluePrimary, Icons.Default.NotificationsActive)
    }

    val urgencyColor = when (message.urgencyLevel) {
        5 -> ErrorRed
        4 -> AmberAccent
        3 -> SkyBluePrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("message_card_${message.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.urgencyLevel >= 4 || message.isImportant) {
                urgencyColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Yuqori qator: Ilova belgisi, Yuboruvchi va Vaqt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(appColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = appIcon,
                            contentDescription = message.appName,
                            tint = appColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = message.sender,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${message.appName} • $timeStr",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Muhimlik yulduzlari
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(message.urgencyLevel.coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = urgencyColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (message.isImportant) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gemini xulosasi
            if (message.summary.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Xabarning o'zi (maskalangan toza matn)
            Text(
                text = message.cleanContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pastki tavsiya va amallar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tavsiya etilgan amal chipi
                if (message.suggestedAction.isNotBlank()) {
                    Surface(
                        color = urgencyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = urgencyColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.suggestedAction,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = urgencyColor
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row {
                    TextButton(
                        onClick = onAskKotib,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Kotibga topshirish", fontSize = 11.sp, color = SkyBluePrimary)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "O'chirish",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMessagesView(onSimulate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IndigoLight.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = IndigoLight,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Hozircha xabarlar yo'q",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Telegram, WhatsApp yoki SMS'dan yangi bildirishnomalar kelishi bilan Kotib ularni avtomatik saralaydi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onSimulate) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sinov Xabari Yuborish (Test)")
        }
    }
}

@Composable
fun SimulateMessageDialog(
    onDismiss: () -> Unit,
    onSend: (app: String, sender: String, text: String) -> Unit
) {
    var selectedApp by remember { mutableStateOf("Telegram") }
    var senderText by remember { mutableStateOf("Akmal Rahimov") }
    var messageContent by remember { mutableStateOf("Salom, loyiha bo'yicha shoshilinch uchrashuv bor, darhol qo'ng'iroq qil!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sinov Xabarini Yuborish") },
        text = {
            Column {
                Text(
                    text = "Xabarlar markazi va Gemini AI saralashini sinab ko'rish uchun test xabari yarating:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Telegram", "WhatsApp", "SMS").forEach { app ->
                        FilterChip(
                            selected = selectedApp == app,
                            onClick = { selectedApp = app },
                            label = { Text(app, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = senderText,
                    onValueChange = { senderText = it },
                    label = { Text("Yuboruvchi (Ism / Raqam)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = messageContent,
                    onValueChange = { messageContent = it },
                    label = { Text("Xabar matni") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(selectedApp, senderText, messageContent) },
                enabled = senderText.isNotBlank() && messageContent.isNotBlank()
            ) {
                Text("Yuborish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
