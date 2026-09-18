package com.wofanmo.course_schedule.data.jwgl

import com.wofanmo.course_schedule.data.model.WeekParity

/** 教务系统学期选项，如 2026-2027-1 */
data class SemesterOption(val id: String, val label: String, val isCurrent: Boolean)

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

/**
 * 教务解析器（正则解析，无外部依赖）。
 *
 * 数据来源：
 * - 学期选项：「学期理论课表」页面 HTML 的 xnxq01id 下拉框；
 * - 课程：课表周视图页面 HTML 的课程单元格。
 *
 * 课程单元格 `li.courselists-item` 内含四条信息（教师 / 节次 / 周次+星期 / 地点），
 * 且**自带完整周次范围**，因此解析不依赖表格的行列坐标：
 * ```
 * <li class="courselists-item qz-hasCourse-1">
 *   <div class="qz-hasCourse-title">离散数学</div>
 *   <div class="qz-hasCourse-detaillists">
 *     <div class="qz-hasCourse-detailitem">教师：赵戈讲师<br>课序号：05</div>
 *     <div class="qz-hasCourse-detailitem">01~02节</div>
 *     <div class="qz-hasCourse-detailitem">[1-5,8-14,18周] 星期三</div>
 *     <div class="qz-hasCourse-detailitem">致新楼致新楼西区-322</div>
 *   </div>
 * </li>
 * ```
 * 非连续周段（如 `[1-5,8-14,18周]`）展开为多条记录。
 * 无固定时间的课程列在表格 `tfoot` 的「备注」行，见 [parseUntimedCourses]。
 */
object JinZhiParser {

    // ---------------- 学期选项（HTML 下拉框） ----------------

    fun parse(html: String): List<SemesterOption> =
        parseOptions(html, "xnxq01id")
            .map { (value, label, selected) -> SemesterOption(value, label, selected) }

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

    // ---------------- 课表周视图（HTML） ----------------

    private val courseItemRegex = Regex(
        """<li\s+class="courselists-item[^"]*"[^>]*>(.*?)</li>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val courseTitleRegex = Regex(
        """<div\s+class="qz-hasCourse-title[^"]*"[^>]*>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val courseDetailRegex = Regex(
        """<div\s+class="qz-hasCourse-detailitem[^"]*"[^>]*>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL
    )

    // 教师行形如「教师：赵戈讲师（高校） 课序号：05」，截到「课序号」为止
    private val teacherRegex = Regex("""教师\s*[：:]\s*(.+?)(?=\s*课序号|$)""")
    private val weekDayRegex = Regex("""\[([^\]]*?)\s*周\]\s*星期([一二三四五六日天])""")

    // 地点形如「致新楼致新楼西区-322」：教务系统把建筑名重复拼了一遍
    private val repeatedPrefixRegex = Regex("""^(.+?)\1""")

    private val dayMap = mapOf(
        "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "日" to 7, "天" to 7,
    )

    /**
     * 解析课表周视图页面 HTML 为课程记录。
     * 按周段展开为多条记录；同一课程会在其开课的每一周页面中重复出现，结果已去重。
     */
    fun parseScheduleGrid(html: String): List<ParsedCourse> {
        val courses = mutableListOf<ParsedCourse>()
        for (item in courseItemRegex.findAll(html)) {
            val block = item.groupValues[1]
            val name = courseTitleRegex.find(block)
                ?.groupValues?.get(1)
                ?.let { tagStripRegex.replace(it, "").trim() }
                ?.takeIf { it.isNotEmpty() }
                ?: continue

            var teacher = ""
            var sections = emptyList<Int>()
            var segments = emptyList<Pair<Int, Int>>()
            var day = 0
            var location = ""

            for (detail in courseDetailRegex.findAll(block)) {
                val text = tagStripRegex.replace(detail.groupValues[1], " ").trim()
                if (text.isEmpty()) continue
                when {
                    text.contains("教师") ->
                        teacher = teacherRegex.find(text)?.groupValues?.get(1)?.trim().orEmpty()

                    text.endsWith("节") -> sections = parseInts(text)

                    else -> {
                        val weekDay = weekDayRegex.find(text)
                        if (weekDay != null) {
                            segments = parseWeekSegments(weekDay.groupValues[1])
                            day = dayMap[weekDay.groupValues[2]] ?: 0
                        } else if (location.isEmpty()) {
                            location = text
                        }
                    }
                }
            }

            if (sections.isEmpty() || segments.isEmpty() || day == 0) continue
            for ((weekStart, weekEnd) in segments) {
                courses += ParsedCourse(
                    name = name,
                    teacher = teacher,
                    location = dedupeLocationPrefix(location),
                    dayOfWeek = day,
                    startSection = sections.min(),
                    endSection = sections.max(),
                    startWeek = weekStart,
                    endWeek = weekEnd,
                    weekParity = WeekParity.EVERY,
                )
            }
        }
        return courses.distinct()
    }

    private val remarkCellRegex = Regex(
        """<td\b[^>]*class="[^"]*qz-weeklyTable-detailtext[^"]*"[^>]*>(.*?)</td>""",
        RegexOption.DOT_MATCHES_ALL
    )

    // 「IT职业素质训练（校企合作） 邓新洋 1-3周」→ 课程名 + 教师
    private val untimedEntryRegex = Regex("""^(.+)\s+(\S+)\s+[\d,，\-]+周$""")

    /**
     * 解析「备注」行中的无固定时间课程，返回 (课程名, 教师) 对。
     * 格式：`劳动教育与训练5 刘通 1-20周;IT职业素质训练（校企合作） 邓新洋 1-3周;`
     */
    fun parseUntimedCourses(html: String): List<Pair<String, String>> {
        val cell = remarkCellRegex.find(html)?.groupValues?.get(1) ?: return emptyList()
        return tagStripRegex.replace(cell, " ").trim()
            .split(';', '；')
            .mapNotNull { raw ->
                val entry = raw.trim()
                if (entry.isEmpty()) return@mapNotNull null
                val m = untimedEntryRegex.find(entry)
                if (m != null) {
                    m.groupValues[1].trim() to m.groupValues[2].trim()
                } else {
                    entry to ""
                }
            }
            .distinct()
    }

    /**
     * 折叠重复的地点前缀：`致新楼致新楼西区-322` → `致新楼西区-322`。
     * 前缀长度不足 2 时不折叠，避免误伤 `11-101` 这类正常字符串。
     */
    private fun dedupeLocationPrefix(location: String): String {
        val m = repeatedPrefixRegex.find(location) ?: return location
        val prefix = m.groupValues[1]
        if (prefix.length < 2) return location
        return prefix + location.substring(m.value.length)
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
