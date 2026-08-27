package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * 主题切换圆形扩散动画
 *
 * 当用户切换主题时，新主题从点击位置以圆形向外扩散覆盖旧主题
 */
@Composable
fun ThemeTransition(
    isDarkTheme: Boolean,
    transitionOffset: Offset?,
    onTransitionComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val radius = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // 计算最大半径（对角线长度）
    val maxRadius = if (size.width > 0 && size.height > 0) {
        kotlin.math.sqrt(
            (size.width * size.width + size.height * size.height).toDouble()
        ).toFloat()
    } else {
        0f
    }

    // 启动扩散动画
    LaunchedEffect(transitionOffset, isDarkTheme) {
        if (transitionOffset != null && maxRadius > 0) {
            radius.snapTo(0f)
            scope.launch {
                radius.animateTo(
                    targetValue = maxRadius,
                    animationSpec = tween(durationMillis = 600)
                )
                onTransitionComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        // 旧主题内容
        content()

        // 新主题覆盖层（圆形扩散）
        if (transitionOffset != null && radius.value > 0) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val path = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            transitionOffset.x - radius.value,
                            transitionOffset.y - radius.value,
                            transitionOffset.x + radius.value,
                            transitionOffset.y + radius.value
                        )
                    )
                }

                clipPath(path) {
                    drawRect(
                        color = if (isDarkTheme) Color.Black else Color.White,
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                }
            }
        }
    }
}
