# 课程提醒采用 AlarmManager 精确闹钟与滚动窗口调度

课程提醒在课程开始前 N 分钟和开始时触发通知。调度采用 AlarmManager 精确闹钟，仅对「今天 + 明天」有课的节次排闹钟（滚动窗口），而非为整个学期一次性排满。

## Considered Options

- **WorkManager 定时任务**：省去精确闹钟权限，但 Doze 模式下可能延迟数分钟到十几分钟；上课提醒对准时敏感，放弃。
- **全学期全量排程**：一次排满整个学期的节次，数据变更需全量重排，且逼近 AlarmManager 约 500 个闹钟的上限；滚动窗口轻量、重排代价小，故采用。

## Consequences

- 依赖 `SCHEDULE_EXACT_ALARM` 权限，未授权时降级为 `setAndAllowWhileIdle`（可能延迟），设置页以状态行提示。
- 需开机广播、时间/时区变更广播、数据变更（scheduleVersion 自增）与每日滚动闹钟兜底，保证窗口持续向前滚动。
