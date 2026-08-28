package com.wofanmo.course_schedule.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wofanmo.course_schedule.MainActivity
import com.wofanmo.course_schedule.R
import com.wofanmo.course_schedule.notification.ReminderConstants.ACTION_MUTE
import com.wofanmo.course_schedule.notification.ReminderConstants.CHANNEL_REMINDER
import com.wofanmo.course_schedule.notification.ReminderConstants.CHANNEL_START
import com.wofanmo.course_schedule.notification.ReminderConstants.EXTRA_END_TIME

/** 通知渠道与通知构建。 */
object ReminderNotificationHelper {

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "课程提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "上课前的提前提醒"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_START, "上课开始", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "课程开始提醒，可一键静音"
            }
        )
    }

    /** 提前提醒：纯文字通知，点击打开应用。 */
    fun postPreReminder(context: Context, title: String, text: String, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        notifyIfAllowed(context, notificationId, notification)
    }

    /** 开始通知：带「静音」按钮，点击静音并安排下课自动恢复。 */
    fun postStartReminder(
        context: Context,
        title: String,
        text: String,
        endTime: String,
        notificationId: Int,
    ) {
        val muteIntent = Intent(context, MuteActionReceiver::class.java).apply {
            action = ACTION_MUTE
            putExtra(EXTRA_END_TIME, endTime)
        }
        val mutePi = PendingIntent.getBroadcast(
            context, notificationId, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val muteAction = NotificationCompat.Action.Builder(0, "静音", mutePi).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_START)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .addAction(muteAction)
            .setAutoCancel(true)
            .build()
        notifyIfAllowed(context, notificationId, notification)
    }

    /**
     * Android 13+ 用户拒绝通知权限后 notify() 会抛 SecurityException（闹钟接收器崩溃）；
     * 发送前先检查通知总开关，未授权时静默跳过。
     */
    private fun notifyIfAllowed(context: Context, notificationId: Int, notification: Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            nm.notify(notificationId, notification)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
