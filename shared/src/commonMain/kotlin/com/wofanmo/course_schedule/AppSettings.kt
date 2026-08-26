package com.wofanmo.course_schedule

import com.russhwolf.settings.Settings
import com.wofanmo.course_schedule.data.storage.AccountStorage
import com.wofanmo.course_schedule.data.storage.ScheduleStorage
import com.wofanmo.course_schedule.data.storage.SettingsStorage
import com.wofanmo.course_schedule.data.storage.createSettings

object AppSettings {
    private val settings: Settings by lazy { createSettings() }

    val settingsStorage: SettingsStorage by lazy { SettingsStorage(settings) }
    val scheduleStorage: ScheduleStorage by lazy { ScheduleStorage(settings) }
    val accountStorage: AccountStorage by lazy { AccountStorage(settings) }
}