# ProGuard / R8 Rules for NutriWorks Production Release

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.nutriworks.app.data.db.** { *; }
-dontwarn androidx.room.paging.**

# --- TensorFlow Lite & Task Vision ---
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.task.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.**
-keepattributes *Annotation*

# Keep native JNI methods for TFLite C++ runtime
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- MPAndroidChart ---
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# --- Retrofit & Gson (USDA API fallback) ---
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.nutriworks.app.data.api.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- CameraX ---
-keep class androidx.camera.core.** { *; }
-dontwarn androidx.camera.core.**

# --- Jetpack Navigation & ViewBinding ---
-keepclassmembers class * extends androidx.navigation.NavController { *; }
-keep class com.nutriworks.app.databinding.** { *; }
