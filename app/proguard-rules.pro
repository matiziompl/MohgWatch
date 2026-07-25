# MoghWatch Phone App ProGuard Rules
-keepattributes *Annotation*

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Compose
-dontwarn androidx.compose.**

# Moshi / Retrofit (from core)
-keep class com.mohgwatch.core.api.dto.** { *; }
-keep class com.mohgwatch.core.model.** { *; }
