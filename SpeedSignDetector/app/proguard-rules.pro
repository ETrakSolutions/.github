# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }

# Keep CameraX classes
-keep class androidx.camera.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.etraksolutions.speedsign.domain.model.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Hide original source file names
-renamesourcefileattribute SourceFile

# Optimization settings
-optimizationpasses 5
-allowaccessmodification

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
