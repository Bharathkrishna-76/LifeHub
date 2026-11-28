package com.example.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_update_prefs"
)

/**
 * Dedicated local cache for Firebase Remote Config update snapshots and dismissal throttling.
 * Kept strictly separate from user expense preferences and Room databases so app updates never
 * disturb user records.
 */
class UpdateCacheStore(private val context: Context) {

    private object Keys {
        val LATEST_VERSION_CODE = longPreferencesKey("cached_latest_version_code")
        val LATEST_VERSION_NAME = stringPreferencesKey("cached_latest_version_name")
        val APK_DOWNLOAD_URL = stringPreferencesKey("cached_apk_download_url")
        val APK_SHA256 = stringPreferencesKey("cached_apk_sha256")
        val RELEASE_NOTES = stringPreferencesKey("cached_release_notes")
        val UPDATE_ENABLED = booleanPreferencesKey("cached_update_enabled")
        val MINIMUM_VERSION_CODE = longPreferencesKey("cached_minimum_version_code")
        val FETCHED_AT_MS = longPreferencesKey("cached_fetched_at_ms")
        val DISMISSED_VERSION_CODE = longPreferencesKey("dismissed_version_code")
        val DISMISSED_AT_MS = longPreferencesKey("dismissed_at_ms")
    }

    suspend fun loadCachedConfig(): RemoteUpdateConfig? {
        val prefs = context.updateDataStore.data.first()
        val fetchedAt = prefs[Keys.FETCHED_AT_MS] ?: return null
        return RemoteUpdateConfig(
            latestVersionCode = prefs[Keys.LATEST_VERSION_CODE] ?: 1L,
            latestVersionName = prefs[Keys.LATEST_VERSION_NAME] ?: "1.0.0",
            apkDownloadUrl = prefs[Keys.APK_DOWNLOAD_URL] ?: "",
            apkSha256 = prefs[Keys.APK_SHA256] ?: "",
            releaseNotes = prefs[Keys.RELEASE_NOTES] ?: "",
            updateEnabled = prefs[Keys.UPDATE_ENABLED] ?: false,
            minimumVersionCode = prefs[Keys.MINIMUM_VERSION_CODE] ?: 1L,
            fetchedAtEpochMs = fetchedAt
        )
    }

    suspend fun saveCachedConfig(config: RemoteUpdateConfig) {
        context.updateDataStore.edit { prefs ->
            prefs[Keys.LATEST_VERSION_CODE] = config.latestVersionCode
            prefs[Keys.LATEST_VERSION_NAME] = config.latestVersionName
            prefs[Keys.APK_DOWNLOAD_URL] = config.apkDownloadUrl
            prefs[Keys.APK_SHA256] = config.apkSha256
            prefs[Keys.RELEASE_NOTES] = config.releaseNotes
            prefs[Keys.UPDATE_ENABLED] = config.updateEnabled
            prefs[Keys.MINIMUM_VERSION_CODE] = config.minimumVersionCode
            prefs[Keys.FETCHED_AT_MS] = config.fetchedAtEpochMs
        }
    }

    suspend fun recordRemindLater(versionCode: Long, nowMs: Long) {
        context.updateDataStore.edit { prefs ->
            prefs[Keys.DISMISSED_VERSION_CODE] = versionCode
            prefs[Keys.DISMISSED_AT_MS] = nowMs
        }
    }

    suspend fun loadRemindLater(): Pair<Long, Long> {
        val prefs = context.updateDataStore.data.first()
        return (prefs[Keys.DISMISSED_VERSION_CODE] ?: 0L) to (prefs[Keys.DISMISSED_AT_MS] ?: 0L)
    }
}
