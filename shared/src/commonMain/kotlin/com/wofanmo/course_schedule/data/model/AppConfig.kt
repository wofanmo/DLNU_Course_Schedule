package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

/** 单节次的起止时间（HH:mm 文本） */
@Serializable
data class SectionTime(
    val start: String = "08:30",
    val end: String = "09:10",
)

/**
 * 固定课表时间表（硬编码，不在设置中提供修改入口）：
 * 一天 12 节，上午/下午/晚上各 4 节，每节 40 分钟。
 * 同一大节内（1-2、3-4、5-6、7-8、9-10、11-12）课间 10 分钟；
 * 跨大节（2-3、6-7、10-11）课间 20 分钟。
 * 上午 08:30 开始，下午 13:30 开始，晚上 18:30 开始。
 */
val DEFAULT_SECTION_TIMES: List<SectionTime> = listOf(
    SectionTime("08:30", "09:10"), // 1
    SectionTime("09:20", "10:00"), // 2
    SectionTime("10:20", "11:00"), // 3
    SectionTime("11:10", "11:50"), // 4
    SectionTime("13:30", "14:10"), // 5
    SectionTime("14:20", "15:00"), // 6
    SectionTime("15:20", "16:00"), // 7
    SectionTime("16:10", "16:50"), // 8
    SectionTime("18:30", "19:10"), // 9
    SectionTime("19:20", "20:00"), // 10
    SectionTime("20:20", "21:00"), // 11
    SectionTime("21:10", "21:50"), // 12
)

/** 固定总节数：12 */
const val DEFAULT_TOTAL_SECTIONS: Int = 12

/** 午休分隔线位置（0-based）：第 4 节后 */
const val DEFAULT_LUNCH_BREAK_INDEX: Int = 3

/** 晚休分隔线位置（0-based）：第 8 节后 */
const val DEFAULT_EVENING_BREAK_INDEX: Int = 7

@Serializable
data class AppConfig(
    val currentScheduleId: String = "",
    val theme: String = "system",
    val firstDayOfWeek: Int = 1,
    val showWeekends: Boolean = true,
    /** 保留向后兼容：旧版本数据可能携带该字段，新版固定为 12 */
    val totalSections: Int = DEFAULT_TOTAL_SECTIONS,
    /** 课程提醒提前分钟数：0=关闭（不发任何通知），正值为提前提醒分钟数（预设 5/10/15/30） */
    val notifyLeadMinutes: Int = 0,
)

/** 总节数（固定 12 节，硬编码） */
fun AppConfig.totalSectionCount(): Int = DEFAULT_TOTAL_SECTIONS

/** 午休分隔线位置（0-based） */
fun AppConfig.lunchBreakIndex(): Int = DEFAULT_LUNCH_BREAK_INDEX

/** 晚休分隔线位置（0-based） */
fun AppConfig.eveningBreakIndex(): Int = DEFAULT_EVENING_BREAK_INDEX
