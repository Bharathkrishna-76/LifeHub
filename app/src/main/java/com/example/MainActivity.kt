package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                            key(user.uid) {
                                LifeHubMainShell(
                                    currentUserId = user.uid,
                                    onSwitchToTripSplit = { tripSplitPrefs.setAppMode(AppMode.TRIPSPLIT) }
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

