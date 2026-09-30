package com.example.kotib.domain.agent.tools.builtin

import android.accessibilityservice.AccessibilityService
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.service.KotibAccessibilityService

class AccessibilityAutomationTool : AgentTool {
    override val name: String = "automate_ui"
    override val description: String =
        "Accessibility Service orqali ekrandagi tugmalarni bosish, ekranda nima ko'rinib turganini o'qish, bosh ekranga yoki orqaga qaytish, bildirishnomalar panelini tushirish."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal turi: 'read_screen' (ekrandagi matn va tugmalarni o'qish), 'click_text' (matnli tugmani bosish), 'go_home' (bosh ekranga o'tish), 'go_back' (orqaga qaytish), 'open_notifications' (bildirishnomalar panelini ochish), 'lock_screen' (ekranni qulflash), 'scroll_down' (pastga aylantirish)"
            ),
            "target_text" to mapOf(
                "type" to "STRING",
                "description" to "click_text amali uchun bosilishi kerak bo'lgan tugma yoki element matni"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        if (!KotibAccessibilityService.isRunning()) {
            return ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Accessibility Service faol emas"),
                userSummary = "Kotib UI Avtomatlashtirish xizmati tizimda yoqilmagan. Iltimos, Sozlamalar -> Maxsus imkoniyatlar (Accessibility) bo'limidan 'Kotib UI Avtomatlashtirish' xizmatini yoqing."
            )
        }

        val action = args["action"]?.toString() ?: "read_screen"
        val targetText = args["target_text"]?.toString() ?: ""

        return when (action.lowercase()) {
            "read_screen" -> {
                val screenDump = KotibAccessibilityService.dumpVisibleScreenTexts()
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("screenContent" to screenDump),
                    userSummary = "Hozirgi ekrandagi ma'lumotlar:\n$screenDump"
                )
            }

            "click_text" -> {
                val clicked = KotibAccessibilityService.clickNodeByText(targetText)
                ToolExecutionResult(
                    isSuccess = clicked,
                    output = mapOf("target" to targetText, "success" to clicked),
                    userSummary = if (clicked) "Ekranda '$targetText' elementi bosildi."
                    else "Ekranda '$targetText' elementi topilmadi yoki uni bosib bo'lmadi."
                )
            }

            "go_home" -> {
                val ok = KotibAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("action" to "home"),
                    userSummary = "Bosh ekranga o'tildi."
                )
            }

            "go_back" -> {
                val ok = KotibAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("action" to "back"),
                    userSummary = "Orqaga qaytildi."
                )
            }

            "open_notifications" -> {
                val ok = KotibAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("action" to "notifications"),
                    userSummary = "Bildirishnomalar paneli ochildi."
                )
            }

            "lock_screen" -> {
                val ok = KotibAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("action" to "lock"),
                    userSummary = "Ekran qulflandi."
                )
            }

            "scroll_down" -> {
                val ok = KotibAccessibilityService.scroll(forward = true)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("action" to "scroll_down"),
                    userSummary = "Ekran pastga surildi."
                )
            }

            else -> {
                ToolExecutionResult(
                    isSuccess = false,
                    output = mapOf("error" to "Noma'lum amal"),
                    userSummary = "Noma'lum UI amali: $action"
                )
            }
        }
    }
}
