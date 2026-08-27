package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 底部导航栏组件，带动画效果
 *
 * 切换 tab 时：
 * - 选中项图标上浮 + 放大（1.15）
 * - 图标颜色渐变
 * - 选中项带胶囊形指示器背景
 */
@Composable
fun AnimatedNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    items: List<NavigationBarItem>
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index

            // 颜色渐变动画
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(durationMillis = 200),
                label = "iconColor"
            )

            // 选中缩放动画
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = tween(durationMillis = 200),
                label = "iconScale"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 选中指示器（胶囊背景）
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 图标（带缩放动画）
                        Box(
                            modifier = Modifier.scale(iconScale)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        color = iconColor,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
        }
    }
}

data class NavigationBarItem(
    val icon: ImageVector,
    val label: String
)
