package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

/**
 * 单双周：课程在周次范围内的奇偶过滤
 */
@Serializable
enum class WeekParity(val displayName: String) {
    EVERY("每周"),
    ODD("仅单周"),
    EVEN("仅双周");

    fun matches(week: Int): Boolean = when (this) {
        EVERY -> true
        ODD -> week % 2 == 1
        EVEN -> week % 2 == 0
    }
}
