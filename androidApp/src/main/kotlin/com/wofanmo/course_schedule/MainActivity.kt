package com.wofanmo.course_schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.tooling.preview.Preview
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.wofanmo.course_schedule.data.crypto.Crypto
import com.wofanmo.course_schedule.data.storage.initStorage
import com.wofanmo.course_schedule.notification.CourseReminderScheduler
import com.wofanmo.course_schedule.ui.bindPlatformActivity
import com.wofanmo.course_schedule.ui.initPlatformContext
import com.wofanmo.course_schedule.widget.TodayCoursesWidget
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化存储、加密与平台上下文
        initStorage(this)
        Crypto.init(this)
        initPlatformContext(this)

        // 数据变化（导入/增删课/切课表/改开学日期）时重排课程提醒闹钟
        lifecycleScope.launch {
            snapshotFlow { AppEvents.scheduleVersion.intValue }
                .drop(1)
                .collect { CourseReminderScheduler.scheduleAll(this@MainActivity) }
        }

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        bindPlatformActivity(this)
        // 应用回到前台时刷新桌面小部件 + 重排课程提醒闹钟
        lifecycleScope.launch {
            TodayCoursesWidget().updateAll(this@MainActivity)
        }
        CourseReminderScheduler.scheduleAll(this)
    }

    override fun onPause() {
        super.onPause()
        bindPlatformActivity(null)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
