package com.nasmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF1ED760)
val Background = Color(0xFF121212)
val Surface1 = Color(0xFF1F1F1F)
val Surface2 = Color(0xFF2A2A2A)
val Muted = Color(0xFFB3B3B3)
val Liked = Color(0xFF5038A0)

private val colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Accent,
    background = Background,
    onBackground = Color.White,
    surface = Background,
    onSurface = Color.White,
    surfaceVariant = Surface1,
    onSurfaceVariant = Muted,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Color(0xFF333333),
    surfaceContainerLow = Color(0xFF181818),
    outline = Color(0xFF727272),
    error = Color(0xFFF15E6C),
)

private val typography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
fun NasMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

/** A stable, muted colour per id, used behind headers when there is no cover. */
fun hueColor(seed: Long): Color {
    val hue = ((seed * 137.508) % 360).toFloat()
    return Color.hsl(hue, 0.45f, 0.35f)
}
