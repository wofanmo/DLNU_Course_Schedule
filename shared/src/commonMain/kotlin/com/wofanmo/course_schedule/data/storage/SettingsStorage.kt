package com.wofanmo.course_schedule.data.storage

import com.russhwolf.settings.Settings
import com.wofanmo.course_schedule.data.model.AppConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsStorage(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getConfig(): AppConfig {
        val configString = settings.getStringOrNull(StorageKeys.APP_CONFIG) ?: return AppConfig()
        return try {
            json.decodeFromString(configString)
        } catch (e: Exception) {
            AppConfig()
        }
    }

    fun saveConfig(config: AppConfig) {
        val configString = json.encodeToString(config)
        settings.putString(StorageKeys.APP_CONFIG, configString)
    }
}
