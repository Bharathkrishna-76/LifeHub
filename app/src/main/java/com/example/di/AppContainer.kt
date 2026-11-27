package com.example.di

import android.content.Context
import com.example.R
import com.example.data.local.LifeHubDatabase
import com.example.data.local.PreferencesManager
import com.example.data.remote.NetworkMonitor
import com.example.data.repository.LifeHubRepository
import com.example.data.worker.ReminderScheduler
import com.example.update.AppUpdateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Modular Dependency Injection container wiring Firebase, Room, DataStore, WorkManager, and Repositories.
 * Resolves the named Firestore database ID from R.string.firestore_database_id at the DI boundary
 * and configures bounded Firestore local cache alongside Room SQLite as the primary local source of truth.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val firestore: FirebaseFirestore by lazy {
        val databaseId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId).apply {
            runCatching {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(40L * 1024L * 1024L)
                            .build()
                    )
                    .build()
            }
        }
    }

    val localDatabase: LifeHubDatabase by lazy {
        LifeHubDatabase.getInstance(appContext)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(appContext)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(appContext)
    }

    val repository: LifeHubRepository by lazy {
        LifeHubRepository(
            db = firestore,
            localDb = localDatabase,
            preferencesManager = preferencesManager,
            reminderScheduler = reminderScheduler,
            networkMonitor = networkMonitor
        )
    }
}
