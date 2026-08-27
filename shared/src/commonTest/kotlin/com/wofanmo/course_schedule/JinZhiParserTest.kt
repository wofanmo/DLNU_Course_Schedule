package com.wofanmo.course_schedule

import com.wofanmo.course_schedule.data.jwgl.JinZhiParser
import com.wofanmo.course_schedule.data.model.WeekParity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 基于大连民族大学金智系统真实数据格式的解析器回归测试
 */
class JinZhiParserTest {

    @Test
    fun parsesMultiSegmentWeeks() {
        // 计算机组成原理（2025-2026-2 真实格式）：非连续周段 3-13,17-18
        val json = """
            {"code":0,"data":[{
              "kc_mc":"计算机组成原理","jg0101mc":"包书哲",
              "sktime":"周一第3、4节{第3-13,17-18周}(全部);周三第3、4节{第4-13,17周}(全部)",
              "skddmc":"致新楼西区-204;致新楼西区-318"
            }],"count":1}
        """.trimIndent()

        val courses = JinZhiParser.parseTimedCourses(json)
        // 周一 3-4节 {3-13} + {17-18}，周三 3-4节 {4-13} + {17} → 4 条
        assertEquals(4, courses.size)

        val monday = courses.filter { it.dayOfWeek == 1 }
        assertEquals(2, monday.size)
        assertEquals(setOf(3 to 13, 17 to 18), monday.map { it.startWeek to it.endWeek }.toSet())
        assertEquals(3, monday.first().startSection)
        assertEquals(4, monday.first().endSection)
        assertEquals("致新楼西区-204", monday.first().location)

        val wednesday = courses.filter { it.dayOfWeek == 3 }
        assertEquals(setOf(4 to 13, 17 to 17), wednesday.map { it.startWeek to it.endWeek }.toSet())
        assertEquals("致新楼西区-318", wednesday.first().location)
        assertEquals(WeekParity.EVERY, courses.first().weekParity)
    }

    @Test
    fun parsesScatteredSingleWeeks() {
        // IT认证1（2026-2027-1 真实格式）：第4,7周 两个孤立周
        val json = """
            {"code":0,"data":[{
              "kc_mc":"IT认证1","jg0101mc":"赵戈,邓新洋,崔永瑞",
              "sktime":"周六第5、6节{第4,7周}(全部)",
              "skddmc":"致新楼东区-324"
            }],"count":1}
        """.trimIndent()

        val courses = JinZhiParser.parseTimedCourses(json)
        assertEquals(2, courses.size)
        assertEquals(6, courses.first().dayOfWeek)
        assertEquals(setOf(4, 7), courses.map { it.startWeek }.toSet())
        assertEquals(setOf(4, 7), courses.map { it.endWeek }.toSet())
    }

    @Test
    fun parsesSectionRangeSyntax() {
        // 数据库原理与应用（2025-2026-2 真实格式）：节次区间写法 第5-8节
        val json = """
            {"code":0,"data":[{
              "kc_mc":"数据库原理与应用","jg0101mc":"某教师",
              "sktime":"周日第5-8节{第4周}(全部);周日第5-8节{第5,7周}(全部)",
              "skddmc":"致新楼西区-101;致新楼西区-101"
            }],"count":1}
        """.trimIndent()

        val courses = JinZhiParser.parseTimedCourses(json)
        assertEquals(3, courses.size)
        assertEquals(7, courses.first().dayOfWeek)
        assertEquals(5, courses.first().startSection)
        assertEquals(8, courses.first().endSection)
        assertEquals(setOf(4, 5, 7), courses.map { it.startWeek }.toSet())
    }

    @Test
    fun parsesOddEvenParity() {
        val json = """
            {"code":0,"data":[{
              "kc_mc":"示例课","jg0101mc":"某教师",
              "sktime":"周二第1、2节{第1-15周}(单周);周四第1、2节{第1-15周}(双周)",
              "skddmc":"教一楼-101;教一楼-102"
            }],"count":1}
        """.trimIndent()

        val courses = JinZhiParser.parseTimedCourses(json)
        assertEquals(2, courses.size)
        assertEquals(WeekParity.ODD, courses[0].weekParity)
        assertEquals(WeekParity.EVEN, courses[1].weekParity)
        assertEquals(2, courses[0].dayOfWeek)
        assertEquals(4, courses[1].dayOfWeek)
    }

    @Test
    fun untimedEntriesComeFromNullSktime() {
        val json = """
            {"code":0,"data":[
              {"kc_mc":"劳动教育与训练5","jg0101mc":"刘通","sktime":null},
              {"kc_mc":"计算机组成原理","jg0101mc":"包书哲","sktime":"周一第3、4节{第3-13周}(全部)"}
            ],"count":2}
        """.trimIndent()

        val entries = JinZhiParser.parseUntimedEntries(json)
        assertEquals(listOf("劳动教育与训练5" to "刘通"), entries)
    }

    @Test
    fun parsesSemesterAndCampusOptionsFromPage() {
        val html = """
            <html><body>
            <select id="xnxq01id" name="xnxq01id">
              <option value="2026-2027-2">2026-2027-2</option>
              <option value="2026-2027-1" selected="selected">2026-2027-1</option>
            </select>
            <select id="kbjcmsid" name="kbjcmsid">
              <option value="KFQ" selected="selected">开发区校区</option>
              <option value="JST">金石滩校区</option>
            </select>
            </body></html>
        """.trimIndent()

        val page = JinZhiParser.parse(html)
        assertEquals(2, page.semesters.size)
        assertEquals("2026-2027-1", page.semesters.first { it.isCurrent }.id)
        assertEquals(listOf("开发区校区", "金石滩校区"), page.campuses.map { it.name })
        assertEquals(listOf("KFQ", "JST"), page.campuses.map { it.id })
    }

    @Test
    fun emptyDataYieldsEmpty() {
        assertEquals(emptyList(), JinZhiParser.parseTimedCourses("""{"code":0,"data":[],"count":0}"""))
        assertEquals(emptyList(), JinZhiParser.parseUntimedEntries("not json"))
    }
}
