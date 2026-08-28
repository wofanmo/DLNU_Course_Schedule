package com.wofanmo.course_schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_ALARM
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_ROLL
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_COURSE_NAMES
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_END_TIME
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_KIND
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_LOCATION
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_SECTION_LABEL
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_START_TIME
import com.wofanmo.course_schedule.notification.ReminderConstants.KIND_PRE

/** 提前提醒 / 开始通知闹钟触发时发通知；每日滚动闹钟触发时重排窗口。 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ROLL -> {
                // 每日 00:05：向前滚动调度窗口（无课日也能推进）
                CourseReminderScheduler.scheduleAll(context)
                return
            }
            ACTION_ALARM -> Unit
            else -> return
        }

        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val names = intent.getStringExtra(EXTRA_COURSE_NAMES) ?: return
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
        val sectionLabel = intent.getStringExtra(EXTRA_SECTION_LABEL) ?: ""

        ReminderNotificationHelper.ensureChannels(context)
        val notificationId = "$kind:$names".hashCode()

        if (kind == KIND_PRE) {
            val locationText = if (location.isNotEmpty()) " @ $location" else ""
            ReminderNotificationHelper.postPreReminder(
                context,
                "课程提醒",
                "$names 将在 $startTime 开始$locationText",
                notificationId,
            )
        } else {
            val locationText = if (location.isNotEmpty()) " · $location" else ""
            ReminderNotificationHelper.postStartReminder(
                context,
                "开始上课",
                "$names · $sectionLabel · $startTime-$endTime$locationText",
                endTime,
                notificationId,
            )
        }

        // 触发后滚动窗口：重排今天 + 明天
        CourseReminderScheduler.scheduleAll(context)
    }
}
