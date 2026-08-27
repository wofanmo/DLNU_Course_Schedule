package com.wofanmo.course_schedule.ui.theme

import androidx.compose.ui.graphics.Color

// ── 主色板（深青 / 靛蓝，课程表应用主流色调）────────────────────
val PrimaryLight = Color(0xFF1A6B52)        // 深青（亮模式主色）
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFA4F4CB)
val OnPrimaryContainerLight = Color(0xFF002115)

val PrimaryDark = Color(0xFF88D8B0)         // 浅青（暗模式主色）
val OnPrimaryDark = Color(0xFF003827)
val PrimaryContainerDark = Color(0xFF00513B)
val OnPrimaryContainerDark = Color(0xFFA4F4CB)

// ── 次色（暖棕，用于辅助强调）────────────────────
val SecondaryLight = Color(0xFF4D6356)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD0E8D8)
val OnSecondaryContainerLight = Color(0xFF0B1F15)

val SecondaryDark = Color(0xFFB4CCBC)
val OnSecondaryDark = Color(0xFF203529)
val SecondaryContainerDark = Color(0xFF364B3F)
val OnSecondaryContainerDark = Color(0xFFD0E8D8)

// ── 第三色（靛蓝，用于警示/高亮）────────────────────
val TertiaryLight = Color(0xFF3D6373)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC1E8FB)
val OnTertiaryContainerLight = Color(0xFF001F29)

val TertiaryDark = Color(0xFFA5CCE0)
val OnTertiaryDark = Color(0xFF073543)
val TertiaryContainerDark = Color(0xFF244B5A)
val OnTertiaryContainerDark = Color(0xFFC1E8FB)

// ── 表面 / 背景 ────────────────────
val SurfaceLight = Color(0xFFF5FBF7)
val OnSurfaceLight = Color(0xFF171D1A)
val SurfaceVariantLight = Color(0xFFDBE5DE)
val OnSurfaceVariantLight = Color(0xFF404943)

val SurfaceDark = Color(0xFF0F1512)
val OnSurfaceDark = Color(0xFFDEE4E0)
val SurfaceVariantDark = Color(0xFF404943)
val OnSurfaceVariantDark = Color(0xFFBFC9C2)

// ── 错误色 ────────────────────
val ErrorLight = Color(0xFFBA1A1A)
val ErrorDark = Color(0xFFFFB4AB)

// ── 背景渐变（用于全局渐变背景）────────────────────
val LightGradientStart = Color(0xFFF0FAF5)
val LightGradientEnd   = Color(0xFFE8F0FE)
val DarkGradientStart  = Color(0xFF0A1A14)
val DarkGradientEnd    = Color(0xFF0E1A24)

// ── 马卡龙色系 - 课程卡片（保留，微调饱和度）────────────
val MacaronBlue    = Color(0xFF7EC8E3)
val MacaronPurple  = Color(0xFFBB8FCE)
val MacaronPink    = Color(0xFFF1948A)
val MacaronGreen   = Color(0xFF82C99B)
val MacaronYellow  = Color(0xFFF7DC6F)
val MacaronOrange  = Color(0xFFF0B27A)
val MacaronCyan    = Color(0xFF76D7C4)
val MacaronIndigo  = Color(0xFF9FA8DA)

val CourseColorPalette = listOf(
    MacaronBlue,
    MacaronGreen,
    MacaronPurple,
    MacaronOrange,
    MacaronCyan,
    MacaronPink,
    MacaronYellow,
    MacaronIndigo,
)
