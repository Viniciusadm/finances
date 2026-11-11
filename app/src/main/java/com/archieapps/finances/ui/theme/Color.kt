package com.archieapps.finances.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

val Ink = Color(0xFF090C13)
val Surface = Color(0xFF131824)
val SurfaceLift = Color(0xFF1D2433)
val SurfaceHigh = Color(0xFF262F41)
val Hairline = Color(0xFF242C3C)

val Primary = Color(0xFF5B8CFF)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryDim = Color(0xFF2B4BB0)
val Accent = Color(0xFF8E6CFF)

val TextStrong = Color(0xFFF2F5FA)
val TextMuted = Color(0xFF9AA4B5)
val TextFaint = Color(0xFF636E82)

val Positive = Color(0xFF34D399)
val Negative = Color(0xFFFF6B7D)
val Warning = Color(0xFFFBBF24)

val HeroColor = Color(0xFF2F5BD8)
val ButtonColor = Color(0xFF3D6CF0)

val HeroBrush: Brush = SolidColor(HeroColor)
val PrimaryBrush: Brush = SolidColor(ButtonColor)

fun Color.soft(alpha: Float = 0.16f) = copy(alpha = alpha)
