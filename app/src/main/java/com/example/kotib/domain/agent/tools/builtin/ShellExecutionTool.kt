package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.domain.automation.RootShellManager

class ShellExecutionTool(
    private val shellManager: RootShellManager
) : AgentTool {
    override val name: String = "run_shell_command"
    override val description: String =
        "Qurilmada haqiqiy Shell (sh yoki root bo'lsa su) buyrug'ini bajaradi. Tizim sozlamalari, input koordinatalari, ilovalarni to'xtatish yoki keshni tozalash uchun ishlatiladi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "command" to mapOf(
                "type" to "STRING",
                "description" to "Bajarilishi kerak bo'lgan shell buyrug'i (masalan: 'input keyevent 3', 'am force-stop com.example', 'getprop ro.build.version.release')"
            ),
            "as_root" to mapOf(
                "type" to "BOOLEAN",
                "description" to "Buyruqni Root (su) huquqlari bilan bajarish kerakmi (standart: false)"
            )
        ),
        "required" to listOf("command")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val command = args["command"]?.toString() ?: ""
        val asRoot = (args["as_root"] as? Boolean) ?: false

        if (command.isBlank()) {
            return ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Buyruq kiritilmadi"),
                userSummary = "Shell buyrug'i ko'rsatilmadi."
            )
        }

        val result = shellManager.executeCommand(command, asRoot = asRoot)

        return ToolExecutionResult(
            isSuccess = result.isSuccess,
            output = mapOf(
                "exitCode" to result.exitCode,
                "stdout" to result.output,
                "stderr" to result.error
            ),
            userSummary = if (result.isSuccess) {
                if (result.output.isNotBlank()) "Natija:\n${result.output}" else "Buyruq muvaffaqiyatli bajarildi (exit code 0)."
            } else {
                "Buyruq xatolik bilan tugadi (${result.exitCode}): ${result.error.ifBlank { result.output }}"
            }
        )
    }
}
