package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.domain.automation.SystemAutomationController

class DeepSystemControlTool(
    private val controller: SystemAutomationController
) : AgentTool {
    override val name: String = "control_system"
    override val description: String =
        "Telefonning real tizim sozlamalarini boshqarish: ovoz rejimini o'zgartirish (jimjit, vibratsiya, normal), musiqa/qo'ng'iroq balandligini sozlash, ilovalarni ochish yoki tizim sozlamalariga kirish."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal: 'set_ringer' (ovoz rejimi), 'set_volume' (balandlik 0-100%), 'launch_app' (ilovani ochish), 'open_settings' (sozlamani ochish), 'get_status' (holatni olish)"
            ),
            "value" to mapOf(
                "type" to "STRING",
                "description" to "Amal uchun qiymat: masalan 'silent', 'vibrate', 'normal', balandlik uchun '50', ilova nomi uchun 'Telegram' yoki 'Kamera', sozlama uchun 'wifi', 'bluetooth', 'sound', 'battery'"
            ),
            "stream" to mapOf(
                "type" to "STRING",
                "description" to "Ovoz turi (ixtiyoriy): 'music', 'ring', 'alarm', 'notification'"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val action = args["action"]?.toString() ?: "get_status"
        val value = args["value"]?.toString() ?: ""
        val stream = args["stream"]?.toString() ?: "music"

        return when (action.lowercase()) {
            "set_ringer" -> {
                val ok = controller.setRingerMode(value)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("mode" to value, "success" to ok),
                    userSummary = if (ok) "Telefon ovoz rejimi '$value' holatiga o'tkazildi."
                    else "Ovoz rejimini o'zgartirib bo'lmadi."
                )
            }

            "set_volume" -> {
                val percent = value.toIntOrNull() ?: 50
                val ok = controller.setVolumePercent(stream, percent)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("stream" to stream, "percent" to percent),
                    userSummary = if (ok) "$stream ovoz balandligi $percent% ga o'rnatildi."
                    else "Ovoz balandligini o'zgartirib bo'lmadi."
                )
            }

            "launch_app" -> {
                val (ok, message) = controller.launchApp(value)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("app" to value, "status" to message),
                    userSummary = message
                )
            }

            "open_settings" -> {
                val ok = controller.openSystemSetting(value)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("setting" to value),
                    userSummary = if (ok) "$value sozlamalari ochildi." else "Sozlamalarni ochib bo'lmadi."
                )
            }

            else -> {
                val summary = controller.getVolumeSummary()
                ToolExecutionResult(
                    isSuccess = true,
                    output = summary,
                    userSummary = "Tizim holati: Rejim: ${summary["ringerMode"]}, Musiqa: ${summary["musicVolumePercent"]}%, Qo'ng'iroq: ${summary["ringVolumePercent"]}%"
                )
            }
        }
    }
}
