package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gemini API kalitlari jadvali.
 * Kalitlar menejeri va avtomatik 429 rotatsiyasi uchun ishlatiladi.
 */
@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val label: String,
    val isActive: Boolean = true,
    val dailyUsageCount: Int = 0,
    val lastUsedDate: String = "", // e.g. "2026-09-30"
    val status: String = STATUS_ACTIVE,
    val lastError: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_RATE_LIMITED = "RATE_LIMITED_429"
        const val STATUS_EXHAUSTED = "EXHAUSTED"
        const val STATUS_DISABLED = "DISABLED"
    }

    /**
     * Kalitni xavfsiz ko'rsatish (UI va loglar uchun maskalangan)
     */
    fun maskedKey(): String {
        return if (key.length > 8) {
            "${key.take(6)}...${key.takeLast(4)}"
        } else {
            "****"
        }
    }
}
