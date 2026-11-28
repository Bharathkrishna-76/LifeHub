package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LimePulse
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber

// ============================================================================
// 1. FROSTED GLASS UI (Specular Translucent Glassmorphism)
// ============================================================================
@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentGlow: Color = NeonMint,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = LocalLifeHubColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceBrush = Brush.linearGradient(
        colors = if (extended.isDark) {
            listOf(
                Color(0xD61A2238),
                Color(0xB8111626),
                accentGlow.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color(0xF2FFFFFF),
                Color(0xD9F1F5FF),
                accentGlow.copy(alpha = 0.08f)
            )
        }
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (extended.isDark) 0.34f else 0.9f),
            accentGlow.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.06f)
        )
    )
    val baseModifier = modifier
        .clip(shape)
        .background(surfaceBrush)
        .border(BorderStroke(1.2.dp, borderBrush), shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Column(
        modifier = baseModifier.padding(18.dp),
        content = content
    )
}

// ============================================================================
// 2. PRIVATE VAULT UI (Obsidian Carbon + Gold in Dark / Champagne Ivory in Light)
// ============================================================================
val ChampagneGold = Color(0xFFE6C67D)
val ChampagneGoldLight = Color(0xFF9A6B16)
val StealthCarbon = Color(0xFF0E1015)
val StealthCarbonElevated = Color(0xFF161922)

@Composable
fun PremiumDarkVaultCard(
    modifier: Modifier = Modifier,
    accentGold: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalLifeHubColors.current.isDark
    val effectiveGold = accentGold ?: if (isDark) ChampagneGold else ChampagneGoldLight
    val shape = RoundedCornerShape(22.dp)
    val bgBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(StealthCarbonElevated, StealthCarbon))
    } else {
        Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFF6F1E6)))
    }
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                effectiveGold.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.12f),
                effectiveGold.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                effectiveGold.copy(alpha = 0.45f),
                Color(0xFFDCD3C2),
                effectiveGold.copy(alpha = 0.28f)
            )
        )
    }
    val mod = modifier
        .clip(shape)
        .background(bgBrush)
        .border(BorderStroke(1.dp, borderBrush), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Column(
        modifier = mod.padding(18.dp),
        content = content
    )
}

// ============================================================================
// 3. CYBERPUNK UI (Chamfered Tech Corners + Neon Glow + High-Key Cyber-Lab in Light)
// ============================================================================
val CyberCyan = Color(0xFF00F0FF)
val CyberMagenta = Color(0xFFFF008C)
val CyberYellow = Color(0xFFFCEE0A)
val CyberCyanLight = Color(0xFF007A8C)
val CyberMagentaLight = Color(0xFFC20066)
val CyberAmberLight = Color(0xFFB45309)

@Composable
fun CyberpunkPanel(
    modifier: Modifier = Modifier,
    neonAccent: Color = CyberCyan,
    secondaryAccent: Color = CyberMagenta,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalLifeHubColors.current.isDark
    val shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0A1A),
                Color(0xFF140F2B),
                neonAccent.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF0F6FC),
                neonAccent.copy(alpha = 0.08f)
            )
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            neonAccent.copy(alpha = if (isDark) 1f else 0.72f),
            secondaryAccent.copy(alpha = if (isDark) 1f else 0.65f),
            neonAccent.copy(alpha = 0.4f)
        )
    )
    val mod = modifier
        .clip(shape)
        .background(bgBrush)
        .border(BorderStroke(1.5.dp, borderBrush), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Column(
        modifier = mod.padding(18.dp),
        content = content
    )
}

// ============================================================================
// 4. 3D UI (Tactile Extruded Depth Slab + Top Rim Highlight + Press Physics)
// ============================================================================
@Composable
fun Extruded3DCard(
    modifier: Modifier = Modifier,
    faceColor: Color = MaterialTheme.colorScheme.surface,
    depthColor: Color = CyberViolet.copy(alpha = 0.65f),
    highlightColor: Color = Color.White.copy(alpha = 0.22f),
    depthOffset: Dp = 7.dp,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedY by animateFloatAsState(
        targetValue = if (pressed) depthOffset.value * 0.65f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_3d_press"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier.padding(bottom = depthOffset)) {
        // Extruded 3D bottom slab
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = depthOffset)
                .clip(shape)
                .background(depthColor)
        )
        // Raised top 3D face
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = animatedY.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            faceColor,
                            faceColor.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            colors = listOf(highlightColor, depthColor.copy(alpha = 0.45f))
                        )
                    ),
                    shape
                )
                .let {
                    if (onClick != null) it.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ) else it
                }
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun Extruded3DButton(
    label: String,
    icon: ImageVector? = null,
    topColor: Color = CyberViolet,
    sideColor: Color = Color(0xFF4C22A6),
    contentColor: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val yShift by animateFloatAsState(
        targetValue = if (pressed) 4f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_3d_press"
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 54.dp)
            .padding(bottom = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(shape)
                .background(sideColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 49.dp)
                .offset(y = yShift.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topColor, topColor.copy(alpha = 0.88f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 5. HOLOGRAPHIC UI (Live Animated Prismatic Iridescent Foil Shader)
// ============================================================================
@Composable
fun HolographicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalLifeHubColors.current.isDark
    val infinite = rememberInfiniteTransition(label = "holo_sweep")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holo_shift"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val holoBorder = Brush.linearGradient(
        colors = if (isDark) {
            listOf(CyberCyan, CyberViolet, HotCoral, SolarAmber, NeonMint, CyberCyan)
        } else {
            listOf(CyberCyanLight, CyberViolet, Color(0xFFE11D48), Color(0xFFD97706), Color(0xFF009E6D), CyberCyanLight)
        },
        start = Offset(shift - 400f, 0f),
        end = Offset(shift + 400f, 600f)
    )
    val holoFoilBg = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color(0xFF101426),
                Color(0xFF1E163B),
                Color(0xFF112936),
                Color(0xFF161326)
            )
        } else {
            listOf(
                Color.White,
                Color(0xFFF2ECFF),
                Color(0xFFE8FAFF),
                Color(0xFFFFF0F6)
            )
        },
        start = Offset(shift * 0.35f, 0f),
        end = Offset(shift * 0.35f + 700f, 700f)
    )
    val mod = modifier
        .clip(shape)
        .background(holoFoilBg)
        .drawBehind {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark) 0.09f else 0.45f),
                        CyberCyan.copy(alpha = if (isDark) 0.07f else 0.12f),
                        Color.Transparent
                    ),
                    start = Offset(shift - 300f, 0f),
                    end = Offset(shift + 180f, size.height)
                )
            )
        }
        .border(BorderStroke(1.6.dp, holoBorder), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Column(
        modifier = mod.padding(18.dp),
        content = content
    )
}

// ============================================================================
// 6. DYNAMIC ISLAND STYLE UI (Morphing Interactive Top Status Capsule)
// ============================================================================
@Composable
fun DynamicIslandBar(
    title: String,
    pillMetric: String,
    unreadAlertsCount: Int,
    isSyncing: Boolean,
    menuOpen: Boolean,
    initials: String,
    onToggleMenu: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWrapped: () -> Unit,
    onOpenMoneyScore: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val capsuleBg = if (isDark) Color(0xFF07090F) else Color.White
    val primaryInk = if (isDark) Color.White else cs.onSurface
    val mutedInk = if (isDark) Color(0xFF9BA6C1) else extended.mutedText
    val mintAccent = extended.accentMint
    val cyanChip = extended.accentCyan
    val coralChip = extended.accentCoral
    val amberChip = extended.accentAmber
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(if (expanded) 28.dp else 50.dp),
            color = capsuleBg,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            NeonMint.copy(alpha = 0.45f),
                            CyberViolet.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.16f)
                        )
                    } else {
                        listOf(
                            cs.outline.copy(alpha = 0.4f),
                            mintAccent.copy(alpha = 0.45f),
                            CyberViolet.copy(alpha = 0.4f),
                            cs.outline.copy(alpha = 0.4f)
                        )
                    }
                )
            ),
            shadowElevation = if (isDark) 10.dp else 5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                .testTag("dynamic_island_capsule")
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Animated Hamburger / Close Menu Icon
                    AnimatedMenuIconButton(
                        isOpen = menuOpen || expanded,
                        tint = primaryInk,
                        onClick = {
                            expanded = !expanded
                            onToggleMenu()
                        }
                    )

                    // Center Live Island Status Pill (Tap to morph Dynamic Island)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSyncing) amberChip else mintAccent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryInk,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = mintAccent.copy(alpha = if (isDark) 0.18f else 0.14f)
                        ) {
                            Text(
                                text = pillMetric,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = mintAccent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        ExpandCollapseChevron(expanded = expanded, tint = mutedInk)
                    }

                    // Right Action Icons: Notification Bell + Profile Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NotificationBellAction(
                            badgeCount = unreadAlertsCount,
                            tint = primaryInk,
                            onClick = onOpenNotifications
                        )
                        ProfileAvatarAction(
                            initials = initials,
                            onClick = onOpenProfile
                        )
                    }
                }

                // Morphing Expanded Dynamic Island Drawer
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp, start = 6.dp, end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IslandQuickChip("Score", cyanChip, Modifier.weight(1f)) {
                                expanded = false; onOpenMoneyScore()
                            }
                            IslandQuickChip("Wrapped", coralChip, Modifier.weight(1f)) {
                                expanded = false; onOpenWrapped()
                            }
                            IslandQuickChip("Badges", amberChip, Modifier.weight(1f)) {
                                expanded = false; onOpenAchievements()
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSyncing) "Syncing cloud vault…" else "Offline-first cache active",
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpinningRefreshAction(
                                    isSpinning = isSyncing,
                                    tint = mintAccent,
                                    onClick = onRefresh
                                )
                                SettingsAction(
                                    tint = primaryInk,
                                    onClick = {
                                        expanded = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandQuickChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 12. NAVIGATION & ACTION ICONS (All 15 Animated & Accessible 48dp Actions)
// ============================================================================

@Composable
fun AnimatedMenuIconButton(
    isOpen: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "menu_morph_rot"
    )
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("animated_menu_icon")
    ) {
        Icon(
            imageVector = if (isOpen) Icons.Rounded.Close else Icons.Rounded.Menu,
            contentDescription = if (isOpen) "Close menu" else "Open menu",
            tint = tint,
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
fun BackArrowAction(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Navigate back",
            tint = tint
        )
    }
}

@Composable
fun CloseAction(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = tint
        )
    }
}

@Composable
fun MoreOptionsAction(
    options: List<Pair<String, () -> Unit>>,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = tint
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
fun SearchAction(
    active: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = if (active) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = if (active) "Close search" else "Search",
            tint = if (active) NeonMint else tint
        )
    }
}

@Composable
fun NotificationBellAction(
    badgeCount: Int,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("notification_bell_action")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Notifications",
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(HotCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else "$badgeCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileAvatarAction(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("profile_avatar_action"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(colors = listOf(NeonMint, CyberViolet))
                )
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsAction(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings",
            tint = tint
        )
    }
}

@Composable
fun ExpandCollapseChevron(
    expanded: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val angle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expand_chevron"
    )
    Icon(
        imageVector = Icons.Rounded.ExpandMore,
        contentDescription = if (expanded) "Collapse" else "Expand",
        tint = tint,
        modifier = modifier
            .size(20.dp)
            .rotate(angle)
    )
}

@Composable
fun FavoriteToggleIcon(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    activeColor: Color = HotCoral,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fav_scale"
    )
    val tint by animateColorAsState(
        targetValue = if (isFavorite) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "fav_color"
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove favorite" else "Mark favorite",
            tint = tint,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun ShareActionButton(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = "Share",
            tint = tint
        )
    }
}

@Composable
fun SpinningRefreshAction(
    isSpinning: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_angle"
    )
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh",
            tint = tint,
            modifier = Modifier.rotate(if (isSpinning) spinAngle else 0f)
        )
    }
}

@Composable
fun FilterActionButton(
    hasActiveFilter: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter",
                tint = if (hasActiveFilter) NeonMint else tint
            )
            if (hasActiveFilter) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LimePulse)
                )
            }
        }
    }
}

/**
 * Cyberpunk neon perspective grid backdrop for Habit Tracker.
 */
@Composable
fun CyberpunkGridBackdrop(modifier: Modifier = Modifier) {
    val isDark = LocalLifeHubColors.current.isDark
    val gridColor = if (isDark) CyberCyan.copy(alpha = 0.08f) else CyberCyanLight.copy(alpha = 0.09f)
    val magentaGlow = if (isDark) CyberMagenta.copy(alpha = 0.14f) else CyberMagentaLight.copy(alpha = 0.10f)
    val cyanGlow = if (isDark) CyberCyan.copy(alpha = 0.11f) else CyberCyanLight.copy(alpha = 0.10f)

    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(magentaGlow, Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.12f),
                radius = size.width * 0.75f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.12f),
            radius = size.width * 0.75f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyanGlow, Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.65f),
                radius = size.width * 0.7f
            ),
            center = Offset(size.width * 0.1f, size.height * 0.65f),
            radius = size.width * 0.7f
        )
        val step = 36.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}
