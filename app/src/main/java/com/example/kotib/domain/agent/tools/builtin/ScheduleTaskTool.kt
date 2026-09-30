package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.domain.automation.KotibAutomationScheduler
import kotlinx.coroutines.flow.first

class ScheduleTaskTool(
    private val scheduler: KotibAutomationScheduler
) : AgentTool {
    override val name: String = "schedule_timed_action"
    override val description: String =
        "Vaqt bo'yicha vazifalarni rejalashtirish va saqlash: masalan 'Soat 18:00 da Telegram'da Ali'ga xabar yoz', 'Soat 22:30 da ovozni o'chir'."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal: 'schedule' (yangi vazifa saqlash), 'list' (mavjud rejalarni ko'rish)"
            ),
            "time" to mapOf(
                "type" to "STRING",
                "description" to "Vaqt (HH:mm formatida, masalan: '18:00', '22:30', '07:15')"
            ),
            "command" to mapOf(
                "type" to "STRING",
                "description" to "Shu vaqtda Kotib bajarishi kerak bo'lgan buyruq (masalan: 'Telegram'da Ali'ga xabar yubor', 'Ovozni jimjit qil')"
            ),
            "title" to mapOf(
                "type" to "STRING",
                "description" to "Reja uchun qisqa sarlavha"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val action = args["action"]?.toString() ?: "schedule"

        return when (action.lowercase()) {
            "list" -> {
                val list = scheduler.allTasks.first()
                val summary = if (list.isEmpty()) {
                    "Hozircha rejalashtirilgan vazifalar yo'q."
                } else {
                    "Rejalashtirilgan vazifalar:\n" + list.joinToString("\n") {
                        "• ${it.triggerTime} — ${it.title}: \"${it.actionCommand}\" (${if (it.isEnabled) "Faol" else "O'chiq"})"
                    }
                }
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("count" to list.size),
                    userSummary = summary
                )
            }

            else -> {
                val time = args["time"]?.toString() ?: "18:00"
                val command = args["command"]?.toString() ?: ""
                val title = args["title"]?.toString()?.ifBlank { null }
                    ?: "Rejalashtirilgan: $time"

                if (command.isBlank()) {
                    return ToolExecutionResult(
                        isSuccess = false,
                        output = mapOf("error" to "Buyruq kiritilmadi"),
                        userSummary = "Iltimos, belgilangan vaqtda bajarilishi kerak bo'lgan buyruqni ayting."
                    )
                }

                scheduler.addTask(title = title, time = time, command = command)

                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("title" to title, "time" to time, "command" to command),
                    userSummary = "Reja saqlandi! Soat $time da Kotib ushbu vazifani avtomatik bajaradi:\n\"$command\""
                )
            }
        }
    }
}
