package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorTool : AgentTool {
    override val name: String = "calculate"
    override val description: String =
        "Matematik hisob-kitoblar, foizlar yoki valyuta (USD va UZS) hisobini bajaradi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "expression" to mapOf(
                "type" to "STRING",
                "description" to "Matematik ifoda (masalan: '150 * 12', '500000 * 0.12', '100 USD to UZS')"
            )
        ),
        "required" to listOf("expression")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val expr = args["expression"]?.toString()?.trim() ?: ""
        if (expr.isBlank()) {
            return ToolExecutionResult(false, mapOf("error" to "Ifoda kiritilmagan"), "Bo'sh ifoda")
        }

        // Valyuta konvertatsiyasi: taxminiy O'zbekiston Markaziy banki kursi (masalan, 12,850 so'm)
        val usdPattern = Regex("""(?i)(\d+(?:\.\d+)?)\s*(?:usd|dollar|\$)\s*(?:to|so['`]?m|uzs)""")
        val usdMatch = usdPattern.find(expr)
        if (usdMatch != null) {
            val amount = usdMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val rate = 12850.0 // O'zbekiston so'mi kursi
            val total = amount * rate
            val formatted = String.format(java.util.Locale.US, "%,.0f", total).replace(",", " ")
            return ToolExecutionResult(
                isSuccess = true,
                output = mapOf("amountUsd" to amount, "rate" to rate, "totalUzs" to total),
                userSummary = "$amount USD = $formatted so'm (kurs: 12 850 so'm)"
            )
        }

        // Oddiy matematik hisoblash (xavfsiz sodda parser: qo'shish, ayirish, ko'paytirish, bo'lish, foiz)
        return try {
            val sanitized = expr.replace(" ", "").replace("x", "*").replace("X", "*")
            val result = safeEval(sanitized)
            ToolExecutionResult(
                isSuccess = true,
                output = mapOf("expression" to expr, "result" to result),
                userSummary = "$expr = $result"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Hisoblashda xatolik")),
                userSummary = "Hisoblash xatosi: ${e.message}"
            )
        }
    }

    private fun safeEval(str: String): String {
        // Oddiy 2 operandli amallarni tahlil qilish
        val ops = listOf("+", "-", "*", "/", "%")
        var chosenOp: String? = null
        var opIndex = -1

        for (op in ops) {
            val idx = str.indexOf(op, startIndex = 1) // 1-indexdan boshlash, manfiy son bo'lsa
            if (idx != -1) {
                chosenOp = op
                opIndex = idx
                break
            }
        }

        if (chosenOp == null || opIndex <= 0) {
            return str.toDoubleOrNull()?.toString() ?: "Noaniq ifoda"
        }

        val left = str.substring(0, opIndex).toDouble()
        val right = str.substring(opIndex + 1).toDouble()

        val res = when (chosenOp) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> if (right != 0.0) left / right else throw ArithmeticException("Nolga bo'lish mumkin emas")
            "%" -> left * (right / 100.0)
            else -> 0.0
        }

        val bd = BigDecimal(res).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString()
    }
}
