package com.example.kotib.domain.automation

import android.app.AlarmManager
import android.content.Context
import com.example.kotib.data.local.dao.AutomationTaskDao
import com.example.kotib.data.local.entity.AutomationTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KotibAutomationScheduler(
    private val context: Context,
    private val dao: AutomationTaskDao
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val allTasks: Flow<List<AutomationTaskEntity>> = dao.getAllTasksFlow()

    suspend fun initializeDefaultTasksIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getEnabledTasks()
        if (existing.isEmpty()) {
            val defaults = listOf(
                AutomationTaskEntity(
                    title = "Tungi Jimjitlik (Ovozni o'chirish)",
                    triggerTime = "23:00",
                    actionCommand = "Telefon ovozini jimjit rejimga o'tkaz va tungi dam olish rejimini yoq",
                    isEnabled = true,
                    repeatType = AutomationTaskEntity.REPEAT_DAILY
                ),
                AutomationTaskEntity(
                    title = "Tonggi Uyg'onish & Hisobot",
                    triggerTime = "07:30",
                    actionCommand = "Telefon ovozini normal rejimga qaytar va 'Bugun kim nima yozdi?' hisobotini tayyorla",
                    isEnabled = true,
                    repeatType = AutomationTaskEntity.REPEAT_DAILY
                ),
                AutomationTaskEntity(
                    title = "Ish vaqti: Ilovalarni tartibga solish",
                    triggerTime = "09:00",
                    actionCommand = "Ovoz balandligini 70% ga sozla va bugungi vazifalarni ko'rsat",
                    isEnabled = false,
                    repeatType = AutomationTaskEntity.REPEAT_DAILY
                )
            )

            defaults.forEach { dao.insertTask(it) }
        }
    }

    suspend fun toggleTask(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.setTaskEnabled(id, enabled)
    }

    suspend fun addTask(title: String, time: String, command: String) = withContext(Dispatchers.IO) {
        dao.insertTask(
            AutomationTaskEntity(
                title = title,
                triggerTime = time,
                actionCommand = command,
                isEnabled = true
            )
        )
    }

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTaskById(id)
    }

    suspend fun markTaskExecuted(id: Long, resultSummary: String) = withContext(Dispatchers.IO) {
        val task = dao.getEnabledTasks().firstOrNull { it.id == id } ?: return@withContext
        dao.updateTask(
            task.copy(
                lastExecutedTimestamp = System.currentTimeMillis(),
                lastExecutionResult = resultSummary
            )
        )
    }
}
