package com.example

import android.app.Application
import com.example.kotib.KotibContainer
import kotlinx.coroutines.launch

class KotibApp : Application() {

    lateinit var container: KotibContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = KotibContainer(this)

        // Boshlang'ich kalitlarni tekshirish va kiritish
        container.applicationScope.launch {
            val buildConfigKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                null
            }
            container.apiKeyRepository.initializeDefaultKeysIfEmpty(buildConfigKey)
        }
    }
}
