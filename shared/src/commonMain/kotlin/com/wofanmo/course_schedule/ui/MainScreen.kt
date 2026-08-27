package com.wofanmo.course_schedule.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wofanmo.course_schedule.ui.components.AnimatedNavigationBar
import com.wofanmo.course_schedule.ui.components.GradientBackground
import com.wofanmo.course_schedule.ui.components.NavigationBarItem
import com.wofanmo.course_schedule.ui.components.ScheduleSpeedDial
import com.wofanmo.course_schedule.ui.components.SpeedDialItem
import com.wofanmo.course_schedule.ui.theme.CourseScheduleTheme

enum class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY("今日", Icons.Filled.CalendarMonth),
    SCHEDULE("课表", Icons.Filled.ViewModule),
    SETTINGS("设置", Icons.Filled.Settings)
}

/** 全屏覆盖层页面（不走底部导航） */
private enum class Overlay {
    ADD_COURSE,
    IMPORT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isDarkTheme: Boolean = false) {
    var selectedScreen by remember { mutableStateOf(Screen.TODAY) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    // 退出动画期间保留最后展示的覆盖层
    var shownOverlay by remember { mutableStateOf<Overlay?>(null) }
    overlay?.let { shownOverlay = it }

    CourseScheduleTheme(darkTheme = isDarkTheme) {
        // 渐变背景
        GradientBackground(
            modifier = Modifier.fillMaxSize(),
            isDarkTheme = isDarkTheme
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                floatingActionButton = {
                    // 仅课表界面显示添加入口
                    if (selectedScreen == Screen.SCHEDULE && overlay == null) {
                        ScheduleSpeedDial(
                            items = listOf(
                                SpeedDialItem(
                                    label = "从教务系统导入课程表",
                                    icon = Icons.Default.Download,
                                    onClick = { overlay = Overlay.IMPORT }
                                ),
                                SpeedDialItem(
                                    label = "手动添加课程",
                                    icon = Icons.Default.Edit,
                                    onClick = { overlay = Overlay.ADD_COURSE }
                                ),
                            )
                        )
                    }
                },
                bottomBar = {
                    AnimatedNavigationBar(
                        selectedIndex = selectedScreen.ordinal,
                        onItemSelected = { index -> selectedScreen = Screen.entries[index] },
                        items = Screen.entries.map { screen ->
                            NavigationBarItem(
                                icon = screen.icon,
                                label = screen.title
                            )
                        }
                    )
                }
            ) { innerPadding ->
                // 页面转场动画 - 淡入淡出
                AnimatedContent(
                    targetState = selectedScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    modifier = Modifier.padding(innerPadding)
                ) { screen ->
                    when (screen) {
                        Screen.TODAY -> TodayScreen()
                        Screen.SCHEDULE -> ScheduleScreen()
                        Screen.SETTINGS -> SettingsScreen()
                    }
                }
            }

            // 全屏覆盖层：手动添加课程 / 教务系统导入
            AnimatedVisibility(
                visible = overlay != null,
                enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(tween(300))
            ) {
                when (shownOverlay) {
                    Overlay.ADD_COURSE -> AddCourseScreen(onClose = { overlay = null })
                    Overlay.IMPORT -> ImportScreen(onClose = { overlay = null })
                    null -> Unit
                }
            }
        }
    }
}
