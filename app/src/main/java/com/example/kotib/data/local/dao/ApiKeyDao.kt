package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kotib.data.local.entity.ApiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY isDefault DESC, id ASC")
    fun getAllKeysFlow(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys ORDER BY isDefault DESC, id ASC")
    suspend fun getAllKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE status = 'ACTIVE' ORDER BY isDefault DESC, id ASC")
    suspend fun getActiveKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getKeyById(id: Long): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE `key` = :key LIMIT 1")
    suspend fun getKeyByValue(key: String): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity): Long

    @Update
    suspend fun updateKey(key: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteKeyById(id: Long)

    @Query("UPDATE api_keys SET dailyUsageCount = 0, lastUsedDate = :today WHERE lastUsedDate != :today")
    suspend fun resetDailyCountsIfNewDay(today: String)

    @Query("UPDATE api_keys SET status = 'ACTIVE', lastError = null WHERE status = 'RATE_LIMITED_429'")
    suspend fun reactivateRateLimitedKeys()

    @Query("SELECT COUNT(*) FROM api_keys")
    suspend fun getKeyCount(): Int
}
