package com.wofanmo.course_schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机 / 时间变更 / 时区变更 / 应用更新后重排课程提醒闹钟。 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 校验 action：manifest 注册了多个系统广播，避免收到无关 intent 时误重排
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> CourseReminderScheduler.scheduleAll(context)
            else -> {}
        }
    }
}
