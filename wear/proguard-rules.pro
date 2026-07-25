# MoghWatch Wear App ProGuard Rules
-keepattributes *Annotation*

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class com.mohgwatch.wear.data.db.** { *; }

# Complications
-keep class com.mohgwatch.wear.service.** { *; }

# Data Layer
-keep class com.google.android.gms.wearable.** { *; }

# Core models
-keep class com.mohgwatch.core.model.** { *; }
