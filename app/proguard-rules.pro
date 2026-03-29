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

# General Proguard setup
-dontwarn net.zetetic.**
-keep class net.zetetic.** { *; }

# Strip Logging (Log.d, Log.v, Log.i) in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int println(...);
}
