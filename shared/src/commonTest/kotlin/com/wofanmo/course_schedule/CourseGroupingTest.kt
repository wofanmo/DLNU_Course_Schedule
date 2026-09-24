package com.wofanmo.course_schedule

import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.CourseGroup
import com.wofanmo.course_schedule.data.model.groupConflictingCourses
import com.wofanmo.course_schedule.data.model.primaryCourse
import com.wofanmo.course_schedule.data.model.togglePreferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CourseGroupingTest {

    private fun course(
        id: String,
        day: Int = 1,
        start: Int = 1,
        end: Int = 2,
        weeks: List<Int> = (1..16).toList(),
        preferred: Boolean = false,
    ) = Course(
        id = id,
        name = "课程$id",
        teacher = "教师$id",
        location = "地点$id",
        dayOfWeek = day,
        startSection = start,
        endSection = end,
        startWeek = weeks.min(),
        endWeek = weeks.max(),
        weeks = weeks,
        isPreferred = preferred,
    )

    private fun List<Course>.preferredIds() =
        filter { it.isPreferred }.map { it.id }.toSet()

    private fun conflictsOf(courses: List<Course>): List<CourseGroup.Conflict> =
        courses.groupConflictingCourses().filterIsInstance<CourseGroup.Conflict>()

    @Test
    fun groupsOverlappingChainIntoOneConflict() {
        // A(1-2) 与 B(2-3) 重叠，B 与 C(3-4) 重叠，A 与 C 不直接重叠 → 传递闭合归为一组
        val groups = course("a", start = 1, end = 2)
            .let { a ->
                listOf(a, course("b", start = 2, end = 3), course("c", start = 3, end = 4))
            }
            .groupConflictingCourses()

        assertEquals(1, groups.size)
        val conflict = assertIs<CourseGroup.Conflict>(groups.first())
        assertEquals(listOf("a", "b", "c"), conflict.courses.map { it.id })
    }

    @Test
    fun keepsNonOverlappingCoursesSeparate() {
        val groups = listOf(course("a", start = 1, end = 2), course("b", start = 5, end = 6))
            .groupConflictingCourses()

        assertEquals(2, groups.size)
        assertTrue(groups.all { it is CourseGroup.Single })
    }

    @Test
    fun groupsCoursesOnDifferentDaysSeparately() {
        // 同节次但不同天：不算冲突
        val groups = listOf(course("a", day = 1), course("b", day = 2))
            .groupConflictingCourses()

        assertEquals(2, groups.size)
        assertTrue(groups.all { it is CourseGroup.Single })
    }

    @Test
    fun primaryCoursePrefersMarkedCourse() {
        val conflict = conflictsOf(
            listOf(course("a", start = 1, end = 2), course("b", start = 2, end = 3, preferred = true))
        ).single()

        // 优先课胜出，即使它的起始节次更晚
        assertEquals("b", conflict.primaryCourse().id)
    }

    @Test
    fun primaryCourseFallsBackToEarliestStartWhenNoneMarked() {
        val conflict = conflictsOf(
            listOf(course("a", start = 1, end = 2), course("b", start = 2, end = 3))
        ).single()

        assertEquals("a", conflict.primaryCourse().id)
    }

    @Test
    fun primaryCourseTiebreaksAmongMarkedByStartSection() {
        val conflict = conflictsOf(
            listOf(
                course("a", start = 1, end = 2, preferred = true),
                course("b", start = 2, end = 3, preferred = true),
            )
        ).single()

        assertEquals("a", conflict.primaryCourse().id)
    }

    @Test
    fun primaryCourseFallsBackWhenMarkedCourseIsNotInThisGroup() {
        // 被标记的课本周不参与该冲突 → 组内无优先课，按起始节次兜底
        val marked = course("marked", day = 3, preferred = true)
        val conflict = conflictsOf(
            listOf(course("a", start = 1, end = 2), course("b", start = 2, end = 3))
        ).single()

        assertTrue(marked.isPreferred)
        assertEquals("a", conflict.primaryCourse().id)
    }

    // ---------------- 标记切换（同组互斥） ----------------

    @Test
    fun markingCourseClearsItsGroupMates() {
        val courses = listOf(
            course("a", start = 1, end = 2, preferred = true),
            course("b", start = 2, end = 3),
            course("c", start = 3, end = 4),
        )

        // a、b、c 传递闭合为一组，标记 b 应清掉 a
        assertEquals(setOf("b"), courses.togglePreferred("b", week = 1).preferredIds())
    }

    @Test
    fun unmarkingCourseLeavesGroupMatesUntouched() {
        val courses = listOf(
            course("a", start = 1, end = 2, preferred = true),
            course("b", start = 2, end = 3, preferred = true),
        )

        // 取消标记不触发互斥，只作用于目标课程本身
        assertEquals(setOf("a"), courses.togglePreferred("b", week = 1).preferredIds())
    }

    @Test
    fun markingCourseLeavesOtherGroupsAlone() {
        val courses = listOf(
            course("x", day = 2, start = 1, end = 2, preferred = true),
            course("a", start = 1, end = 2),
            course("b", start = 2, end = 3),
        )

        // 另一天的冲突组不受影响
        assertEquals(setOf("x", "b"), courses.togglePreferred("b", week = 1).preferredIds())
    }

    @Test
    fun markingNonConflictingCourseJustMarksIt() {
        val courses = listOf(course("solo", start = 1, end = 2))

        assertEquals(setOf("solo"), courses.togglePreferred("solo", week = 1).preferredIds())
    }

    @Test
    fun groupMatesAreScopedToTheGivenWeek() {
        val a = course("a", start = 1, end = 2, preferred = true)
        val b = course("b", start = 2, end = 3, weeks = (9..16).toList())

        // 第 1 周 b 不上课：两者不同组，标记 b 不动 a
        assertEquals(
            setOf("a", "b"),
            listOf(a, b).togglePreferred("b", week = 1).preferredIds(),
        )

        // 第 9 周两者同组：标记 b 清掉 a
        assertEquals(
            setOf("b"),
            listOf(a, b).togglePreferred("b", week = 9).preferredIds(),
        )
    }

    @Test
    fun togglingUnknownCourseIsANoOp() {
        val courses = listOf(course("a"))

        assertEquals(courses, courses.togglePreferred("missing", week = 1))
    }
}
