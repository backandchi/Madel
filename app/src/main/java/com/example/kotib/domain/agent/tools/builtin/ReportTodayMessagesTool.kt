package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.data.local.dao.NotificationMessageDao
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportTodayMessagesTool(
    private val notificationMessageDao: NotificationMessageDao
) : AgentTool {
    override val name: String = "get_today_messages_report"
    override val description: String =
        "Bugun kelgan barcha xabarlar, SMS'lar va bildirishnomalar (Telegram, WhatsApp, SMS) bo'yicha to'liq hisobot beradi. 'Bugun kim nima yozdi?' so'rovlari uchun ishlatiladi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "filter" to mapOf(
                "type" to "STRING",
                "description" to "Filtr: 'all' (barchasi), 'important' (faqat muhim 4-5 daraja)"
            )
        ),
        "required" to emptyList<String>()
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val messages = notificationMessageDao.getTodayMessages(startOfDay)

        if (messages.isEmpty()) {
            return ToolExecutionResult(
                isSuccess = true,
                output = mapOf("totalMessages" to 0, "messages" to emptyList<String>()),
                userSummary = "Bugun hech qanday yangi xabar yoki bildirishnoma kelmagan."
            )
        }

        // Yuboruvchilar bo'yicha guruhlash
        val groupedBySender = messages.groupBy { it.sender }
        val importantCount = messages.count { it.urgencyLevel >= 4 || it.isImportant }

        val summaryBuilder = StringBuilder()
        summaryBuilder.append("Bugun jami ${messages.size} ta xabar keldi (shulardan $importantCount tasi muhim):\n")

        groupedBySender.forEach { (sender, list) ->
            val app = list.firstOrNull()?.appName ?: "Xabarlar"
            val lastMsg = list.last()
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMsg.timestamp))
            val stars = "★".repeat(lastMsg.urgencyLevel.coerceIn(1, 5))

            summaryBuilder.append("\n• $sender ($app, soat $time) [$stars]: ")
            if (lastMsg.summary.isNotBlank()) {
                summaryBuilder.append(lastMsg.summary)
            } else {
                summaryBuilder.append(lastMsg.cleanContent.take(50))
            }
            if (lastMsg.suggestedAction.isNotBlank()) {
                summaryBuilder.append(" → Tavsiya: ${lastMsg.suggestedAction}")
            }
        }

        return ToolExecutionResult(
            isSuccess = true,
            output = mapOf(
                "totalMessages" to messages.size,
                "importantCount" to importantCount,
                "sendersCount" to groupedBySender.size,
                "details" to summaryBuilder.toString()
            ),
            userSummary = summaryBuilder.toString()
        )
    }
}
