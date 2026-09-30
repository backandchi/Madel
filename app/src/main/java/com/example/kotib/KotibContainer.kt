package com.example.kotib

import android.content.Context
import com.example.kotib.data.local.KotibDatabase
import com.example.kotib.data.repository.ApiKeyRepository
import com.example.kotib.data.repository.ChatRepository
import com.example.kotib.data.repository.IntegrationRepository
import com.example.kotib.domain.agent.AgentEngine
import com.example.kotib.domain.agent.ToolDispatcher
import com.example.kotib.domain.agent.tools.builtin.AccessibilityAutomationTool
import com.example.kotib.domain.agent.tools.builtin.CalculatorTool
import com.example.kotib.domain.agent.tools.builtin.DeepSystemControlTool
import com.example.kotib.domain.agent.tools.builtin.DeviceStatusTool
import com.example.kotib.domain.agent.tools.builtin.GitHubIntegrationTool
import com.example.kotib.domain.agent.tools.builtin.KeyStatusTool
import com.example.kotib.domain.agent.tools.builtin.NotionIntegrationTool
import com.example.kotib.domain.agent.tools.builtin.ObsidianIntegrationTool
import com.example.kotib.domain.agent.tools.builtin.QuickNotesTool
import com.example.kotib.domain.agent.tools.builtin.ReportTodayMessagesTool
import com.example.kotib.domain.agent.tools.builtin.ScheduleTaskTool
import com.example.kotib.domain.agent.tools.builtin.ShellExecutionTool
import com.example.kotib.domain.agent.tools.builtin.TelegramIntegrationTool
import com.example.kotib.domain.agent.tools.builtin.TodoistIntegrationTool
import com.example.kotib.domain.agent.tools.builtin.TriggerPhoneCallTool
import com.example.kotib.domain.agent.tools.builtin.UniversalAppControllerTool
import com.example.kotib.domain.automation.KotibAutomationScheduler
import com.example.kotib.domain.automation.RootShellManager
import com.example.kotib.domain.automation.SystemAutomationController
import com.example.kotib.domain.call.KotibCallManager
import com.example.kotib.domain.messages.MessageAnalysisEngine
import com.example.kotib.domain.messages.QuietHoursManager
import com.example.kotib.domain.voice.SpeechRecognizerManager
import com.example.kotib.domain.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KotibContainer(private val context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: KotibDatabase by lazy {
        KotibDatabase.getInstance(context)
    }

    val apiKeyRepository: ApiKeyRepository by lazy {
        ApiKeyRepository(
            apiKeyDao = database.apiKeyDao(),
            actionLogDao = database.agentActionLogDao()
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            chatMessageDao = database.chatMessageDao()
        )
    }

    val integrationRepository: IntegrationRepository by lazy {
        IntegrationRepository(
            dao = database.integrationConfigDao()
        ).also { repo ->
            applicationScope.launch {
                repo.initializeDefaultsIfNeeded()
            }
        }
    }

    val rootShellManager: RootShellManager by lazy {
        RootShellManager(context)
    }

    val systemAutomationController: SystemAutomationController by lazy {
        SystemAutomationController(context)
    }

    val automationScheduler: KotibAutomationScheduler by lazy {
        KotibAutomationScheduler(
            context = context,
            dao = database.automationTaskDao()
        ).also { scheduler ->
            applicationScope.launch {
                scheduler.initializeDefaultTasksIfEmpty()
            }
        }
    }

    val quietHoursManager: QuietHoursManager by lazy {
        QuietHoursManager(context)
    }

    val callManager: KotibCallManager by lazy {
        KotibCallManager(context)
    }

    val messageAnalysisEngine: MessageAnalysisEngine by lazy {
        MessageAnalysisEngine(
            notificationMessageDao = database.notificationMessageDao(),
            vipContactDao = database.vipContactDao(),
            apiKeyRepository = apiKeyRepository,
            callManager = callManager,
            quietHoursManager = quietHoursManager,
            actionLogDao = database.agentActionLogDao(),
            scope = applicationScope
        )
    }

    val tools by lazy {
        listOf(
            DeviceStatusTool(context),
            QuickNotesTool(context),
            CalculatorTool(),
            KeyStatusTool(apiKeyRepository),
            ReportTodayMessagesTool(database.notificationMessageDao()),
            DeepSystemControlTool(systemAutomationController),
            UniversalAppControllerTool(context, systemAutomationController, rootShellManager),
            ScheduleTaskTool(automationScheduler),
            TriggerPhoneCallTool(callManager),
            AccessibilityAutomationTool(),
            ShellExecutionTool(rootShellManager),
            TelegramIntegrationTool(integrationRepository),
            GitHubIntegrationTool(integrationRepository),
            TodoistIntegrationTool(integrationRepository),
            NotionIntegrationTool(integrationRepository),
            ObsidianIntegrationTool(context, integrationRepository)
        )
    }

    val toolDispatcher: ToolDispatcher by lazy {
        ToolDispatcher(tools)
    }

    val agentEngine: AgentEngine by lazy {
        AgentEngine(
            apiKeyRepository = apiKeyRepository,
            chatRepository = chatRepository,
            toolDispatcher = toolDispatcher,
            actionLogDao = database.agentActionLogDao(),
            scope = applicationScope
        )
    }

    val speechRecognizerManager: SpeechRecognizerManager by lazy {
        SpeechRecognizerManager(context)
    }

    val textToSpeechManager: TextToSpeechManager by lazy {
        TextToSpeechManager(context)
    }
}
