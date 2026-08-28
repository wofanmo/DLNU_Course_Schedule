package com.wofanmo.course_schedule.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.MainActivity
import com.wofanmo.course_schedule.data.model.DEFAULT_SECTION_TIMES
import com.wofanmo.course_schedule.data.model.matchesWeek
import com.wofanmo.course_schedule.data.storage.initStorage
import com.wofanmo.course_schedule.ui.parseColor
import com.wofanmo.course_schedule.ui.theme.CourseColorPalette
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

/** 小部件展示用的一门今日剩余课程 */
private data class WidgetCourse(
    val name: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val sectionLabel: String,
    val color: Color,
)

/** 小部件整体数据：日期标签 + 剩余课程列表（或空状态文案） */
private data class WidgetData(
    val dateLabel: String,
    val courses: List<WidgetCourse>,
    val emptyMessage: String?,
)

/** 今日剩余课程桌面小部件 */
class TodayCoursesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadRemainingCourses(context)
        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }

    /** 读取存储中的当前课表，计算今日尚未结束的课程 */
    private fun loadRemainingCourses(context: Context): WidgetData {
        initStorage(context)

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        val dateLabel = "${today.monthNumber}/${today.dayOfMonth} ${dayOfWeekName(today.dayOfWeek.isoDayNumber)}"

        val config = AppSettings.settingsStorage.getConfig()
        val schedule = AppSettings.scheduleStorage.getById(config.currentScheduleId)
            ?: return WidgetData(dateLabel, emptyList(), "暂无课程表\n点击打开应用导入")

        // 当前周次：开学日期到今天经过的完整周数 + 1
        val week = try {
            val start = LocalDate.parse(schedule.startDate)
            (start.until(today, DateTimeUnit.DAY) / 7) + 1
        } catch (e: Exception) {
            1
        }.coerceIn(1, schedule.totalWeeks)

        val dayOfWeek = today.dayOfWeek.isoDayNumber
        val nowMinutes = now.hour * 60 + now.minute

        val courses = schedule.courses
            .filter { it.dayOfWeek == dayOfWeek && it.matchesWeek(week) }
            // 剩余课程：当前时间早于课程最后一节的下课时间
            .filter { course ->
                val end = DEFAULT_SECTION_TIMES.getOrNull(course.endSection - 1)?.end
                end != null && end.toMinutesOfDay() > nowMinutes
            }
            .sortedBy { it.startSection }
            .mapIndexed { index, course ->
                val startTime = DEFAULT_SECTION_TIMES.getOrNull(course.startSection - 1)?.start ?: ""
                val endTime = DEFAULT_SECTION_TIMES.getOrNull(course.endSection - 1)?.end ?: ""
                val accentColor = if (course.color.isNotEmpty()) {
                    parseColor(course.color) ?: CourseColorPalette[index % CourseColorPalette.size]
                } else {
                    CourseColorPalette[index % CourseColorPalette.size]
                }
                WidgetCourse(
                    name = course.name,
                    location = course.location,
                    startTime = startTime,
                    endTime = endTime,
                    sectionLabel = "第 ${course.startSection}-${course.endSection} 节",
                    color = accentColor,
                )
            }

        val emptyMessage = if (courses.isEmpty()) "今天的课程都结束啦 🎉" else null
        return WidgetData(dateLabel, courses, emptyMessage)
    }

    private fun dayOfWeekName(day: Int): String =
        listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(day - 1) { "" }

    private fun String.toMinutesOfDay(): Int {
        val parts = split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(data: WidgetData) {
    val context = androidx.glance.LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(
                actionStartActivity(android.content.Intent(context, MainActivity::class.java))
            )
    ) {
        // 标题行：标题 + 日期
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日剩余课程",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = data.dateLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        if (data.courses.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.emptyMessage ?: "暂无课程",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(data.courses) { course ->
                    WidgetCourseRow(course)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetCourseRow(course: WidgetCourse) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间列：开始/结束时间 + 节次
        Column(modifier = GlanceModifier.width(58.dp)) {
            Text(
                text = course.startTime,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = "~${course.endTime}",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
            Text(
                text = course.sectionLabel,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
        // 课程色条
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(40.dp)
                .cornerRadius(2.dp)
                .background(ColorProvider(day = course.color, night = course.color))
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        // 课程名 + 地点
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = course.name,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
            if (course.location.isNotEmpty()) {
                Text(
                    text = course.location,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
