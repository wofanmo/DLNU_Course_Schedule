package com.wofanmo.course_schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.wofanmo.course_schedule.ui.theme.DarkGradientEnd
import com.wofanmo.course_schedule.ui.theme.DarkGradientStart
import com.wofanmo.course_schedule.ui.theme.LightGradientEnd
import com.wofanmo.course_schedule.ui.theme.LightGradientStart

/**
 * 渐变背景组件
 * 根据明暗主题显示不同的柔和渐变
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val gradientColors = if (isDarkTheme) {
        listOf(DarkGradientStart, DarkGradientEnd)
    } else {
        listOf(LightGradientStart, LightGradientEnd)
    }

    Box(
        modifier = modifier
            .background(brush = Brush.verticalGradient(colors = gradientColors)),
        content = content
    )
}
