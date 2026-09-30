package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kotib.data.local.entity.AgentActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentActionLogDao {
    @Query("SELECT * FROM agent_action_logs ORDER BY timestamp DESC, id DESC LIMIT 200")
    fun getAllLogsFlow(): Flow<List<AgentActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AgentActionLogEntity): Long

    @Query("DELETE FROM agent_action_logs")
    suspend fun clearLogs()
}
