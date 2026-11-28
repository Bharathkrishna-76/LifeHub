# LifeHub

An all-in-one Android personal finance, productivity, and group trip expense app built with **Kotlin** and **Jetpack Compose**.

[![Latest Release](https://img.shields.io/github/v/release/Bharathkrishna-76/LifeHub?style=flat-square&label=Download%20APK)](https://github.com/Bharathkrishna-76/LifeHub/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://github.com/Bharathkrishna-76/LifeHub/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#tech-stack)

---

## Download & Install

1. Open the [**Latest Release**](https://github.com/Bharathkrishna-76/LifeHub/releases/latest) page on your Android phone.
2. Download the `LifeHub-vX.Y.Z-release.apk` file from **Assets**.
3. Open the downloaded `.apk` file and allow installation from your browser/file manager if prompted.
4. *(Optional)* Verify the download integrity against the attached `SHA256SUMS.txt` file.

Once installed, **LifeHub** automatically checks for new updates via Firebase Remote Config and lets you download and install verified updates directly inside the app.

---

## Features

### Daily Expenses & Personal Finance
- **Expense & Income Tracking**: Log transactions by category, track daily/monthly spending, and manage category budgets.
- **Money Score**: Dynamic financial health score based on your spending, savings, and budgeting habits.
- **Savings Goals**: Create custom or preset savings goals and monitor your progress over time.
- **Monthly Wrapped**: Visual monthly recap of your spending highlights, top categories, and streaks.
- **Export & Share**: Export sanitized CSV statements and shareable summary cards.

### Habits, Planner & Gamification
- **Habit Tracker**: Build daily routines with automatic streak tracking.
- **Planner & Reminders**: Manage tasks and schedule local bill/payment reminders with background notifications.
- **Achievements**: Unlock milestones and earn XP as you stay consistent.

### TripSplit (Group Trip Expenses)
- **Trip Management**: Create trips, add travelers and route waypoints, and track shared expenses.
- **Smart Splitting & Settlements**: Automatically calculate individual balances and simplify who owes whom.

### Account, Sync & Sharing
- **Google Sign-In & Cloud Sync**: Secure sign-in backed by Firebase Authentication and Cloud Firestore with offline-first local Room caching.
- **Responsive Design**: Adapts across compact phones, large phones, foldables, tablets, and landscape layouts with edge-to-edge display support.
- **Refer This App**: Share a direct download link with friends from inside the app.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose & Material Design 3
- **Architecture**: MVVM with Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Navigation Compose
- **Local Storage**: Room Database & Jetpack DataStore Preferences
- **Cloud & Auth**: Firebase Authentication (Google Sign-In via Jetpack Credential Manager), Cloud Firestore, Firebase Remote Config
- **Background Work**: AndroidX WorkManager
- **Testing**: JUnit 4, Robolectric, and Firebase Firestore Rules Emulator test suite

---

## Security & Privacy

- **Per-User Cloud Isolation**: Cloud Firestore security rules enforce strict owner-only access, schema validation, and reject anonymous sessions.
- **Full Local Data Wipe on Sign-Out**: Signing out or switching accounts clears local Room databases, preferences, scheduled reminders, and cached exports.
- **Transport & Build Hardening**: HTTPS-only network security configuration, disabled Android cloud/ADB backup (`allowBackup="false"`), and R8 minification/obfuscation for release builds.
- **Verified In-App Updates**: Update downloads are pinned strictly to this repository's official HTTPS GitHub Releases URLs and verified for package ID, version anti-downgrade, signing certificate match, and SHA-256 digest before installation.

---

## Building From Source

### Prerequisites
- Android Studio (latest stable) or JDK 21 + Android SDK (API 36)
- A configured Firebase project with **Authentication (Google Sign-In)**, **Cloud Firestore**, and **Firebase Remote Config**

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/Bharathkrishna-76/LifeHub.git
   cd LifeHub
   ```
2. Place your Firebase `google-services.json` inside `app/`.
3. Build a debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
4. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## Releases & CI/CD

Releases are automated via GitHub Actions (`.github/workflows/release.yml`), which builds the signed release APK/AAB, generates `SHA256SUMS.txt`, publishes a GitHub Release, and syncs the latest version metadata to Firebase Remote Config.

See these guides for repository maintainer setup:
- [GITHUB_RELEASE.md](GITHUB_RELEASE.md) — Signing secrets and GitHub Actions release workflow
- [UPDATE_RELEASE_GUIDE.md](UPDATE_RELEASE_GUIDE.md) — In-app update delivery and Firebase Remote Config sync
