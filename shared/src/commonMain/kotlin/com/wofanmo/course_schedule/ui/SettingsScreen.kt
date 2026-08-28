package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wofanmo.course_schedule.AppEvents
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Schedule
import com.wofanmo.course_schedule.ui.components.AnimatedListItem
import com.wofanmo.course_schedule.ui.components.ExpandableSettingItem
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var config by remember { mutableStateOf(AppSettings.settingsStorage.getConfig()) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "设置",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 课程表选择 + 删除
        item {
            AnimatedListItem(index = 0) {
                ExpandableSettingItem(
                    title = "课程表管理",
                    expanded = expandedSection == "schedule",
                    onToggle = {
                        expandedSection = if (expandedSection == "schedule") null else "schedule"
                    }
                ) {
                    val dataVersion = AppEvents.scheduleVersion.intValue
                    val schedules = remember(dataVersion) { AppSettings.scheduleStorage.getAll() }
                    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }
                    var datePickTarget by remember { mutableStateOf<Schedule?>(null) }

                    if (schedules.isEmpty()) {
                        Text(
                            text = "暂无课程表，可通过教务导入或手动添加课程",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        schedules.forEach { schedule ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 课表名称 + 选中标记
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                config = config.copy(currentScheduleId = schedule.id)
                                                AppSettings.settingsStorage.saveConfig(config)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = schedule.name,
                                            fontSize = 16.sp,
                                            fontWeight = if (schedule.id == config.currentScheduleId) FontWeight.Bold else FontWeight.Normal,
                                            color = if (schedule.id == config.currentScheduleId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (schedule.id == config.currentScheduleId) {
                                            Text(
                                                text = "✓",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    // 删除按钮
                                    Surface(
                                        onClick = { scheduleToDelete = schedule },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // 开学日期行（点击弹出日期选择器单独修改）
                                Row(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { datePickTarget = schedule }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "开学日期",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = schedule.startDate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改开学日期",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 删除确认对话框
                    scheduleToDelete?.let { target ->
                        AlertDialog(
                            onDismissRequest = { scheduleToDelete = null },
                            title = { Text("删除课程表") },
                            text = {
                                Text(
                                    "确定删除「${target.name}」吗？\n共 ${target.courses.size} 门课程，删除后无法恢复。",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        AppSettings.scheduleStorage.delete(target.id)
                                        // 删除的是当前课表 → 自动切换到剩余第一个
                                        if (target.id == config.currentScheduleId) {
                                            val remaining = AppSettings.scheduleStorage.getAll()
                                            config = config.copy(
                                                currentScheduleId = remaining.firstOrNull()?.id ?: ""
                                            )
                                            AppSettings.settingsStorage.saveConfig(config)
                                        }
                                        scheduleToDelete = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("删除")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { scheduleToDelete = null }) {
                                    Text("取消")
                                }
                            }
                        )
                    }

                    // 开学日期选择对话框
                    datePickTarget?.let { target ->
                        ScheduleDatePickerDialog(
                            initialDate = target.startDate,
                            onConfirm = { newDate ->
                                AppSettings.scheduleStorage.save(target.copy(startDate = newDate))
                                datePickTarget = null
                            },
                            onDismiss = { datePickTarget = null }
                        )
                    }
                }
            }
        }

        // 显示设置
        item {
            AnimatedListItem(index = 1) {
                ExpandableSettingItem(
                    title = "显示设置",
                    expanded = expandedSection == "display",
                    onToggle = {
                        expandedSection = if (expandedSection == "display") null else "display"
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "显示周末", fontSize = 16.sp)
                        Switch(
                            checked = config.showWeekends,
                            onCheckedChange = {
                                config = config.copy(showWeekends = it)
                                AppSettings.settingsStorage.saveConfig(config)
                            }
                        )
                    }
                }
            }
        }

        // 课程提醒
        item {
            AnimatedListItem(index = 2) {
                ExpandableSettingItem(
                    title = "课程提醒",
                    expanded = expandedSection == "reminder",
                    onToggle = {
                        expandedSection = if (expandedSection == "reminder") null else "reminder"
                    }
                ) {
                    val leadOptions = listOf(
                        0 to "关闭",
                        5 to "提前 5 分钟",
                        10 to "提前 10 分钟",
                        15 to "提前 15 分钟",
                        30 to "提前 30 分钟",
                    )
                    leadOptions.forEach { (value, label) ->
                        val selected = config.notifyLeadMinutes == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val wasOff = config.notifyLeadMinutes == 0
                                    config = config.copy(notifyLeadMinutes = value)
                                    AppSettings.settingsStorage.saveConfig(config)
                                    if (wasOff && value > 0) requestNotificationPermission()
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    val wasOff = config.notifyLeadMinutes == 0
                                    config = config.copy(notifyLeadMinutes = value)
                                    AppSettings.settingsStorage.saveConfig(config)
                                    if (wasOff && value > 0) requestNotificationPermission()
                                }
                            )
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val notifStatus = notificationPermissionStatus()
                    PermissionRow(
                        label = "通知权限",
                        statusText = when (notifStatus) {
                            PermissionStatus.GRANTED -> "已授权"
                            PermissionStatus.DENIED -> "未授权，点击授权"
                            PermissionStatus.NOT_APPLICABLE -> "无需授权"
                        },
                        needsGrant = notifStatus == PermissionStatus.DENIED,
                        onClick = { openNotificationPermissionSettings() },
                    )
                    PermissionRow(
                        label = "闹钟与提醒",
                        statusText = if (exactAlarmPermissionGranted()) "已授权" else "未授权，点击授权",
                        needsGrant = !exactAlarmPermissionGranted(),
                        onClick = { openExactAlarmPermissionSettings() },
                    )
                    PermissionRow(
                        label = "勿扰访问（静音需要）",
                        statusText = if (dndAccessGranted()) "已授权" else "未授权，点击授权",
                        needsGrant = !dndAccessGranted(),
                        onClick = { openDndPermissionSettings() },
                    )
                }
            }
        }

        // 主题设置
        item {
            AnimatedListItem(index = 3) {
                ExpandableSettingItem(
                    title = "主题",
                    expanded = expandedSection == "theme",
                    onToggle = {
                        expandedSection = if (expandedSection == "theme") null else "theme"
                    }
                ) {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, label) ->
                        val selected = config.theme == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    config = config.copy(theme = value)
                                    AppSettings.settingsStorage.saveConfig(config)
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    config = config.copy(theme = value)
                                    AppSettings.settingsStorage.saveConfig(config)
                                }
                            )
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 关于
        item {
            AnimatedListItem(index = 4) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "版本",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "课程表 v1.0.0",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** 权限状态行：左侧名称，右侧状态文字；未授权时可点击跳转系统设置。 */
@Composable
private fun PermissionRow(
    label: String,
    statusText: String,
    needsGrant: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (needsGrant) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            )
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = statusText,
            fontSize = 13.sp,
            color = if (needsGrant) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 开学日期选择对话框（Material3 DatePicker），确认后回传 yyyy-MM-dd。
 * DatePicker 内部用 UTC 毫秒表示所选日期（UTC 零点），转换时必须用 UTC，否则时区偏移会导致日期差一天。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDatePickerDialog(
    initialDate: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = try {
        LocalDate.parse(initialDate).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onConfirm(date.toString()) // ISO 格式 yyyy-MM-dd
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(
            state = dateState,
            title = {
                Text(
                    text = "选择开学日期",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
    }
}
