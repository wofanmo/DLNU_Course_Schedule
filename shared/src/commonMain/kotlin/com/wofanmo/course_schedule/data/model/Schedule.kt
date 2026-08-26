package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String,
    val name: String,
    val startDate: String,       // 开学日期 yyyy-MM-dd
    val totalWeeks: Int,
    val courses: List<Course> = emptyList(),
)
