package com.wofanmo.course_schedule.data.jwgl

import com.wofanmo.course_schedule.data.model.WeekParity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 教务系统学期选项，如 2026-2027-1 */
data class SemesterOption(val id: String, val label: String, val isCurrent: Boolean)

/** 校区选项（金智将校区建模为「课表节次模式」kbjcmsid） */
data class CampusOption(val id: String, val name: String)

/** 从教务系统解析出的单条课程（已按周段展开） */
data class ParsedCourse(
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,      // 1=周一 ... 7=周日
    val startSection: Int,   // 开始节次
    val endSection: Int,     // 结束节次
    val startWeek: Int,
    val endWeek: Int,
    val weekParity: WeekParity,
)

/** 课表页（viweType=0）中的学期与校区下拉选项 */
data class ParsedSchedulePage(
    val semesters: List<SemesterOption>,
    val campuses: List<CampusOption>,
)

/**
 * 金智教务解析器。
 * - 学期/校区选项来自「学期理论课表」页面 HTML 的下拉框（正则解析，无外部依赖）；
 * - 课程数据来自「有课表课程/无课表课程」列表 JSON 接口（该部署的周视图网格不承载数据）。
 *
 * sktime 格式：`周六第5、6节{第4,7周}(全部);周一第3、4节{第3-13,17-18周}(单周);...`
 * skddmc 为与时间段一一对应的分号分隔地点列表。
 * 非连续周段（如第4,7周）展开为多条记录。
 */
object JinZhiParser {

    private val json by lazy { Json { ignoreUnknownKeys = true } }

    // ---------------- 页面选项（HTML 下拉框，正则解析） ----------------

    fun parse(html: String): ParsedSchedulePage =
        ParsedSchedulePage(
            semesters = parseOptions(html, "xnxq01id")
                .map { (value, label, selected) -> SemesterOption(value, label, selected) },
            campuses = parseOptions(html, "kbjcmsid")
                .map { (value, label, _) -> CampusOption(value, label) },
        )

    private val optionTagRegex =
        Regex("""<option\b([^>]*)>(.*?)</option>""", RegexOption.DOT_MATCHES_ALL)
    private val valueAttrRegex = Regex("""value\s*=\s*"([^"]*)"""")
    private val tagStripRegex = Regex("""<[^>]+>""")

    /** 提取指定 select 的 (value, label, selected) 选项列表 */
    private fun parseOptions(html: String, selectId: String): List<Triple<String, String, Boolean>> {
        val block = Regex(
            """<select[^>]*id\s*=\s*"$selectId"[^>]*>(.*?)</select>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.get(1) ?: return emptyList()

        return optionTagRegex.findAll(block).mapNotNull { m ->
            val attrs = m.groupValues[1]
            val value = valueAttrRegex.find(attrs)?.groupValues?.get(1)?.trim().orEmpty()
            if (value.isEmpty()) return@mapNotNull null
            val label = tagStripRegex.replace(m.groupValues[2], "").trim()
            Triple(value, label, attrs.contains("selected"))
        }.toList()
    }

    // ---------------- 课程列表（JSON） ----------------

    @Serializable
    private data class CourseRow(
        val kc_mc: String = "",
        val jg0101mc: String = "",
        val sktime: String? = null,
        val skddmc: String? = null,
    )

    @Serializable
    private data class LayuiResponse(
        val code: Int = 0,
        val data: List<CourseRow> = emptyList(),
    )

    // 周X第a、b节{第c-d,e周}(全部|单周|双周)；节次也支持区间写法（第5-8节）
    // 连字符 `-` 必须放字符类末尾才能不转义；花括号两端都加 \ 避免严格模式报错
    private val slotRegex =
        Regex("""周([一二三四五六日])第([\d、,，~-]+)节\{第([\d,，~-]+)周\}\(([^)]*)\)""")

    private val dayMap = mapOf(
        "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "日" to 7, "天" to 7,
    )

    /** 解析「有课表课程」JSON（viweType=1）为课程记录（按周段展开、跨行去重） */
    fun parseTimedCourses(jsonText: String): List<ParsedCourse> {
        val resp = runCatching { json.decodeFromString<LayuiResponse>(jsonText) }.getOrNull()
            ?: return emptyList()
        val courses = mutableListOf<ParsedCourse>()
        for (row in resp.data) {
            val sktime = row.sktime?.takeIf { it.isNotBlank() } ?: continue
            val locations = row.skddmc?.split(';')?.map { it.trim() } ?: emptyList()
            val slots = slotRegex.findAll(sktime).toList()
            slots.forEachIndexed { index, m ->
                val day = dayMap[m.groupValues[1]] ?: return@forEachIndexed
                val sections = parseInts(m.groupValues[2])
                if (sections.isEmpty()) return@forEachIndexed
                val parity = when {
                    m.groupValues[4].contains("单") -> WeekParity.ODD
                    m.groupValues[4].contains("双") -> WeekParity.EVEN
                    else -> WeekParity.EVERY
                }
                val location = locations.getOrElse(index) { locations.lastOrNull() ?: "" }
                for ((weekStart, weekEnd) in parseWeekSegments(m.groupValues[3])) {
                    courses += ParsedCourse(
                        name = row.kc_mc.trim(),
                        teacher = row.jg0101mc.trim(),
                        location = location,
                        dayOfWeek = day,
                        startSection = sections.min(),
                        endSection = sections.max(),
                        startWeek = weekStart,
                        endWeek = weekEnd,
                        weekParity = parity,
                    )
                }
            }
        }
        // 同一课程可能拆成多行（讲课/实践学时各一行），slot 级别去重
        return courses.distinct()
    }

    /** 解析「无课表课程」JSON（viweType=2）为 (课程名, 教师) 对列表 */
    fun parseUntimedEntries(jsonText: String): List<Pair<String, String>> {
        val resp = runCatching { json.decodeFromString<LayuiResponse>(jsonText) }.getOrNull()
            ?: return emptyList()
        return resp.data
            .filter { it.sktime.isNullOrBlank() }
            .map { it.kc_mc.trim() to it.jg0101mc.trim() }
            .filter { it.first.isNotEmpty() }
            .distinct()
    }

    private fun parseInts(text: String): List<Int> =
        Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()

    /** 「4,7」「3-13,17-18」→ 连续周段列表 */
    private fun parseWeekSegments(text: String): List<Pair<Int, Int>> =
        text.split(',', '，').mapNotNull { seg ->
            val nums = parseInts(seg)
            when {
                nums.isEmpty() -> null
                nums.size == 1 -> nums[0] to nums[0]
                else -> nums.first() to nums.last()
            }
        }
}
