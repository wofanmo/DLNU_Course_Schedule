package com.wofanmo.course_schedule.ui

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.*

actual fun getCurrentDate(): DateInfo {
    val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return DateInfo(
        year = today.year,
        month = today.monthNumber,
        day = today.dayOfMonth,
        dayOfWeek = today.dayOfWeek.isoDayNumber
    )
}

actual fun parseColor(colorString: String): Color? {
    return try {
        val color = android.graphics.Color.parseColor(colorString)
        Color(color)
    } catch (e: Exception) {
        null
    }
}

actual fun calculateWeeksBetween(startDate: String, currentDate: DateInfo): Int {
    return try {
        val start = LocalDate.parse(startDate)
        val current = LocalDate(currentDate.year, currentDate.month, currentDate.day)
        val daysBetween = start.until(current, DateTimeUnit.DAY)
        (daysBetween / 7) + 1
    } catch (e: Exception) {
        1
    }
}
