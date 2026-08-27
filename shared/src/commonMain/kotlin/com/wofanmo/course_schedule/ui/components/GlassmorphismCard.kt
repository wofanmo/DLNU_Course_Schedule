package com.wofanmo.course_schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 跨平台高斯模糊卡片组件
 * Android 12+ 使用 RenderEffect 原生模糊，其他版本/平台使用半透明叠加层模拟
 */
@Composable
expect fun GlassmorphismCard(
    modifier: Modifier,
    blurRadius: Dp,
    content: @Composable () -> Unit
)
