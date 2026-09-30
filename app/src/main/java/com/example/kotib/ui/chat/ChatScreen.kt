package com.example.kotib.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotib.KotibContainer
import com.example.kotib.data.local.entity.ChatMessageEntity
import com.example.kotib.domain.agent.AgentState
import com.example.kotib.ui.chat.components.MessageBubble
import com.example.kotib.ui.chat.components.ToolExecutionCard
import com.example.kotib.ui.chat.components.VoiceInputBar
import com.example.kotib.ui.keys.ApiKeysSheet
import com.example.kotib.ui.keys.ApiKeysViewModel
import com.example.kotib.ui.logs.ActionLogsDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    container: KotibContainer
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showKeysSheet by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }

    val keysSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val logsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Xabarlar soni o'zgarganda ro'yxatni pastga tushirish
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val keysViewModel = remember {
        ApiKeysViewModel(container.apiKeyRepository)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SkyBluePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Kotib",
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aiko",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Faol holat yashil nuqtasi
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.agentState is AgentState.Idle) SuccessGreen else AmberAccent
                                        )
                                )
                            }

                            // Faol kalit chipi (bosilganda sozlamalar ochiladi)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showKeysSheet = true }
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = SkyBluePrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = uiState.activeKey?.let { "${it.label} • ${it.dailyUsageCount}" } ?: "Kalit yo'q",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Ovozli qo'ng'iroq rejimi (Voice Call)
                    IconButton(
                        onClick = { container.callManager.startDirectCall() },
                        modifier = Modifier.testTag("voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Ovozli qo'ng'iroq (Voice Call)",
                            tint = SuccessGreen
                        )
                    }

                    // Favqulodda to'xtatish (Emergency Stop)
                    IconButton(
                        onClick = { viewModel.emergencyStop() },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .testTag("emergency_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Favqulodda to'xtatish",
                            tint = ErrorRed
                        )
                    }

                    // Fonda ishlash (Foreground Service) kaliti
                    IconButton(
                        onClick = { viewModel.toggleForegroundService() },
                        modifier = Modifier.testTag("toggle_service_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isForegroundServiceRunning) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Fonda ishlash xizmati",
                            tint = if (uiState.isForegroundServiceRunning) SkyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Kalitlar menejeri
                    IconButton(
                        onClick = { showKeysSheet = true },
                        modifier = Modifier.testTag("open_keys_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Kalitlar menejeri",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Jurnal (Audit logs)
                    IconButton(
                        onClick = { showLogsSheet = true },
                        modifier = Modifier.testTag("open_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Amallar jurnali",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Tarixni tozalash
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Chatni tozalash",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            VoiceInputBar(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSend = viewModel::sendMessage,
                speechState = uiState.speechInputState,
                onStartVoice = viewModel::startVoiceInput,
                onStopVoice = viewModel::stopVoiceInput,
                isAutoTts = uiState.isAutoTtsEnabled,
                onToggleAutoTts = viewModel::toggleAutoTts
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Agent State Banner (Thinking, Tool call, Key rotation, Speaking)
            AnimatedVisibility(
                visible = uiState.agentState !is AgentState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AgentStateBanner(
                    state = uiState.agentState,
                    onStop = viewModel::emergencyStop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ovozli Qo'ng'iroq Boshqaruv Paneli
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoSecondary.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Aiko Qo'ng'irog'i orqali boshqarish",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Aiko bilan gaplashib ilovalarni boshqaring",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        FilledTonalButton(
                            onClick = { container.callManager.startDirectCall() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Qo'ng'iroq", fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledTonalButton(
                            onClick = {
                                container.callManager.triggerIncomingCall(
                                    summaryText = "Siz so'ragan ilovalar va vazifalarni boshqarishga tayyorman!",
                                    urgencyLevel = 5,
                                    attempt = 1
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Menga qil", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Agar API kalit kiritilmagan bo'lsa, aniq va qulay taklif kartasi
            if (uiState.activeKey == null) {
                Surface(
                    color = AmberAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { showKeysSheet = true }
                        .testTag("missing_api_key_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Gemini API kaliti kiritilmagan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                                Text(
                                    text = "Aiko bilan to'liq suhbatlashish uchun kalitingizni kiriting",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showKeysSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Kiritish", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Chat xabarlari ro'yxati yoki bo'sh boshlang'ich ekran
            if (uiState.messages.isEmpty()) {
                EmptyChatSuggestions(
                    onSelect = { prompt ->
                        when {
                            prompt.contains("jonli ovozli suhbat", ignoreCase = true) -> {
                                container.callManager.startDirectCall()
                            }
                            prompt.contains("qo'ng'iroq qil", ignoreCase = true) -> {
                                container.callManager.triggerIncomingCall(
                                    summaryText = "Siz so'ragan ilovalarni boshqarishga tayyorman! Buyrug'ingizni ayting.",
                                    urgencyLevel = 5,
                                    attempt = 1
                                )
                            }
                            prompt.contains("Kiruvchi shoshilinch", ignoreCase = true) -> {
                                container.callManager.triggerIncomingCall(
                                    summaryText = "Shoshilinch xabar: Akmal 3 marta qo'ng'iroq qildi va muhim xabar qoldirdi",
                                    urgencyLevel = 5,
                                    attempt = 1
                                )
                            }
                            else -> {
                                viewModel.onInputTextChanged(prompt)
                                viewModel.sendMessage()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("messages_list")
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        if (message.role == ChatMessageEntity.ROLE_TOOL_CALL ||
                            message.role == ChatMessageEntity.ROLE_TOOL_RESULT
                        ) {
                            ToolExecutionCard(message = message)
                        } else {
                            MessageBubble(
                                message = message,
                                onSpeak = viewModel::speakText
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKeysSheet) {
        ApiKeysSheet(
            viewModel = keysViewModel,
            sheetState = keysSheetState,
            onDismiss = { showKeysSheet = false }
        )
    }

    if (showLogsSheet) {
        ActionLogsDialog(
            container = container,
            sheetState = logsSheetState,
            onDismiss = { showLogsSheet = false }
        )
    }
}

@Composable
fun AgentStateBanner(
    state: AgentState,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (state) {
        is AgentState.Thinking -> Triple(
            SkyBluePrimary.copy(alpha = 0.12f),
            SkyBluePrimary,
            state.statusText
        )
        is AgentState.ExecutingTool -> Triple(
            AmberAccent.copy(alpha = 0.14f),
            AmberAccent,
            "Vosita ishga tushmoqda: ${state.toolName}..."
        )
        is AgentState.RotatingKey -> Triple(
            IndigoLight.copy(alpha = 0.15f),
            IndigoLight,
            "429 Quota chegarasi: ${state.newLabel} kalitiga o'tilmoqda..."
        )
        is AgentState.Speaking -> Triple(
            SuccessGreen.copy(alpha = 0.14f),
            SuccessGreen,
            "Kotib ovoz bilan o'qimoqda..."
        )
        is AgentState.Error -> Triple(
            ErrorRed.copy(alpha = 0.15f),
            ErrorRed,
            state.message
        )
        else -> Triple(Color.Transparent, Color.Transparent, "")
    }

    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (state !is AgentState.Error) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = textColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "To'xtatish",
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyChatSuggestions(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "📞 Aiko bilan jonli ovozli suhbat (Telefon orqali boshqarish)",
        "🔔 Menga hozir qo'ng'iroq qil",
        "💬 Telegram'da Ali'ga 'Salom, hisobot tayyormi?' deb xabar yoz",
        "⏰ Soat 18:00 da Telegram'da Jamshidga 'Reja o'zgardi' deb yozishni saqla",
        "▶️ YouTube'ni och va 'O'zbekiston yangiliklari' deb qidir",
        "🌙 Soat 23:00 da telefonni jimjit rejimga o'tkazishni rejalashtir"
    )

    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(SkyBluePrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = SkyBluePrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Aiko — Shaxsiy AI Agent",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Sizning aqlli, chaqqon va mehrli yordamchingiz. Savol bering yoki ovoz orqali buyruq bering.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(prompt) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
