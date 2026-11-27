package com.example.tripsplit.data

import android.content.Context
import com.example.tripsplit.domain.TripSplitThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppMode {
    DAILY_EXPENSES,
    TRIPSPLIT
}

class TripSplitPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tripsplit_standalone_prefs", Context.MODE_PRIVATE)

    private val _appMode = MutableStateFlow(readAppMode())
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _themePreference = MutableStateFlow(readTheme())
    val themePreference: StateFlow<TripSplitThemePreference> = _themePreference.asStateFlow()

    private val _activeTripId = MutableStateFlow(prefs.getString(KEY_ACTIVE_TRIP_ID, null))
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()

    private val _favoriteTripIds = MutableStateFlow(readFavorites())
    val favoriteTripIds: StateFlow<Set<String>> = _favoriteTripIds.asStateFlow()

    fun setAppMode(mode: AppMode) {
        prefs.edit().putString(KEY_APP_MODE, mode.name).apply()
        _appMode.value = mode
    }

    fun setThemePreference(theme: TripSplitThemePreference) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _themePreference.value = theme
    }

    fun toggleLightDark() {
        val next = if (_themePreference.value == TripSplitThemePreference.DARK) {
            TripSplitThemePreference.LIGHT
        } else {
            TripSplitThemePreference.DARK
        }
        setThemePreference(next)
    }

    fun setActiveTripId(tripId: String?) {
        prefs.edit().putString(KEY_ACTIVE_TRIP_ID, tripId).apply()
        _activeTripId.value = tripId
    }

    fun toggleFavoriteTrip(tripId: String) {
        val current = _favoriteTripIds.value.toMutableSet()
        if (!current.add(tripId)) {
            current.remove(tripId)
        }
        prefs.edit().putStringSet(KEY_FAVORITE_TRIPS, current).apply()
        _favoriteTripIds.value = current.toSet()
    }

    private fun readAppMode(): AppMode {
        val raw = prefs.getString(KEY_APP_MODE, AppMode.DAILY_EXPENSES.name)
        return AppMode.entries.firstOrNull { it.name == raw } ?: AppMode.DAILY_EXPENSES
    }

    private fun readTheme(): TripSplitThemePreference {
        val raw = prefs.getString(KEY_THEME, TripSplitThemePreference.LIGHT.name)
        return TripSplitThemePreference.entries.firstOrNull { it.name == raw }
            ?: TripSplitThemePreference.LIGHT
    }

    private fun readFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITE_TRIPS, emptySet())?.toSet() ?: emptySet()

    companion object {
        private const val KEY_APP_MODE = "app_mode"
        private const val KEY_THEME = "tripsplit_theme"
        private const val KEY_ACTIVE_TRIP_ID = "active_trip_id"
        private const val KEY_FAVORITE_TRIPS = "favorite_trip_ids"

        @Volatile
        private var INSTANCE: TripSplitPreferences? = null

        fun getInstance(context: Context): TripSplitPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripSplitPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
