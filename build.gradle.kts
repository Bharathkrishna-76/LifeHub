// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

tasks.register("copyDebugApk") {
  dependsOn(":app:assembleDebug")
  doLast {
    val apk = file("app/build/outputs/apk/debug/app-debug.apk")
    if (!apk.exists()) {
      throw GradleException("Debug APK not found at ${apk.absolutePath}")
    }
    apk.copyTo(file("LifeHub-debug.apk"), overwrite = true)
    val publicDir = file("public")
    if (!publicDir.exists()) publicDir.mkdirs()
    apk.copyTo(file("public/LifeHub-debug.apk"), overwrite = true)
    println("Copied APK (${apk.length() / 1024} KB) to LifeHub-debug.apk and public/LifeHub-debug.apk")
  }
}

