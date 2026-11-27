# Centralized APK Update & Release Guide

This guide explains how to publish a new version of **Daily Expenses Tracker (LifeHub)** via **GitHub Releases** and roll it out automatically to all existing users via **Firebase Remote Config** — without Google Play Store and without erasing user data.

---

## Automatic Firebase Remote Config Sync (One-Time Setup)

You do **not** need to manually edit Firebase Remote Config on every release. Once you add one GitHub secret (`FIREBASE_SERVICE_ACCOUNT`), `.github/workflows/release.yml` automatically updates and publishes all 6 Remote Config parameters right after uploading each new APK to GitHub Releases.

### Why you won't see a project named "LifeHub" in Firebase Console
Google AI Studio created and linked this app to your Google Cloud / Firebase project ID **`gen-lang-client-0916654373`** (often displayed in Firebase Console as **`Gemini API`** or **`gen-lang-client-0916654373`**). Inside that project, your app appears as Android package **`com.aistudio.lifehub.mzkpqr`**.

- **Direct Firebase Remote Config link:** https://console.firebase.google.com/project/gen-lang-client-0916654373/config
- **Direct Service Accounts link:** https://console.firebase.google.com/project/gen-lang-client-0916654373/settings/serviceaccounts/adminsdk

*(Make sure Firebase Console is signed in with **`echo.cloud76@gmail.com`** in the top-right avatar menu.)*

### One-Time Setup
1. Open the **Direct Service Accounts link** above (or go to [Firebase Console](https://console.firebase.google.com/) → project **`gen-lang-client-0916654373`** / **Gemini API** → **Project Settings (gear icon) → Service accounts**).
2. Click **Generate new private key** and download the `.json` file (keep it private — never commit it to Git).
3. Open your GitHub repository → **Settings → Secrets and variables → Actions → New repository secret**:
   - **Name:** `FIREBASE_SERVICE_ACCOUNT`
   - **Secret:** Paste the entire contents of the downloaded service account `.json` file (or its base64 string).
4. Done! From now on, every time you push a version tag (e.g., `git tag v1.0.2 && git push origin v1.0.2`) or run **Actions → Build & Publish GitHub Release**, GitHub Actions automatically:
   - Builds the signed APK with `versionName = 1.0.2` and `versionCode = 10002`
   - Publishes the GitHub Release and attaches the `.apk`
   - Automatically updates and publishes all 6 Firebase Remote Config parameters (`latest_version_code`, `latest_version_name`, `apk_download_url`, `release_notes`, `update_enabled = true`, and optional `minimum_version_code`)

*(You can also initialize or deploy the parameter template manually via Firebase CLI using `firebase deploy --only remoteconfig`, which uses `remoteconfig.template.json`.)*

---

## Firebase Remote Config Parameters

The app reads these **6 exact parameters** from **Firebase Console → Run → Remote Config** (project `gen-lang-client-0916654373`):

| Parameter Key | Type | Example Value | Purpose |
|---|---|---|---|
| `latest_version_code` | Number | `10002` | Integer build number (`versionCode`) of the newest APK. Must be strictly greater than the installed build to trigger an update. |
| `latest_version_name` | String | `1.0.2` | Human-readable version displayed to users (e.g. `v1.0.2`). |
| `apk_download_url` | String | `https://github.com/<owner>/<repo>/releases/download/v1.0.2/LifeHub-v1.0.2-release.apk` | Direct HTTPS `.apk` asset link from GitHub Releases. Only trusted GitHub Releases HTTPS URLs are accepted by the app. |
| `release_notes` | String | `• Faster expense sync\n• Fixed dark mode contrast` | Changelog shown inside the update dialog/screen. |
| `update_enabled` | Boolean | `true` | Master kill switch. Set to `false` anytime to pause a rollout immediately. |
| `minimum_version_code` | Number | `1` | Users with `installedVersionCode < minimum_version_code` see a mandatory update screen. Keep at `1` for optional updates. |

> **Spark Free-Tier Friendly:** Firebase Remote Config is free on the Spark plan, cached locally in DataStore, fetched at most once per hour (4 hours on foreground resume), and uses **zero** Firestore reads.

---

## Step-by-Step Release Workflow (Example: `1.0.1` → `1.0.2`)

Assume users currently have **version `1.0.1` (`versionCode = 10001`)** installed, and you are releasing **version `1.0.2` (`versionCode = 10002`)**.

### 1. Modify Your Existing Project in Google AI Studio
Make your feature changes or bug fixes in Google AI Studio and verify the app compiles cleanly.

### 2. Increase `versionCode` and Update `versionName`
- **If building via GitHub Actions (`.github/workflows/release.yml`):**
  Tagging `v1.0.2` automatically computes `VERSION_NAME=1.0.2` and `VERSION_CODE=10002` (`1*10000 + 0*100 + 2`).
- **If building locally or manually in AI Studio:**
  Update the fallback defaults in `app/build.gradle.kts` (`defaultConfig`):
  ```kotlin
  versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 10002
  versionName = providers.environmentVariable("VERSION_NAME").orNull?.takeIf { it.isNotBlank() } ?: "1.0.2"
  ```
  > **CRITICAL:** Never change `applicationId` (`com.aistudio.lifehub.mzkpqr`) or the signing keystore! Keeping both identical guarantees Android upgrades in place and preserves all expense records, Room databases, settings, and signed-in Firebase sessions.

### 3. Build a New APK
- Push your code to GitHub and push tag `v1.0.2`:
  ```bash
  git tag v1.0.2
  git push origin v1.0.2
  ```
  *(Or run **Actions → Build & Publish GitHub Release → Run workflow** with tag `v1.0.2`.)*
- Or build locally with `gradle :app:assembleRelease` (signed with the same keystore used for `1.0.1`).

### 4. Upload the APK to GitHub Releases
- If you used the GitHub Actions workflow, the release **`v1.0.2`** and APK asset (`LifeHub-v1.0.2-release.apk` or `LifeHub-v1.0.2-debug.apk`) are created and uploaded automatically.
- If uploading manually: open your GitHub repo → **Releases → Draft a new release** → create tag `v1.0.2` → attach your signed `.apk` → click **Publish release**.

### 5. Copy the Direct APK Download URL
1. Open the published **`v1.0.2`** release page on GitHub.
2. Under **Assets**, right-click the `.apk` file → **Copy link address**.
3. Verify the URL looks like:
   ```text
   https://github.com/<your-username>/<your-repo>/releases/download/v1.0.2/LifeHub-v1.0.2-release.apk
   ```
   *(Must start with `https://github.com/`, contain `/releases/download/`, and end with `.apk`.)*

### 6. Open Firebase Remote Config
Go to [Firebase Console](https://console.firebase.google.com/) → select your project (`gen-lang-client-0916654373`) → left sidebar **Run → Remote Config**.

### 7. Update `latest_version_code`
Set `latest_version_code` to `10002` (matching the new APK's `versionCode`).

### 8. Update `latest_version_name`
Set `latest_version_name` to `1.0.2`.

### 9. Update `apk_download_url`
Paste the exact GitHub Releases direct `.apk` download URL from Step 5.

### 10. Update `release_notes`
Write concise user-friendly release notes, for example:
```text
• Improved daily budget summary speed
• Fixed dark mode card contrast
• Enhanced offline expense sync reliability
```

### 11. Change `minimum_version_code` Only When Necessary
- **Optional update (default):** Leave `minimum_version_code` at `1` (or `<= 10001`). Users on `1.0.1` can tap **Download Update** or **Remind Me Later**.
- **Mandatory update (critical security/data fix only):** Set `minimum_version_code` to `10002`. Users on `1.0.1` (`10001 < 10002`) will see the full-screen **Update Required** screen until they install `1.0.2`.
- Also make sure `update_enabled` is set to **`true`**.

### 12. Publish the Remote Config Changes
Click **Publish changes** in the top-right corner of Firebase Remote Config and confirm.

### 13. How Existing Users Detect and Install `1.0.2`
1. **Automatic Detection:** When a user on `1.0.1` launches the app (or returns to it after 4+ hours), the app fetches Firebase Remote Config in the background without blocking the UI. Users can also tap **Profile & Settings → Check for Updates** anytime.
2. **Update Prompt:** Because `10002 > 10001` and `update_enabled == true`, the user sees the Material 3 update prompt showing **Daily Expenses Tracker**, `v1.0.1 → v1.0.2`, and your release notes.
3. **Secure Download & Verification:** When the user taps **Download Update** (or **Update Now**), the app streams the APK over HTTPS (validating every redirect hop), saves it into app-private cache (`cacheDir/updates/`), and verifies:
   - Complete, uncorrupted ZIP/APK file
   - Exact package name (`com.aistudio.lifehub.mzkpqr`)
   - Newer `versionCode` (rejects downgrades)
   - Matching SHA-256 signing certificate (rejects mismatched/tampered APKs)
4. **Official Android Installation:** If Android 8+ requires permission, the app guides the user to **Allow from this source** and automatically resumes when they return. Android's system Package Installer opens via `FileProvider` and upgrades the app in place—preserving all expense records, Room/DataStore caches, and the user's Firebase sign-in session.
