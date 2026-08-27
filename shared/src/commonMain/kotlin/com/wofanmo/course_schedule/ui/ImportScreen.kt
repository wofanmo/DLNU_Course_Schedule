package com.wofanmo.course_schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.jwgl.CampusOption
import com.wofanmo.course_schedule.data.jwgl.JinZhiClient
import com.wofanmo.course_schedule.data.jwgl.JinZhiParser
import com.wofanmo.course_schedule.data.jwgl.ParsedCourse
import com.wofanmo.course_schedule.data.jwgl.SemesterOption
import com.wofanmo.course_schedule.data.model.Account
import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.Schedule
import com.wofanmo.course_schedule.data.model.newId
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** 展示完整异常因果链（ExceptionInInitializerError 等包装异常的真正原因在 cause 里） */
private fun Throwable.fullChain(): String =
    generateSequence(this) { it.cause }
        .joinToString(" ← ") { "${it::class.simpleName}: ${it.message}" }

/** 导入流程的各个步骤 */
private sealed interface ImportStep {
    data object Login : ImportStep
    data class Working(val status: String) : ImportStep
    data class PickSemester(val semesters: List<SemesterOption>) : ImportStep
    data class ConfirmStartDate(
        val courses: List<ParsedCourse>,
        val untimedCourseNames: List<String>,
    ) : ImportStep
    data class Done(val courseCount: Int, val untimedCourseNames: List<String>) : ImportStep
    data class Failed(val message: String) : ImportStep
}

/**
 * 教务系统导入（全屏覆盖层）：
 * 登录 → 选择学期 → 遍历两校区抓取 → 确认开学日期 → 生成新课表快照。
 * 路线与语义见 docs/adr/0001、0002。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val client = remember { JinZhiClient() }
    DisposableEffect(Unit) { onDispose { client.close() } }

    var step by remember { mutableStateOf<ImportStep>(ImportStep.Login) }

    // 登录表单（已保存的账号自动填充）
    val savedAccount = remember { AppSettings.accountStorage.getAll().firstOrNull() }
    var username by remember { mutableStateOf(savedAccount?.username ?: "") }
    var password by remember { mutableStateOf(savedAccount?.password ?: "") }

    // 学期与校区（登录成功后填充）
    var campuses by remember { mutableStateOf<List<CampusOption>>(emptyList()) }
    var selectedSemesterId by remember { mutableStateOf("") }
    var scheduleName by remember { mutableStateOf("") }

    // 开学日期（默认本周周一）
    val defaultMondayMillis = remember {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    var startDateMillis by remember { mutableStateOf(defaultMondayMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var semesterTotalWeeks by remember { mutableStateOf<Int?>(null) }

    fun login() {
        scope.launch {
            step = ImportStep.Working("正在登录教务系统…")
            runCatching {
                client.login(username, password).getOrThrow()
                // 账号加密保存只是尽力而为，失败不应中断导入
                runCatching {
                    AppSettings.accountStorage.save(
                        Account(
                            id = username,
                            platform = "金智教务（大连民族大学）",
                            username = username,
                            password = password,
                            url = JinZhiClient.DEFAULT_BASE_URL,
                        )
                    )
                }
                step = ImportStep.Working("正在获取学期信息…")
                JinZhiParser.parse(client.fetchSchedulePage())
            }.onSuccess { page ->
                if (page.semesters.isEmpty()) {
                    step = ImportStep.Failed("未在课表页找到学期信息，教务系统页面可能已变更")
                    return@onSuccess
                }
                campuses = page.campuses
                val current = page.semesters.firstOrNull { it.isCurrent }
                    ?: page.semesters.first()
                selectedSemesterId = current.id
                scheduleName = "${current.label} 教务导入"
                step = ImportStep.PickSemester(page.semesters)
            }.onFailure { e ->
                step = ImportStep.Failed("登录失败：${e.fullChain()}")
            }
        }
    }

    fun startImport() {
        if (campuses.isEmpty()) {
            step = ImportStep.Failed("未能获取校区列表，教务系统页面可能已变更")
            return
        }
        scope.launch {
            step = ImportStep.Working("正在抓取课表…")
            runCatching {
                val allCourses = mutableListOf<ParsedCourse>()
                val untimedNames = mutableListOf<Pair<String, String>>() // 课程名 to 教师
                // 学生只属于一个校区：列表接口的校区参数必填，非本校区返回空，遍历两校区后合并
                for (campus in campuses) {
                    val timedJson = client.fetchCourseListJson(1, selectedSemesterId, campus.id)
                    allCourses += JinZhiParser.parseTimedCourses(timedJson)
                    val untimedJson = client.fetchCourseListJson(2, selectedSemesterId, campus.id)
                    untimedNames += JinZhiParser.parseUntimedEntries(untimedJson)
                }
                // 无课表名单在此部署中含全部选课记录，需剔除已有定时记录的课程
                val timedKeys = allCourses.map { it.name to it.teacher }.toSet()
                val deduped = allCourses.distinct()
                val untimedOnly = untimedNames.distinct()
                    .filter { it !in timedKeys }
                    .map { it.first }
                    .distinct()
                Triple(deduped, untimedOnly, client.fetchSemesterTotalWeeks(selectedSemesterId))
            }.onSuccess { (courses, untimed, totalWeeks) ->
                semesterTotalWeeks = totalWeeks
                step = ImportStep.ConfirmStartDate(courses, untimed)
            }.onFailure { e ->
                step = ImportStep.Failed("抓取课表失败：${e.fullChain()}")
            }
        }
    }

    fun confirmImport(courses: List<ParsedCourse>, untimed: List<String>) {
        val date = Instant.fromEpochMilliseconds(startDateMillis)
            .toLocalDateTime(TimeZone.UTC).date
        val schedule = Schedule(
            id = newId(),
            name = scheduleName.ifBlank { "教务导入" },
            startDate = date.toString(),
            totalWeeks = maxOf(semesterTotalWeeks ?: 20, courses.maxOfOrNull { it.endWeek } ?: 0),
            courses = courses.map { c ->
                Course(
                    id = newId(),
                    name = c.name,
                    teacher = c.teacher,
                    location = c.location,
                    dayOfWeek = c.dayOfWeek,
                    startSection = c.startSection,
                    endSection = c.endSection,
                    startWeek = c.startWeek,
                    endWeek = c.endWeek,
                    weekParity = c.weekParity,
                )
            },
        )
        AppSettings.scheduleStorage.save(schedule)
        val config = AppSettings.settingsStorage.getConfig()
        AppSettings.settingsStorage.saveConfig(config.copy(currentScheduleId = schedule.id))
        step = ImportStep.Done(courses.size, untimed)
    }

    fun confirmImportSafely(courses: List<ParsedCourse>, untimed: List<String>) {
        runCatching { confirmImport(courses, untimed) }
            .onFailure { e -> step = ImportStep.Failed("保存课表失败：${e.fullChain()}") }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("从教务系统导入课程表") },
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val s = step) {
                    ImportStep.Login -> {
                        Text(
                            "登录大连民族大学教务系统",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("学号") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "账号密码仅加密保存在本机，用于登录教务系统",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = ::login,
                            enabled = username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("登录")
                        }
                    }

                    is ImportStep.Working -> {
                        Spacer(Modifier.height(48.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(s.status)
                        }
                    }

                    is ImportStep.PickSemester -> {
                        Text("选择要导入的学期", style = MaterialTheme.typography.titleMedium)
                        SemesterDropdown(
                            semesters = s.semesters,
                            selectedId = selectedSemesterId,
                            onSelected = {
                                selectedSemesterId = it.id
                                scheduleName = "${it.label} 教务导入"
                            }
                        )
                        OutlinedTextField(
                            value = scheduleName,
                            onValueChange = { scheduleName = it },
                            label = { Text("课表名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = ::startImport,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("开始导入")
                        }
                    }

                    is ImportStep.ConfirmStartDate -> {
                        Text("确认开学日期", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "解析到 ${s.courses.size} 条课程记录。开学日期（第一周周一）用于计算「今天是第几周」。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (s.untimedCourseNames.isNotEmpty()) {
                            Text(
                                "以下 ${s.untimedCourseNames.size} 门课程无固定时间安排，不会出现在课表网格中：\n" +
                                    s.untimedCourseNames.joinToString("、"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(Modifier.padding(4.dp))
                            val date = Instant.fromEpochMilliseconds(startDateMillis)
                                .toLocalDateTime(TimeZone.UTC).date
                            Text("开学日期：$date")
                        }
                        Button(
                            onClick = { confirmImportSafely(s.courses, s.untimedCourseNames) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("确认导入")
                        }
                    }

                    is ImportStep.Done -> {
                        Spacer(Modifier.height(48.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("导入完成", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "「$scheduleName」已创建并设为当前课表，共导入 ${s.courseCount} 条课程记录",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (s.untimedCourseNames.isNotEmpty()) {
                                Text(
                                    "${s.untimedCourseNames.size} 门无固定时间的课程未导入：${s.untimedCourseNames.joinToString("、")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                                Text("完成")
                            }
                        }
                    }

                    is ImportStep.Failed -> {
                        Spacer(Modifier.height(48.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("导入失败", style = MaterialTheme.typography.titleLarge)
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { step = ImportStep.Login },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("返回重试")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterDropdown(
    semesters: List<SemesterOption>,
    selectedId: String,
    onSelected: (SemesterOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = semesters.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.label ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("学期") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            semesters.forEach { semester ->
                DropdownMenuItem(
                    text = { Text(semester.label) },
                    onClick = {
                        onSelected(semester)
                        expanded = false
                    }
                )
            }
        }
    }
}
