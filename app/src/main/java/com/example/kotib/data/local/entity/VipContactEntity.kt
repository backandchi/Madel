package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Muhim (VIP) kontaktlar jadvali.
 * Ushbu kontaktlardan kelgan xabarlar avtomatik ravishda yuqori muhimlikka ega bo'ladi
 * va Jim soatlarda ham e'tibordan chetda qolmaydi.
 */
@Entity(tableName = "vip_contacts")
data class VipContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val identifier: String, // Telefon raqami, Telegram username yoki ism
    val urgencyBoost: Int = 2, // Muhimlik darajasini oshirish
    val allowInQuietHours: Boolean = true,
    val notes: String = ""
)
