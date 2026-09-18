package com.wofanmo.course_schedule

import com.wofanmo.course_schedule.data.jwgl.JinZhiParser
import com.wofanmo.course_schedule.data.model.WeekParity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 基于大连民族大学教务系统真实页面结构的解析器回归测试
 */
class JinZhiParserTest {

    /** 课程单元格：外层 li.courselists-item + 内层 qz-tooltip（后者不应被解析成课程） */
    private fun courseCell(
        name: String,
        teacher: String,
        sections: String,
        weeksAndDay: String,
        location: String,
    ) = """
        <td rowspan="2" class="qz-weeklyTable-td qz-hasCourse qz-mixrow" width="13%">
            <div class="td-cell qz-flex-col">
                <ul class="courselists qz-flex-col hasMoreCourse">
                    <li class="courselists-item qz-hasCourse-1">
                        <div class="qz-hasCourse-title qz-ellipse">$name</div>
                        <div class="qz-hasCourse-detaillists">
                            <div class="qz-hasCourse-detailitem" style="display:none;"></div>
                            <div class="qz-hasCourse-detailitem qz-FullInfo">
                                教师：$teacher
                                <br>课序号：05
                            </div>
                            <div class="qz-hasCourse-detailitem qz-FullInfo">$sections</div>
                            <div class="qz-hasCourse-detailitem qz-FullInfo">$weeksAndDay</div>
                            <div class="qz-hasCourse-detailitem">$location</div>
                            <div class="qz-default qz-showlink qz-FullInfo" style="display:none;" name="wkxx"></div>
                        </div>
                    </li>
                </ul>
                <div class="qz-tooltip">
                    <div class="qz-tooltipContent">
                        <ul class="qz-toolitiplists">
                            <li class="qz-toolitiplists">
                                <div class="qz-tooltipContent-title qz-ellipse">$name</div>
                                <div class="qz-tooltipContent-detaillists">
                                    <div class="qz-tooltipContent-detailitem">教师：$teacher<br> 课序号：05</div>
                                    <div class="qz-tooltipContent-detailitem">上课地点：$location</div>
                                    <div class="qz-tooltipContent-detailitem">周次：$weeksAndDay</div>
                                    <div class="qz-tooltipContent-detailitem">节次：$sections</div>
                                    <div class="qz-tooltipContent-detailitem">学分：3.5学分</div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </td>
    """.trimIndent()

    @Test
    fun parsesCourseCellFields() {
        val html = courseCell(
            name = "离散数学",
            teacher = "赵戈讲师（高校）",
            sections = "01~02节",
            weeksAndDay = "[1-5周] 星期三",
            location = "致新楼致新楼西区-322",
        )

        val courses = JinZhiParser.parseScheduleGrid(html)
        assertEquals(1, courses.size)

        val c = courses.first()
        assertEquals("离散数学", c.name)
        assertEquals("赵戈讲师（高校）", c.teacher)
        assertEquals(1, c.startSection)
        assertEquals(2, c.endSection)
        assertEquals(3, c.dayOfWeek)          // 星期三
        assertEquals(1, c.startWeek)
        assertEquals(5, c.endWeek)
        assertEquals(WeekParity.EVERY, c.weekParity)
    }

    @Test
    fun expandsMultiSegmentWeeks() {
        // [1-5,8-14,18周] → 三个周段各一条记录
        val html = courseCell(
            name = "离散数学",
            teacher = "赵戈讲师（高校）",
            sections = "01~02节",
            weeksAndDay = "[1-5,8-14,18周] 星期三",
            location = "致新楼致新楼西区-322",
        )

        val courses = JinZhiParser.parseScheduleGrid(html)
        assertEquals(3, courses.size)
        assertEquals(
            setOf(1 to 5, 8 to 14, 18 to 18),
            courses.map { it.startWeek to it.endWeek }.toSet(),
        )
    }

    @Test
    fun parsesSectionRangeAndOddSections() {
        val html = courseCell(
            name = "自然语言处理",
            teacher = "赵丹丹副教授",
            sections = "07~08节",
            weeksAndDay = "[4-5,7-10周] 星期四",
            location = "致新楼致新楼西区-104",
        )

        val c = JinZhiParser.parseScheduleGrid(html).first()
        assertEquals(7, c.startSection)
        assertEquals(8, c.endSection)
        assertEquals(4, c.dayOfWeek)
    }

    @Test
    fun parsesScatteredSingleWeeks() {
        // [4,7周] → 两个孤立周
        val html = courseCell(
            name = "IT认证1",
            teacher = "邓新洋讲师（高校）",
            sections = "05~06节",
            weeksAndDay = "[4,7周] 星期六",
            location = "致新楼致新楼东区-324",
        )

        val courses = JinZhiParser.parseScheduleGrid(html)
        assertEquals(2, courses.size)
        assertEquals(setOf(4, 7), courses.map { it.startWeek }.toSet())
        assertEquals(6, courses.first().dayOfWeek)
    }

    @Test
    fun dedupesRepeatedLocationPrefix() {
        // 教务系统把建筑名重复拼了一遍
        val html = courseCell(
            name = "Web前端开发技术",
            teacher = "邓新洋讲师（高校）",
            sections = "07~08节",
            weeksAndDay = "[4,7-12周] 星期五",
            location = "弘德楼A区弘德楼A区-301",
        )

        assertEquals("弘德楼A区-301", JinZhiParser.parseScheduleGrid(html).first().location)
    }

    @Test
    fun keepsNormalLocationUnchanged() {
        val html = courseCell(
            name = "示例课",
            teacher = "某教师",
            sections = "01~02节",
            weeksAndDay = "[1-4周] 星期一",
            location = "致新楼西区-204",
        )

        assertEquals("致新楼西区-204", JinZhiParser.parseScheduleGrid(html).first().location)
    }

    @Test
    fun stripsCourseSequenceSuffixFromTeacher() {
        val html = courseCell(
            name = "计算机网络",
            teacher = "王泽宇讲师（高校）",
            sections = "01~02节",
            weeksAndDay = "[4-5,7-17周] 星期四",
            location = "致新楼致新楼西区-322",
        )

        // 「教师：王泽宇讲师（高校） <br>课序号：02」不应把课序号并入教师名
        assertEquals("王泽宇讲师（高校）", JinZhiParser.parseScheduleGrid(html).first().teacher)
    }

    @Test
    fun parsesUntimedCoursesFromRemarkRow() {
        val html = """
            <tfoot class="qz-weeklyTable-tfoot">
            <tr class="qz-weeklyTable-tr">
                <td class="qz-weeklyTable-td qz-weeklyTable-label" width="122px">
                    <div class="td-cell qz-ellipse qz-flex-col">
                        <div class="index-title">备注</div>
                    </div>
                </td>
                <td class="qz-weeklyTable-td qz-weeklyTable-detailtext" colspan="7" width="91%">
                    <div class="td-cell">
                        劳动教育与训练5 刘通 1-20周;IT职业素质训练（校企合作） 邓新洋 1-3周;
                    </div>
                </td>
            </tr>
            </tfoot>
        """.trimIndent()

        assertEquals(
            listOf("劳动教育与训练5" to "刘通", "IT职业素质训练（校企合作）" to "邓新洋"),
            JinZhiParser.parseUntimedCourses(html),
        )
    }

    @Test
    fun parsesSemesterOptionsFromPage() {
        val html = """
            <html><body>
            <select id="xnxq01id" name="xnxq01id">
              <option value="2026-2027-2">2026-2027-2</option>
              <option value="2026-2027-1" selected="selected">2026-2027-1</option>
            </select>
            </body></html>
        """.trimIndent()

        val semesters = JinZhiParser.parse(html)
        assertEquals(2, semesters.size)
        assertEquals("2026-2027-1", semesters.first { it.isCurrent }.id)
    }

    @Test
    fun emptyOrUnrelatedHtmlYieldsEmpty() {
        assertTrue(JinZhiParser.parseScheduleGrid("<html><body>请先登录系统</body></html>").isEmpty())
        assertTrue(JinZhiParser.parseUntimedCourses("<html><body></body></html>").isEmpty())
    }
}
