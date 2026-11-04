package com.example.cutpilot.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Vôlavo design tokens — dark "midnight" palette.
// Every text/background pair below passes WCAG AA (≥ 4.5:1) on Bg and Surface.
// =============================================================================

// Surfaces (elevation goes up = lighter)
val Bg = Color(0xFF0D1017)
val Surface1 = Color(0xFF151922)
val Surface2 = Color(0xFF1C2130)
val Surface3 = Color(0xFF262C3D)
val Outline = Color(0xFF323A4F)

// Brand & semantic colors
val Primary = Color(0xFF8B7CFF)      // Vôlavo violet — main actions, selection
val PrimarySoft = Color(0x298B7CFF)  // 16 % tint for selected backgrounds
val Accent = Color(0xFF2DD4BF)       // teal — secondary highlights
val Success = Color(0xFF3DDC97)      // connected, playing
val Warning = Color(0xFFFFB547)      // modes, attention
val Danger = Color(0xFFFF6B6B)       // cut, delete, errors
val Info = Color(0xFF60A5FA)

// Text
val OnBg = Color(0xFFECEFF5)
val OnBgMuted = Color(0xFFA7B0C2)
val OnBgSubtle = Color(0xFF8590A6)

// -----------------------------------------------------------------------------
// Legacy names used across the screens, mapped onto the new tokens.
// -----------------------------------------------------------------------------
val DarkBg = Bg
val SurfaceDark = Surface1
val SurfaceContainerDark = Surface2
val SurfaceContainerHighDark = Surface3
val BorderDark = Outline

val NeonCyan = Primary
val NeonCyanDim = PrimarySoft
val AmberAccent = Warning
val AmberDim = Color(0x29FFB547)
val RazorRed = Danger
val RazorRedDim = Color(0x29FF6B6B)
val EmeraldGreen = Success
val EmeraldGreenDim = Color(0x293DDC97)
val PurpleAccent = Accent
val PurpleDim = Color(0x292DD4BF)
val ElectricBlue = Info

val TextPrimary = OnBg
val TextSecondary = OnBgMuted
val TextTertiary = OnBgSubtle

val Purple80 = Primary
val PurpleGrey80 = OnBgMuted
val Pink80 = Warning
val Purple40 = Color(0xFF5B4BD6)
val PurpleGrey40 = Surface3
val Pink40 = Color(0xFFE09A2E)
