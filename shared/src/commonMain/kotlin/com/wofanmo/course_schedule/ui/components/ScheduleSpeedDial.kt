package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector

data class SpeedDialItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * 课表界面右下角的 Speed Dial：圆形加号 FAB，点击展开带标签的动作项，
 * 展开时加号旋转为关闭态。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleSpeedDial(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val iconRotation by animateFloatAsState(if (expanded) 135f else 0f)

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            FloatingActionButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (expanded) "收起" else "添加课程",
                    modifier = Modifier.rotate(iconRotation)
                )
            }
        }
    ) {
        items.forEach { item ->
            FloatingActionButtonMenuItem(
                onClick = {
                    expanded = false
                    item.onClick()
                },
                text = { Text(item.label) },
                icon = { Icon(item.icon, contentDescription = null) }
            )
        }
    }
}
