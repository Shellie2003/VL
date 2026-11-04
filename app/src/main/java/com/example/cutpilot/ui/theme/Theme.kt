package com.example.cutpilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBg,
    primaryContainer = SurfaceContainerHighDark,
    onPrimaryContainer = NeonCyan,
    secondary = AmberAccent,
    onSecondary = DarkBg,
    secondaryContainer = AmberDim,
    onSecondaryContainer = AmberAccent,
    tertiary = PurpleAccent,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = RazorRed,
    onError = DarkBg
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
        content = content
    )
}
