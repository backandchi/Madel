package com.example.kotib.domain.agent.tools.builtin

import android.content.Context
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import com.example.kotib.data.repository.IntegrationRepository
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ObsidianIntegrationTool(
    private val context: Context,
    private val integrationRepository: IntegrationRepository
) : AgentTool {
    override val name: String = "manage_obsidian"
    override val description: String =
        "Obsidian lokal Markdown (.md) vault bilan ishlash: yangi qayd yaratish, fayllarni o'qish, qidirish va g'oyalarni saqlash."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal: 'create_note' (yangi .md qayd), 'read_note' (o'qish), 'list_notes' (ro'yxat), 'search_notes' (qidirish)"
            ),
            "filename" to mapOf(
                "type" to "STRING",
                "description" to "Fayl nomi (masalan: 'AI_Reja.md' yoki 'Loyiha/Hisobot.md')"
            ),
            "content" to mapOf(
                "type" to "STRING",
                "description" to "Qayd matni (Markdown formatida)"
            ),
            "tags" to mapOf(
                "type" to "STRING",
                "description" to "Obsidian teglari (vergul bilan, masalan: 'kotib, g'oyalar, loyiha')"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val config = integrationRepository.getConfig(IntegrationConfigEntity.SERVICE_OBSIDIAN)
        val vaultFolderName = config?.extraParam1?.ifBlank { "KotibVault" } ?: "KotibVault"

        // Xavfsiz lokal Obsidian papkasi
        val vaultDir = File(context.getExternalFilesDir(null) ?: context.filesDir, vaultFolderName)
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }

        val action = args["action"]?.toString() ?: "list_notes"

        try {
            when (action) {
                "create_note" -> {
                    val rawFilename = args["filename"]?.toString() ?: "Qayd_${System.currentTimeMillis()}.md"
                    val filename = if (rawFilename.endsWith(".md")) rawFilename else "$rawFilename.md"
                    val content = args["content"]?.toString() ?: ""
                    val tags = args["tags"]?.toString() ?: "kotib, ai"

                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                    val file = File(vaultDir, filename)
                    file.parentFile?.mkdirs()

                    val frontmatter = """
---
title: "${filename.removeSuffix(".md")}"
created: $dateStr
tags: [$tags]
source: Kotib AI Agent
---

# ${filename.removeSuffix(".md")}

$content
""".trimIndent()

                    file.writeText(frontmatter)

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("path" to file.absolutePath, "filename" to filename),
                        userSummary = "Obsidian vault'ga '$filename' qaydi muvaffaqiyatli saqlandi."
                    )
                }

                "read_note" -> {
                    val rawFilename = args["filename"]?.toString() ?: ""
                    val filename = if (rawFilename.endsWith(".md")) rawFilename else "$rawFilename.md"
                    val file = File(vaultDir, filename)

                    if (file.exists()) {
                        val text = file.readText()
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("filename" to filename, "content" to text),
                            userSummary = "'$filename' fayli mazmuni:\n\n$text"
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to "Fayl topilmadi"),
                            userSummary = "'$filename' nomli Obsidian qaydi topilmadi."
                        )
                    }
                }

                "search_notes" -> {
                    val query = args["content"]?.toString()?.lowercase() ?: ""
                    val matching = vaultDir.walkTopDown()
                        .filter { it.isFile && it.extension == "md" }
                        .filter { it.name.lowercase().contains(query) || it.readText().lowercase().contains(query) }
                        .map { it.name }
                        .toList()

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("query" to query, "results" to matching),
                        userSummary = if (matching.isEmpty()) "'$query' so'rovi bo'yicha qaydlar topilmadi."
                        else "Topilgan Obsidian qaydlari:\n" + matching.joinToString("\n• ", prefix = "• ")
                    )
                }

                else -> {
                    // list_notes
                    val files = vaultDir.walkTopDown()
                        .filter { it.isFile && it.extension == "md" }
                        .map { it.name }
                        .toList()

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("count" to files.size, "files" to files),
                        userSummary = if (files.isEmpty()) "Obsidian vault hozircha bo'sh."
                        else "Obsidian qaydlari (${files.size} ta):\n" + files.joinToString("\n• ", prefix = "• ")
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "Obsidian fayl tizimi bilan ishlashda xatolik: ${e.message}"
            )
        }
    }
}
