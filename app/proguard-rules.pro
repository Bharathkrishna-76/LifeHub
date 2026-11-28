# R8 / ProGuard security and compatibility rules for LifeHub release builds

# Preserve Firebase Firestore deserialization models
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.remote.Firestore** { *; }
-keepclassmembers class com.example.data.remote.Firestore** {
    public <init>(...);
    <fields>;
    <methods>;
}

# Preserve Room entities and generated database implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class com.example.data.local.**Entity { *; }
-keep class com.example.tripsplit.data.**Entity { *; }

# Preserve WorkManager CoroutineWorker subclasses instantiated by reflection
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserve Jetpack Credential Manager & Google Identity Services
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Strip debug/verbose/info Log calls from release bytecode to prevent diagnostic leaks
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Obfuscate source file names in stack traces
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
