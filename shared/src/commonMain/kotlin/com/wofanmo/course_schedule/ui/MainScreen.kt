package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class Screen(val title: String, val iconText: String) {
    TODAY("今日", "今"),
    SCHEDULE("课表", "课"),
    SETTINGS("设置", "设")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedScreen by remember { mutableStateOf(Screen.TODAY) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(screen.iconText, style = MaterialTheme.typography.titleMedium) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedScreen) {
            Screen.TODAY -> TodayScreen(modifier = Modifier.padding(innerPadding))
            Screen.SCHEDULE -> ScheduleScreen(modifier = Modifier.padding(innerPadding))
            Screen.SETTINGS -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
