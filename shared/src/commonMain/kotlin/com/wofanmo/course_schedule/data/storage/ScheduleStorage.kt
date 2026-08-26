package com.wofanmo.course_schedule.data.storage

import com.russhwolf.settings.Settings
import com.wofanmo.course_schedule.data.model.Schedule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScheduleStorage(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<Schedule> {
        val schedulesString = settings.getStringOrNull(StorageKeys.SCHEDULES) ?: return emptyList()
        return try {
            json.decodeFromString(schedulesString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getById(id: String): Schedule? {
        return getAll().find { it.id == id }
    }

    fun save(schedule: Schedule) {
        val schedules = getAll().toMutableList()
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index >= 0) {
            schedules[index] = schedule
        } else {
            schedules.add(schedule)
        }
        val schedulesString = json.encodeToString(schedules)
        settings.putString(StorageKeys.SCHEDULES, schedulesString)
    }

    fun delete(id: String) {
        val schedules = getAll().toMutableList()
        schedules.removeAll { it.id == id }
        val schedulesString = json.encodeToString(schedules)
        settings.putString(StorageKeys.SCHEDULES, schedulesString)
    }
}
