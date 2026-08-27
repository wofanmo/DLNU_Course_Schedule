package com.wofanmo.course_schedule.ui

import androidx.compose.ui.graphics.Color

// 获取当前日期信息（简化版，实际应该使用 kotlinx-datetime）
data class DateInfo(val year: Int, val month: Int, val day: Int, val dayOfWeek: Int)

expect fun getCurrentDate(): DateInfo
expect fun parseColor(colorString: String): Color?
expect fun calculateWeeksBetween(startDate: String, currentDate: DateInfo): Int
expect fun getDateForWeekDay(startDate: String, week: Int, dayOfWeek: Int): DateInfo?
