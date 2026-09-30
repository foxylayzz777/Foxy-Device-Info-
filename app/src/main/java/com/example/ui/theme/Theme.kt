package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeStyle(val displayName: String, val badge: String) {
    FOXY("Classic Fox", "🦊"),
    GAMING("RGB Gaming", "🎮"),
    CYBERPUNK("Cyberpunk", "⚡"),
    CUTE("Cute Kawaii", "🌸"),
    AMOLED("AMOLED Dark", "🖤"),
    DYNAMIC("Material You", "🎨")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Auto (System)"),
    LIGHT("Light"),
    DARK("Dark")
}

// 1. Classic Foxy
private val FoxyDarkScheme = darkColorScheme(
    primary = FoxyOrangeDark,
    onPrimary = Color(0xFF3E0A00),
    primaryContainer = FoxyOrangeLight,
    onPrimaryContainer = Color.White,
    secondary = FoxyAmberDark,
    onSecondary = Color(0xFF2E1500),
    secondaryContainer = FoxyAmberLight,
    onSecondaryContainer = Color.White,
    tertiary = FoxyTealDark,
    onTertiary = Color(0xFF003731),
    background = FoxySurfaceDark,
    onBackground = Color(0xFFEDE0DD),
    surface = FoxyCardDark,
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = FoxySurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD7C2B9),
    outline = Color(0xFF9F8C84),
    outlineVariant = Color(0xFF52443D)
)

private val FoxyLightScheme = lightColorScheme(
    primary = FoxyOrangeLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = FoxyAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC1),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = FoxyTealLight,
    onTertiary = Color.White,
    background = FoxySurfaceLight,
    onBackground = Color(0xFF201A18),
    surface = FoxyCardLight,
    onSurface = Color(0xFF201A18),
    surfaceVariant = FoxySurfaceVariantLight,
    onSurfaceVariant = Color(0xFF52443D),
    outline = Color(0xFF85736C),
    outlineVariant = Color(0xFFD7C2B9)
)

// 2. Gaming RGB Theme
private val GamingDarkScheme = darkColorScheme(
    primary = GamingGreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF005A28),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = GamingGreenSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFCCFF90),
    tertiary = GamingAccentRed,
    onTertiary = Color.White,
    background = GamingBgDark,
    onBackground = Color(0xFFE8F5E9),
    surface = GamingCardDark,
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = GamingSurfaceDark,
    onSurfaceVariant = Color(0xFFA5D6A7),
    outline = Color(0xFF4CAF50),
    outlineVariant = Color(0xFF1E3324)
)

private val GamingLightScheme = lightColorScheme(
    primary = Color(0xFF007E3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF003314),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002204),
    tertiary = Color(0xFFD50000),
    onTertiary = Color.White,
    background = Color(0xFFF3FBF5),
    onBackground = Color(0xFF0D1F12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1F12),
    surfaceVariant = Color(0xFFE0EFE4),
    onSurfaceVariant = Color(0xFF385540),
    outline = Color(0xFF6B9374),
    outlineVariant = Color(0xFFC4DCC8)
)

// 3. Cyberpunk Neon Theme
private val CyberpunkDarkScheme = darkColorScheme(
    primary = CyberYellow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF524800),
    onPrimaryContainer = CyberYellow,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D59),
    onSecondaryContainer = CyberCyan,
    tertiary = CyberMagenta,
    onTertiary = Color.White,
    background = CyberBgDark,
    onBackground = Color(0xFFF0ECF8),
    surface = CyberCardDark,
    onSurface = Color(0xFFF0ECF8),
    surfaceVariant = CyberSurfaceDark,
    onSurfaceVariant = Color(0xFFD0C9E0),
    outline = Color(0xFF7E729C),
    outlineVariant = Color(0xFF312A45)
)

private val CyberpunkLightScheme = lightColorScheme(
    primary = Color(0xFFA67C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF176),
    onPrimaryContainer = Color(0xFF332600),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color.White,
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1B1824),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1824),
    surfaceVariant = Color(0xFFEDE9F5),
    onSurfaceVariant = Color(0xFF4B445A),
    outline = Color(0xFF7E7590),
    outlineVariant = Color(0xFFD2CCE0)
)

// 4. Cute Kawaii Pastel Theme
private val CuteDarkScheme = darkColorScheme(
    primary = CutePinkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF880E4F),
    onPrimaryContainer = Color(0xFFFF80AB),
    secondary = CutePeachSecondary,
    onSecondary = Color(0xFF3E1118),
    secondaryContainer = Color(0xFF5D101E),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = CuteLavenderTertiary,
    onTertiary = Color(0xFF4A148C),
    background = CuteBgDark,
    onBackground = Color(0xFFFFF0F5),
    surface = CuteCardDark,
    onSurface = Color(0xFFFFF0F5),
    surfaceVariant = CuteSurfaceDark,
    onSurfaceVariant = Color(0xFFFFB6C1),
    outline = Color(0xFFBA68C8),
    outlineVariant = Color(0xFF482D3D)
)

private val CuteLightScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDE6),
    onPrimaryContainer = Color(0xFF4A0021),
    secondary = Color(0xFFFF6F60),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECEB),
    onSecondaryContainer = Color(0xFF441216),
    tertiary = Color(0xFF9C27B0),
    onTertiary = Color.White,
    background = CuteBgLight,
    onBackground = Color(0xFF28181F),
    surface = CuteCardLight,
    onSurface = Color(0xFF28181F),
    surfaceVariant = CuteSurfaceLight,
    onSurfaceVariant = Color(0xFF6B4E59),
    outline = Color(0xFF9E7784),
    outlineVariant = Color(0xFFF1D4DE)
)

// 5. AMOLED Pitch Black Theme
private val AmoledDarkScheme = darkColorScheme(
    primary = FoxyOrangeDark,
    onPrimary = Color(0xFF3E0A00),
    primaryContainer = Color(0xFF3E1C12),
    onPrimaryContainer = Color(0xFFFFCCBC),
    secondary = FoxyAmberDark,
    onSecondary = Color(0xFF2E1500),
    secondaryContainer = Color(0xFF3E2713),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = CyberCyan,
    onTertiary = Color.Black,
    background = AmoledBgDark,
    onBackground = Color(0xFFF5F5F5),
    surface = AmoledCardDark,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = AmoledSurfaceDark,
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF262626)
)

@Composable
fun FoxyTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.FOXY,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val context = LocalContext.current

    val colorScheme: ColorScheme = when (themeStyle) {
        AppThemeStyle.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) FoxyDarkScheme else FoxyLightScheme
            }
        }
        AppThemeStyle.GAMING -> if (isDark) GamingDarkScheme else GamingLightScheme
        AppThemeStyle.CYBERPUNK -> if (isDark) CyberpunkDarkScheme else CyberpunkLightScheme
        AppThemeStyle.CUTE -> if (isDark) CuteDarkScheme else CuteLightScheme
        AppThemeStyle.AMOLED -> if (isDark) AmoledDarkScheme else FoxyLightScheme
        AppThemeStyle.FOXY -> if (isDark) FoxyDarkScheme else FoxyLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = FoxyTheme(
    themeStyle = if (dynamicColor) AppThemeStyle.DYNAMIC else AppThemeStyle.FOXY,
    themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    content = content
)
