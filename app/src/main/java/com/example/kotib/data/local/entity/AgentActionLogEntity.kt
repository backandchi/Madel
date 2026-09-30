package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Agent xatti-harakatlari va xavfsizlik jurnali.
 * Kalit rotatsiyalari, vosita bajarilishi va favqulodda to'xtatishlar qayd etiladi.
 */
@Entity(tableName = "agent_action_logs")
data class AgentActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "KEY_ROTATION", "TOOL_EXECUTION", "VOICE_INPUT", "EMERGENCY_STOP", "SECURITY_MASK"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
) {
    companion object {
        const val TYPE_KEY_ROTATION = "KEY_ROTATION"
        const val TYPE_TOOL_EXECUTION = "TOOL_EXECUTION"
        const val TYPE_VOICE_INPUT = "VOICE_INPUT"
        const val TYPE_EMERGENCY_STOP = "EMERGENCY_STOP"
        const val TYPE_SECURITY_MASK = "SECURITY_MASK"
    }
}
