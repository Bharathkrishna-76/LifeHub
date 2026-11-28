package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.ErrorBanner
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.LocalLifeHubColors
import kotlinx.coroutines.delay

@Composable
fun LifeHubSplashScreen(
    modifier: Modifier = Modifier
) {
    SplashBackdrop(modifier = modifier.testTag("splash_loading_screen")) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding(),
            contentAlignment = Alignment.Center
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
            val emblemSize = if (metrics.isCompactHeight) 112.dp else 152.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (metrics.isNarrowPhone) 20.dp else 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedSplashEmblem(isLoading = true, emblemSize = emblemSize)
                Spacer(Modifier.height(if (metrics.isCompactHeight) 16.dp else 24.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (metrics.isCompactHeight) 18.dp else 28.dp))
                SplashLoadingBar()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(true) }
    var splashTimerDone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(950L)
        splashTimerDone = true
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isAutoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isAutoChecking = false
            },
            scope = scope
        )
    }

    val isSplashLoading = isAutoChecking || !splashTimerDone
    val cs = MaterialTheme.colorScheme

    SplashBackdrop(modifier = Modifier.testTag("auth_login_screen")) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding(),
            contentAlignment = Alignment.Center
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
            val emblemSize = if (metrics.isCompactHeight) 112.dp else 152.dp
            val hPad = if (metrics.isNarrowPhone) 20.dp else 28.dp
            val vPad = if (metrics.isCompactHeight) 16.dp else 32.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .defaultMinSize(minHeight = maxHeight)
                    .padding(horizontal = hPad, vertical = vPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(if (metrics.isCompactHeight) 8.dp else 24.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedSplashEmblem(
                        isLoading = isSplashLoading || isSigningIn,
                        emblemSize = emblemSize
                    )

                    Spacer(Modifier.height(if (metrics.isCompactHeight) 14.dp else 24.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = cs.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(if (metrics.isCompactHeight) 16.dp else 24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isSplashLoading,
                        transitionSpec = {
                            (fadeIn(tween(260)) + slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { it / 4 }) togetherWith fadeOut(tween(200))
                        },
                        label = "splash_to_login"
                    ) { loading ->
                        if (loading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                SplashLoadingBar()
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(visible = errorMessage != null) {
                                    Column {
                                        errorMessage?.let { msg ->
                                            ErrorBanner(
                                                message = msg,
                                                onDismiss = { errorMessage = null }
                                            )
                                            Spacer(Modifier.height(14.dp))
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        isSigningIn = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isSigningIn = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isSigningIn = false
                                                errorMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isSigningIn = false
                                            }
                                        )
                                    },
                                    enabled = !isSigningIn,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = cs.primary,
                                        contentColor = cs.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 56.dp)
                                        .testTag("google_sign_in_button")
                                ) {
                                    if (isSigningIn) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp,
                                            color = cs.onPrimary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.signing_in),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.sign_in_google),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_aura")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topAuraRadius = size.minDimension * 0.65f * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        extended.accentMint.copy(alpha = if (extended.isDark) 0.16f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = topAuraRadius
                ),
                radius = topAuraRadius,
                center = Offset(size.width * 0.5f, size.height * 0.36f)
            )
            val bottomAuraRadius = size.minDimension * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        extended.accentViolet.copy(alpha = if (extended.isDark) 0.14f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.82f),
                    radius = bottomAuraRadius
                ),
                radius = bottomAuraRadius,
                center = Offset(size.width * 0.82f, size.height * 0.82f)
            )
        }
        content()
    }
}

@Composable
private fun AnimatedSplashEmblem(
    isLoading: Boolean,
    emblemSize: Dp = 152.dp
) {
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current
    val enterScale = remember { Animatable(0.82f) }
    val enterAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, animationSpec = tween(320))
    }
    LaunchedEffect(Unit) {
        enterScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "emblem_motion")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLoading) 1600 else 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    Box(
        modifier = Modifier
            .size(emblemSize)
            .alpha(enterAlpha.value)
            .scale(enterScale.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.5.dp.toPx()
            val inset = strokePx + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Soft static guide track
            drawOval(
                color = extended.accentMint.copy(alpha = if (extended.isDark) 0.14f else 0.18f),
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Luminous orbiting ring arc
            rotate(degrees = orbitAngle) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.35f to extended.accentViolet.copy(alpha = 0.55f),
                        0.7f to extended.accentCyan.copy(alpha = 0.85f),
                        1.0f to extended.accentMint
                    ),
                    startAngle = 0f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = cs.surface,
            border = BorderStroke(1.5.dp, extended.accentMint.copy(alpha = 0.55f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .size(emblemSize * 0.59f)
                .scale(if (isLoading) breathScale else 1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lifehub_mark),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SplashLoadingBar() {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "dots_wave")

    val dotAlphas = listOf(0, 180, 360).map { delayMs ->
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 650,
                    delayMillis = delayMs,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$delayMs"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dotAlphas.forEachIndexed { index, alphaState ->
                val dotColor = when (index) {
                    0 -> extended.accentMint
                    1 -> extended.accentCyan
                    else -> extended.accentViolet
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alphaState.value)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        LinearProgressIndicator(
            modifier = Modifier
                .width(136.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = cs.primary,
            trackColor = cs.surfaceVariant
        )
    }
}
