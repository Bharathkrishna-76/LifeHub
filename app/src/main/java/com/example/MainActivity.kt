package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.authStateFlow
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserSettings
import com.example.tripsplit.data.AppMode
import com.example.tripsplit.data.TripSplitPreferences
import com.example.tripsplit.navigation.TripSplitRoot
import com.example.ui.navigation.LifeHubMainShell
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LifeHubSplashScreen
import com.example.ui.theme.LifeHubTheme
import com.example.update.AppUpdateHost
import com.example.update.AppUpdateLifecycleEffect
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeHubRootApp()
        }
    }
}

@Composable
fun LifeHubRootApp() {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val settings by app.container.preferencesManager.settingsFlow
        .collectAsStateWithLifecycle(initialValue = UserSettings(themeMode = ThemeMode.DARK))

    val authStateFlow = remember { Firebase.auth.authStateFlow() }
    val observedUser by authStateFlow
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var manualSignInUid by remember { mutableStateOf<String?>(null) }
    val currentUser = observedUser ?: Firebase.auth.currentUser.takeIf {
        it != null && it.uid == manualSignInUid
    }

    var showLaunchSplash by remember { mutableStateOf(Firebase.auth.currentUser != null) }
    LaunchedEffect(Unit) {
        if (showLaunchSplash) {
            delay(950L)
            showLaunchSplash = false
        }
    }

    val tripSplitPrefs = remember(context) { TripSplitPreferences.getInstance(context) }
    val appMode by tripSplitPrefs.appMode.collectAsStateWithLifecycle()

    val updateManager = app.container.appUpdateManager
    val updateState by updateManager.uiState.collectAsStateWithLifecycle()
    AppUpdateLifecycleEffect(updateManager = updateManager)

    if (updateState.isMandatoryBlocking) {
        LifeHubTheme(themeMode = settings.themeMode) {
            AppUpdateHost(updateManager = updateManager, state = updateState)
        }
        return
    }

    if (showLaunchSplash && currentUser != null) {
        LifeHubTheme(themeMode = settings.themeMode) {
            LifeHubSplashScreen()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = appMode, label = "app_mode_switch") { mode ->
            when (mode) {
                AppMode.DAILY_EXPENSES -> {
                    LifeHubTheme(themeMode = settings.themeMode) {
                        val user = currentUser
                        if (user == null) {
                            AuthScreen(
                                onAuthSuccess = {
                                    manualSignInUid = Firebase.auth.currentUser?.uid
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                key(user.uid) {
                                    LifeHubMainShell(currentUserId = user.uid)
                                }
                                TripSplitModeLauncherPill(
                                    onClick = { tripSplitPrefs.setAppMode(AppMode.TRIPSPLIT) },
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .navigationBarsPadding()
                                        .padding(start = 14.dp, bottom = 92.dp)
                                )
                            }
                        }
                    }
                }

                AppMode.TRIPSPLIT -> {
                    val travelerName = currentUser?.displayName
                        ?.trim()
                        ?.substringBefore(" ")
                        ?.takeIf { it.isNotBlank() }
                        ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Traveler"

                    TripSplitRoot(
                        travelerFirstName = travelerName,
                        onSwitchToDailyExpenses = {
                            tripSplitPrefs.setAppMode(AppMode.DAILY_EXPENSES)
                        }
                    )
                }
            }
        }

        if (updateState.isOptionalPromptVisible) {
            LifeHubTheme(themeMode = settings.themeMode) {
                AppUpdateHost(updateManager = updateManager, state = updateState)
            }
        }
    }
}

@Composable
private fun TripSplitModeLauncherPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("switch_to_tripsplit_mode_button"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16241E),
        border = BorderStroke(1.dp, Color(0xFFC8552B)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = stringResource(R.string.ts_mode_switch_to_tripsplit_cd),
                tint = Color(0xFFE06E43),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.ts_mode_switch_to_tripsplit),
                color = Color(0xFFFBF9F5),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

