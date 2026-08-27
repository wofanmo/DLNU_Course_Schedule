package com.wofanmo.course_schedule

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wofanmo.course_schedule.ui.MainScreen

@Composable
fun App() {
    // 订阅全局版本号：设置页保存配置后 scheduleVersion 自增，此处重组并重新读取主题
    val dataVersion = AppEvents.scheduleVersion.intValue
    val theme = remember(dataVersion) { AppSettings.settingsStorage.getConfig().theme }
    val isDarkTheme = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme() // 跟随系统
    }

    MainScreen(isDarkTheme = isDarkTheme)
}
