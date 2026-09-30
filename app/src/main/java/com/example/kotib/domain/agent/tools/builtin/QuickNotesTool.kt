package com.example.kotib.domain.agent.tools.builtin

import android.content.Context
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickNotesTool(private val context: Context) : AgentTool {
    override val name: String = "manage_notes"
    override val description: String =
        "Kotib xotirasiga eslatma yoki muhim ma'lumotlarni saqlaydi, qidiradi yoki barcha eslatmalarni o'qiydi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal turi: 'add' (qo'shish), 'list' (ko'rish), 'clear' (tozalash)"
            ),
            "noteText" to mapOf(
                "type" to "STRING",
                "description" to "Eslatma matni ('add' amali uchun)"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    private val prefs by lazy {
        context.getSharedPreferences("kotib_quick_notes", Context.MODE_PRIVATE)
    }

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val action = args["action"]?.toString() ?: "list"
        return when (action.lowercase()) {
            "add" -> {
                val text = args["noteText"]?.toString() ?: ""
                if (text.isBlank()) {
                    return ToolExecutionResult(false, mapOf("error" to "Eslatma matni bo'sh bo'lishi mumkin emas"), "Xatolik: matn bo'sh")
                }
                val raw = prefs.getString("notes_json", "[]") ?: "[]"
                val array = JSONArray(raw)
                val newObj = JSONObject().apply {
                    put("id", System.currentTimeMillis())
                    put("text", text)
                    put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                }
                array.put(newObj)
                prefs.edit().putString("notes_json", array.toString()).apply()

                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("status" to "saved", "text" to text, "totalNotes" to array.length()),
                    userSummary = "Eslatma saqlandi: \"$text\""
                )
            }
            "list" -> {
                val raw = prefs.getString("notes_json", "[]") ?: "[]"
                val array = JSONArray(raw)
                val notes = mutableListOf<Map<String, String>>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    notes.add(mapOf(
                        "text" to obj.optString("text"),
                        "date" to obj.optString("date")
                    ))
                }
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("notes" to notes, "count" to notes.size),
                    userSummary = if (notes.isEmpty()) "Hozircha hech qanday eslatma yo'q" else "${notes.size} ta eslatma topildi"
                )
            }
            "clear" -> {
                prefs.edit().remove("notes_json").apply()
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("status" to "cleared"),
                    userSummary = "Barcha eslatmalar o'chirildi"
                )
            }
            else -> {
                ToolExecutionResult(false, mapOf("error" to "Noma'lum amal"), "Noma'lum amal: $action")
            }
        }
    }
}
