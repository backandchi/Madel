package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotib.ui.MainScreen
import com.example.kotib.ui.automation.AutomationViewModel
import com.example.kotib.ui.chat.ChatViewModel
import com.example.kotib.ui.integrations.IntegrationsViewModel
import com.example.kotib.ui.messages.MessagesViewModel
import com.example.ui.theme.KotibTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KotibApp
        val container = app.container

        setContent {
            KotibTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val chatViewModel: ChatViewModel = viewModel(
                        factory = ChatViewModel.Factory(container, this)
                    )
                    val messagesViewModel: MessagesViewModel = viewModel(
                        factory = MessagesViewModel.Factory(container)
                    )
                    val integrationsViewModel: IntegrationsViewModel = viewModel(
                        factory = IntegrationsViewModel.Factory(container)
                    )
                    val automationViewModel: AutomationViewModel = viewModel(
                        factory = AutomationViewModel.Factory(container)
                    )

                    MainScreen(
                        container = container,
                        chatViewModel = chatViewModel,
                        messagesViewModel = messagesViewModel,
                        integrationsViewModel = integrationsViewModel,
                        automationViewModel = automationViewModel
                    )
                }
            }
        }
    }
}
