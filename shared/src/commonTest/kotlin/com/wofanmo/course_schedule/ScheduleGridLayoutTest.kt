package com.wofanmo.course_schedule

import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.DEFAULT_EVENING_BREAK_INDEX
import com.wofanmo.course_schedule.data.model.DEFAULT_LUNCH_BREAK_INDEX
import com.wofanmo.course_schedule.data.model.DEFAULT_TOTAL_SECTIONS
import com.wofanmo.course_schedule.ui.FAB_RESERVE_DP
import com.wofanmo.course_schedule.ui.MIN_DETAIL_FONT_SP
import com.wofanmo.course_schedule.ui.MIN_NAME_FONT_SP
import com.wofanmo.course_schedule.ui.MIN_SECTION_HEIGHT_DP
import com.wofanmo.course_schedule.ui.TIME_RAIL_COMPACT_WIDTH_DP
import com.wofanmo.course_schedule.ui.TIME_RAIL_WIDTH_DP
import com.wofanmo.course_schedule.ui.adaptiveFontSp
import com.wofanmo.course_schedule.ui.computeGridMetrics
import com.wofanmo.course_schedule.ui.fixedGridMetrics
import com.wofanmo.course_schedule.ui.naturalGridHeightDp
import com.wofanmo.course_schedule.ui.needsFabReserve
import com.wofanmo.course_schedule.ui.timeRailWidthDp
import com.wofanmo.course_schedule.ui.trimmedSectionCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val LUNCH = DEFAULT_LUNCH_BREAK_INDEX
private const val EVENING = DEFAULT_EVENING_BREAK_INDEX

private fun course(
    day: Int = 1,
    start: Int = 1,
    end: Int = 2,
) = Course(
    id = "c-$day-$start-$end",
    name = "测试课",
    teacher = "教师",
    location = "教学楼",
    dayOfWeek = day,
    startSection = start,
    endSection = end,
    startWeek = 1,
    endWeek = 16,
)

class ScheduleGridLayoutTest {

    // ---------- 自然高度 ----------

    @Test
    fun `full 12 section grid is 800dp tall`() {
        // 12 × 60 + 午休 32 + 晚休 32 + 内边距 16
        assertEquals(800f, naturalGridHeightDp(12, LUNCH, EVENING))
    }

    @Test
    fun `grid trimmed to 8 sections is 528dp tall`() {
        // 8 × 60 + 午休 32（晚休在第 8 节之后，已裁掉故不计）+ 内边距 16
        assertEquals(528f, naturalGridHeightDp(8, LUNCH, EVENING))
    }

    // ---------- 缩放 ----------

    @Test
    fun `grid at natural height needs no scaling`() {
        val metrics = computeGridMetrics(12, 800f, LUNCH, EVENING)

        assertEquals(1f, metrics.scale)
        assertEquals(60, metrics.sectionHeightDp)
        assertEquals(32, metrics.breakHeightDp)
        assertEquals(800, metrics.totalHeightDp)
        assertFalse(metrics.overflows)
    }

    @Test
    fun `grid shrinks to fit a smaller budget`() {
        val metrics = computeGridMetrics(12, 557f, LUNCH, EVENING)

        // 557 / 800
        assertEquals(0.69625f, metrics.scale, 0.0001f)
        assertEquals(42, metrics.sectionHeightDp)
        assertFalse(metrics.overflows, "尚未触及几何下限，不应退回滚动")
    }

    @Test
    fun `grid never shrinks below the 40dp floor and reports overflow instead`() {
        // 400dp 预算只够 0.5 倍，低于下限 40/60 ≈ 0.667
        val metrics = computeGridMetrics(12, 400f, LUNCH, EVENING)

        assertEquals(MIN_SECTION_HEIGHT_DP / 60f, metrics.scale, 0.0001f)
        assertEquals(MIN_SECTION_HEIGHT_DP.toInt(), metrics.sectionHeightDp)
        assertTrue(metrics.overflows, "缩到底仍放不下，应标记为退回滚动")
    }

    @Test
    fun `exactly at the floor is still considered fitting`() {
        val metrics = computeGridMetrics(12, 800f * (MIN_SECTION_HEIGHT_DP / 60f), LUNCH, EVENING)

        assertFalse(metrics.overflows)
    }

    @Test
    fun `grid never grows beyond natural size`() {
        val metrics = computeGridMetrics(4, 5000f, LUNCH, EVENING)

        assertEquals(1f, metrics.scale)
        assertEquals(60, metrics.sectionHeightDp)
    }

    @Test
    fun `row offsets stay consistent with section height`() {
        val metrics = computeGridMetrics(12, 800f, LUNCH, EVENING)

        assertEquals(12, metrics.rowOffsets.size)
        assertEquals(0, metrics.rowOffsets[0])
        // 第 5 节前面有 4 节 + 午休
        assertEquals(4 * 60 + 32, metrics.rowOffsets[4])
        // 第 9 节前面有 8 节 + 午休 + 晚休
        assertEquals(8 * 60 + 32 + 32, metrics.rowOffsets[8])
    }

    // ---------- 关闭自适应 ----------

    @Test
    fun `disabled adaptive layout keeps fixed 60dp rows regardless of budget`() {
        val metrics = fixedGridMetrics(12, LUNCH, EVENING)

        assertEquals(1f, metrics.scale)
        assertEquals(60, metrics.sectionHeightDp)
        assertEquals(800, metrics.totalHeightDp)
        assertFalse(metrics.overflows)
    }

    // ---------- 尾部裁剪 ----------

    @Test
    fun `trims to the latest section used during the week`() {
        val courses = listOf(course(end = 2), course(end = 8), course(start = 5, end = 6))

        assertEquals(8, trimmedSectionCount(courses, DEFAULT_TOTAL_SECTIONS))
    }

    @Test
    fun `keeps full grid when the week has no courses`() {
        assertEquals(DEFAULT_TOTAL_SECTIONS, trimmedSectionCount(emptyList(), DEFAULT_TOTAL_SECTIONS))
    }

    @Test
    fun `clamps out of range section numbers`() {
        assertEquals(DEFAULT_TOTAL_SECTIONS, trimmedSectionCount(listOf(course(end = 99)), DEFAULT_TOTAL_SECTIONS))
        assertEquals(1, trimmedSectionCount(listOf(course(end = 0)), DEFAULT_TOTAL_SECTIONS))
    }

    // ---------- Speed Dial 让位 ----------

    @Test
    fun `reserves height only when a card sits in the bottom right corner`() {
        // 最后两节 × 最右两列
        assertTrue(needsFabReserve(listOf(course(day = 7, end = 12)), 12, 7))
        assertTrue(needsFabReserve(listOf(course(day = 6, end = 11)), 12, 7))
        // 同一节次但最左列，Speed Dial 压在右边，不必让位
        assertFalse(needsFabReserve(listOf(course(day = 1, end = 12)), 12, 7))
        // 同一列但很早的节次
        assertFalse(needsFabReserve(listOf(course(day = 7, end = 5)), 12, 7))
    }

    @Test
    fun `corner region follows the hidden weekend setting`() {
        // 不显示周末时最右两列是周四周五
        assertTrue(needsFabReserve(listOf(course(day = 5, end = 12)), 12, 5))
        assertFalse(needsFabReserve(listOf(course(day = 6, end = 12)), 12, 5))
    }

    @Test
    fun `no reserve for an empty week`() {
        assertFalse(needsFabReserve(emptyList(), 12, 7))
    }

    @Test
    fun `reserve accounts for a noticeable slice of the budget`() {
        assertEquals(72f, FAB_RESERVE_DP)
    }

    // ---------- 时间栏宽度 ----------

    @Test
    fun `narrow screens give width back to the day columns`() {
        // (360 - 56) / 7 ≈ 43dp，低于 46dp 阈值
        assertEquals(TIME_RAIL_COMPACT_WIDTH_DP, timeRailWidthDp(360f, 7, adaptive = true))
    }

    @Test
    fun `wide screens keep the standard rail width`() {
        assertEquals(TIME_RAIL_WIDTH_DP, timeRailWidthDp(600f, 7, adaptive = true))
        // 5 列时每列本来就更宽
        assertEquals(TIME_RAIL_WIDTH_DP, timeRailWidthDp(360f, 5, adaptive = true))
    }

    @Test
    fun `disabled adaptive layout keeps the standard rail width`() {
        assertEquals(TIME_RAIL_WIDTH_DP, timeRailWidthDp(360f, 7, adaptive = false))
    }

    // ---------- 字号下限 ----------

    @Test
    fun `fonts stop shrinking at their readable floor`() {
        // 缩到底时课程名 11 × 0.667 ≈ 7.3sp，被抬到 9sp
        assertEquals(MIN_NAME_FONT_SP, adaptiveFontSp(11f, 0.667f, MIN_NAME_FONT_SP))
        assertEquals(MIN_DETAIL_FONT_SP, adaptiveFontSp(9f, 0.667f, MIN_DETAIL_FONT_SP))
    }

    @Test
    fun `fonts keep their natural size when there is no scaling`() {
        assertEquals(11f, adaptiveFontSp(11f, 1f, MIN_NAME_FONT_SP))
        assertEquals(9f, adaptiveFontSp(9f, 1f, MIN_DETAIL_FONT_SP))
    }

    @Test
    fun `fonts still shrink proportionally above the floor`() {
        assertEquals(9.9f, adaptiveFontSp(11f, 0.9f, MIN_NAME_FONT_SP), 0.0001f)
    }
}