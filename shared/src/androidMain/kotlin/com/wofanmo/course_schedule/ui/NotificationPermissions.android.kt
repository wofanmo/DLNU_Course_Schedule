package com.wofanmo.course_schedule.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** 应用上下文（由 androidApp 的 MainActivity 注入），供权限查询与打开系统设置使用。 */
private lateinit var appContext: Context

/** 当前前台 Activity（由 MainActivity 在 onResume/onPause 绑定），供运行时权限请求使用。 */
private var currentActivity: Activity? = null

fun initPlatformContext(context: Context) {
    appContext = context.applicationContext
}

fun bindPlatformActivity(activity: Activity?) {
    currentActivity = activity
}

actual fun notificationPermissionStatus(): PermissionStatus {
    if (Build.VERSION.SDK_INT < 33) return PermissionStatus.NOT_APPLICABLE
    return if (appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) PermissionStatus.GRANTED else PermissionStatus.DENIED
}

actual fun requestNotificationPermission() {
    val activity = currentActivity ?: return
    if (Build.VERSION.SDK_INT >= 33 &&
        activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}

actual fun exactAlarmPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    val am = appContext.getSystemService(AlarmManager::class.java)
    return am.canScheduleExactAlarms()
}

actual fun dndAccessGranted(): Boolean {
    val nm = appContext.getSystemService(NotificationManager::class.java)
    return nm.isNotificationPolicyAccessGranted
}

actual fun openNotificationPermissionSettings() {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    } catch (_: Exception) {
    }
}

actual fun openExactAlarmPermissionSettings() {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    } catch (_: Exception) {
    }
}

actual fun openDndPermissionSettings() {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    } catch (_: Exception) {
    }
}
