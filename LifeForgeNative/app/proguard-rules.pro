# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all app classes (for debugging - can be optimized later)
-keep class com.lifeforge.app.** { *; }

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Room entities and DAOs
-keep class com.lifeforge.app.data.local.database.entities.** { *; }
-keep class com.lifeforge.app.data.local.database.dao.** { *; }
-keep class com.lifeforge.app.data.remote.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationException
-keep,allowobfuscation,allowoptimization class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keep class kotlinx.serialization.** { *; }

# Hilt / Dagger
-keep class com.lifeforge.app.LifeForgeApp_HiltComponents { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper$1
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper$LayoutInflaterFactoryWrapper$1
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-dontwarn dagger.hilt.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**

# Services & Receivers (Critical for Background/Accessibility)
-keep class com.lifeforge.app.accessibility.AppDetectorService { *; }
-keep class com.lifeforge.app.service.** { *; }
-keep class com.lifeforge.app.ui.screens.overlay.LockOverlayActivity { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
