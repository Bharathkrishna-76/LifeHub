package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.MoneyScoreBreakdown
import com.example.ui.components.BackArrowAction
import com.example.ui.components.CyberCyan
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.HolographicCard
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.ShareActionButton
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber
import kotlin.math.roundToInt

/**
 * Module 7: Money Score Screen — HOLOGRAPHIC UI ("IRIDESCENT PRISMATIC CREDIT INDEX").
 * Features live animated holographic foil cards, sweeping rainbow credit gauge,
 * expandable factor coaching insights, and native share action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScoreScreen(
    moneyScore: MoneyScoreBreakdown,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val inkColor = if (isDark) Color.White else cs.onSurface
    val mutedColor = if (isDark) Color(0xFFA6B2CF) else extended.mutedText
    val bodyMuted = if (isDark) Color(0xFFC5CEE6) else cs.onSurfaceVariant
    val cyanAccent = extended.accentCyan
    val trackTint = if (isDark) Color.White.copy(alpha = 0.12f) else cs.outline.copy(alpha = 0.28f)

    val animScore = remember { Animatable(0f) }
    var expandedFactor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(moneyScore.totalScore) {
        animScore.animateTo(
            targetValue = moneyScore.totalScore.toFloat(),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val tierColor = when {
        moneyScore.totalScore >= 85 -> extended.accentMint
        moneyScore.totalScore >= 70 -> extended.accentCyan
        moneyScore.totalScore >= 50 -> extended.accentAmber
        else -> extended.accentCoral
    }

    fun shareScore() {
        val text = buildString {
            append("My LifeHub Money Score: ${moneyScore.totalScore}/100 (${moneyScore.tierLabel})\n")
            moneyScore.factors.forEach { f ->
                append("• ${f.name}: ${f.score}/${f.maxScore}\n")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share Money Score")) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF070914), Color(0xFF0F1028), Color(0xFF080A14))
                    } else {
                        listOf(Color(0xFFF5F7FC), Color(0xFFEEEBFF), Color(0xFFF4F8FB))
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "HOLOGRAPHIC INDEX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = cyanAccent
                            )
                            Text("Money Score", color = inkColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        BackArrowAction(
                            onClick = onBack,
                            tint = inkColor,
                            modifier = Modifier.testTag("money_score_back")
                        )
                    },
                    actions = {
                        ShareActionButton(onClick = { shareScore() }, tint = inkColor)
                        MoreOptionsAction(
                            tint = inkColor,
                            options = listOf(
                                "Expand All Insights" to {
                                    expandedFactor = if (expandedFactor == "__ALL__") null else "__ALL__"
                                },
                                "Share Score Card" to { shareScore() }
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("money_score_screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "holo_gauge_card") {
                    HolographicCard(
                        cornerRadius = 30.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PRISMATIC CREDIT TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = cyanAccent
                            )
                            Spacer(Modifier.height(18.dp))

                            Box(
                                modifier = Modifier.size(236.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(236.dp)) {
                                    val stroke = 18.dp.toPx()
                                    val inset = stroke / 2f
                                    val arcSize = Size(size.width - stroke, size.height - stroke)
                                    drawArc(
                                        color = trackTint,
                                        startAngle = 150f,
                                        sweepAngle = 240f,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    // Iridescent glow aura
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(HotCoral, SolarAmber, CyberCyan, CyberViolet, NeonMint)
                                        ),
                                        startAngle = 150f,
                                        sweepAngle = 240f * (animScore.value / 100f).coerceIn(0f, 1f),
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = stroke + 6.dp.toPx(), cap = StrokeCap.Round),
                                        alpha = 0.25f
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(HotCoral, SolarAmber, CyberCyan, NeonMint)
                                        ),
                                        startAngle = 150f,
                                        sweepAngle = 240f * (animScore.value / 100f).coerceIn(0f, 1f),
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${animScore.value.roundToInt()}",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = inkColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "out of 100",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = mutedColor
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = tierColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = moneyScore.tierLabel.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tierColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = moneyScore.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = bodyMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item(key = "factors_title") {
                    Text(
                        text = "5-Factor Holographic Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = inkColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(moneyScore.factors, key = { it.name }) { factor ->
                    val ratio = if (factor.maxScore == 0) 0f else factor.score.toFloat() / factor.maxScore
                    val barColor = when {
                        ratio >= 0.8f -> extended.accentMint
                        ratio >= 0.55f -> extended.accentCyan
                        else -> extended.accentAmber
                    }
                    val isExpanded = expandedFactor == "__ALL__" || expandedFactor == factor.name

                    HolographicCard(
                        cornerRadius = 22.dp,
                        onClick = {
                            expandedFactor = if (isExpanded) null else factor.name
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = factor.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = inkColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${factor.score} / ${factor.maxScore}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = barColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            ExpandCollapseChevron(expanded = isExpanded, tint = cyanAccent)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = barColor,
                            trackColor = trackTint
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = factor.insight,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor
                        )
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else cyanAccent.copy(alpha = 0.09f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Text(
                                    text = if (ratio >= 0.8f) {
                                        "Elite band! Keep maintaining this consistency to lock in your peak Money Score."
                                    } else {
                                        "Boost tip: Improving ${factor.name.lowercase()} can add up to +${factor.maxScore - factor.score} pts to your score."
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cyanAccent,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
