package com.wofanmo.course_schedule

import androidx.compose.runtime.mutableIntStateOf

/**
 * 全局数据版本号：存储层写入后自增，驱动界面重新读取数据。
 * （课程表数据经由 multiplatform-settings 持久化，本身不具备可观察性）
 */
object AppEvents {
    val scheduleVersion = mutableIntStateOf(0)
}
