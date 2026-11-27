package com.example.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.ThemeMode

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

data class LifeHubExtendedColors(
    val isDark: Boolean,
    val glassSurface: Color,
    val glassBorder: Color,
    val accentMint: Color,
    val accentViolet: Color,
    val accentCyan: Color,
    val accentCoral: Color,
    val accentAmber: Color,
    val accentLime: Color,
    val mutedText: Color,
)

val LocalLifeHubColors = staticCompositionLocalOf {
    LifeHubExtendedColors(
        isDark = true,
        glassSurface = Color(0xCC151B2B),
        glassBorder = Color(0x28FFFFFF),
        accentMint = NeonMint,
        accentViolet = CyberViolet,
        accentCyan = ElectricCyan,
        accentCoral = HotCoral,
        accentAmber = SolarAmber,
        accentLime = LimePulse,
        mutedText = InkDarkMuted
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NeonMint,
    onPrimary = Color(0xFF032016),
    primaryContainer = Color(0xFF0A3A2B),
    onPrimaryContainer = Color(0xFF7BFFD0),
    secondary = CyberVioletLight,
    onSecondary = Color(0xFF1F0B47),
    secondaryContainer = Color(0xFF2E1E5C),
    onSecondaryContainer = Color(0xFFE7DDFF),
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF04222A),
    tertiaryContainer = Color(0xFF0B3540),
    onTertiaryContainer = Color(0xFFB5F4FF),
    background = ObsidianBg,
    onBackground = InkDark,
    surface = ObsidianSurface,
    onSurface = InkDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = InkDarkMuted,
    surfaceContainerHigh = ObsidianSurfaceContainerHigh,
    outline = ObsidianOutline,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = Color(0xFF4C1120),
    onErrorContainer = Color(0xFFFFD8DF)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonMintDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5FAEC),
    onPrimaryContainer = Color(0xFF003824),
    secondary = CyberViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF280F66),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF083344),
    background = PearlBg,
    onBackground = InkLight,
    surface = PearlSurface,
    onSurface = InkLight,
    surfaceVariant = PearlSurfaceElevated,
    onSurfaceVariant = InkLightMuted,
    surfaceContainerHigh = PearlSurfaceContainerHigh,
    outline = PearlOutline,
    error = Color(0xFFD92D50),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4EA),
    onErrorContainer = Color(0xFF6B0B21)
)

val LifeHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LifeHubTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(useDark) {
            val activity = view.context.findActivity()
            if (activity != null) {
                val barStyle = if (useDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                activity.enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
                val insetsController = WindowCompat.getInsetsController(activity.window, view)
                insetsController.isAppearanceLightStatusBars = !useDark
                insetsController.isAppearanceLightNavigationBars = !useDark
            }
            onDispose {}
        }
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (useDark) {
        LifeHubExtendedColors(
            isDark = true,
            glassSurface = Color(0xCC161C2E),
            glassBorder = Color(0x2BFFFFFF),
            accentMint = NeonMint,
            accentViolet = CyberVioletLight,
            accentCyan = ElectricCyan,
            accentCoral = HotCoral,
            accentAmber = SolarAmber,
            accentLime = LimePulse,
            mutedText = InkDarkMuted
        )
    } else {
        LifeHubExtendedColors(
            isDark = false,
            glassSurface = Color(0xE6FFFFFF),
            glassBorder = Color(0x220F172A),
            accentMint = NeonMintDark,
            accentViolet = CyberViolet,
            accentCyan = Color(0xFF0891B2),
            accentCoral = Color(0xFFE11D48),
            accentAmber = Color(0xFFD97706),
            accentLime = Color(0xFF65A30D),
            mutedText = InkLightMuted
        )
    }

    CompositionLocalProvider(LocalLifeHubColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeHubTypography,
            shapes = LifeHubShapes,
            content = content
        )
    }
}
