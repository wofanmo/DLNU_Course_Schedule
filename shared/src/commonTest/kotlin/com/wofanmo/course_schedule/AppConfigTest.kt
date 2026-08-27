package com.wofanmo.course_schedule

import com.wofanmo.course_schedule.data.model.AppConfig
import com.wofanmo.course_schedule.data.model.DEFAULT_EVENING_BREAK_INDEX
import com.wofanmo.course_schedule.data.model.DEFAULT_LUNCH_BREAK_INDEX
import com.wofanmo.course_schedule.data.model.DEFAULT_SECTION_TIMES
import com.wofanmo.course_schedule.data.model.DEFAULT_TOTAL_SECTIONS
import com.wofanmo.course_schedule.data.model.eveningBreakIndex
import com.wofanmo.course_schedule.data.model.lunchBreakIndex
import com.wofanmo.course_schedule.data.model.totalSectionCount
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppConfigTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `hardcoded schedule has 12 sections`() {
        assertEquals(DEFAULT_TOTAL_SECTIONS, DEFAULT_SECTION_TIMES.size)
    }

    @Test
    fun `hardcoded schedule matches morning afternoon evening start times`() {
        // 上午第一节课 08:30
        assertEquals("08:30", DEFAULT_SECTION_TIMES[0].start)
        // 下午第一节课 13:30（第5节）
        assertEquals("13:30", DEFAULT_SECTION_TIMES[4].start)
        // 晚上第一节课 18:30（第9节）
        assertEquals("18:30", DEFAULT_SECTION_TIMES[8].start)
    }

    @Test
    fun `hardcoded schedule has 40 minute sections and correct breaks`() {
        // 每节 40 分钟：start + 40 = end
        DEFAULT_SECTION_TIMES.forEach { time ->
            val startMinutes = time.start.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val endMinutes = time.end.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            assertEquals(40, endMinutes - startMinutes, "节次 ${time.start} 应为 40 分钟")
        }

        // 同大节内 10 分钟课间：1-2、3-4、5-6、7-8、9-10、11-12
        listOf(0, 2, 4, 6, 8, 10).forEach { i ->
            val end = DEFAULT_SECTION_TIMES[i].end.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val nextStart = DEFAULT_SECTION_TIMES[i + 1].start.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            assertEquals(10, nextStart - end, "第 ${i + 1}-${i + 2} 节课间应为 10 分钟")
        }

        // 跨大节 20 分钟课间：2-3、6-7、10-11
        listOf(1, 5, 9).forEach { i ->
            val end = DEFAULT_SECTION_TIMES[i].end.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val nextStart = DEFAULT_SECTION_TIMES[i + 1].start.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            assertEquals(20, nextStart - end, "第 ${i + 1}-${i + 2} 节课间应为 20 分钟")
        }
    }

    @Test
    fun `break indices are after section 4 and section 8`() {
        assertEquals(3, DEFAULT_LUNCH_BREAK_INDEX)
        assertEquals(7, DEFAULT_EVENING_BREAK_INDEX)

        // AppConfig 扩展返回固定值
        val config = AppConfig()
        assertEquals(12, config.totalSectionCount())
        assertEquals(3, config.lunchBreakIndex())
        assertEquals(7, config.eveningBreakIndex())
    }

    @Test
    fun `AppConfig serialization round-trip`() {
        val original = AppConfig(currentScheduleId = "test", theme = "dark")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppConfig>(encoded)

        assertEquals(original, decoded)
    }
}
