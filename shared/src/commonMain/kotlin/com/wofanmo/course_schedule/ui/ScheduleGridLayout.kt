package com.wofanmo.course_schedule.ui

import com.wofanmo.course_schedule.data.model.Course
import kotlin.math.roundToInt

/**
 * 自适应课表网格的尺寸计算。
 *
 * 规则见 docs/adr/0006-adaptive-schedule-grid.md：
 * - 网格在可用高度内按需等比缩小，缩到几何下限 [MIN_SECTION_HEIGHT_DP] 就停手，再放不下则退回滚动；
 * - 字号不跟随等比缩到看不清，而是各自守住可读下限（[MIN_NAME_FONT_SP] / [MIN_DETAIL_FONT_SP]）；
 * - 尾部整周都无课的节次直接裁掉，裁掉的部分由留白与说明文字表达。
 *
 * 全部为纯函数与不可变数据，便于单元测试。
 */

/** 一节的标准高度 */
internal const val BASE_SECTION_HEIGHT_DP = 60f

/** 午休/晚休分隔行高度 */
internal const val BASE_BREAK_HEIGHT_DP = 32f

/** 网格上下内边距合计（上下各 8dp） */
internal const val GRID_VERTICAL_PADDING_DP = 16f

/** 网格左右内边距合计（左右各 8dp），用于把可用宽度换算成内容宽度 */
internal const val GRID_HORIZONTAL_PADDING_DP = 16f

/** 行高几何下限：低于此值不再缩，改为退回滚动 */
internal const val MIN_SECTION_HEIGHT_DP = 40f

/** 右下角 Speed Dial 需要让出的高度（56dp 直径 + 16dp 边距） */
internal const val FAB_RESERVE_DP = 72f

/** 课程名字号下限 */
internal const val MIN_NAME_FONT_SP = 9f

/** 地点/教师等次要文字的字号下限 */
internal const val MIN_DETAIL_FONT_SP = 8f

/** 左侧时间栏标准宽度 */
internal const val TIME_RAIL_WIDTH_DP = 56f

/** 左侧时间栏收窄后的宽度：窄屏时把宽度让给课程列 */
internal const val TIME_RAIL_COMPACT_WIDTH_DP = 44f

/** 每列低于此宽度就收窄时间栏 */
internal const val NARROW_DAY_WIDTH_DP = 46f

/** 缩小到几何下限时对应的缩放系数（40 / 60） */
internal val MIN_SCALE: Float = MIN_SECTION_HEIGHT_DP / BASE_SECTION_HEIGHT_DP

/**
 * 一次网格布局的全部尺寸。时间栏、课程卡片区、分隔线共用同一份，
 * 避免各处各自换算导致错位。
 */
internal class GridMetrics(
    val sectionCount: Int,
    val sectionHeightDp: Int,
    val breakHeightDp: Int,
    val rowOffsets: IntArray,
    val totalHeightDp: Int,
    /** 1f 表示原始尺寸，低于此值表示已压缩 */
    val scale: Float,
    /** true 表示缩到几何下限仍放不下，界面退回垂直滚动 */
    val overflows: Boolean,
)

/** 给定节数时，网格在原始尺寸下的自然高度 */
internal fun naturalGridHeightDp(
    sectionCount: Int,
    lunchBreakIndex: Int,
    eveningBreakIndex: Int,
): Float {
    var y = 0f
    for (i in 0 until sectionCount) {
        y += BASE_SECTION_HEIGHT_DP
        if (i == lunchBreakIndex && sectionCount > lunchBreakIndex + 1) y += BASE_BREAK_HEIGHT_DP
        if (i == eveningBreakIndex && sectionCount > eveningBreakIndex + 1) y += BASE_BREAK_HEIGHT_DP
    }
    return y + GRID_VERTICAL_PADDING_DP
}

/**
 * 把可用高度预算换算成具体尺寸。
 *
 * [availableHeightDp] 为扣除顶栏、星期栏与可能让给 Speed Dial 的高度后，
 * 留给整个网格（含其上下内边距）的高度。
 */
internal fun computeGridMetrics(
    sectionCount: Int,
    availableHeightDp: Float,
    lunchBreakIndex: Int,
    eveningBreakIndex: Int,
): GridMetrics {
    val natural = naturalGridHeightDp(sectionCount, lunchBreakIndex, eveningBreakIndex)
    val rawScale = if (natural > 0f && availableHeightDp > 0f) availableHeightDp / natural else 1f
    val scale = rawScale.coerceIn(MIN_SCALE, 1f)

    val sectionHeight = (BASE_SECTION_HEIGHT_DP * scale).roundToInt()
    val breakHeight = (BASE_BREAK_HEIGHT_DP * scale).roundToInt()

    val offsets = IntArray(sectionCount)
    var y = 0
    for (i in 0 until sectionCount) {
        offsets[i] = y
        y += sectionHeight
        if (i == lunchBreakIndex && sectionCount > lunchBreakIndex + 1) y += breakHeight
        if (i == eveningBreakIndex && sectionCount > eveningBreakIndex + 1) y += breakHeight
    }

    return GridMetrics(
        sectionCount = sectionCount,
        sectionHeightDp = sectionHeight,
        breakHeightDp = breakHeight,
        rowOffsets = offsets,
        totalHeightDp = y + (GRID_VERTICAL_PADDING_DP * scale).roundToInt(),
        scale = scale,
        overflows = rawScale < MIN_SCALE,
    )
}

/** 关闭自适应时的固定布局：标准行高，放不下的部分交给垂直滚动 */
internal fun fixedGridMetrics(
    sectionCount: Int,
    lunchBreakIndex: Int,
    eveningBreakIndex: Int,
): GridMetrics = computeGridMetrics(
    sectionCount = sectionCount,
    availableHeightDp = naturalGridHeightDp(sectionCount, lunchBreakIndex, eveningBreakIndex),
    lunchBreakIndex = lunchBreakIndex,
    eveningBreakIndex = eveningBreakIndex,
)

/**
 * 本周实际需要渲染到的节数：裁掉尾部整周都无课的节次。
 *
 * 本周一门课都没有时保持完整节数，沿用既有观感，也避免空课表只剩一行空白。
 */
internal fun trimmedSectionCount(courses: List<Course>, totalSections: Int): Int {
    val lastUsed = courses.maxOfOrNull { it.endSection } ?: return totalSections
    return lastUsed.coerceIn(1, totalSections)
}

/**
 * 右下角 Speed Dial 是否真的压住了课程卡。
 *
 * 只覆盖「最后两节 × 最右两列」这一小块区域，只有它真的有课时才让出高度，
 * 否则白白缩掉 72dp 划不来。
 */
internal fun needsFabReserve(courses: List<Course>, sectionCount: Int, daysToShow: Int): Boolean {
    if (sectionCount <= 0 || daysToShow <= 0) return false
    val minSection = sectionCount - 1
    // 上界同样必要：隐藏周末时第 6、7 天的课不渲染，自然也谈不上被 Speed Dial 遮住
    val minDay = daysToShow - 1
    return courses.any { it.endSection >= minSection && it.dayOfWeek in minDay..daysToShow }
}

/** 左侧时间栏宽度：每列过窄时收窄，把宽度让给课程列 */
internal fun timeRailWidthDp(availableWidthDp: Float, daysToShow: Int, adaptive: Boolean): Float {
    if (!adaptive || daysToShow <= 0) return TIME_RAIL_WIDTH_DP
    val perDay = (availableWidthDp - TIME_RAIL_WIDTH_DP) / daysToShow
    return if (perDay < NARROW_DAY_WIDTH_DP) TIME_RAIL_COMPACT_WIDTH_DP else TIME_RAIL_WIDTH_DP
}

/** 字号随缩放缩小，但不低于可读下限 */
internal fun adaptiveFontSp(baseSp: Float, scale: Float, minSp: Float): Float =
    maxOf(baseSp * scale, minSp)