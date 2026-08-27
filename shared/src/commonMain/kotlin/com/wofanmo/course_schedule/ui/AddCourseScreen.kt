package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppEvents
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.Schedule
import com.wofanmo.course_schedule.data.model.WeekParity
import com.wofanmo.course_schedule.data.model.matchesWeek
import com.wofanmo.course_schedule.data.model.newId
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * 时间冲突检测：同星期、节次区间相交、周次有交集（兼容 weeks 列表与范围+单双周）。
 * 若新课程定义了精确周列表，则只检查这些周是否与已有课程重合。
 */
private fun findConflicts(existing: List<Course>, new: Course): List<Course> {
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
 * 手动添加课程（全屏覆盖层）。课程写入当前课表；时间冲突直接禁止保存。
 * 周次支持精确多选：点击切换单周、滑动连续多选、快捷按钮（全部/单周/双周）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(onClose: () -> Unit) {
    val version = AppEvents.scheduleVersion.intValue
    val config = remember(version) { AppSettings.settingsStorage.getConfig() }
    val schedule = remember(version) { AppSettings.scheduleStorage.getById(config.currentScheduleId) }
    val totalWeeks = schedule?.totalWeeks ?: 20

    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startSection by remember { mutableIntStateOf(1) }
    var endSection by remember { mutableIntStateOf(2) }
    // 周次多选：默认全选（等价于「每周」）
    var selectedWeeks by remember { mutableStateOf<Set<Int>>((1..totalWeeks).toSet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun buildCourse(): Course {
        val sorted = selectedWeeks.sorted()
        val minWeek = sorted.firstOrNull() ?: 1
        val maxWeek = sorted.lastOrNull() ?: 1
        // 根据选择模式推导单双周（仅用于兜底字段，实际生效的是 weeks 列表）
        val parity = when {
            selectedWeeks.isEmpty() -> WeekParity.EVERY
            selectedWeeks.size == (totalWeeks + 1) / 2 && sorted.all { it % 2 == 1 } -> WeekParity.ODD
            selectedWeeks.size == totalWeeks / 2 && sorted.all { it % 2 == 0 } -> WeekParity.EVEN
            else -> WeekParity.EVERY
        }
        return Course(
            id = newId(),
            name = name.trim(),
            teacher = teacher.trim(),
            location = location.trim(),
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection,
            startWeek = minWeek,
            endWeek = maxWeek,
            weekParity = parity,
            weeks = sorted,
        )
    }

    fun saveInternal() {
        val newCourse = buildCourse()
        val current = schedule
        if (current != null) {
            AppSettings.scheduleStorage.save(current.copy(courses = current.courses + newCourse))
        } else {
            // 尚无课表：创建默认课表，开学日期定位到本周周一
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
            val created = Schedule(
                id = newId(),
                name = "我的课表",
                startDate = monday.toString(),
                totalWeeks = totalWeeks,
                courses = listOf(newCourse),
            )
            AppSettings.scheduleStorage.save(created)
            AppSettings.settingsStorage.saveConfig(config.copy(currentScheduleId = created.id))
        }
        onClose()
    }

    fun save() {
        runCatching { saveInternal() }.onFailure { e ->
            errorMessage = "保存失败：${e.message ?: e::class.simpleName}"
        }
    }

    fun attemptSave() {
        errorMessage = when {
            name.isBlank() -> "请填写课程名称"
            startSection > endSection -> "开始节次不能大于结束节次"
            selectedWeeks.isEmpty() -> "请至少选择一周"
            else -> null
        }
        if (errorMessage != null) return
        // 直接禁止时间冲突：显示冲突详情，不保存
        val found = findConflicts(schedule?.courses ?: emptyList(), buildCourse())
        if (found.isNotEmpty()) {
            errorMessage = buildString {
                append("与已有课程时间冲突，无法添加：\n")
                found.forEach { c ->
                    val weekText = if (c.weeks.isNotEmpty()) {
                        "共 ${c.weeks.size} 周"
                    } else {
                        "第 ${c.startWeek}-${c.endWeek} 周"
                    }
                    append("《${c.name}》${DAY_NAMES[c.dayOfWeek - 1]} 第 ${c.startSection}-${c.endSection} 节（$weekText）\n")
                }
            }
        } else {
            save()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("手动添加课程") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                IntDropdown(
                    label = "星期",
                    value = dayOfWeek,
                    range = 1..7,
                    itemLabel = { DAY_NAMES[it - 1] },
                    onValueChange = { dayOfWeek = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IntDropdown(
                        label = "开始节次",
                        value = startSection,
                        range = 1..config.totalSections,
                        itemLabel = { "第 $it 节" },
                        onValueChange = {
                            startSection = it
                            if (endSection < it) endSection = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IntDropdown(
                        label = "结束节次",
                        value = endSection,
                        range = 1..config.totalSections,
                        itemLabel = { "第 $it 节" },
                        onValueChange = {
                            endSection = it
                            if (startSection > it) startSection = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 周次选择：精确多选 + 滑动连续 + 快捷按钮
                Text(
                    text = "上课周次（已选 ${selectedWeeks.size} 周）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                WeekSelector(
                    totalWeeks = totalWeeks,
                    selectedWeeks = selectedWeeks,
                    onSelectedChange = { selectedWeeks = it }
                )

                // 快捷选择按钮
                Text("快捷选择", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = { selectedWeeks = (1..totalWeeks).toSet() },
                        label = { Text("每周") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedWeeks = (1..totalWeeks).filter { it % 2 == 1 }.toSet() },
                        label = { Text("仅单周") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet() },
                        label = { Text("仅双周") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedWeeks = emptySet() },
                        label = { Text("清空") }
                    )
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = ::attemptSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            }
        }
    }

}

/**
 * 周次多选面板：
 * - 点击单元格切换选中/取消
 * - 按住并拖动可快速连续选择/取消
 * - 选中高亮为主色胶囊
 */
@Composable
private fun WeekSelector(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = 7
    val gap = 8.dp
    // rememberUpdatedState：保证拖拽协程内每次读取的都是最新选中集合（避免 stale closure）
    val latestSelected by rememberUpdatedState(selectedWeeks)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(totalWeeks) {
                // 基于实际布局计算格子尺寸（自适应屏幕宽度，避免溢出错位）
                val gapPx = gap.toPx()
                val cellW = ((size.width - (columns - 1) * gapPx) / columns)
                    .coerceAtLeast(1f)
                val step = cellW + gapPx

                fun weekAt(x: Float, y: Float): Int? {
                    val col = (x / step).toInt()
                    val row = (y / step).toInt()
                    val week = row * columns + col + 1
                    return if (week in 1..totalWeeks) week else null
                }

                // 拖拽连续选择：起点决定模式（起点已选中→拖动取消，未选中→拖动选择）
                var adding = true
                var lastWeek = -1

                detectDragGestures(
                    onDragStart = { offset ->
                        val week = weekAt(offset.x, offset.y)
                        if (week != null) {
                            adding = week !in latestSelected
                            lastWeek = week
                            onSelectedChange(
                                if (adding) latestSelected + week else latestSelected - week
                            )
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val week = weekAt(change.position.x, change.position.y)
                        if (week != null && week != lastWeek) {
                            lastWeek = week
                            onSelectedChange(
                                if (adding) latestSelected + week else latestSelected - week
                            )
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        val rows = (totalWeeks + columns - 1) / columns
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                for (col in 0 until columns) {
                    val week = row * columns + col + 1
                    if (week > totalWeeks) {
                        // 占位，保持格子对齐
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val selected = week in selectedWeeks
                        Surface(
                            onClick = {
                                onSelectedChange(
                                    if (selected) selectedWeeks - week else selectedWeeks + week
                                )
                            },
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$week",
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntDropdown(
    label: String,
    value: Int,
    range: IntRange,
    itemLabel: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = itemLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { v ->
                DropdownMenuItem(
                    text = { Text(itemLabel(v)) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
