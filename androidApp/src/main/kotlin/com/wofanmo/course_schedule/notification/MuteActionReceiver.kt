package com.wofanmo.course_schedule.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_MUTE
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_END_TIME
import com.wofanmo.course_schedule.notification.ReminderConstants.KEY_MUTED_BY_US
import com.wofanmo.course_schedule.notification.ReminderConstants.KEY_PRIOR_RINGER_MODE
import com.wofanmo.course_schedule.notification.ReminderConstants.PREFS

/** 开始通知里的「静音」按钮：未授权勿扰访问则跳转授权页；已授权则静音并排下课恢复闹钟。 */
class MuteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MUTE) return
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: return

        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            val settings = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
        }

        val am = context.getSystemService(AudioManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // 仅首次静音时记录静音前模式；连堂课再次点静音不覆盖原始模式
        if (!prefs.getBoolean(KEY_MUTED_BY_US, false)) {
            prefs.edit()
                .putInt(KEY_PRIOR_RINGER_MODE, am.ringerMode)
                .putBoolean(KEY_MUTED_BY_US, true)
                .apply()
            am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }

        CourseReminderScheduler.scheduleRestoreAlarm(context, endTime)
    }
}
