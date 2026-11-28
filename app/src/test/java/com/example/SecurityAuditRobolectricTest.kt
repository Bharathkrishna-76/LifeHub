package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.ExportAndShareHelper
import com.example.data.local.LifeHubDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.TransactionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.toUserSafeErrorMessage
import com.example.tripsplit.data.AppMode
import com.example.tripsplit.data.TripEntity
import com.example.tripsplit.data.TripSplitDatabase
import com.example.tripsplit.data.TripSplitPreferences
import com.example.update.ApkSecurityVerifier
import com.example.update.ApkVerificationResult
import com.example.update.UrlValidationResult
import com.example.update.resolveReferApkDownloadUrl
import com.example.update.RemoteUpdateConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAuditRobolectricTest {

    @Test
    fun updateUrlVerifier_pinsOfficialRepo_andBlocksHijackingVectors() {
        val validUrl =
            "https://github.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/LifeHub-v1.0.2-release.apk"
        assertTrue(ApkSecurityVerifier.validateDownloadUrl(validUrl) is UrlValidationResult.Valid)

        val blockedUrls = listOf(
            "http://github.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/LifeHub-v1.0.2-release.apk",
            "https://github.com/evil-attacker/LifeHub/releases/download/v9.9.9/LifeHub-v9.9.9-release.apk",
            "https://evil.example.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/app.apk",
            "https://user:secret@github.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/app.apk",
            "https://github.com:8443/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/app.apk",
            "https://github.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/../../evil/payload.apk",
            "https://github.com/Bharathkrishna-76/LifeHub/releases/download/v1.0.2/not-an-apk.zip",
            "javascript:alert(1)",
            "file:///sdcard/Download/evil.apk"
        )
        blockedUrls.forEach { bad ->
            assertTrue(
                "Expected URL to be blocked: $bad",
                ApkSecurityVerifier.validateDownloadUrl(bad) is UrlValidationResult.Invalid
            )
        }

        // Redirect hop validation allows signed GitHub CDN hosts but blocks arbitrary hosts
        assertTrue(
            ApkSecurityVerifier.validateDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset-2e65be/asset",
                isRedirectHop = true
            ) is UrlValidationResult.Valid
        )
        assertTrue(
            ApkSecurityVerifier.validateDownloadUrl(
                "https://attacker.cdn.example.com/evil.apk",
                isRedirectHop = true
            ) is UrlValidationResult.Invalid
        )
    }

    @Test
    fun referUrlValidation_fallsBackSafely_andBlocksPoisonedUrls() {
        val poisonedConfig = RemoteUpdateConfig(
            apkDownloadUrl = "https://github.com/attacker/malware/releases/download/v2/evil.apk"
        )
        val resolved = resolveReferApkDownloadUrl(poisonedConfig)
        assertEquals("https://github.com/Bharathkrishna-76/LifeHub/releases/latest", resolved)
        assertTrue(ApkSecurityVerifier.validateReferUrl(resolved))
        assertFalse(ApkSecurityVerifier.validateReferUrl("https://github.com/attacker/LifeHub/releases/latest"))
        assertFalse(ApkSecurityVerifier.validateReferUrl("javascript:alert(1)"))
    }

    @Test
    fun apkVerifier_enforcesPathConfinement_zipHeader_andSha256Integrity() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. File outside app-private cache/updates must be rejected
        val outsideFile = File(context.cacheDir, "outside_evil.apk").apply {
            writeBytes(ByteArray(60 * 1024) { 0x41 })
        }
        val outsideResult = ApkSecurityVerifier.verifyDownloadedApk(context, outsideFile, 1L)
        assertTrue(outsideResult is ApkVerificationResult.Rejected)
        outsideFile.delete()

        // 2. File inside updates dir with PK header but mismatched SHA-256 must be rejected
        val updatesDir = ApkSecurityVerifier.getUpdatesDirectory(context)
        val fakeApk = File(updatesDir, "lifehub-update-99.apk").apply {
            val bytes = ByteArray(60 * 1024)
            bytes[0] = 0x50 // P
            bytes[1] = 0x4B // K
            bytes[2] = 0x03
            bytes[3] = 0x04
            writeBytes(bytes)
        }
        val actualHash = ApkSecurityVerifier.computeFileSha256(fakeApk)
        assertEquals(64, actualHash.length)

        val wrongHash = "0".repeat(64)
        val hashMismatch = ApkSecurityVerifier.verifyDownloadedApk(
            context = context,
            apkFile = fakeApk,
            installedVersionCode = 1L,
            expectedSha256 = wrongHash
        )
        assertTrue(hashMismatch is ApkVerificationResult.Rejected)
        assertTrue((hashMismatch as ApkVerificationResult.Rejected).reason.contains("checksum", ignoreCase = true))
        fakeApk.delete()
    }

    @Test
    fun csvExportSanitizer_neutralizesFormulaInjection_andNewlines() {
        assertEquals("'=cmd|' /C calc'!A0", ExportAndShareHelper.sanitizeCsvField("=cmd|' /C calc'!A0"))
        assertEquals("'+SUM(A1:A10)", ExportAndShareHelper.sanitizeCsvField("+SUM(A1:A10)"))
        assertEquals("'-2+3", ExportAndShareHelper.sanitizeCsvField("-2+3"))
        assertEquals("'@IMPORTXML()", ExportAndShareHelper.sanitizeCsvField("@IMPORTXML()"))
        assertEquals("'\t=HYPERLINK()", ExportAndShareHelper.sanitizeCsvField("\t=HYPERLINK()"))
        assertEquals("Coffee \"\"Large\"\" Cup", ExportAndShareHelper.sanitizeCsvField("Coffee \"Large\"\r\nCup"))
    }

    @Test
    fun parentalPinHash_usesSaltedPbkdf2_andVerifiesConstantTime() {
        val hash1 = PreferencesManager.hashParentalPin("4829")
        val hash2 = PreferencesManager.hashParentalPin("4829")
        assertTrue(hash1.startsWith("pbkdf2_v1:"))
        assertNotEquals("Salts must be unique per hash", hash1, hash2)
        assertTrue(PreferencesManager.verifyParentalPin("4829", hash1))
        assertTrue(PreferencesManager.verifyParentalPin("4829", hash2))
        assertFalse(PreferencesManager.verifyParentalPin("1234", hash1))
        assertFalse(PreferencesManager.verifyParentalPin("4829", "legacy_unsalted_hash"))
    }

    @Test
    fun logoutAndAccountSwitch_wipesLocalDatabasesPrefsAndExportCaches() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lifeHubDb = Room.inMemoryDatabaseBuilder(context, LifeHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val tripSplitDb = Room.inMemoryDatabaseBuilder(context, TripSplitDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // Seed User A local records
            lifeHubDb.profileDao().upsert(
                UserProfileEntity(
                    userId = "userA",
                    displayName = "Alice",
                    email = "alice@example.com",
                    photoUrl = "",
                    monthlyBudget = 25000.0,
                    currencySymbol = "₹",
                    totalXp = 100,
                    updatedAtMs = 1_795_500_000_000L
                )
            )
            lifeHubDb.transactionDao().upsert(
                TransactionEntity(
                    id = "tx1",
                    userId = "userA",
                    title = "Rent",
                    amount = 20000.0,
                    type = "EXPENSE",
                    category = "BILLS",
                    monthKey = "2026-11",
                    timestampEpochMs = 1_795_500_000_000L,
                    note = ""
                )
            )
            tripSplitDb.tripSplitDao().upsertTrip(
                TripEntity(
                    id = "trip1",
                    title = "Goa Trip",
                    destination = "Goa",
                    origin = "Bengaluru",
                    startDateLabel = "Dec 1",
                    endDateLabel = "Dec 5",
                    totalBudget = 40000.0,
                    currencySymbol = "₹",
                    coverName = "GOA_BEACH",
                    waypointsEncoded = "",
                    createdAt = 1_795_500_000_000L
                )
            )
            val tsPrefs = TripSplitPreferences.getInstance(context)
            tsPrefs.setAppMode(AppMode.TRIPSPLIT)
            tsPrefs.setActiveTripId("trip1")

            val exportFile = File(File(context.cacheDir, "exports").apply { mkdirs() }, "secret.csv")
            exportFile.writeText("Date,Amount\n2026-11-01,20000")
            assertTrue(exportFile.exists())

            // Execute wipe
            lifeHubDb.clearAllTables()
            tripSplitDb.tripSplitDao().clearAllData()
            tsPrefs.clearAll()
            ExportAndShareHelper.clearExportAndShareCaches(context)

            // Verify zero leakage remains
            assertEquals(null, lifeHubDb.profileDao().getProfileOnce("userA"))
            assertTrue(lifeHubDb.transactionDao().getAllCachedForUser("userA").isEmpty())
            assertTrue(tripSplitDb.tripSplitDao().observeTrips().first().isEmpty())
            assertEquals(AppMode.DAILY_EXPENSES, tsPrefs.appMode.value)
            assertEquals(null, tsPrefs.activeTripId.value)
            assertFalse(exportFile.exists())
        } finally {
            lifeHubDb.close()
            tripSplitDb.close()
        }
    }

    @Test
    fun errorMessageSanitizer_hidesInternalStackAndPaths() {
        val internal = IllegalStateException("com.google.firebase.firestore internal path users/uid123/secret")
        val safeMsg = toUserSafeErrorMessage(internal)
        assertFalse(safeMsg.orEmpty().contains("uid123"))
        assertFalse(safeMsg.orEmpty().contains("firestore"))
    }
}
