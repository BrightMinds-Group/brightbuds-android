# ===============================================================
# BrightBuds Project - ProGuard Configuration
# ===============================================================

# Preserve Konfetti animation library classes (used for in-game effects)
-keep class nl.dionsegijn.konfetti.** { *; }

# Preserve Firebase Firestore model classes (loaded via reflection)
-keep class com.example.brightbuds_app.models.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepattributes *Annotation*

# Preserve all Fragment subclasses (required for FragmentManager)
-keep class com.example.brightbuds_app.ui.** { *; }

# Preserve Android Parcelable classes (needed for data transfer)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Preserve classes with @Keep annotation (used by Firebase and Play Services)
-keep @androidx.annotation.Keep class * { *; }

# Preserve Firebase Authentication and Firestore runtime
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontnote com.google.firebase.**

# Preserve Google Play services libraries
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Preserve AndroidX classes (support libraries)
-keep class androidx.** { *; }
-dontwarn androidx.**

# Preserve TextToSpeech and MediaPlayer for in-game audio and narration
-keep class android.speech.tts.** { *; }
-keep class android.media.MediaPlayer { *; }

#  retain logging for debugging crashes
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ===============================================================
# End of ProGuard Rules
# ===============================================================
