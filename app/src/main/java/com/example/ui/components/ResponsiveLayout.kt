package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 adaptive window width breakpoints derived strictly from available container width
 * (via BoxWithConstraints) so split-screen, foldables, tablets, and landscape behave accurately.
 */
enum class ScreenWidthTier {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class ResponsiveMetrics(
    val maxWidth: Dp,
    val maxHeight: Dp,
    val fontScale: Float
) {
    val widthTier: ScreenWidthTier
        get() = when {
            maxWidth >= 840.dp -> ScreenWidthTier.EXPANDED
            maxWidth >= 600.dp -> ScreenWidthTier.MEDIUM
            else -> ScreenWidthTier.COMPACT
        }

    val isNarrowPhone: Boolean get() = maxWidth < 360.dp
    val isCompactHeight: Boolean get() = maxHeight < 500.dp
    val isLandscape: Boolean get() = maxWidth > maxHeight
    val isLargeFont: Boolean get() = fontScale >= 1.2f

    /** True when side-by-side 2-column dense widgets should stack vertically to prevent clipping. */
    val shouldStackTightPairs: Boolean
        get() = maxWidth < 360.dp || (maxWidth < 410.dp && fontScale >= 1.2f) || fontScale >= 1.35f

    /** Adaptive screen edge horizontal padding that keeps narrow 320dp phones spacious. */
    val horizontalPadding: Dp
        get() = when {
            maxWidth < 340.dp -> 12.dp
            maxWidth >= 840.dp -> 24.dp
            else -> 16.dp
        }

    /** Readable maximum content width on tablets and landscape screens. */
    val contentMaxWidth: Dp
        get() = when {
            maxWidth >= 840.dp -> 840.dp
            maxWidth >= 600.dp -> 700.dp
            else -> maxWidth
        }
}

@Composable
fun rememberResponsiveMetrics(
    maxWidth: Dp,
    maxHeight: Dp
): ResponsiveMetrics {
    val fontScale = LocalDensity.current.fontScale
    return remember(maxWidth, maxHeight, fontScale) {
        ResponsiveMetrics(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            fontScale = fontScale
        )
    }
}

/**
 * Centers content within a readable max width on tablets/landscape while filling 100% width on phones.
 */
@Composable
fun ResponsiveCenteredPane(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 840.dp,
    content: @Composable BoxWithConstraintsScope.(ResponsiveMetrics) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
        val scope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxContentWidth)
        ) {
            scope.content(metrics)
        }
    }
}

/**
 * Renders two peer elements side-by-side in a Row on standard widths, or stacked vertically in a Column
 * when screen width is narrow or system font scale is enlarged.
 */
@Composable
fun AdaptivePairLayout(
    stackVertically: Boolean,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (stackVertically) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
