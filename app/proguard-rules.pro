# =============================================================================
# RoadWatch — R8/ProGuard rules
# Applied only to release builds (minifyEnabled true). Goal: shrink + obfuscate
# without breaking reflection-based libraries (Gson, Retrofit, Room, Glide,
# Firebase, Maps Utils).
# =============================================================================

# Keep line numbers so production crashes are still readable in stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all annotations (Room, Gson @SerializedName, Retrofit, etc).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Strip every Log.v / Log.d call from release. Higher levels (i / w / e) survive
# so production issues remain debuggable from logcat or Crashlytics.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

# -----------------------------------------------------------------------------
# Application classes — keep entry points and Application subclass intact
# -----------------------------------------------------------------------------
-keep class com.roadwatch.mobile.RoadWatchApplication { *; }
-keep class com.roadwatch.mobile.MainActivity { *; }

# Keep BuildConfig fields readable so reflection-based libs that peek at them
# don't break.
-keep class com.roadwatch.mobile.BuildConfig { *; }

# -----------------------------------------------------------------------------
# DTOs / Room entities — Gson + Room rely on field names at runtime.
# Without these the fields would be renamed and JSON parsing would break.
# -----------------------------------------------------------------------------
-keep class com.roadwatch.mobile.network.dto.** { *; }
-keep class com.roadwatch.mobile.data.** { *; }

# -----------------------------------------------------------------------------
# Retrofit + OkHttp
# -----------------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# -----------------------------------------------------------------------------
# Gson — generic types and TypeAdapter classes need to survive.
# -----------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# -----------------------------------------------------------------------------
# Room — generated _Impl classes are referenced by name.
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# -----------------------------------------------------------------------------
# Glide
# -----------------------------------------------------------------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# -----------------------------------------------------------------------------
# Firebase Cloud Messaging
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.roadwatch.mobile.notifications.** { *; }
-dontwarn com.google.firebase.**

# -----------------------------------------------------------------------------
# Google Maps + Maps Utils (clustering)
# -----------------------------------------------------------------------------
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.android.gms.**

# -----------------------------------------------------------------------------
# CameraX
# -----------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# -----------------------------------------------------------------------------
# WorkManager — workers are instantiated by class name.
# -----------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# -----------------------------------------------------------------------------
# Kotlin metadata (LoginActivity is Kotlin)
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# -----------------------------------------------------------------------------
# Parcelables created via reflection
# -----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
