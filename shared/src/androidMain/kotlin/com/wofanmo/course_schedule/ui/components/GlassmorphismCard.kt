package com.wofanmo.course_schedule.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader

/**
 * Android 平台高斯模糊卡片实现
 *
 * Android 12+ (API 31): 使用 RenderEffect.createBlurEffect 实现原生高斯模糊
 * Android 12-: 使用半透明白色叠加层模拟毛玻璃效果
 *
 * 背景层模糊，内容层保持清晰
 */
@Composable
actual fun GlassmorphismCard(
    modifier: Modifier,
    blurRadius: Dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val radiusPx = blurRadius.value
        // Android 12+: 背景层原生模糊，内容层清晰
        Box(modifier = modifier) {
            // 模糊背景层
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .graphicsLayer {
                        this.renderEffect = AndroidRenderEffect.createBlurEffect(
                            radiusPx, radiusPx, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                        this.clip = true
                    }
            )
            // 清晰内容层
            Box(modifier = Modifier.clip(shape)) {
                content()
            }
        }
    } else {
        // Android 12 以下: 半透明叠加层模拟
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            content()
        }
    }
}
