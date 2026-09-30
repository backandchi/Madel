package com.example.kotib.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotib.KotibContainer
import com.example.kotib.ui.automation.AutomationScreen
import com.example.kotib.ui.automation.AutomationViewModel
import com.example.kotib.ui.chat.ChatScreen
import com.example.kotib.ui.chat.ChatViewModel
import com.example.kotib.ui.integrations.IntegrationsScreen
import com.example.kotib.ui.integrations.IntegrationsViewModel
import com.example.kotib.ui.messages.MessagesScreen
import com.example.kotib.ui.messages.MessagesViewModel
import com.example.kotib.ui.settings.SettingsScreen
import com.example.ui.theme.SkyBluePrimary

enum class KotibTab {
    CHAT,
    MESSAGES,
    AUTOMATION,
    INTEGRATIONS,
    SETTINGS
}

@Composable
fun MainScreen(
    container: KotibContainer,
    chatViewModel: ChatViewModel,
    messagesViewModel: MessagesViewModel,
    integrationsViewModel: IntegrationsViewModel,
    automationViewModel: AutomationViewModel
) {
    var currentTab by rememberSaveable { mutableStateOf(KotibTab.CHAT) }
    val messagesState by messagesViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // 1. CHAT TAB
                NavigationBarItem(
                    selected = currentTab == KotibTab.CHAT,
                    onClick = { currentTab = KotibTab.CHAT },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == KotibTab.CHAT) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        indicatorColor = SkyBluePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                // 2. MESSAGES TAB (with badge)
                NavigationBarItem(
                    selected = currentTab == KotibTab.MESSAGES,
                    onClick = { currentTab = KotibTab.MESSAGES },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (messagesState.todayCount > 0) {
                                    Badge {
                                        Text("${messagesState.todayCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == KotibTab.MESSAGES) Icons.Filled.Inbox else Icons.Outlined.Inbox,
                                contentDescription = "Xabarlar"
                            )
                        }
                    },
                    label = { Text("Xabarlar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        indicatorColor = SkyBluePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_messages")
                )

                // 3. AUTOMATION & ROOT TAB
                NavigationBarItem(
                    selected = currentTab == KotibTab.AUTOMATION,
                    onClick = { currentTab = KotibTab.AUTOMATION },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == KotibTab.AUTOMATION) Icons.Filled.Tune else Icons.Outlined.Tune,
                            contentDescription = "Avtomat"
                        )
                    },
                    label = { Text("Avtomat", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        indicatorColor = SkyBluePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_automation")
                )

                // 4. INTEGRATIONS TAB
                NavigationBarItem(
                    selected = currentTab == KotibTab.INTEGRATIONS,
                    onClick = { currentTab = KotibTab.INTEGRATIONS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == KotibTab.INTEGRATIONS) Icons.Filled.Hub else Icons.Outlined.Hub,
                            contentDescription = "Integratsiya"
                        )
                    },
                    label = { Text("Integratsiya", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        indicatorColor = SkyBluePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_integrations")
                )

                // 5. SETTINGS TAB
                NavigationBarItem(
                    selected = currentTab == KotibTab.SETTINGS,
                    onClick = { currentTab = KotibTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == KotibTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Sozlamalar"
                        )
                    },
                    label = { Text("Sozlamalar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        indicatorColor = SkyBluePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tabCrossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                KotibTab.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        container = container
                    )
                }
                KotibTab.MESSAGES -> {
                    MessagesScreen(
                        viewModel = messagesViewModel,
                        onOpenChatWithQuery = { query ->
                            chatViewModel.onInputTextChanged(query)
                            chatViewModel.sendMessage()
                            currentTab = KotibTab.CHAT
                        }
                    )
                }
                KotibTab.AUTOMATION -> {
                    AutomationScreen(
                        viewModel = automationViewModel,
                        onNavigateToChatWithCommand = { cmd ->
                            chatViewModel.onInputTextChanged(cmd)
                            chatViewModel.sendMessage()
                            currentTab = KotibTab.CHAT
                        }
                    )
                }
                KotibTab.INTEGRATIONS -> {
                    IntegrationsScreen(
                        viewModel = integrationsViewModel,
                        onExecutePromptInChat = { prompt ->
                            chatViewModel.onInputTextChanged(prompt)
                            chatViewModel.sendMessage()
                            currentTab = KotibTab.CHAT
                        }
                    )
                }
                KotibTab.SETTINGS -> {
                    SettingsScreen(
                        container = container
                    )
                }
            }
        }
    }
}
