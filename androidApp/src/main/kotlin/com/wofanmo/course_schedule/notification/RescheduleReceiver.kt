package com.wofanmo.course_schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机 / 时间变更 / 时区变更 / 应用更新后重排课程提醒闹钟。 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CourseReminderScheduler.scheduleAll(context)
    }
}
