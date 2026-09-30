package com.example.kotib.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kotib.data.local.entity.VipContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VipContactDao {
    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    fun getAllVipContactsFlow(): Flow<List<VipContactEntity>>

    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    suspend fun getAllVipContacts(): List<VipContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVipContact(contact: VipContactEntity): Long

    @Query("DELETE FROM vip_contacts WHERE id = :id")
    suspend fun deleteVipContactById(id: Long)

    @Query("SELECT COUNT(*) FROM vip_contacts WHERE LOWER(:sender) LIKE '%' || LOWER(identifier) || '%' OR LOWER(:sender) LIKE '%' || LOWER(name) || '%'")
    suspend fun countMatchingVip(sender: String): Int
}
