# Publishing LifeHub via GitHub Releases

This repository includes an automated GitHub Actions workflow (`.github/workflows/release.yml`) that builds the Android app with Gradle 9.3.1 (no `gradlew` required) and publishes installable artifacts directly to **GitHub Releases**.

---

## Option 1: Publish Immediately (Zero Setup)

If you have not configured a production signing keystore yet, the workflow automatically builds an installable APK signed with the project's existing keystore (`debug.keystore.base64`, whose SHA-1 is already registered for Firebase Google Sign-In).

### Publish from GitHub Web UI
1. Push this project to your GitHub repository.
2. Open the **Actions** tab → select **Build & Publish GitHub Release**.
3. Click **Run workflow**, enter a tag (for example `v1.0.0`), keep `build_type` as `auto`, and click **Run workflow**.
4. Once finished, download the APK from the **Releases** section of your repository.

### Publish by Pushing a Git Tag
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Option 2: Production Signed Release APK & AAB (Recommended)

To publish production-signed `release.apk` and `release.aab` files:

### 1. Generate a Release Keystore (one time, keep it private!)
> The alias **must** be `upload` to match `app/build.gradle.kts`.

```bash
keytool -genkey -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode the Keystore to Base64
- **macOS / Linux:**
  ```bash
  base64 -w 0 my-upload-key.jks > keystore.b64
  # On macOS (BSD base64): base64 -i my-upload-key.jks | tr -d '\n' > keystore.b64
  ```
- **Windows (PowerShell):**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Out-File -NoNewline keystore.b64
  ```

### 3. Add GitHub Repository Secrets
Go to your GitHub repository → **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Contents of `keystore.b64` |
| `STORE_PASSWORD` | Your keystore password |
| `KEY_PASSWORD` | Your `upload` key password |

*(Never commit `my-upload-key.jks` or `keystore.b64` to Git — `.gitignore` already blocks `*.jks`.)*

### 4. Register Release SHA-1 in Firebase (for Google Sign-In)
Get your release keystore's SHA-1 fingerprint:
```bash
keytool -list -v -keystore my-upload-key.jks -alias upload
```
Then add that **SHA-1** fingerprint in **Firebase Console → Project Settings → Your Apps (`com.aistudio.lifehub.mzkpqr`) → Add fingerprint** so Google Sign-In works in release builds.

### 5. Trigger a Release
Push a new version tag (e.g., `git tag v1.0.1 && git push origin v1.0.1`) or run **Build & Publish GitHub Release** from the **Actions** tab. The workflow will automatically build and attach:
- `LifeHub-v1.0.1-release.apk`
- `LifeHub-v1.0.1-release.aab`
- `SHA256SUMS.txt`
