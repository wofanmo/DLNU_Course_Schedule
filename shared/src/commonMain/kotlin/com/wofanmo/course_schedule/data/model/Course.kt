package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val id: String,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,       // 1=周一 ... 7=周日
    val startSection: Int,    // 开始节次
    val endSection: Int,      // 结束节次
    val startWeek: Int,       // 起始周
    val endWeek: Int,         // 结束周
    val color: String = "",
)
