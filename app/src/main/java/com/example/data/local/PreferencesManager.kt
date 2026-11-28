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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
        val LAST_SIGNED_IN_UID = stringPreferencesKey("last_signed_in_uid")
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

    suspend fun getLastSignedInUid(): String? =
        context.lifeHubDataStore.data.first()[Keys.LAST_SIGNED_IN_UID]?.takeIf { it.isNotBlank() }

    suspend fun setLastSignedInUid(uid: String?) {
        context.lifeHubDataStore.edit { prefs ->
            if (uid.isNullOrBlank()) {
                prefs.remove(Keys.LAST_SIGNED_IN_UID)
            } else {
                prefs[Keys.LAST_SIGNED_IN_UID] = uid
            }
        }
    }

    suspend fun clearUserPreferences() {
        context.lifeHubDataStore.edit { prefs ->
            val currentTheme = prefs[Keys.THEME_MODE]
            prefs.clear()
            // Preserve only non-sensitive visual theme choice so auth screen does not flicker
            if (currentTheme != null) {
                prefs[Keys.THEME_MODE] = currentTheme
            }
        }
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
        val safeBudget = if (monthlyBudget.isFinite()) monthlyBudget.coerceIn(0.0, 1_000_000_000.0) else 0.0
        context.lifeHubDataStore.edit {
            it[Keys.MONTHLY_BUDGET] = safeBudget
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
        val safeBudget = if (settings.monthlyBudget.isFinite()) {
            settings.monthlyBudget.coerceIn(0.0, 1_000_000_000.0)
        } else 25000.0
        context.lifeHubDataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode.name
            it[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            it[Keys.CURRENCY_SYMBOL] = settings.currencySymbol.take(4).ifBlank { "₹" }
            it[Keys.MONTHLY_BUDGET] = safeBudget
            it[Keys.SEPARATE_TRIP_EVENT_LEDGER] = settings.separateTripEventLedger
        }
    }

    companion object {
        private const val PBKDF2_PREFIX = "pbkdf2_v1"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_BITS = 256
        private const val SALT_BYTES = 16
        private val secureRandom = SecureRandom()

        fun hashParentalPin(rawPin: String): String {
            val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
            val derived = derivePbkdf2(rawPin.trim(), salt, PBKDF2_ITERATIONS)
            return "$PBKDF2_PREFIX:$PBKDF2_ITERATIONS:${salt.toHexString()}:${derived.toHexString()}"
        }

        fun verifyParentalPin(rawPin: String, storedHash: String): Boolean {
            if (storedHash.isBlank()) return false
            val parts = storedHash.split(":")
            if (parts.size != 4 || parts[0] != PBKDF2_PREFIX) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            if (iterations < 10_000 || iterations > 1_000_000) return false
            val salt = parts[2].hexToByteArray() ?: return false
            val expected = parts[3].hexToByteArray() ?: return false
            val actual = derivePbkdf2(rawPin.trim(), salt, iterations)
            return MessageDigest.isEqual(actual, expected)
        }

        private fun derivePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PBKDF2_KEY_BITS)
            return try {
                SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

        private fun String.hexToByteArray(): ByteArray? {
            if (length % 2 != 0) return null
            return try {
                ByteArray(length / 2) { i ->
                    substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}
