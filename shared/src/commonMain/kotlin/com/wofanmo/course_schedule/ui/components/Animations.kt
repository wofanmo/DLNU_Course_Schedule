package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 列表项渐入动画
 * 从下方 20dp 处淡入滑上
 */
@Composable
fun AnimatedListItem(
    modifier: Modifier = Modifier,
    index: Int,
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = index * 50,
            easing = EaseOut
        ),
        label = "offset"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = index * 50,
            easing = EaseOut
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(0, animatedOffset.toInt()) }
            .alpha(animatedAlpha)
    ) {
        content()
    }
}

/**
 * 页面转场动画 - 淡入淡出
 */
@Composable
fun PageTransition(
    modifier: Modifier = Modifier,
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 空状态动画 - 漂浮书本
 * 书本缓慢上下漂浮，书页有轻微翻动效果
 */
@Composable
fun FloatingBookAnimation(
    modifier: Modifier = Modifier,
    size: Int = 120
) {
    val infiniteTransition = rememberInfiniteTransition(label = "book")

    // 上下漂浮动画
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    // 书本旋转动画（轻微摇摆）
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // 书页翻动动画
    val pageFlip by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pageFlip"
    )

    Canvas(modifier = modifier.size(size.dp)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2 + offsetY

        rotate(rotation) {
            // 绘制书本封面
            drawRect(
                color = Color(0xFF6366F1),
                topLeft = Offset(centerX - 40, centerY - 30),
                size = androidx.compose.ui.geometry.Size(80f, 60f)
            )

            // 绘制书脊
            drawRect(
                color = Color(0xFF4F46E5),
                topLeft = Offset(centerX - 40, centerY - 30),
                size = androidx.compose.ui.geometry.Size(8f, 60f)
            )

            // 绘制书页（动态翻动）
            val pageWidth = 70f * (1f - pageFlip * 0.3f)
            drawRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(centerX - 32, centerY - 25),
                size = androidx.compose.ui.geometry.Size(pageWidth, 50f)
            )

            // 绘制文字线条
            for (i in 0..3) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(centerX - 25, centerY - 15 + i * 12),
                    end = Offset(centerX + 25, centerY - 15 + i * 12),
                    strokeWidth = 2f
                )
            }
        }
    }
}

/**
 * 可展开设置项动画
 * 箭头旋转 + 高度展开 + 内容淡入
 */
@Composable
fun ExpandableSection(
    modifier: Modifier = Modifier,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200, easing = EaseOut),
        label = "arrowRotation"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(200, easing = EaseOut),
        label = "contentAlpha"
    )

    Column(modifier = modifier) {
        // 标题行（可点击）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )
            Canvas(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotation)
            ) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(6f, 9f)
                    lineTo(12f, 15f)
                    lineTo(18f, 9f)
                }
                drawPath(
                    path = path,
                    color = Color.Gray,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }

        // 展开的内容
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier.alpha(contentAlpha)
            ) {
                content()
            }
        }
    }
}

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
private val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
