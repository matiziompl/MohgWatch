# MoghWatch Phone App ProGuard Rules
-keepattributes *Annotation*

# Compose
-dontwarn androidx.compose.**

# Moshi / Retrofit (from core)
-keep class com.mohgwatch.core.api.dto.** { *; }
-keep class com.mohgwatch.core.model.** { *; }
