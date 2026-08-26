package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val currentScheduleId: String = "",
    val theme: String = "system",
    val firstDayOfWeek: Int = 1,
    val showWeekends: Boolean = true,
    val totalSections: Int = 12,
)
