package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FoxyOrangeDark,
    onPrimary = FoxySurfaceDark,
    primaryContainer = FoxyOrangeLight,
    onPrimaryContainer = FoxyCardLight,
    secondary = FoxyAmberDark,
    onSecondary = FoxySurfaceDark,
    secondaryContainer = FoxyAmberLight,
    onSecondaryContainer = FoxyCardLight,
    tertiary = FoxyTealDark,
    background = FoxySurfaceDark,
    surface = FoxyCardDark,
    surfaceVariant = FoxySurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = FoxyOrangeLight,
    onPrimary = FoxyCardLight,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = FoxyAmberLight,
    onSecondary = FoxyCardLight,
    secondaryContainer = Color(0xFFFFDCC1),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = FoxyTealLight,
    background = FoxySurfaceLight,
    surface = FoxyCardLight,
    surfaceVariant = FoxySurfaceVariantLight
)

@Composable
fun FoxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for any legacy references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = FoxyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
