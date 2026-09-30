package com.example.kotib.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.KotibContainer
import com.example.kotib.data.local.entity.NotificationMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MessagesUiState(
    val messages: List<NotificationMessageEntity> = emptyList(),
    val filteredMessages: List<NotificationMessageEntity> = emptyList(),
    val selectedFilter: String = "ALL", // "ALL", "TELEGRAM", "WHATSAPP", "SMS", "IMPORTANT"
    val todayCount: Int = 0,
    val todayImportantCount: Int = 0,
    val isGeneratingReport: Boolean = false,
    val todayReportText: String? = null
)

class MessagesViewModel(
    private val container: KotibContainer
) : ViewModel() {

    private val notificationDao = container.database.notificationMessageDao()
    private val messageEngine = container.messageAnalysisEngine

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _todayReport = MutableStateFlow<String?>(null)
    val todayReport: StateFlow<String?> = _todayReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val startOfDay: Long by lazy {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val filterAndReportFlow = combine(_selectedFilter, _isGeneratingReport, _todayReport) { filter, isGenerating, reportText ->
        Triple(filter, isGenerating, reportText)
    }

    val uiState: StateFlow<MessagesUiState> = combine(
        notificationDao.getAllMessagesFlow(),
        notificationDao.getTodayCountFlow(startOfDay),
        notificationDao.getTodayImportantCountFlow(startOfDay),
        filterAndReportFlow
    ) { messages, todayCount, importantCount, (filter, isGenerating, reportText) ->
        val filtered = when (filter) {
            "TELEGRAM" -> messages.filter { it.appName.contains("telegram", ignoreCase = true) }
            "WHATSAPP" -> messages.filter { it.appName.contains("whatsapp", ignoreCase = true) }
            "SMS" -> messages.filter { it.messageType == NotificationMessageEntity.TYPE_SMS || it.appName.contains("sms", ignoreCase = true) }
            "IMPORTANT" -> messages.filter { it.urgencyLevel >= 4 || it.isImportant }
            else -> messages
        }

        MessagesUiState(
            messages = messages,
            filteredMessages = filtered,
            selectedFilter = filter,
            todayCount = todayCount,
            todayImportantCount = importantCount,
            isGeneratingReport = isGenerating,
            todayReportText = reportText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessagesUiState()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun generateTodayReport() {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val reportTool = container.tools.filterIsInstance<com.example.kotib.domain.agent.tools.builtin.ReportTodayMessagesTool>().firstOrNull()
            val result = reportTool?.execute(emptyMap())
            _isGeneratingReport.value = false
            _todayReport.value = result?.userSummary ?: "Bugun yangi xabarlar kelmagan."
        }
    }

    fun clearReport() {
        _todayReport.value = null
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            notificationDao.deleteMessageById(id)
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            notificationDao.clearAllMessages()
            _todayReport.value = null
        }
    }

    /**
     * Sinov uchun (Test Simulyatsiya): Yangi Telegram / SMS xabarni simulyatsiya qilish
     */
    fun simulateMessage(appName: String, sender: String, text: String, isImportant: Boolean = false) {
        val packageName = when (appName) {
            "Telegram" -> "org.telegram.messenger"
            "WhatsApp" -> "com.whatsapp"
            else -> "com.android.mms"
        }
        val type = if (appName == "SMS") NotificationMessageEntity.TYPE_SMS else NotificationMessageEntity.TYPE_NOTIFICATION

        messageEngine.onNewMessageReceived(
            packageName = packageName,
            appName = appName,
            sender = sender,
            title = sender,
            content = text,
            messageType = type
        )
    }

    class Factory(private val container: KotibContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagesViewModel(container) as T
        }
    }
}
