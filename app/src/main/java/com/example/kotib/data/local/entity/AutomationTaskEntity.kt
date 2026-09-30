package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Avtomatik skriptlar va rejalashtirilgan vazifalar jadvali.
 * Belgilangan vaqtda (masalan, 23:00 da yoki 07:00 da) Kotib AI orqali
 * fon rejimida buyruqni avtomatik ijro etadi.
 */
@Entity(tableName = "automation_tasks")
data class AutomationTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerTime: String, // "HH:mm" masalan "23:00", "07:30"
    val actionCommand: String, // Masalan: "Telefonni jimjit rejimga o'tkaz va bugungi xulosani tayyorla"
    val isEnabled: Boolean = true,
    val repeatType: String = REPEAT_DAILY, // "DAILY", "ONCE"
    val lastExecutedTimestamp: Long = 0,
    val lastExecutionResult: String = ""
) {
    companion object {
        const val REPEAT_DAILY = "DAILY"
        const val REPEAT_ONCE = "ONCE"
    }
}
