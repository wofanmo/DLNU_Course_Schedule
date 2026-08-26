package com.wofanmo.course_schedule.data.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var appContext: Context

fun initStorage(context: Context) {
    appContext = context.applicationContext
}

actual fun createSettings(): Settings {
    val sharedPreferences = appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPreferences)
}
