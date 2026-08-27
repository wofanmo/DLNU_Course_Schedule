package com.wofanmo.course_schedule.data.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** 生成课程/课表等实体的本地唯一 ID */
fun newId(): String =
    "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100_000)}"

@Serializable
data class Course(
    val id: String,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,       // 1=周一 ... 7=周日
    val startSection: Int,    // 开始节次
    val endSection: Int,      // 结束节次
    val startWeek: Int,       // 起始周（当 weeks 为空时的兜底）
    val endWeek: Int,         // 结束周
    val weekParity: WeekParity = WeekParity.EVERY,  // 单双周（当 weeks 为空时生效）
    /** 精确周次列表（手动添加时可为任意周）；为空时用 startWeek..endWeek + weekParity 计算 */
    val weeks: List<Int> = emptyList(),
    val color: String = "",
)

/** 课程是否在指定周上课（兼容 weeks 列表与范围+单双周两种模式） */
fun Course.matchesWeek(week: Int): Boolean {
    if (weeks.isNotEmpty()) return week in weeks
    return week in startWeek..endWeek && weekParity.matches(week)
}
