package com.wofanmo.course_schedule.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.DEFAULT_SECTION_TIMES
import com.wofanmo.course_schedule.data.model.matchesWeek
import com.wofanmo.course_schedule.ui.components.AnimatedListItem
import com.wofanmo.course_schedule.ui.components.FloatingBookAnimation
import com.wofanmo.course_schedule.ui.components.TopBar
import com.wofanmo.course_schedule.ui.theme.CourseColorPalette

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
            course.dayOfWeek == currentDayOfWeek && course.matchesWeek(currentWeek)
        }?.sortedBy { it.startSection } ?: emptyList()
    }

    // 滚动状态
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemScrollOffset > 0 || listState.firstVisibleItemIndex > 0 }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部栏（带滚动反馈）
        TopBar(
            title = "今日课程",
            subtitle = "第 $currentWeek 周 · ${getDayOfWeekName(currentDayOfWeek)}",
            isScrolled = isScrolled
        )

        if (todayCourses.isEmpty()) {
            // 空状态：漂浮书本动画 + 鼓励文字
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FloatingBookAnimation()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "今天没有课程 🎉",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "好好休息，为明天的学习养精蓄锐",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(todayCourses) { index, course ->
                    AnimatedListItem(index = index) {
                        CourseCard(course, index, DEFAULT_SECTION_TIMES)
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    index: Int = 0,
    sectionTimes: List<com.wofanmo.course_schedule.data.model.SectionTime> = emptyList(),
) {
    // 使用马卡龙色系或用户自定义颜色
    val accentColor = if (course.color.isNotEmpty()) {
        parseColor(course.color) ?: CourseColorPalette[index % CourseColorPalette.size]
    } else {
        CourseColorPalette[index % CourseColorPalette.size]
    }

    // 交互反馈：涟漪 + 缩放
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150, easing = EaseOut),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = { /* TODO: 课程详情 */ }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧课程色带
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .background(accentColor)
            )

            // 主要内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Text(
                    text = course.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 课程时间徽章（显示时间范围，未配置时回退显示节次）
                    val startTime = sectionTimes.getOrNull(course.startSection - 1)?.start
                    val endTime = sectionTimes.getOrNull(course.endSection - 1)?.end
                    val timeText = if (startTime != null && endTime != null) {
                        "$startTime-$endTime"
                    } else {
                        "第${course.startSection}-${course.endSection}节"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = timeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = course.teacher,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 右侧地点
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)

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
