package com.example.cutpilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Bg,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Accent,
    onSecondary = Bg,
    secondaryContainer = Color(0x292DD4BF),
    onSecondaryContainer = Accent,
    tertiary = Warning,
    onTertiary = Bg,
    background = Bg,
    onBackground = OnBg,
    surface = Surface1,
    onSurface = OnBg,
    surfaceVariant = Surface2,
    onSurfaceVariant = OnBgMuted,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface3,
    surfaceContainerHighest = Surface3,
    outline = Outline,
    outlineVariant = Surface3,
    error = Danger,
    onError = Bg
)

@Composable
fun CutPilotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = StudioDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBg.toArgb()
                window.navigationBarColor = DarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VolavoShapes,
        content = content
    )
}
