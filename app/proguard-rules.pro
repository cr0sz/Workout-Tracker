# Jetpack Compose
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keep class androidx.compose.material.icons.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.Entity
-keep class com.workouttracker.data.model.** { *; }

# Firebase
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class com.google.firebase.** {
  @com.google.firebase.database.PropertyName <fields>;
  @com.google.firebase.database.PropertyName <methods>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services & Auth
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.HandlerContext {
    public <init>(android.os.Handler, java.lang.String, boolean);
}

# ViewModel & LiveData
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**

# SupportFactory
-keep class net.sqlcipher.database.SupportFactory { *; }

# Strip Logging (Log.d, Log.v, Log.i) in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int println(...);
}
