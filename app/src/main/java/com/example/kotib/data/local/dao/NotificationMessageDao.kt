package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kotib.data.local.entity.NotificationMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationMessageDao {
    @Query("SELECT * FROM incoming_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<NotificationMessageEntity>>

    @Query("SELECT * FROM incoming_messages WHERE urgencyLevel >= :minUrgency ORDER BY timestamp DESC")
    fun getMessagesByUrgencyFlow(minUrgency: Int): Flow<List<NotificationMessageEntity>>

    @Query("SELECT * FROM incoming_messages WHERE timestamp >= :startOfDay ORDER BY timestamp ASC")
    suspend fun getTodayMessages(startOfDay: Long): List<NotificationMessageEntity>

    @Query("SELECT * FROM incoming_messages WHERE isProcessed = 0 ORDER BY timestamp ASC LIMIT 10")
    suspend fun getUnprocessedMessages(): List<NotificationMessageEntity>

    @Query("SELECT * FROM incoming_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): NotificationMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: NotificationMessageEntity): Long

    @Update
    suspend fun updateMessage(message: NotificationMessageEntity)

    @Query("DELETE FROM incoming_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM incoming_messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM incoming_messages WHERE timestamp >= :startOfDay")
    fun getTodayCountFlow(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM incoming_messages WHERE timestamp >= :startOfDay AND (urgencyLevel >= 4 OR isImportant = 1)")
    fun getTodayImportantCountFlow(startOfDay: Long): Flow<Int>
}
