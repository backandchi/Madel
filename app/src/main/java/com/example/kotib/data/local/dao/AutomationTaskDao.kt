package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kotib.data.local.entity.AutomationTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationTaskDao {
    @Query("SELECT * FROM automation_tasks ORDER BY triggerTime ASC")
    fun getAllTasksFlow(): Flow<List<AutomationTaskEntity>>

    @Query("SELECT * FROM automation_tasks WHERE isEnabled = 1")
    suspend fun getEnabledTasks(): List<AutomationTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AutomationTaskEntity): Long

    @Update
    suspend fun updateTask(task: AutomationTaskEntity)

    @Query("DELETE FROM automation_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE automation_tasks SET isEnabled = :enabled WHERE id = :id")
    suspend fun setTaskEnabled(id: Long, enabled: Boolean)
}
