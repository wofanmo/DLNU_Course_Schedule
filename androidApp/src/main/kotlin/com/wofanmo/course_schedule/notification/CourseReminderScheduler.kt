package com.wofanmo.course_schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wofanmo.course_schedule.AppSettings
import com.wofanmo.course_schedule.data.model.Course
import com.wofanmo.course_schedule.data.model.DEFAULT_SECTION_TIMES
import com.wofanmo.course_schedule.data.model.matchesWeek
import com.wofanmo.course_schedule.data.storage.initStorage
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_ALARM
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_ROLL
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_RESTORE
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_COURSE_NAMES
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_END_TIME
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_KIND
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_LOCATION
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_SECTION_LABEL
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_START_TIME
import com.wofanmo.course_schedule.notification.ReminderConstants.KEY_REQUEST_CODES
import com.wofanmo.course_schedule.notification.ReminderConstants.KIND_PRE
import com.wofanmo.course_schedule.notification.ReminderConstants.KIND_START
import com.wofanmo.course_schedule.notification.ReminderConstants.PREFS
import com.wofanmo.course_schedule.notification.ReminderConstants.ROLL_REQUEST_CODE
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

/**
 * 课程提醒调度器：用 AlarmManager 精确闹钟对「今天 + 明天」有课的节次排提醒与开始通知。
 * 滚动窗口 + 每日 00:05 滚动闹钟兜底；数据变更/开机/时间变更时调用 [scheduleAll] 重排。
 */
object CourseReminderScheduler {

    fun scheduleAll(context: Context) {
        initStorage(context)
        cancelTracked(context)

        val config = AppSettings.settingsStorage.getConfig()
        val leadMinutes = config.notifyLeadMinutes
        if (leadMinutes <= 0) {
            // 关闭 = 全部通知不发，顺带取消每日滚动闹钟
            cancelRollAlarm(context)
            return
        }

        val schedule = AppSettings.scheduleStorage.getById(config.currentScheduleId) ?: return
        ReminderNotificationHelper.ensureChannels(context)

        val tz = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val startDate = LocalDate.parse(schedule.startDate)

        val requestCodes = mutableListOf<Int>()

        for (offset in 0..1) {
            val date = today.plus(offset, DateTimeUnit.DAY)
            val week = (startDate.until(date, DateTimeUnit.DAY) / 7) + 1
            if (week !in 1..schedule.totalWeeks) continue
            val dayOfWeek = date.dayOfWeek.isoDayNumber

            // 同一起止节次的课程合并成一条通知
            val grouped = schedule.courses
                .filter { it.dayOfWeek == dayOfWeek && it.matchesWeek(week) }
                .groupBy { it.startSection to it.endSection }

            for ((sections, courses) in grouped) {
                val startTime = DEFAULT_SECTION_TIMES.getOrNull(sections.first - 1)?.start ?: continue
                val endTime = DEFAULT_SECTION_TIMES.getOrNull(sections.second - 1)?.end ?: continue
                val startMinutes = startTime.toMinutes()

                val startMillis = millisAt(date, startMinutes, tz)
                if (startMillis > nowMillis) {
                    requestCodes += scheduleAlarm(
                        context, KIND_START, courses, date, startTime, endTime, sections, startMillis
                    )
                }

                val preMillis = millisAt(date, startMinutes - leadMinutes, tz)
                if (preMillis > nowMillis) {
                    requestCodes += scheduleAlarm(
                        context, KIND_PRE, courses, date, startTime, endTime, sections, preMillis
                    )
                }
            }
        }

        writeTracked(context, requestCodes)
        scheduleRollAlarm(context)
    }

    /** 用户点了「静音」按钮后，为课程结束时间排一个恢复铃声的闹钟。 */
    fun scheduleRestoreAlarm(context: Context, endTime: String) {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val triggerMillis = millisAt(today, endTime.toMinutes(), tz)

        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, RestoreReceiver::class.java).setAction(ACTION_RESTORE)
        val pi = PendingIntent.getBroadcast(
            context,
            ("restore:$endTime:$today").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(am, triggerMillis, pi)
    }

    private fun scheduleAlarm(
        context: Context,
        kind: String,
        courses: List<Course>,
        date: LocalDate,
        startTime: String,
        endTime: String,
        sections: Pair<Int, Int>,
        triggerMillis: Long,
    ): Int {
        val names = courses.joinToString("、") { it.name }
        val location = courses.map { it.location }.filter { it.isNotEmpty() }.joinToString("、")
        val sectionLabel = "第 ${sections.first}-${sections.second} 节"

        // 同名课程同天多节次（如周一 1-2 节与 3-4 节同名）时，requestCode 必须区分节次，否则后者覆盖前者
        val requestCode = ("$kind:$names:$date:${sections.first}-${sections.second}").hashCode()
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_COURSE_NAMES, names)
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_END_TIME, endTime)
            putExtra(EXTRA_SECTION_LABEL, sectionLabel)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(am, triggerMillis, pi)
        return requestCode
    }

    /** 每日 00:05 的滚动闹钟：保证无课的日子也能把调度窗口向前滚动到明天。 */
    private fun scheduleRollAlarm(context: Context) {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(tz)
        var date = now.date
        var millis = millisAt(date, 5, tz)
        if (millis <= Clock.System.now().toEpochMilliseconds()) {
            date = date.plus(1, DateTimeUnit.DAY)
            millis = millisAt(date, 5, tz)
        }
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).setAction(ACTION_ROLL)
        val pi = PendingIntent.getBroadcast(
            context, ROLL_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(am, millis, pi)
    }

    private fun cancelRollAlarm(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).setAction(ACTION_ROLL)
        val pi = PendingIntent.getBroadcast(
            context, ROLL_REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { am.cancel(it) }
    }

    private fun setExact(am: AlarmManager, triggerMillis: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun millisAt(date: LocalDate, minutes: Int, tz: TimeZone): Long {
        return LocalDateTime(date, LocalTime(minutes / 60, minutes % 60))
            .toInstant(tz)
            .toEpochMilliseconds()
    }

    private fun String.toMinutes(): Int {
        val parts = split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun cancelTracked(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val codes = prefs.getString(KEY_REQUEST_CODES, "")
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        for (code in codes) {
            val intent = Intent(context, ReminderAlarmReceiver::class.java).setAction(ACTION_ALARM)
            val pi = PendingIntent.getBroadcast(
                context, code.toInt(), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { am.cancel(it) }
        }
        prefs.edit().putString(KEY_REQUEST_CODES, "").apply()
    }

    private fun writeTracked(context: Context, codes: List<Int>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REQUEST_CODES, codes.joinToString(",")).apply()
    }
}
