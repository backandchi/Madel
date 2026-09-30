package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntegrationConfigDao {
    @Query("SELECT * FROM integration_configs ORDER BY displayName ASC")
    fun getAllConfigsFlow(): Flow<List<IntegrationConfigEntity>>

    @Query("SELECT * FROM integration_configs WHERE serviceId = :serviceId")
    suspend fun getConfigById(serviceId: String): IntegrationConfigEntity?

    @Query("SELECT * FROM integration_configs WHERE serviceId = :serviceId")
    fun getConfigByIdFlow(serviceId: String): Flow<IntegrationConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: IntegrationConfigEntity)

    @Update
    suspend fun update(config: IntegrationConfigEntity)

    @Query("UPDATE integration_configs SET isEnabled = :enabled WHERE serviceId = :serviceId")
    suspend fun setEnabled(serviceId: String, enabled: Boolean)
}
