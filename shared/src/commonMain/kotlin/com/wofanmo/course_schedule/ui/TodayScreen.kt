package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course

// 获取当前日期信息（简化版，实际应该使用 kotlinx-datetime）
data class DateInfo(val year: Int, val month: Int, val day: Int, val dayOfWeek: Int)

expect fun getCurrentDate(): DateInfo
expect fun parseColor(colorString: String): Color?
expect fun calculateWeeksBetween(startDate: String, currentDate: DateInfo): Int

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    val config = remember { AppSettings.settingsStorage.getConfig() }
    val schedule = remember { AppSettings.scheduleStorage.getById(config.currentScheduleId) }
    val today = remember { getCurrentDate() }
    val currentDayOfWeek = today.dayOfWeek // 1=周一, 7=周日

    // 计算当前是第几周
    val currentWeek = remember {
        if (schedule != null) {
            calculateWeeksBetween(schedule.startDate, today).coerceIn(1, schedule.totalWeeks)
        } else {
            1
        }
    }

    // 获取今日课程
    val todayCourses = remember(schedule, currentWeek) {
        schedule?.courses?.filter { course ->
            course.dayOfWeek == currentDayOfWeek &&
            course.startWeek <= currentWeek &&
            course.endWeek >= currentWeek
        }?.sortedBy { it.startSection } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "今日课程",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "第 $currentWeek 周 · ${getDayOfWeekName(currentDayOfWeek)}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (todayCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今天没有课程 🎉",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(todayCourses) { course ->
                    CourseCard(course)
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course) {
    val backgroundColor = if (course.color.isNotEmpty()) {
        parseColor(course.color) ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.teacher,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "第${course.startSection}-${course.endSection}节",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun getDayOfWeekName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }
}
