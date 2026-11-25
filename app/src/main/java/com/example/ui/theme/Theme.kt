package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WinterDarkColorScheme = darkColorScheme(
    primary = IcyCyan,
    onPrimary = ArcticMidnight,
    primaryContainer = GlacialSurfaceVariant,
    onPrimaryContainer = SnowWhite,
    secondary = GlacierBlue,
    onSecondary = ArcticMidnight,
    secondaryContainer = FrostedSlate,
    onSecondaryContainer = SnowWhite,
    tertiary = WarmCottageAmber,
    onTertiary = ArcticMidnight,
    background = ArcticMidnight,
    onBackground = SnowWhite,
    surface = DeepWinterNavy,
    onSurface = SnowWhite,
    surfaceVariant = FrostedSlate,
    onSurfaceVariant = FrostTextMuted,
    outline = Color(0xFF345685)
)

private val WinterLightColorScheme = lightColorScheme(
    primary = LightSapphirePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAFF),
    onPrimaryContainer = Color(0xFF072342),
    secondary = LightSkySecondary,
    onSecondary = Color.White,
    tertiary = LightAmberTertiary,
    onTertiary = Color.White,
    background = LightSnowBackground,
    onBackground = Color(0xFF0D1B2A),
    surface = LightIceSurface,
    onSurface = Color(0xFF0D1B2A),
    surfaceVariant = Color(0xFFE1EEF9),
    onSurfaceVariant = Color(0xFF3E5670),
    outline = Color(0xFFAEC7E0)
)

@Composable
fun QorliFonTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WinterDarkColorScheme
        else -> WinterLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
