package com.example.kotib.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kotib.data.local.dao.AgentActionLogDao
import com.example.kotib.data.local.dao.ApiKeyDao
import com.example.kotib.data.local.dao.AutomationTaskDao
import com.example.kotib.data.local.dao.ChatMessageDao
import com.example.kotib.data.local.dao.IntegrationConfigDao
import com.example.kotib.data.local.dao.NotificationMessageDao
import com.example.kotib.data.local.dao.VipContactDao
import com.example.kotib.data.local.entity.AgentActionLogEntity
import com.example.kotib.data.local.entity.ApiKeyEntity
import com.example.kotib.data.local.entity.AutomationTaskEntity
import com.example.kotib.data.local.entity.ChatMessageEntity
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import com.example.kotib.data.local.entity.NotificationMessageEntity
import com.example.kotib.data.local.entity.VipContactEntity

@Database(
    entities = [
        ApiKeyEntity::class,
        ChatMessageEntity::class,
        AgentActionLogEntity::class,
        NotificationMessageEntity::class,
        VipContactEntity::class,
        IntegrationConfigEntity::class,
        AutomationTaskEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KotibDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun agentActionLogDao(): AgentActionLogDao
    abstract fun notificationMessageDao(): NotificationMessageDao
    abstract fun vipContactDao(): VipContactDao
    abstract fun integrationConfigDao(): IntegrationConfigDao
    abstract fun automationTaskDao(): AutomationTaskDao

    companion object {
        @Volatile
        private var INSTANCE: KotibDatabase? = null

        fun getInstance(context: Context): KotibDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KotibDatabase::class.java,
                    "kotib_database.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
