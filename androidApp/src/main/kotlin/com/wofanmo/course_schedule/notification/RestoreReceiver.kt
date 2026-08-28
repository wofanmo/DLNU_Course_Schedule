package com.wofanmo.course_schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_RESTORE
import com.wofanmo.course_schedule.notification.ReminderConstants.KEY_MUTED_BY_US
import com.wofanmo.course_schedule.notification.ReminderConstants.KEY_PRIOR_RINGER_MODE
import com.wofanmo.course_schedule.notification.ReminderConstants.PREFS

/** 课程结束：若仍处于我们设置的震动，则恢复静音前的铃声模式。 */
class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESTORE) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MUTED_BY_US, false)) return

        val am = context.getSystemService(AudioManager::class.java)
        // 仅当用户未中途手动调过铃声（仍为震动）时恢复，避免覆盖用户意图
        if (am.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            val prior = prefs.getInt(KEY_PRIOR_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
            try {
                am.ringerMode = prior
            } catch (_: Exception) {
                // 勿扰访问被撤销时无法恢复，忽略
            }
        }
        prefs.edit().putBoolean(KEY_MUTED_BY_US, false).apply()
    }
}
