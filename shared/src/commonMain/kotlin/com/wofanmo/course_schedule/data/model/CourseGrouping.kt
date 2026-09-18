package com.wofanmo.course_schedule.data.model

/**
 * 课程分组结果：单门课或冲突组。
 * 用于课表网格的传递闭合冲突分组展示。
 */
sealed class CourseGroup {
    /** 无冲突的单门课程，原样展示 */
    data class Single(val course: Course) : CourseGroup()

    /** 冲突组（传递闭合分组），包含 ≥2 门节次区间重叠的课程 */
    data class Conflict(val courses: List<Course>) : CourseGroup()
}

/**
 * 将本周有效的课程列表按传递闭合规则分组。
 *
 * 算法：
 * 1. 按 dayOfWeek 分组
 * 2. 每天组内按 startSection 排序
 * 3. 扫描：若课程的 startSection ≤ 当前组的 maxEnd，则合并（传递闭合）
 *    否则结算当前组并开始新组
 * 4. 组内 ≥2 门课归为 [CourseGroup.Conflict]，1 门课归为 [CourseGroup.Single]
 *
 * 输入应为已经按当前周过滤过的课程列表（调用方通过 [Course.matchesWeek] 过滤）。
 */
fun List<Course>.groupConflictingCourses(): List<CourseGroup> {
    if (isEmpty()) return emptyList()

    val result = mutableListOf<CourseGroup>()

    for ((_, dayCourses) in this.groupBy { it.dayOfWeek }.entries.sortedBy { it.key }) {
        val sorted = dayCourses.sortedWith(compareBy({ it.startSection }, { it.endSection }))
        if (sorted.isEmpty()) continue

        var groupStart = 0
        var groupMaxEnd = sorted[0].endSection

        for (i in 1 until sorted.size) {
            val course = sorted[i]
            if (course.startSection <= groupMaxEnd) {
                // 传递闭合：合并到当前组，更新最大结束节次
                groupMaxEnd = maxOf(groupMaxEnd, course.endSection)
            } else {
                // 不重叠：结算当前组
                result.add(buildGroup(sorted.subList(groupStart, i)))
                groupStart = i
                groupMaxEnd = course.endSection
            }
        }

        // 结算最后一组
        result.add(buildGroup(sorted.subList(groupStart, sorted.size)))
    }

    return result
}

private fun buildGroup(group: List<Course>): CourseGroup =
    if (group.size == 1) {
        CourseGroup.Single(group[0])
    } else {
        CourseGroup.Conflict(group.toList())
    }

/**
 * 时间冲突检测：检查新课程是否与已有课程列表中的任一课程存在时间冲突。
 * 冲突条件：同星期 + 节次区间相交 + 周次有交集。
 *
 * 与 [groupConflictingCourses] 使用相同的节次重叠语义，额外增加了周次匹配。
 */
fun findConflicts(existing: List<Course>, new: Course): List<Course> {
    val newWeeks = if (new.weeks.isNotEmpty()) {
        new.weeks
    } else {
        (new.startWeek..new.endWeek).toList()
    }
    return existing.filter { c ->
        c.dayOfWeek == new.dayOfWeek &&
            c.startSection <= new.endSection && new.startSection <= c.endSection &&
            newWeeks.any { week -> c.matchesWeek(week) }
    }
}

/**
 * 冲突组在课表格子上代表显示的课程。
 *
 * 优先课（[Course.isPreferred]）优先；同组多门优先课时取起始节次最早的
 * （组内已按 `(startSection, endSection)` 排序，取首个匹配即可）；无优先课时取起始节次最早的。
 */
fun CourseGroup.Conflict.primaryCourse(): Course =
    courses.firstOrNull { it.isPreferred } ?: courses.first()

/**
 * 切换 [courseId] 对应课程的「优先显示」标记，返回更新后的课程列表。
 *
 * 同一冲突组内只保留一门优先课：标记某门课时，取消该课所在冲突组（按 [week] 计算）内
 * 其他课程的标记。取消标记时不影响其他课程，组内可回到「无偏好」状态。
 *
 * 冲突按周计算，跨周标记仍可能让同一组出现多门优先课，由 [primaryCourse] 的
 * 「起始节次最早」规则兜底。
 */
fun List<Course>.togglePreferred(courseId: String, week: Int): List<Course> {
    val target = firstOrNull { it.id == courseId } ?: return this
    val marked = !target.isPreferred

    val groupMateIds = if (marked) {
        filter { it.matchesWeek(week) }
            .groupConflictingCourses()
            .filterIsInstance<CourseGroup.Conflict>()
            .firstOrNull { group -> group.courses.any { it.id == courseId } }
            ?.courses
            ?.map { it.id }
            ?.toSet()
            .orEmpty()
    } else {
        emptySet()
    }

    return map { course ->
        when {
            course.id == courseId -> course.copy(isPreferred = marked)
            course.id in groupMateIds -> course.copy(isPreferred = false)
            else -> course
        }
    }
}