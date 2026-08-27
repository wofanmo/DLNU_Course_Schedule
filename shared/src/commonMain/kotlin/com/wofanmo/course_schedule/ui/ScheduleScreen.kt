package com.wofanmo.course_schedule.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppEvents
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.DEFAULT_SECTION_TIMES
import com.wofanmo.course_schedule.data.model.eveningBreakIndex
import com.wofanmo.course_schedule.data.model.lunchBreakIndex
import com.wofanmo.course_schedule.data.model.matchesWeek
import com.wofanmo.course_schedule.data.model.totalSectionCount
import com.wofanmo.course_schedule.ui.theme.CourseColorPalette
import kotlin.math.abs

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    // 订阅数据版本号：添加课程/导入课表后自动刷新
    val dataVersion = AppEvents.scheduleVersion.intValue
    val config = remember(dataVersion) { AppSettings.settingsStorage.getConfig() }
    val schedule = remember(dataVersion) { AppSettings.scheduleStorage.getById(config.currentScheduleId) }
    val today = remember { getCurrentDate() }

    // 计算初始周次
    val initialWeek = remember(schedule) {
        if (schedule != null) {
            calculateWeeksBetween(schedule.startDate, today).coerceIn(1, schedule.totalWeeks)
        } else {
            1
        }
    }

    // 可变周次状态，支持切换；课表切换时重置为初始周
    var currentWeek by remember(schedule?.id) { mutableStateOf(initialWeek) }
    val totalWeeks = schedule?.totalWeeks ?: 20

    // 周次选择器状态
    var showWeekPicker by remember { mutableStateOf(false) }

    // 课程详情弹窗状态
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部标题栏 - 带周次切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = schedule?.name ?: "课程表",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalWeeks > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "共 $totalWeeks 周",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 周次显示 - 点击弹出选择器（胶囊按钮样式）
            Surface(
                onClick = { showWeekPicker = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.pointerInput(currentWeek, totalWeeks) {
                    detectTapGestures { }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 $currentWeek 周",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "切换周次",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 星期标题行 - 显示具体日期（带缓存）
        WeekDayHeader(
            schedule = schedule,
            currentWeek = currentWeek,
            today = today,
            showWeekends = config.showWeekends
        )

        HorizontalDivider()

        // 课程表网格 - 支持滑动手势
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentWeek, totalWeeks) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            // 滑动阈值：超过 100px 触发切换
                            if (abs(totalDrag) > 100) {
                                if (totalDrag > 0 && currentWeek > 1) {
                                    // 向右滑 - 上一周
                                    currentWeek--
                                } else if (totalDrag < 0 && currentWeek < totalWeeks) {
                                    // 向左滑 - 下一周
                                    currentWeek++
                                }
                            }
                        }
                    )
                }
        ) {
            ScheduleGrid(
                schedule = schedule,
                week = currentWeek,
                totalSections = config.totalSectionCount(),
                showWeekends = config.showWeekends,
                sectionTimes = DEFAULT_SECTION_TIMES,
                lunchBreakIndex = config.lunchBreakIndex(),
                eveningBreakIndex = config.eveningBreakIndex(),
                onCourseClick = { selectedCourse = it }
            )
        }
    }

    // 课程详情底部弹窗（带动画展开）
    selectedCourse?.let { course ->
        CourseDetailSheet(
            course = course,
            week = currentWeek,
            sectionTimes = DEFAULT_SECTION_TIMES,
            onDismiss = { selectedCourse = null }
        )
    }

    // 周次选择器对话框
    if (showWeekPicker) {
        WeekPickerDialog(
            currentWeek = currentWeek,
            totalWeeks = totalWeeks,
            onWeekSelected = { week ->
                currentWeek = week
                showWeekPicker = false
            },
            onDismiss = { showWeekPicker = false }
        )
    }
}

@Composable
fun WeekPickerDialog(
    currentWeek: Int,
    totalWeeks: Int,
    onWeekSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择周次",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..totalWeeks).toList()) { week ->
                    Surface(
                        onClick = { onWeekSelected(week) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (week == currentWeek) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第 $week 周",
                                fontSize = 16.sp,
                                fontWeight = if (week == currentWeek) FontWeight.Bold else FontWeight.Normal,
                                color = if (week == currentWeek) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (week == currentWeek) {
                                Text(
                                    text = "✓",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun WeekDayHeader(
    schedule: com.wofanmo.course_schedule.data.model.Schedule?,
    currentWeek: Int,
    today: DateInfo,
    showWeekends: Boolean
) {
    val daysToShow = if (showWeekends) 7 else 5

    // 缓存日期计算结果
    val weekDates = remember(schedule?.startDate, currentWeek, daysToShow) {
        if (schedule?.startDate != null) {
            (1..daysToShow).map { day ->
                getDateForWeekDay(schedule.startDate, currentWeek, day)
            }
        } else {
            emptyList()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // 节次列标题
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "节",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 周一到周日 - 显示日期
        for (day in 1..daysToShow) {
            val dateForDay = weekDates.getOrNull(day - 1)
            val isToday = dateForDay?.let {
                it.year == today.year && it.month == today.month && it.day == today.day
            } ?: false

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                // 今天是主色胶囊背景
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = getDayOfWeekName(day),
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        if (dateForDay != null) {
                            Text(
                                text = "${dateForDay.day}",
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                color = if (isToday) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleGrid(
    schedule: com.wofanmo.course_schedule.data.model.Schedule?,
    week: Int,
    totalSections: Int,
    showWeekends: Boolean,
    sectionTimes: List<com.wofanmo.course_schedule.data.model.SectionTime> = emptyList(),
    lunchBreakIndex: Int = 3,
    eveningBreakIndex: Int = 7,
    onCourseClick: (Course) -> Unit = {},
) {
    val daysToShow = if (showWeekends) 7 else 5
    val sectionHeight = 60.dp
    val breakHeight = 32.dp

    // 预计算本周课程（只保留在本周有效的）
    val weekCourses = remember(schedule, week) {
        schedule?.courses?.filter { course ->
            course.matchesWeek(week)
        } ?: emptyList()
    }

    // 稳定的颜色映射 - 基于课程名称的 hash
    val courseColorMap by remember(schedule) {
        derivedStateOf {
            schedule?.courses?.distinctBy { it.name }?.associate { course ->
                course.name to (abs(course.name.hashCode()) % CourseColorPalette.size)
            } ?: emptyMap()
        }
    }

    // 每行的起始 Y 偏移（累计：节次行 + 分隔线）
    val rowOffsets = remember(totalSections, lunchBreakIndex, eveningBreakIndex) {
        val offsets = IntArray(totalSections)
        var y = 0
        for (i in 0 until totalSections) {
            offsets[i] = y
            y += sectionHeight.value.toInt()
            if (i == lunchBreakIndex && totalSections > lunchBreakIndex + 1) {
                y += breakHeight.value.toInt()
            }
            if (i == eveningBreakIndex && totalSections > eveningBreakIndex + 1) {
                y += breakHeight.value.toInt()
            }
        }
        offsets
    }

    // 整表总高度（底部留 8dp padding）
    val totalGridHeight = remember(rowOffsets) {
        (rowOffsets.lastOrNull() ?: 0) + sectionHeight.value.toInt() + 16
    }

    // 定位卡片：只绘制每门课在起始节的卡片，用 offset(y) 撑满跨节高度
    val cards = remember(weekCourses, rowOffsets, totalSections) {
        weekCourses.mapNotNull { course ->
            val startIdx = course.startSection - 1
            if (startIdx !in rowOffsets.indices) return@mapNotNull null
            val endIdx = minOf(course.endSection - 1, totalSections - 1)
            Triple(
                course,
                rowOffsets[startIdx],
                rowOffsets[endIdx] - rowOffsets[startIdx] + sectionHeight.value.toInt()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        // 网格容器：绝对定位所有课程卡片
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalGridHeight.dp)
        ) {
            val dayWidth = (maxWidth - 56.dp) / daysToShow

            // 逐行绘制节次标签
            for (section in 1..totalSections) {
                val sectionIndex = section - 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sectionHeight)
                        .offset(y = rowOffsets[sectionIndex].dp)
                ) {
                    // 节次标签：节次号 + 起止时间
                    val time = sectionTimes.getOrNull(sectionIndex)
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = section.toString(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (time != null) {
                                Text(
                                    text = "${time.start}-${time.end}",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // 课程区域占位（透明，卡片由绝对定位层绘制）
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 午休/晚休分隔线
            if (lunchBreakIndex in 0 until totalSections - 1) {
                BreakDivider(
                    text = "午休",
                    offsetY = rowOffsets[lunchBreakIndex] + sectionHeight.value.toInt()
                )
            }
            if (eveningBreakIndex in 0 until totalSections - 1) {
                BreakDivider(
                    text = "晚休",
                    offsetY = rowOffsets[eveningBreakIndex] + sectionHeight.value.toInt()
                )
            }

            // 同天同节次多课程水平平分宽度，避免重叠
            val grouped = remember(cards) {
                // key = (day, yStart, height) —— 同天同起止位置的课程归为一组
                cards.groupBy { (c, y, h) -> Triple(c.dayOfWeek, y, h) }
            }

            // 绘制课程卡片（绝对定位，跨节撑满高度）
            grouped.forEach { (key, group) ->
                val day = key.first
                if (day !in 1..daysToShow) return@forEach
                val yOffset = key.second
                val height = key.third
                val count = group.size
                val cellWidth = dayWidth / count   // 同格多课平分宽度

                group.forEachIndexed { idx, (course, _, _) ->
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(height.dp)
                            .offset(
                                x = 56.dp + dayWidth * (day - 1) + cellWidth * idx,
                                y = yOffset.dp
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        CourseCell(
                            course = course,
                            spanSections = course.endSection - course.startSection + 1,
                            colorIndex = courseColorMap[course.name] ?: 0,
                            onClick = { onCourseClick(course) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCell(course: Course, spanSections: Int, colorIndex: Int = 0, onClick: () -> Unit = {}) {
    // 使用自定义颜色或调色板颜色
    val backgroundColor = if (course.color.isNotEmpty()) {
        parseColor(course.color) ?: CourseColorPalette[colorIndex % CourseColorPalette.size]
    } else {
        CourseColorPalette[colorIndex % CourseColorPalette.size]
    }

    // 深色模式降低饱和度
    val cellColor = if (isSystemInDarkTheme()) {
        backgroundColor.copy(alpha = 0.85f)
    } else {
        backgroundColor
    }

    // 单节卡片高度有限：课程名最多 1 行，避免文字被裁切
    val singleSection = spanSections <= 1

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(10.dp),
        color = cellColor,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = course.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = if (singleSection) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
            if (!singleSection) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = course.location,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

/** 大节之间的休整分隔行（午休/晚休），横跨整行，通过 offsetY 绝对定位 */
@Composable
private fun BreakDivider(text: String, offsetY: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .offset(y = offsetY.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧节次列留空
        Spacer(Modifier.width(56.dp))
        // 分隔线 + 图标 + 文字
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        // 分隔文字（无背景，纯文字）
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * 课程详情底部弹窗：带展开动画，显示完整课程信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailSheet(
    course: Course,
    week: Int,
    sectionTimes: List<com.wofanmo.course_schedule.data.model.SectionTime>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = if (course.color.isNotEmpty()) {
        parseColor(course.color) ?: CourseColorPalette[abs(course.name.hashCode()) % CourseColorPalette.size]
    } else {
        CourseColorPalette[abs(course.name.hashCode()) % CourseColorPalette.size]
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // 顶部色带 + 关闭按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // 课程名
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(16.dp))

                // 详情信息
                DetailRow(icon = "📚", label = "节次", value = "第${course.startSection}-${course.endSection}节")
                DetailRow(icon = "👨‍🏫", label = "教师", value = course.teacher)
                DetailRow(icon = "📍", label = "地点", value = course.location)

                // 当前节次时间
                val startTime = sectionTimes.getOrNull(course.startSection - 1)
                if (startTime != null) {
                    Spacer(Modifier.height(8.dp))
                    DetailRow(
                        icon = "⏰",
                        label = "时间",
                        value = "${startTime.start} - ${
                            sectionTimes.getOrNull(course.endSection - 1)?.end ?: ""
                        }"
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            modifier = Modifier.width(32.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
