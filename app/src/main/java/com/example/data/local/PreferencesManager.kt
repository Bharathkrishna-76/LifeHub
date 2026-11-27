package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.lifeHubDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lifehub_preferences"
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val SEPARATE_TRIP_EVENT_LEDGER = booleanPreferencesKey("separate_trip_event_ledger")
        val PARENTAL_CONTROL_ENABLED = booleanPreferencesKey("parental_control_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
    }

    val settingsFlow: Flow<UserSettings> = context.lifeHubDataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            monthlyBudget = prefs[Keys.MONTHLY_BUDGET] ?: 25000.0,
            separateTripEventLedger = prefs[Keys.SEPARATE_TRIP_EVENT_LEDGER] ?: true,
            parentalControlEnabled = prefs[Keys.PARENTAL_CONTROL_ENABLED] ?: false,
            parentalPinHash = prefs[Keys.PARENTAL_PIN_HASH] ?: ""
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.lifeHubDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.lifeHubDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateSeparateTripEventLedger(enabled: Boolean) {
        context.lifeHubDataStore.edit { it[Keys.SEPARATE_TRIP_EVENT_LEDGER] = enabled }
    }

    suspend fun updateBudgetAndCurrency(monthlyBudget: Double, currencySymbol: String) {
        context.lifeHubDataStore.edit {
            it[Keys.MONTHLY_BUDGET] = monthlyBudget.coerceAtLeast(0.0)
            it[Keys.CURRENCY_SYMBOL] = currencySymbol.take(4).ifBlank { "₹" }
        }
    }

    suspend fun setParentalControl(enabled: Boolean, pin: String? = null) {
        context.lifeHubDataStore.edit {
            it[Keys.PARENTAL_CONTROL_ENABLED] = enabled
            if (!pin.isNullOrBlank()) {
                it[Keys.PARENTAL_PIN_HASH] = hashParentalPin(pin)
            } else if (!enabled) {
                it[Keys.PARENTAL_PIN_HASH] = ""
            }
        }
    }

    suspend fun applyRemoteSettings(settings: UserSettings) {
        context.lifeHubDataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode.name
            it[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            it[Keys.CURRENCY_SYMBOL] = settings.currencySymbol
            it[Keys.MONTHLY_BUDGET] = settings.monthlyBudget
            it[Keys.SEPARATE_TRIP_EVENT_LEDGER] = settings.separateTripEventLedger
        }
    }

    companion object {
        private const val PIN_SALT = "lifehub_guardian_v1::"

        fun hashParentalPin(rawPin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$PIN_SALT${rawPin.trim()}".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyParentalPin(rawPin: String, storedHash: String): Boolean {
            if (storedHash.isBlank()) return false
            return hashParentalPin(rawPin) == storedHash
        }
    }
}
