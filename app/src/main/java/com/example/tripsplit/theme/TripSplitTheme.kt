package com.example.tripsplit.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.R
import com.example.tripsplit.domain.TripSplitThemePreference

val TripSansFont = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

val TripMonoFont = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

/**
 * Clean, modern travel finance palette:
 * Warm ivory off-white, deep forest-charcoal text, burnt terracotta accent,
 * and subtle muted botanical sage surfaces. Minimal shadows, flat surfaces, zero navy/blue borders.
 */
@Immutable
data class TripSplitPalette(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceSage: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val divider: Color,
    val borderSubtle: Color,
    val accent: Color,             // Burnt orange / terracotta
    val accentSoft: Color,
    val onAccent: Color,
    val forest: Color,             // Deep botanical green (positive / wallet / on-track)
    val forestSoft: Color,
    val onForest: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val navBarBg: Color,
    val navIconInactive: Color
)

val TripSplitLightPalette = TripSplitPalette(
    isDark = false,
    canvas = Color(0xFFFBF9F5),         // Warm ivory off-white
    surface = Color(0xFFFFFFFF),        // Crisp flat white surface
    surfaceMuted = Color(0xFFF4F1EA),   // Warm stone neutral
    surfaceSage = Color(0xFFEEF3EF),    // Subtle muted botanical sage surface
    inkPrimary = Color(0xFF16241E),     // Deep forest-charcoal primary text
    inkSecondary = Color(0xFF475750),   // Muted pine secondary text
    inkMuted = Color(0xFF7B8881),       // Clean neutral caption ink
    divider = Color(0xFFE9E4DB),        // Hairline warm neutral divider
    borderSubtle = Color(0xFFE3DDD3),   // Subtle warm card outline
    accent = Color(0xFFC8552B),         // Burnt terracotta orange
    accentSoft = Color(0xFFFAEDE7),     // Soft warm terracotta tint
    onAccent = Color(0xFFFFFFFF),
    forest = Color(0xFF1F6F54),         // Rich botanical forest green
    forestSoft = Color(0xFFE4F1EC),     // Muted green pill surface
    onForest = Color(0xFFFFFFFF),
    warning = Color(0xFFB7791F),        // Warm ochre
    warningSoft = Color(0xFFFAF1DF),
    danger = Color(0xFFC23B2B),         // Editorial brick red
    dangerSoft = Color(0xFFFBEAE8),
    navBarBg = Color(0xFFFFFFFF),
    navIconInactive = Color(0xFF7B8881)
)

val TripSplitDarkPalette = TripSplitPalette(
    isDark = true,
    canvas = Color(0xFF121714),         // Deep warm charcoal-forest (not dark navy)
    surface = Color(0xFF1B221E),        // Flat charcoal surface
    surfaceMuted = Color(0xFF232C27),   // Subtle elevated neutral surface
    surfaceSage = Color(0xFF1C2923),    // Subtle dark sage surface
    inkPrimary = Color(0xFFF4F1EA),     // Warm ivory ink
    inkSecondary = Color(0xFFB3BEB7),   // Soft sage-grey ink
    inkMuted = Color(0xFF7E8C84),       // Muted slate caption
    divider = Color(0xFF2A342E),        // Subtle neutral dark divider
    borderSubtle = Color(0xFF2F3A34),   // Clean hairline outline
    accent = Color(0xFFE06E43),         // Warm terracotta accent
    accentSoft = Color(0xFF35221B),     // Muted dark terracotta surface
    onAccent = Color(0xFF140B07),
    forest = Color(0xFF42A884),         // Clean botanical green
    forestSoft = Color(0xFF183027),     // Muted dark green pill
    onForest = Color(0xFF071A13),
    warning = Color(0xFFE0A443),
    warningSoft = Color(0xFF332917),
    danger = Color(0xFFE86A5C),
    dangerSoft = Color(0xFF361D1B),
    navBarBg = Color(0xFF161D19),
    navIconInactive = Color(0xFF7E8C84)
)

val LocalTripSplitColors = staticCompositionLocalOf { TripSplitLightPalette }

private val TripSplitLightM3 = lightColorScheme(
    primary = Color(0xFFC8552B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFAEDE7),
    onPrimaryContainer = Color(0xFF5A210D),
    secondary = Color(0xFF1F6F54),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4F1EC),
    onSecondaryContainer = Color(0xFF0E392A),
    background = Color(0xFFFBF9F5),
    onBackground = Color(0xFF16241E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16241E),
    surfaceVariant = Color(0xFFF4F1EA),
    onSurfaceVariant = Color(0xFF475750),
    outline = Color(0xFFD7D0C4),
    outlineVariant = Color(0xFFE9E4DB),
    error = Color(0xFFC23B2B),
    onError = Color(0xFFFFFFFF)
)

private val TripSplitDarkM3 = darkColorScheme(
    primary = Color(0xFFE06E43),
    onPrimary = Color(0xFF140B07),
    primaryContainer = Color(0xFF35221B),
    onPrimaryContainer = Color(0xFFFFD8C9),
    secondary = Color(0xFF42A884),
    onSecondary = Color(0xFF071A13),
    secondaryContainer = Color(0xFF183027),
    onSecondaryContainer = Color(0xFFC2EDE0),
    background = Color(0xFF121714),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF1B221E),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF232C27),
    onSurfaceVariant = Color(0xFFB3BEB7),
    outline = Color(0xFF3C4942),
    outlineVariant = Color(0xFF2A342E),
    error = Color(0xFFE86A5C),
    onError = Color(0xFF2B0B08)
)

/**
 * Clean, modern sans-serif typography scale with restrained, practical sizing.
 */
val TripSplitTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = TripSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = TripMonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val TripSplitShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun TripSplitTheme(
    themePreference: TripSplitThemePreference = TripSplitThemePreference.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        TripSplitThemePreference.LIGHT -> false
        TripSplitThemePreference.DARK -> true
        TripSplitThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (isDark) TripSplitDarkPalette else TripSplitLightPalette
    val colorScheme = if (isDark) TripSplitDarkM3 else TripSplitLightM3

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalTripSplitColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TripSplitTypography,
            shapes = TripSplitShapes,
            content = content
        )
    }
}
