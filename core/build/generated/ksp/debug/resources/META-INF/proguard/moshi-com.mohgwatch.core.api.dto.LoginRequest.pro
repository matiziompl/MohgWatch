-if class com.mohgwatch.core.api.dto.LoginRequest
-keepnames class com.mohgwatch.core.api.dto.LoginRequest
-if class com.mohgwatch.core.api.dto.LoginRequest
-keep class com.mohgwatch.core.api.dto.LoginRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
