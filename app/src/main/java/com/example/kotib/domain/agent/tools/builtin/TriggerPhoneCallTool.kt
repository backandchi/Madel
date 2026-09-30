package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.domain.call.KotibCallManager

class TriggerPhoneCallTool(
    private val callManager: KotibCallManager
) : AgentTool {
    override val name: String = "call_user"
    override val description: String =
        "Foydalanuvchiga ovozli qo'ng'iroq (telefon) qilish. Kotib foydalanuvchiga to'liq jiringlash va vibratsiya bilan qo'ng'iroq qiladi, foydalanuvchi ko'targach, ovoz bilan suhbatlashadi va buyruqlarni qabul qiladi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "reason" to mapOf(
                "type" to "STRING",
                "description" to "Qo'ng'iroq sababi yoki boshlang'ich hisobot matni"
            ),
            "urgency" to mapOf(
                "type" to "INTEGER",
                "description" to "Muhimlik darajasi (1 dan 5 gacha, standart: 5)"
            )
        ),
        "required" to emptyList<String>()
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val reason = args["reason"]?.toString() ?: "Siz so'ragan vazifalar va ilovalarni ovoz bilan boshqarish uchun qo'ng'iroq qilmoqdaman."
        val urgency = (args["urgency"] as? Number)?.toInt() ?: 5

        callManager.triggerIncomingCall(
            summaryText = reason,
            urgencyLevel = urgency,
            attempt = 1
        )

        return ToolExecutionResult(
            isSuccess = true,
            output = mapOf("status" to "ringing", "reason" to reason),
            userSummary = "Sizga telefon qilinmoqda! Qo'ng'iroqni ko'taring va ovoz orqali buyruqlaringizni ayting."
        )
    }
}
