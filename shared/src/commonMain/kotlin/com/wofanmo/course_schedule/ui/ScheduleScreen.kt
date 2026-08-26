package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val config = remember { AppSettings.settingsStorage.getConfig() }
    val schedule = remember { AppSettings.scheduleStorage.getById(config.currentScheduleId) }
    val today = remember { getCurrentDate() }

    // 计算当前是第几周
    val currentWeek = remember {
        if (schedule != null) {
            calculateWeeksBetween(schedule.startDate, today).coerceIn(1, schedule.totalWeeks)
        } else {
            1
        }
    }

    val totalWeeks = schedule?.totalWeeks ?: 20

    // 创建 PagerState，初始页为当前周
    val pagerState = rememberPagerState(
        initialPage = currentWeek - 1,
        pageCount = { totalWeeks }
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = schedule?.name ?: "课程表",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "第 ${pagerState.currentPage + 1} 周",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 星期标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // 节次列标题
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "节",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 周一到周日
            val daysToShow = if (config.showWeekends) 7 else 5
            for (day in 1..daysToShow) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getDayOfWeekName(day),
                        fontSize = 14.sp,
                        fontWeight = if (day == today.dayOfWeek) FontWeight.Bold else FontWeight.Normal,
                        color = if (day == today.dayOfWeek) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        HorizontalDivider()

        // 课程表网格
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val weekNumber = page + 1
            ScheduleGrid(
                schedule = schedule,
                week = weekNumber,
                totalSections = config.totalSections,
                showWeekends = config.showWeekends
            )
        }
    }
}

@Composable
fun ScheduleGrid(
    schedule: com.wofanmo.course_schedule.data.model.Schedule?,
    week: Int,
    totalSections: Int,
    showWeekends: Boolean
) {
    val daysToShow = if (showWeekends) 7 else 5

    // 获取本周的课程
    val weekCourses = remember(schedule, week) {
        schedule?.courses?.filter { course ->
            course.startWeek <= week && course.endWeek >= week
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        for (section in 1..totalSections) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                // 节次标签
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 每天的课程格子
                for (day in 1..daysToShow) {
                    val coursesInCell = weekCourses.filter { course ->
                        course.dayOfWeek == day &&
                        course.startSection <= section &&
                        course.endSection >= section
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                    ) {
                        if (coursesInCell.isNotEmpty()) {
                            val course = coursesInCell.first()
                            // 只在课程的开始节次显示
                            if (course.startSection == section) {
                                CourseCell(
                                    course = course,
                                    spanSections = course.endSection - course.startSection + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCell(course: Course, spanSections: Int) {
    val backgroundColor = if (course.color.isNotEmpty()) {
        parseColor(course.color) ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = course.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
            if (spanSections > 1) {
                Text(
                    text = course.location,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
