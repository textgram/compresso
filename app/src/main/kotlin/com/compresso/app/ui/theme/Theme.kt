package com.compresso.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = SignalIndigo,
    onPrimary = InkBackground,
    secondary = ReadoutAmber,
    onSecondary = InkBackground,
    tertiary = VerifiedGreen,
    onTertiary = InkBackground,
    background = InkBackground,
    onBackground = InkOnBackground,
    surface = InkSurface,
    onSurface = InkOnBackground,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = InkOnSurfaceVariant,
    outline = InkOutline,
    error = ErrorRed,
    onError = InkBackground
)

private val LightColors = lightColorScheme(
    primary = SignalIndigoOnLight,
    onPrimary = Color.White,
    secondary = ReadoutAmberOnLight,
    onSecondary = Color.White,
    tertiary = VerifiedGreenOnLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorRedOnLight,
    onError = Color.White
)

@Composable
fun CompressoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            val window = activity?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CompressoTypography,
        content = content
    )
}
