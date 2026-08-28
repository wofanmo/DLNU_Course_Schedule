package com.wofanmo.course_schedule.notification

/** 课程提醒通知相关的广播 action、extras、通知渠道与存储 key。 */
object ReminderConstants {
    // 广播 action
    const val ACTION_ALARM = "com.wofanmo.course_schedule.notification.ALARM"
    const val ACTION_ROLL = "com.wofanmo.course_schedule.notification.ROLL"
    const val ACTION_MUTE = "com.wofanmo.course_schedule.notification.MUTE"
    const val ACTION_RESTORE = "com.wofanmo.course_schedule.notification.RESTORE"

    // intent extras
    const val EXTRA_KIND = "kind"
    const val EXTRA_COURSE_NAMES = "course_names"
    const val EXTRA_LOCATION = "location"
    const val EXTRA_START_TIME = "start_time"
    const val EXTRA_END_TIME = "end_time"
    const val EXTRA_SECTION_LABEL = "section_label"

    // 闹钟种类
    const val KIND_PRE = "pre"
    const val KIND_START = "start"

    // 通知渠道
    const val CHANNEL_REMINDER = "course_reminder"
    const val CHANNEL_START = "class_start"

    // 存储
    const val PREFS = "course_reminder_prefs"
    const val KEY_REQUEST_CODES = "request_codes"
    const val KEY_MUTED_BY_US = "muted_by_us"
    const val KEY_PRIOR_RINGER_MODE = "prior_ringer_mode"

    // 每日滚动闹钟固定 requestCode（用于在无课日向前滚动调度窗口）
    const val ROLL_REQUEST_CODE = 0x5EED
}
