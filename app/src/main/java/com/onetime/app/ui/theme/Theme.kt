package com.onetime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 色值取自「全面屏」基准图取样：
 * 纸面 #F8F4F0 / 珊瑚 #C87261 / 金 #F1C057 / 灰绿 #88937F / 墨 #3A3836
 */
data class PaperPalette(
    val paper: Color,
    val paperDeep: Color,
    val card: Color,
    val cardHigh: Color,
    val inset: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val line: Color,
    val coral: Color,
    val coralDeep: Color,
    val coralWash: Color,
    val gold: Color,
    val goldDeep: Color,
    val goldWash: Color,
    val sage: Color,
    val sageDeep: Color,
    val danger: Color,
    val shadow: Color,
    val isNight: Boolean,
)

val LightPalette = PaperPalette(
    paper = Color(0xFFF8F4F0),
    paperDeep = Color(0xFFF1EBE4),
    card = Color(0xFFFCFAF7),
    cardHigh = Color(0xFFFFFFFF),
    inset = Color(0xFFF1ECE5),
    ink = Color(0xFF3A3836),
    inkSoft = Color(0xFF6B6661),
    muted = Color(0xFF9C958E),
    line = Color(0xFFE7DFD6),
    coral = Color(0xFFC87261),
    coralDeep = Color(0xFFB25F4E),
    coralWash = Color(0xFFF9E4DE),
    gold = Color(0xFFF1C057),
    goldDeep = Color(0xFFB07E2E),
    goldWash = Color(0xFFF8E7C8),
    sage = Color(0xFF88937F),
    sageDeep = Color(0xFF6F7A66),
    danger = Color(0xFFB4453C),
    shadow = Color(0xFF9C8674),
    isNight = false,
)

/** 夜间主题：同一套纸面语言压暗，色相不变 */
val NightPalette = PaperPalette(
    paper = Color(0xFF191715),
    paperDeep = Color(0xFF141210),
    card = Color(0xFF232120),
    cardHigh = Color(0xFF2A2725),
    inset = Color(0xFF1E1C1A),
    ink = Color(0xFFEDE6DE),
    inkSoft = Color(0xFFB4ABA1),
    muted = Color(0xFF8B837A),
    line = Color(0xFF3A3531),
    coral = Color(0xFFD98878),
    coralDeep = Color(0xFFE09C8C),
    coralWash = Color(0xFF3E2A26),
    gold = Color(0xFFF1C057),
    goldDeep = Color(0xFFE0AE55),
    goldWash = Color(0xFF3A2F1F),
    sage = Color(0xFF9DAB92),
    sageDeep = Color(0xFFB7C4AC),
    danger = Color(0xFFE08A80),
    shadow = Color(0xFF000000),
    isNight = true,
)

val LocalPaper = staticCompositionLocalOf { LightPalette }

/** 便捷访问：Paper.ink / Paper.coral … */
object Paper {
    val current: PaperPalette
        @Composable get() = LocalPaper.current
}

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun OneTimeTheme(night: Boolean, content: @Composable () -> Unit) {
    val palette = if (night) NightPalette else LightPalette
    val scheme = if (night) {
        darkColorScheme(
            primary = palette.coral,
            onPrimary = Color(0xFF2A1310),
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.inset,
            onSurfaceVariant = palette.inkSoft,
            background = palette.paper,
            onBackground = palette.ink,
            outline = palette.line,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.coral,
            onPrimary = Color.White,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.inset,
            onSurfaceVariant = palette.inkSoft,
            background = palette.paper,
            onBackground = palette.ink,
            outline = palette.line,
            error = palette.danger,
        )
    }
    CompositionLocalProvider(LocalPaper provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
