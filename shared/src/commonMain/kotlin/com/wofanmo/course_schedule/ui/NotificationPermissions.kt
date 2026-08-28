package com.wofanmo.course_schedule.ui

/** 权限状态：已授权 / 未授权 / 当前系统版本无需该权限 */
enum class PermissionStatus { GRANTED, DENIED, NOT_APPLICABLE }

/** Android 13+ 通知权限状态 */
expect fun notificationPermissionStatus(): PermissionStatus

/** 请求通知权限（Android 13+ 运行时弹窗；更早版本无操作） */
expect fun requestNotificationPermission()

/** 是否已授予精确闹钟权限（Android 12+ 才有，更早版本视为已授权） */
expect fun exactAlarmPermissionGranted(): Boolean

/** 是否已授予勿扰模式访问权限（Android 6+） */
expect fun dndAccessGranted(): Boolean

/** 打开系统的通知权限设置页 */
expect fun openNotificationPermissionSettings()

/** 打开系统的精确闹钟权限设置页 */
expect fun openExactAlarmPermissionSettings()

/** 打开系统的勿扰访问设置页 */
expect fun openDndPermissionSettings()
