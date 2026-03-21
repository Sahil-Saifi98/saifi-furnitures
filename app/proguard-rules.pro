# ── Room Database ─────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }
-keep class com.saififurnitures.app.data.local.** { *; }

# ── Retrofit + Gson (API models) ─────────────────────────────────
-keep class com.saififurnitures.app.data.remote.** { *; }
-keepclassmembers class com.saififurnitures.app.data.remote.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ── Session + Repository ──────────────────────────────────────────
-keep class com.saififurnitures.app.data.session.** { *; }
-keep class com.saififurnitures.app.data.repository.** { *; }

# ── ViewModels ────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ── Navigation ────────────────────────────────────────────────────
-keep class com.saififurnitures.app.navigation.** { *; }

# ── Coil (image loading) ─────────────────────────────────────────
-dontwarn coil.**

# ── Kotlin coroutines ─────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── General Android ───────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity