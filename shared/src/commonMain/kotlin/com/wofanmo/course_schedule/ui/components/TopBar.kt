package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 顶部栏组件，带滚动反馈效果
 *
 * 当页面滚动时：
 * - 背景从透明渐变到毛玻璃效果
 * - 底部出现阴影
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String,
    isScrolled: Boolean
) {
    // 背景颜色动画：透明 → 毛玻璃
    val bgColor by animateColorAsState(
        targetValue = if (isScrolled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "topBarBg"
    )

    // 阴影高度动画：0 → 4dp
    val shadowElevation = if (isScrolled) 4.dp else 0.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = shadowElevation)
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
