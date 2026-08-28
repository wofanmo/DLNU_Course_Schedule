package com.wofanmo.course_schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.wofanmo.course_schedule.data.crypto.Crypto
import com.wofanmo.course_schedule.data.storage.initStorage
import com.wofanmo.course_schedule.widget.TodayCoursesWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化存储和加密模块
        initStorage(this)
        Crypto.init(this)

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // 应用回到前台时刷新桌面小部件（应用内的课程改动即时反映到桌面）
        lifecycleScope.launch {
            TodayCoursesWidget().updateAll(this@MainActivity)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}