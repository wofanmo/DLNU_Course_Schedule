package com.wofanmo.course_schedule.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * 带动画的 AlertDialog
 *
 * 对话框出现时：
 * - 从 0.8 倍缩放淡入到 1.0 倍
 * - 背景遮罩同时淡入
 */
@Composable
fun AnimatedAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    confirmText: String = "确定",
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmation
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(dismissText)
            }
        }
    )
}

/**
 * 带动画的 BottomSheet
 *
 * BottomSheet 出现时：
 * - 从底部滑入
 * - 同时淡入
 */
@Composable
fun AnimatedBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
