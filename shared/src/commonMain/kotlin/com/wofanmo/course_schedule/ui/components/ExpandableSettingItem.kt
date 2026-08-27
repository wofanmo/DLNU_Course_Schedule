package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 可展开的设置项组件
 * 包含箭头旋转 + 高度展开 + 内容淡入动画
 */
@Composable
fun ExpandableSettingItem(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // 箭头旋转动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = EaseOut),
        label = "arrowRotation"
    )

    // 在 Composable 上下文中获取颜色
    val surfaceColor = MaterialTheme.colorScheme.onSurface

    // 图标大小随系统字体缩放自适应（放大字体时箭头同步放大）
    val fontScale = LocalDensity.current.fontScale
    val iconSize = (24 * fontScale).dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行（可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = surfaceColor
                )

                // 箭头图标：坐标相对 Canvas 尺寸按比例绘制，随 iconSize 缩放
                Canvas(
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(arrowRotation)
                ) {
                    val arrowPath = Path().apply {
                        moveTo(0.25f * size.width, 0.375f * size.height)
                        lineTo(0.5f * size.width, 0.625f * size.height)
                        lineTo(0.75f * size.width, 0.375f * size.height)
                    }
                    drawPath(
                        path = arrowPath,
                        color = surfaceColor,
                        style = Stroke(
                            width = 2f * fontScale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // 展开的内容
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)) +
                        expandVertically(
                            animationSpec = tween(200),
                            expandFrom = Alignment.Top
                        ),
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(
                            animationSpec = tween(200),
                            shrinkTowards = Alignment.Top
                        )
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
